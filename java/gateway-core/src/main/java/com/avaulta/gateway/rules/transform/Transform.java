package com.avaulta.gateway.rules.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * general points:
 *  - transforms should be preserve type/format; as our goal is that most applications can be
 *    ignorant of dealing with pseudonyms/tokens instead of real values
 *
 *
 * q: can we do this as an interface? avoid re-use through inheritance ... simpler to migrate to
 * languages that lack inheritance (Go) as this is more modern paradigm.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")
@JsonSubTypes({
    // NOTE: no more Redact, as we're getting this from Schema logic (implicitly, anything not in
    //       schema is dropped)
    // NOTE: also no RedactRegexMatches; with "default deny" paradigm, we have to accomplish this
    //  by using FilterTokenByRegex and explicitly matching what we want to include
    //@JsonSubTypes.Type(value = Transform.RedactRegexMatches.class, name = "redactRegexMatches"),
    @JsonSubTypes.Type(value = Pseudonymize.class, name = "pseudonymize"),
    @JsonSubTypes.Type(value = PseudonymizeEmailHeader.class, name = "pseudonymizeEmailHeader"),
    @JsonSubTypes.Type(value = FilterTokenByRegex.class, name = "filterTokenByRegex"),
    @JsonSubTypes.Type(value = Tokenize.class, name = "tokenize"),
})
@SuperBuilder(toBuilder = true)
@AllArgsConstructor //for builder
@NoArgsConstructor //for Jackson
@Getter
@EqualsAndHashCode(callSuper = false)
public abstract class Transform {

    //NOTE: this is filled for JSON, but for YAML a YAML-specific type syntax is used:
    // !<pseudonymize>
    // Jackson YAML can still *read* yaml-encoded transform with `method: "pseudonymize"`
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String method;


    //TODO: can we implement abstract portion of this somehow??
    public abstract Transform clone();

}
