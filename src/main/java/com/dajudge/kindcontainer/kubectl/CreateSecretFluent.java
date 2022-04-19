package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;

public class CreateSecretFluent<T> {
    public final CreateSecretDockerRegistryFluent<T> dockerRegistry;

    public CreateSecretFluent(final BaseSidecarContainer.ExecInContainer exec) {
        dockerRegistry = new CreateSecretDockerRegistryFluent<>(exec);
    }
}
