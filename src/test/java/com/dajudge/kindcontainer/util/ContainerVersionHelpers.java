package com.dajudge.kindcontainer.util;

import com.dajudge.kindcontainer.*;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ContainerVersionHelpers {
    public static final String CONTAINERS_WITH_KUBELET = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#containersWithKubelet";
    public static final String ALL_CONTAINERS = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#allContainers";
    public static final String REUSABLE_CONTAINERS = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#reusableContainers";

    private ContainerVersionHelpers() {
        throw new IllegalStateException("Do not instantiate");
    }

    public static Stream<Supplier<KubernetesContainer<?>>> allContainers() {
        return Stream.of(
                factoryFor(ApiServerContainer::new, ApiServerContainer.class.getSimpleName()),
                factoryFor(K3sContainer::new, K3sContainer.class.getSimpleName()),
                factoryFor(KindContainer::new, KindContainer.class.getSimpleName())
        );
    }

    public static Stream<Supplier<KubernetesWithKubeletContainer<?>>> containersWithKubelet() {
        return Stream.of(
                factoryFor(K3sContainer::new, K3sContainer.class.getSimpleName()),
                factoryFor(KindContainer::new, KindContainer.class.getSimpleName())
        );
    }

    public static Stream<Supplier<KubernetesContainer<?>>> reusableContainers() {
        return Stream.of(
                factoryFor(K3sContainer::new, K3sContainer.class.getSimpleName()),
                factoryFor(KindContainer::new, KindContainer.class.getSimpleName())
        );
    }

    private static <T> Supplier<T> factoryFor(final Supplier<T> supplier, final String name) {
        return new Supplier<T>() {
            @Override
            public T get() {
                return supplier.get();
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    @NotNull
    public static <T> Stream<Supplier<T>> configure(
            final Stream<Supplier<T>> stream,
            final Consumer<T> mod
    ) {
        return stream.map(s -> new Supplier<T>() {
            @Override
            public T get() {
                final T k8s = s.get();
                mod.accept(k8s);
                return k8s;
            }

            @Override
            public String toString() {
                return s.toString();
            }
        });
    }

    public static <T extends GenericContainer<?>> void runWithK8s(T container, final Consumer<T> consumer) {
        try {
            container.start();
            consumer.accept(container);
        } finally {
            container.stop();
        }
    }
}
