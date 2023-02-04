package co.worklytics.psoxy;

import co.worklytics.psoxy.gateway.ConfigService;
import co.worklytics.psoxy.gateway.ProxyConfigProperty;
import co.worklytics.psoxy.impl.SanitizerImpl;

import com.avaulta.gateway.pseudonyms.PseudonymImplementation;
import com.avaulta.gateway.rules.RuleSet;
import dagger.assisted.AssistedFactory;

@AssistedFactory
public interface SanitizerFactory {

    SanitizerImpl create(Sanitizer.ConfigurationOptions configurationOptions);

    //q: right place? mapping config+rules --> Sanitizer.Options isn't implementation-specific
    default Sanitizer.ConfigurationOptions buildOptions(ConfigService config, RuleSet rules) {
        Sanitizer.ConfigurationOptions.ConfigurationOptionsBuilder builder = Sanitizer.ConfigurationOptions.builder()
            .pseudonymizationSalt(config.getConfigPropertyAsOptional(ProxyConfigProperty.PSOXY_SALT)
                .orElseThrow(() -> new Error("Must configure value for SALT to generate pseudonyms")))
            .defaultScopeId(config.getConfigPropertyAsOptional(ProxyConfigProperty.IDENTIFIER_SCOPE_ID)
                .orElse(config.getConfigPropertyAsOptional(ProxyConfigProperty.SOURCE).orElse(null)));

        config.getConfigPropertyAsOptional(ProxyConfigProperty.PSEUDONYM_IMPLEMENTATION)
            .map(PseudonymImplementation::parseConfigPropertyValue)
                .ifPresent(builder::pseudonymImplementation);

        builder.rules(rules);

        return builder.build();
    }

}
