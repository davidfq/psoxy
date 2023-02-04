package com.avaulta.gateway.rules;

import com.avaulta.gateway.rules.api.RESTRules;
import dagger.assisted.AssistedFactory;

@AssistedFactory
public interface RESTRulesRequestValidatorFactory {

    RESTRulesRequestValidator create(RESTRules restRules);

}
