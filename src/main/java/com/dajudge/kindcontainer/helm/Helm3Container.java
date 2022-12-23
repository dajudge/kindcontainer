package com.dajudge.kindcontainer.helm;


import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.Utils;
import com.dajudge.kindcontainer.Utils.LazyContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;

public class Helm3Container<SELF extends Helm3Container<SELF>> extends BaseSidecarContainer<SELF> {

    public final RepoFluent<Helm3Container<SELF>> repo = new RepoFluent<>(this::safeExecInContainer, this);
    public final InstallFluent<Helm3Container<SELF>> install = new InstallFluent<>(this::safeExecInContainer, this);

    public Helm3Container(final DockerImageName dockerImageName, final KubeConfigSupplier kubeConfigSupplier) {
        super(dockerImageName, kubeConfigSupplier);
    }

    public static LazyContainer<Helm3Container<?>> lazy(
            final Supplier<DockerImageName> image,
            final Supplier<String> containerId,
            final KubeConfigSupplier kubeConfigSupplier
    ) {
        return LazyContainer.from(() -> new Helm3Container<>(image.get(), kubeConfigSupplier).withNetworkMode("container:" + containerId.get()));
    }
}
