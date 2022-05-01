package com.dajudge.kindcontainer;

import org.testcontainers.utility.DockerImageName;

public abstract class KubernetesWithKubeletContainer<T extends KubernetesWithKubeletContainer<T>> extends KubernetesContainer<T> {
    public KubernetesWithKubeletContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public abstract T withNodePortRange(final int minPort, final int maxPort);
}
