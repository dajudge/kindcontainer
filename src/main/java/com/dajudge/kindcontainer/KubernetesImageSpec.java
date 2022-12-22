package com.dajudge.kindcontainer;

import org.testcontainers.utility.DockerImageName;

public class KubernetesImageSpec<T extends KubernetesVersionEnum<T>> implements KubernetesImageSource<T> {
    private final T version;

    private String image;

    private String kubectlImage = "bitnami/kubectl:1.21.9-debian-10-r10";
    private String helm3Image = "alpine/helm:3.7.2";

    public KubernetesImageSpec(final T version) {
        this.version = version;
    }

    @Override
    public KubernetesImageSpec<T> withImage(final String image) {
        this.image = image;
        return this;
    }

    @Override
    public KubernetesImageSpec<T> withKubectlImage(final String kubectlImage) {
        this.kubectlImage = kubectlImage;
        return this;
    }

    @Override
    public KubernetesImageSpec<T> withHelm3Image(final String helm3Image) {
        this.helm3Image = helm3Image;
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

    public String getKubectlImage() {
        return kubectlImage;
    }

    public String getHelm3Image() {
        return helm3Image;
    }
}
