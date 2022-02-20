package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.testcontainers.utility.DockerImageName;

public class KubectlContainer<T extends KubectlContainer<T>> extends BaseSidecarContainer<T> {
    public static final DockerImageName DEFAULT_KUBECTL_IMAGE = DockerImageName.parse("bitnami/kubectl:1.21.9-debian-10-r10");

    public ApplyFluent<KubectlContainer<T>> apply = new ApplyFluent<>(
            this::safeExecInContainer,
            this::copyFileToContainer,
            this
    );
    public DeleteFluent<KubectlContainer<T>> delete = new DeleteFluent<>(
            this::safeExecInContainer,
            this
    );

    public KubectlContainer(final DockerImageName dockerImageName, final KubeConfigSupplier kubeConfigSupplier) {
        super(dockerImageName, kubeConfigSupplier);
    }
}
