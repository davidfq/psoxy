package com.avaulta.gateway.rules.transform;

import com.avaulta.gateway.pseudonyms.PseudonymEncoder;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor //for builder
@NoArgsConstructor //for Jackson
@Getter
public class Pseudonymize extends Transform {

    /**
     * use if still need original, but also want its pseudonym to be able to match against
     * pseudonymized fields
     * <p>
     * use case: group mailing lists; if they're attendees to an event, the email in that
     * context will be pseudonymized; so when we pull list of groups, we need pseudonyms to
     * match against those, but can also get the original for use in UX/reports, as it's not PII
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @Builder.Default
    Boolean includeOriginal = false;

    /**
     * whether to include reversible form of pseudonymized value in output
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @Builder.Default
    Boolean includeReversible = false;

    /**
     * how to encode to the resulting pseudonym
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) //doesn't work for enums ...
    @Builder.Default
    PseudonymEncoder.Implementations encoding = PseudonymEncoder.Implementations.JSON;

    public com.avaulta.gateway.rules.transform.Pseudonymize clone() {
        return this.toBuilder()
            .build();
    }
}
