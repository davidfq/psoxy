package com.avaulta.gateway.rules.transform;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor //for jackson
@SuperBuilder(toBuilder = true)
@Getter
public class PseudonymizeEmailHeader extends Transform {

    public com.avaulta.gateway.rules.transform.PseudonymizeEmailHeader clone() {
        return this.toBuilder()
            .build();
    }
}
