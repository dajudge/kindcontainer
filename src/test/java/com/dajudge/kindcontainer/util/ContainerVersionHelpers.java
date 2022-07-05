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
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public final class ContainerVersionHelpers {
    private ContainerVersionHelpers() {
        throw new IllegalStateException("Do not instantiate");
    }

    public interface KubernetesTest<T extends KubernetesContainer<?>> {
        void run(final KubernetesTestPackage<? extends T> testPkg);
    }

    public static Stream<DynamicTest> allContainers(final KubernetesTest<? super KubernetesContainer<?>> test) {
        return testFactory(allContainers(), test);
    }

    public static Stream<DynamicTest> kubeletContainers(final KubernetesTest<? super KubernetesWithKubeletContainer<?>> test) {
        return testFactory(containersWithKubelet(), test);
    }

    public static Stream<DynamicTest> reusableContainers(final KubernetesTest<? super KubernetesContainer<?>> test) {
        return testFactory(reusableContainers(), test);
    }

    public static Stream<DynamicTest> kindContainers(final KubernetesTest<? super KindContainer<?>> test) {
        return testFactory(kindContainers(), test);
    }

    public static Stream<DynamicTest> k3sContainers(final KubernetesTest<? super K3sContainer<?>> test) {
        return testFactory(k3sContainers(), test);
    }

    public static Stream<DynamicTest> apiServerContainers(final KubernetesTest<? super ApiServerContainer<?>> test) {
        return testFactory(apiServerContainers(), test);
    }

    public static Stream<KubernetesTestPackage<? extends KubernetesContainer<?>>> allContainers() {
        return Stream.of(apiServerContainers(), k3sContainers(), kindContainers()).flatMap(identity());
    }

    public static Stream<KubernetesTestPackage<? extends KubernetesWithKubeletContainer<?>>> containersWithKubelet() {
        return Stream.of(k3sContainers(), kindContainers()).flatMap(identity());
    }

    public static Stream<KubernetesTestPackage<? extends KubernetesContainer<?>>> reusableContainers() {
        return Stream.of(k3sContainers(), kindContainers()).flatMap(identity());
    }

    private static Stream<KubernetesTestPackage<? extends KindContainer<?>>> kindContainers() {
        return Stream.of(KindContainerVersion.values())
                .filter(isInContainerFilter(latest(KindContainerVersion.class)))
                .map(version -> containerFactory(
                        () -> new KindContainer<>(version),
                        KindContainer.class.getSimpleName(),
                        version
                ));
    }

    private static Stream<KubernetesTestPackage<? extends K3sContainer<?>>> k3sContainers() {
        return Stream.of(K3sContainerVersion.values())
                .filter(isInContainerFilter(latest(K3sContainerVersion.class)))
                .map(version -> containerFactory(
                        () -> new K3sContainer<>(version),
                        K3sContainer.class.getSimpleName(),
                        version
                ));
    }

    private static Stream<KubernetesTestPackage<? extends ApiServerContainer<?>>> apiServerContainers() {
        return Stream.of(ApiServerContainerVersion.values())
                .filter(isInContainerFilter(latest(ApiServerContainerVersion.class)))
                .map(version -> containerFactory(
                        () -> new ApiServerContainer<>(version),
                        ApiServerContainer.class.getSimpleName(),
                        version
                ));
    }

    private static <T extends KubernetesContainer<?>> Stream<DynamicTest> testFactory(
            final Stream<KubernetesTestPackage<? extends T>> containers,
            final KubernetesTest<? super T> test
    ) {
        return containers.map(testPkg -> dynamicTest(testPkg.toString(), () -> test.run(testPkg)));
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

    private static <T extends KubernetesContainer<?>> KubernetesTestPackage<T> containerFactory(
            final Supplier<T> supplier,
            final String containerClassName,
            final KubernetesVersionEnum<?> version
    ) {
        return new KubernetesTestPackage<>(containerClassName, supplier, version.descriptor());
    }

    public static <T extends GenericContainer<?>> void runWithK8s(T container, final Consumer<T> consumer) {
        try {
            container.start();
            consumer.accept(container);
        } finally {
            container.stop();
        }
    }

    public static final class KubernetesTestPackage<T extends KubernetesContainer<?>> {
        private final String containerClassName;
        private final Supplier<? extends T> factory;
        private final KubernetesVersionDescriptor version;

        public KubernetesTestPackage(
                final String containerClassName,
                final Supplier<? extends T> factory,
                final KubernetesVersionDescriptor version
        ) {
            this.containerClassName = containerClassName;
            this.factory = factory;
            this.version = version;
        }

        public T newContainer() {
            return factory.get();
        }

        @Override
        public String toString() {
            return describe();
        }

        public KubernetesVersionDescriptor version() {
            return version;
        }

        public String describe() {
            return format("%s %s", containerClassName, version.getKubernetesVersion());
        }
    }
}
