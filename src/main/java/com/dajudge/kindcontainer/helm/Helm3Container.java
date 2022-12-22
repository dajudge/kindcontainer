package com.dajudge.kindcontainer.helm;


import com.dajudge.kindcontainer.BaseSidecarContainer;
import org.testcontainers.utility.DockerImageName;

public class Helm3Container<SELF extends Helm3Container<SELF>> extends BaseSidecarContainer<SELF> {
    public final RepoFluent<Helm3Container<SELF>> repo = new RepoFluent<>(this::safeExecInContainer, this);
    public final InstallFluent<Helm3Container<SELF>> install = new InstallFluent<>(this::safeExecInContainer, this);


    public Helm3Container(final DockerImageName dockerImageName, final KubeConfigSupplier kubeConfigSupplier) {
        super(dockerImageName, kubeConfigSupplier);
    }


}
