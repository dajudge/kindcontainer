package com.dajudge.kindcontainer;

public interface KubernetesImageSource<T extends KubernetesVersionEnum<T>> {
    KubernetesImageSource<T> withImage(String image);

    KubernetesImageSpec<T> withKubectlImage(String kubectlImage);

    KubernetesImageSpec<T> withHelm3Image(String helm3Image);
}
