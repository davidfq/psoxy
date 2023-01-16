package com.avaulta.gateway.rules.transform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor //for builder
@NoArgsConstructor //for Jackson
@Getter
public class Tokenize extends Transform {

    /**
     * if provided, only group within matched by this regex will be tokenized
     * <p>
     * example usage: .regex("^https://graph.microsoft.com/(.*)$") will tokenize the path
     * of a MSFT graph URL (prev/next links in paged endpoints), which may be useful if path
     * might contain PII or something like that
     * <p>
     * HUGE CAVEAT: as of Aug 2022, reversing encapsulated tokens BACK to their original values
     * will work if and only if token is bounded by non-base64-urlencoded character
     */
    String regex;

    //NOTE: always format to URL-safe
    public com.avaulta.gateway.rules.transform.Tokenize clone() {
        return this.toBuilder()
            .build();
    }
}
