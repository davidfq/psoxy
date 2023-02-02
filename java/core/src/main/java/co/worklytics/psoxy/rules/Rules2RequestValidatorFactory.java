package co.worklytics.psoxy.rules;

import dagger.assisted.AssistedFactory;

@AssistedFactory
public interface Rules2RequestValidatorFactory {

    Rules2RequestValidator create(Rules2 rules2);
}
