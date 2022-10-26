package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.TinyK8sClient;

import java.util.function.Consumer;

public class AdmissionControllerBuilderImpl implements AdmissionControllerBuilder {
    private final AdmissionControllerManager manager;
    private Consumer<Consumer<TinyK8sClient>> onContainerStarted;

    public AdmissionControllerBuilderImpl(
            final AdmissionControllerManager manager,
            final Consumer<Consumer<TinyK8sClient>> onContainerStarted
    ) {
        this.manager = manager;
        this.onContainerStarted = onContainerStarted;
    }

    @Override
    public ValidatingAdmissionControllerBuilder validating() {
        return new ValidatingAdmissionControllerBuilderImpl(manager, onContainerStarted);
    }

    @Override
    public MutatingAdmissionControllerBuilder mutating() {
        return new MutatingAdmissionControllerBuilderImpl(manager, onContainerStarted);
    }
}
