package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;

public class CreateFluent<T> {
    public final CreateSecretFluent<T> secret;

    public CreateFluent(final BaseSidecarContainer.ExecInContainer exec) {
        this.secret = new CreateSecretFluent<>(exec);
    }
}
