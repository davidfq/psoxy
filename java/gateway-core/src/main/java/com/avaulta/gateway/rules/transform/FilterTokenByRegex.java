package com.avaulta.gateway.rules.transform;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * transform to tokenize String field by delimiter (if provided), then return any matches against
 * filter regex
 * <p>
 * q: is this really TWO transforms? (split, then apply transform to split??)
 */
@NoArgsConstructor //for jackson
@SuperBuilder(toBuilder = true)
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class FilterTokenByRegex extends Transform {

    /**
     * token delimiter, if any (if null, token is the whole string)
     */
    @Builder.Default
    String delimiter = "\\s+";

    /**
     * redact content EXCEPT tokens matching at least one of these regexes
     */
    @Singular
    List<String> filters;

    public com.avaulta.gateway.rules.transform.FilterTokenByRegex clone() {
        return this.toBuilder()
            .clearFilters()
            .filters(new ArrayList<>(this.filters))
            .build();
    }
}
