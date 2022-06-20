package com.dajudge.kindcontainer.util;

import com.dajudge.kindcontainer.*;
import org.testcontainers.containers.GenericContainer;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static java.lang.String.format;

public final class ContainerVersionHelpers {
    public static final String CONTAINERS_WITH_KUBELET = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#containersWithKubelet";
    public static final String ALL_CONTAINERS = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#allContainers";
    public static final String REUSABLE_CONTAINERS = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#reusableContainers";
    public static final String KIND_CONTAINER = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#kindContainers";
    public static final String APISERVER_CONTAINER = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#apiServerContainers";
    public static final String K3S_CONTAINER = "com.dajudge.kindcontainer.util.ContainerVersionHelpers#k3sContainers";

    private ContainerVersionHelpers() {
        throw new IllegalStateException("Do not instantiate");
    }

    public static Stream<Supplier<KubernetesContainer<?>>> allContainers() {
        return Stream.of(apiServerContainers(), k3sContainers(), kindContainers())
                .flatMap(i -> i)
                .map(ContainerVersionHelpers::castSupplier);

    }

    public static Stream<Supplier<KubernetesWithKubeletContainer<?>>> containersWithKubelet() {
        return Stream.of(k3sContainers(), kindContainers())
                .flatMap(i -> i)
                .map(ContainerVersionHelpers::castSupplier);
    }

    public static Stream<Supplier<KubernetesContainer<?>>> reusableContainers() {
        return Stream.of(k3sContainers(), kindContainers())
                .flatMap(i -> i)
                .map(ContainerVersionHelpers::castSupplier);
    }

    public static Stream<Supplier<KindContainer<?>>> kindContainers() {
        return Stream.of(containerFactory(
                KindContainer::new,
                KindContainer.class.getSimpleName(),
                latest(KindContainerVersion.class)
        ));
    }

    public static Stream<Supplier<K3sContainer<?>>> k3sContainers() {
        return Stream.of(containerFactory(
                K3sContainer::new,
                K3sContainer.class.getSimpleName(),
                latest(K3sContainerVersion.class)
        ));
    }

    public static Stream<Supplier<ApiServerContainer<?>>> apiServerContainers() {
        return Stream.of(containerFactory(
                ApiServerContainer::new,
                ApiServerContainer.class.getSimpleName(),
                latest(ApiServerContainerVersion.class)
        ));
    }

    private static <T> Supplier<T> containerFactory(
            final Supplier<T> supplier,
            final String containerName,
            final KubernetesVersionEnum<?> version
    ) {
        return namedFactory(supplier, String.format("%s %s", containerName, version.descriptor().getKubernetesVersion()));
    }

    private static <T> Supplier<T> namedFactory(final Supplier<T> supplier, final String name) {
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

    public static <T extends KubernetesVersionEnum<T>> Predicate<KubernetesVersionEnum<T>> versionFilter(
            final String container,
            final KubernetesVersionEnum<T> defaultValue
    ) {
        return versionFilter(container, defaultValue.descriptor().getKubernetesVersion());
    }

    public static <T extends KubernetesVersionEnum<T>> Predicate<KubernetesVersionEnum<T>> versionFilter(
            final String container,
            final String defaultValue
    ) {
        final String filterValue = System.getProperty(format("com.dajudge.kindcontainer.filter.%s", container), defaultValue);
        return version -> {
            if (filterValue.equals("none")) {
                return false;
            }
            if (filterValue.equals("all")) {
                return true;
            }
            return version.descriptor().getKubernetesVersion().equals(filterValue);
        };
    }

    private static <T> Supplier<T> castSupplier(final Supplier<? extends T> supplier) {
        return new Supplier<T>() {
            @Override
            public T get() {
                return supplier.get();
            }

            @Override
            public String toString() {
                return supplier.toString();
            }
        };
    }

    ;
}
