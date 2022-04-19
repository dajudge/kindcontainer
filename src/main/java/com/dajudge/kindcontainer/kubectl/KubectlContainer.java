package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.testcontainers.utility.DockerImageName;

public class KubectlContainer<T extends KubectlContainer<T, C>, C> extends BaseSidecarContainer<T> {
    public static final DockerImageName DEFAULT_KUBECTL_IMAGE = DockerImageName.parse("bitnami/kubectl:1.21.9-debian-10-r10");

    public final ApplyFluent<KubectlContainer<T, C>, C> apply;
    public final DeleteFluent<KubectlContainer<T, C>> delete;
    public final WaitFluent<KubectlContainer<T, C>> wait;
    public final CreateFluent<KubectlContainer<T, C>> create;

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
    }
}
