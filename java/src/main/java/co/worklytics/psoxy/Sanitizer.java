package co.worklytics.psoxy;

import com.google.api.client.http.GenericUrl;
import lombok.*;

import java.io.Serializable;

public interface Sanitizer {


    @With
    @Builder
    @Value
    class Options implements Serializable {

        /**
         * salt used to generate pseudonyms
         *
         * q: split this out? it's per-customer, not per-source API (although it *could* be the
         * later, if you don't need to match with it)
         */
        String pseudonymizationSalt;


        //q: add regexes to whitelist endpoints that we actually use??
        Rules rules;
    }

    /**
     * sanitize jsonResponse received from url, according any options set on Sanitizer
     */
    String sanitize(GenericUrl url, String jsonResponse);
}
