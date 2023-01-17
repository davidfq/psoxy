package co.worklytics.psoxy.gateway.impl.oauth;

import co.worklytics.psoxy.gateway.ConfigService;
import co.worklytics.psoxy.gateway.RequiresConfiguration;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.UrlEncodedContent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * build payload for canonical OAuth access token request authenticated by a long-lived refresh
 * token + client secret
 *
 * (apologies for awkward name, but to be clear: this is using one kind of token to request another,
 *  so it's a token request, using a token)
 *
 * @see OAuthAccessTokenSourceAuthStrategy
 *
 */
@NoArgsConstructor(onConstructor_ = @Inject)
public class RefreshTokenTokenRequestBuilder
        implements OAuthRefreshTokenSourceAuthStrategy.TokenRequestBuilder, RequiresConfiguration {

    @Inject
    ConfigService config;

    @Getter(onMethod_ = @Override)
    private final String grantType = "refresh_token";

    public enum ConfigProperty implements ConfigService.ConfigProperty {
        REFRESH_TOKEN, //NOTE: you should configure this as a secret in Secret Manager
        CLIENT_SECRET, //NOTE: you should configure this as a secret in Secret Manager
    }


    public HttpContent buildPayload() {

        Map<String, String> data = new HashMap<>();

        data.put("grant_type", getGrantType());
        data.put("refresh_token", config.getConfigPropertyOrError(ConfigProperty.REFRESH_TOKEN));
        data.put("client_id", config.getConfigPropertyOrError(OAuthRefreshTokenSourceAuthStrategy.ConfigProperty.CLIENT_ID));
        data.put("client_secret", config.getConfigPropertyOrError(ConfigProperty.CLIENT_SECRET));

        return new UrlEncodedContent(data);

    }

    @Override
    public Set<ConfigService.ConfigProperty> getRequiredConfigProperties() {
        return Set.of(
            OAuthRefreshTokenSourceAuthStrategy.ConfigProperty.CLIENT_ID,
            ConfigProperty.CLIENT_SECRET,
            ConfigProperty.REFRESH_TOKEN
            );
    }

    @Override
    public Set<ConfigService.ConfigProperty> getAllConfigProperties() {
        return Set.of(ClientCredentialsGrantTokenRequestBuilder.ConfigProperty.values());
    }
}