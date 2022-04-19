package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;

public class CreateSecretFluent<P> {
    public final CreateSecretDockerRegistryFluent<P> dockerRegistry;

    public CreateSecretFluent(final BaseSidecarContainer.ExecInContainer exec, final P parent) {
        dockerRegistry = new CreateSecretDockerRegistryFluent<>(exec, parent);
    }
}
