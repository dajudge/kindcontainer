package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.Utils;
import com.dajudge.kindcontainer.Utils.LazyContainer;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;

public class KubectlContainer<T extends KubectlContainer<T, C>, C> extends BaseSidecarContainer<T> {
    public static final DockerImageName DEFAULT_KUBECTL_IMAGE = DockerImageName.parse("bitnami/kubectl:1.21.9-debian-10-r10");

    public final ApplyFluent<KubectlContainer<T, C>, C> apply;
    public final DeleteFluent<KubectlContainer<T, C>> delete;
    public final WaitFluent<KubectlContainer<T, C>> wait;
    public final CreateFluent<KubectlContainer<T, C>> create;
    public final LabelFluent<KubectlContainer<T, C>> label;

    private KubectlContainer(
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

    public static <T extends KubernetesContainer<T>> LazyContainer<KubectlContainer<?, T>> lazy(
            final Supplier<DockerImageName> image,
            final Supplier<String> containerId,
            final KubeConfigSupplier kubeconfig,
            final T k8s
    ) {
        return LazyContainer.from(() -> new KubectlContainer<>(image.get(), kubeconfig, k8s).withNetworkMode("container:" + containerId.get()));
    }
}
