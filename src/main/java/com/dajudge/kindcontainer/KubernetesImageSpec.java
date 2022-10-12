package com.dajudge.kindcontainer;

import org.testcontainers.utility.DockerImageName;

public class KubernetesImageSpec<T extends KubernetesVersionEnum<T>> implements KubernetesImageSource<T> {
    private final T version;

    private String image;

    public KubernetesImageSpec(final T version) {
        this.version = version;
    }

    @Override
    public KubernetesImageSpec<T> withImage(final String image) {
        this.image = image;
        return this;
    }

    public DockerImageName getImage() {
        return DockerImageName.parse(image != null ? image : version.defaultImageTemplate()
                .replace("${major}", String.valueOf(version.descriptor().getMajor()))
                .replace("${minor}", String.valueOf(version.descriptor().getMinor()))
                .replace("${patch}", String.valueOf(version.descriptor().getPatch())));
    }

    public T getVersion() {
        return version;
    }
}
