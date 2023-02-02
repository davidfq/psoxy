package co.worklytics.psoxy;

import com.avaulta.gateway.pseudonyms.PseudonymImplementation;
import co.worklytics.psoxy.rules.RuleSet;
import lombok.*;

import java.io.Serializable;
import java.net.URL;

public interface Sanitizer {

    /**
     * immutable sanitizer options
     */
    @With
    @Builder
    @Value
    class ConfigurationOptions implements Serializable {

        private static final long serialVersionUID = 4L;

        /**
         * salt used to generate pseudonyms
         *
         * q: split this out? it's per-customer, not per-source API (although it *could* be the
         * later, if you don't need to match with it)
         */
        String pseudonymizationSalt;

        /**
         * scope to use where logic + rules don't imply a match
         */
        @Deprecated
        String defaultScopeId;

        RuleSet rules;

        @Builder.Default
        PseudonymImplementation pseudonymImplementation = PseudonymImplementation.DEFAULT;

    }

    /**
     * sanitize jsonResponse received from url, according any options set on Sanitizer
     */
    String sanitize(String httpMethod, URL url, String jsonResponse);



    /**
     * @param identifier to pseudonymize
     * @return identifier as a PseudonymizedIdentity
     */
    PseudonymizedIdentity pseudonymize(String identifier);

    /**
     * @param identifier to pseudonymize
     * @return identifier as a PseudonymizedIdentity
     */
    PseudonymizedIdentity pseudonymize(Number identifier);

    ConfigurationOptions getConfigurationOptions();
}
