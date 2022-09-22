package co.worklytics.psoxy.gateway.impl.oauth;

import co.worklytics.psoxy.gateway.ConfigService;
import co.worklytics.psoxy.gateway.RequiresConfiguration;
import co.worklytics.psoxy.gateway.SourceAuthStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2CredentialsWithRefresh;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * source auth strategy to authenticate using a short-lived OAuth 2.0 access token which must be
 * periodically refreshed.
 *   Options for refresh method are configured by
 * <p>
 * A new access token will be retrieved for every psoxy instance that spins up; as well as when the
 * current one expires.  We'll endeavor to minimize the number of token requests by sharing this
 * states across API requests
 * <p>
 * If the source API you're connecting to offers long-lived access tokens (or does not offer refresh
 * tokens), you may opt for the access-token only strategy:
 * @see OAuthAccessTokenSourceAuthStrategy
 *
 */
@Log
@NoArgsConstructor(onConstructor_ = @Inject)
public class OAuthRefreshTokenSourceAuthStrategy implements SourceAuthStrategy {

    @Getter
    private final String configIdentifier = "oauth2_refresh_token";

    /**
     * default access token expiration to assume, if 'expires_in' value is omitted from response
     * (which is allowed under OAuth 2.0 spec)
     */
    public static final Duration DEFAULT_ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);


    //q: should we put these as config properties? creates potential for inconsistent configs
    // eg, orphaned config properties for SourceAuthStrategy not in use; missing config properties
    //  expected by this
    public enum ConfigProperty implements ConfigService.ConfigProperty {
        REFRESH_ENDPOINT,
        CLIENT_ID,
        GRANT_TYPE,
    }

    @Inject OAuth2CredentialsWithRefresh.OAuth2RefreshHandler refreshHandler;
    @Inject RedissonClient redissonClient;
    @Inject
    ConfigService config;

    @Override
    public Set<ConfigService.ConfigProperty> getRequiredConfigProperties() {
        Stream<ConfigService.ConfigProperty> propertyStream = Arrays.stream(ConfigProperty.values());
        if (refreshHandler instanceof RequiresConfiguration) {
            propertyStream = Stream.concat(propertyStream,
                ((RequiresConfiguration) refreshHandler).getRequiredConfigProperties().stream());
        }
        return propertyStream.collect(Collectors.toSet());
    }


    @Override
    public Credentials getCredentials(Optional<String> userToImpersonate) {

        // client id should be unique
        String clientId = config.getConfigPropertyOrError(ConfigProperty.CLIENT_ID);
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("oauth-refresh-lock-" + clientId);

        RLock readLock = readWriteLock.readLock();
        try {
            readLock.tryLock(30, 10, TimeUnit.SECONDS);
            RBucket<AccessToken> bucket = redissonClient.getBucket("oauth-refresh-token-" + clientId);
            AccessToken accessToken = bucket.get();
            return OAuth2CredentialsWithRefresh.newBuilder()
                .setAccessToken(accessToken)
                .setRefreshHandler(refreshHandler)
                .build();
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Read lock was lost, check Redis cluster", e);
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    public interface TokenRequestPayloadBuilder {

        String getGrantType();

        HttpContent buildPayload();

        /**
         * Add any headers to the request if needed, by default, does nothing
         * @param httpHeaders the request headers to modify
         */
        default void addHeaders(HttpHeaders httpHeaders) {}
    }

    @NoArgsConstructor(onConstructor_ = @Inject)
    @Log
    public static class TokenRefreshHandlerImpl implements OAuth2CredentialsWithRefresh.OAuth2RefreshHandler,
            RequiresConfiguration {

        @Inject
        ConfigService config;
        @Inject
        ObjectMapper objectMapper;
        @Inject
        HttpRequestFactory httpRequestFactory;
        @Inject
        OAuthRefreshTokenSourceAuthStrategy.TokenRequestPayloadBuilder payloadBuilder;
        @Inject
        RedissonClient redissonClient;

        @VisibleForTesting
        protected final Duration TOKEN_REFRESH_THRESHOLD = Duration.ofMinutes(1L);

        private AccessToken currentToken = null;

        /**
         * implements canonical oauth flow to exchange refreshToken for accessToken
         *
         * @return the resulting AccessToken
         * @throws IOException if anything went wrong;
         * @throws Error       if config values missing
         */
        @Override
        public AccessToken refreshAccessToken() throws IOException {
            if (isCurrentTokenValid(this.currentToken, Instant.now())) {
                return this.currentToken;
            }
            // client id should be unique
            String clientId = config.getConfigPropertyOrError(ConfigProperty.CLIENT_ID);
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("oauth-refresh-lock-" + clientId);
            RLock writeLock = readWriteLock.writeLock();

            try {
                writeLock.tryLock(30, 10, TimeUnit.SECONDS);

                RBucket<AccessToken> bucket = redissonClient.getBucket("oauth-refresh-token-" + clientId);
                String refreshEndpoint =
                    config.getConfigPropertyOrError(OAuthRefreshTokenSourceAuthStrategy.ConfigProperty.REFRESH_ENDPOINT);

                HttpRequest tokenRequest = httpRequestFactory
                    .buildPostRequest(new GenericUrl(refreshEndpoint), payloadBuilder.buildPayload());

                // modify any header if needed
                payloadBuilder.addHeaders(tokenRequest.getHeaders());

                HttpResponse response = tokenRequest.execute();

                CanonicalOAuthAccessTokenResponseDto tokenResponse =
                    objectMapper.readerFor(CanonicalOAuthAccessTokenResponseDto.class)
                        .readValue(response.getContent());

                //TODO: this is obviously not great; if we're going to support refresh token rotation,
                // need to have some way to control the logic based on grant type
                config.getConfigPropertyAsOptional(RefreshTokenPayloadBuilder.ConfigProperty.REFRESH_TOKEN)
                    .ifPresent(currentRefreshToken -> {
                        if (!Objects.equals(currentRefreshToken, tokenResponse.getRefreshToken())) {
                            //TODO: update refreshToken (some source APIs do this; TBC whether ones currently
                            // in scope for psoxy use do)
                            //q: write to secret? (most correct ...)
                            //q: write to file system?
                            log.severe("Source API rotated refreshToken, which is currently NOT supported by psoxy");
                        }
                    });

                Integer expiresIn = tokenResponse.getExpiresIn();
                this.currentToken = asAccessToken(tokenResponse);

                bucket.set(this.currentToken, expiresIn, TimeUnit.SECONDS);

                this.currentToken = asAccessToken(tokenResponse);
                return this.currentToken;

            } catch (InterruptedException e) {
                log.log(Level.SEVERE, "Write lock was lost, check Redis cluster", e);
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
            }
        }


        AccessToken asAccessToken(CanonicalOAuthAccessTokenResponseDto tokenResponse) {
            //expires_in is RECOMMENDED, not REQUIRED in response; if omitted, we're supposed to
            // assume a default value for service OR retrieve via some other means
            Integer expiresIn = Optional.ofNullable(tokenResponse.getExpiresIn())
                .orElse((int) DEFAULT_ACCESS_TOKEN_EXPIRATION.toSeconds());
            return new AccessToken(tokenResponse.getAccessToken(),
                Date.from(Instant.now().plusSeconds(expiresIn)));
        }

        @VisibleForTesting
        protected boolean isCurrentTokenValid(AccessToken accessToken, Instant now) {
            if (accessToken == null) {
                return false;
            }
            Instant expiresAt = accessToken.getExpirationTime().toInstant();
            Instant minimumValid = expiresAt.minus(TOKEN_REFRESH_THRESHOLD);
            return now.isBefore(minimumValid);
        }

        @Override
        public Set<ConfigService.ConfigProperty> getRequiredConfigProperties() {
            Stream<ConfigService.ConfigProperty> propertyStream = Arrays.stream(ConfigProperty.values());
            if (payloadBuilder instanceof RequiresConfiguration) {
                propertyStream = Stream.concat(propertyStream,
                    ((RequiresConfiguration) payloadBuilder).getRequiredConfigProperties().stream());
            }
            return propertyStream.collect(Collectors.toSet());
        }
    }


}
