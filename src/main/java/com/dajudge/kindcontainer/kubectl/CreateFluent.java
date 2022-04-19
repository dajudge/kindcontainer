package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;

public class CreateFluent<P> {
    public final CreateSecretFluent<P> secret;

    public CreateFluent(final BaseSidecarContainer.ExecInContainer exec, final P parent) {
        this.secret = new CreateSecretFluent<>(exec, parent);
    }
}
