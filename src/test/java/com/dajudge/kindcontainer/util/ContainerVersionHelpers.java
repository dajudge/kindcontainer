package com.dajudge.kindcontainer.util;

import com.dajudge.kindcontainer.*;
import org.junit.jupiter.api.DynamicTest;
import org.testcontainers.containers.GenericContainer;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static java.lang.String.format;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

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

    public static Stream<DynamicTest> allContainers(final Consumer<? super KubernetesContainer<?>> test) {
        return testFactory(allContainers(), test);
    }

    public static Stream<DynamicTest> kubeletContainers(final Consumer<? super KubernetesWithKubeletContainer<?>> test) {
        return testFactory(containersWithKubelet(), test);
    }

    public static Stream<DynamicTest> reusableContainers(final Consumer<? super KubernetesContainer<?>> test) {
        return testFactory(reusableContainers(), test);
    }

    public static Stream<DynamicTest> kindContainers(final Consumer<? super KindContainer<?>> test) {
        return testFactory(kindContainers(), test);
    }

    public static Stream<DynamicTest> k3sContainers(final Consumer<? super K3sContainer<?>> test) {
        return testFactory(k3sContainers(), test);
    }

    public static Stream<DynamicTest> apiServerContainers(final Consumer<? super ApiServerContainer<?>> test) {
        return testFactory(apiServerContainers(), test);
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
        return Stream.of(KindContainerVersion.values())
                .filter(isInContainerFilter(latest(KindContainerVersion.class)))
                .map(version -> containerFactory(
                        KindContainer::new,
                        KindContainer.class.getSimpleName(),
                        version
                ));
    }

    public static Stream<Supplier<K3sContainer<?>>> k3sContainers() {
        return Stream.of(K3sContainerVersion.values())
                .filter(isInContainerFilter(latest(K3sContainerVersion.class)))
                .map(version -> containerFactory(
                        K3sContainer::new,
                        K3sContainer.class.getSimpleName(),
                        version
                ));
    }

    public static Stream<Supplier<ApiServerContainer<?>>> apiServerContainers() {
        return Stream.of(ApiServerContainerVersion.values())
                .filter(isInContainerFilter(latest(ApiServerContainerVersion.class)))
                .map(version -> containerFactory(
                        ApiServerContainer::new,
                        ApiServerContainer.class.getSimpleName(),
                        version
                ));
    }

    private static <T> Stream<DynamicTest> testFactory(
            final Stream<Supplier<T>> containers,
            final Consumer<? super T> test
    ) {
        return containers.map(factory -> dynamicTest(factory.toString(), () -> test.accept(factory.get())));
    }

    private static <T extends KubernetesVersionEnum<?>> Predicate<T> isInContainerFilter(final T defaultVersion) {
        assert defaultVersion != null;
        return Optional.ofNullable(System.getenv("CONTAINER_FILTER"))
                .map(filter -> (Predicate<T>) version -> {
                    final String[] parts = filter.split(" ", 2);
                    final String container = parts[0];
                    final String versionString = parts[1];
                    if (!version.getClass().getSimpleName().equals(format("%sVersion", container))) {
                        return false;
                    }
                    return format("v%s", versionString).equals(version.descriptor().getKubernetesVersion());
                })
                .orElse(version -> version.descriptor().equals(version.descriptor()));
    }

    private static <T> Supplier<T> containerFactory(
            final Supplier<T> supplier,
            final String containerName,
            final KubernetesVersionEnum<?> version
    ) {
        return namedFactory(supplier, format("%s %s", containerName, version.descriptor().getKubernetesVersion()));
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
