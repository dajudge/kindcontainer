package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.testcontainers.utility.DockerImageName;

public class KubectlContainer<T extends KubectlContainer<T, C>, C> extends BaseSidecarContainer<T> {

    public final ApplyFluent<KubectlContainer<T, C>, C> apply;
    public final DeleteFluent<KubectlContainer<T, C>> delete;
    public final WaitFluent<KubectlContainer<T, C>> wait;
    public final CreateFluent<KubectlContainer<T, C>> create;
    public final LabelFluent<KubectlContainer<T, C>> label;

    public KubectlContainer(
            final DockerImageName dockerImageName,
            final KubeConfigSupplier kubeConfigSupplier,
            final C k8s
    ) {
        super(dockerImageName, kubeConfigSupplier);
        apply = new ApplyFluent<>(
                this::safeExecInContainer,
                this::copyFileToContainer,
                this,
                k8s
        );
        delete = new DeleteFluent<>(
                this::safeExecInContainer,
                this
        );
        wait = new WaitFluent<>(
                this::safeExecInContainer,
                this
        );
        create = new CreateFluent<>(
                this::safeExecInContainer,
                this
        );
        label = new LabelFluent<>(
                this::safeExecInContainer,
                this
        );
    }
}
