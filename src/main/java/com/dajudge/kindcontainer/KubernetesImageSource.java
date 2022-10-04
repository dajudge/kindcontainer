package com.dajudge.kindcontainer;

public interface KubernetesImageSource<T extends KubernetesVersionEnum<T>> {
    KubernetesImageSource<T> withImage(String image);
}
