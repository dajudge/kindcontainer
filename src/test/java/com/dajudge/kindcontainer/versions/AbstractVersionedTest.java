package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.KubernetesVersionDescriptor;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractVersionedTest {

    protected static final class KubernetesTestPackage<T extends KubernetesContainer<?>> {
        private final String containerClassName;
        private final Supplier<T> factory;
        private final KubernetesVersionDescriptor version;

        public KubernetesTestPackage(
                final String containerClassName,
                final Supplier<T> factory,
                final KubernetesVersionDescriptor version
        ) {
            this.containerClassName = containerClassName;
            this.factory = factory;
            this.version = version;
        }
    }

    protected abstract Stream<KubernetesTestPackage<?>> testPackages();

    @TestFactory
    public Stream<DynamicTest> can_start() {
        return testPackages().map(pkg -> {
            final Supplier<? extends KubernetesContainer<?>> factory = pkg.factory;
            final KubernetesVersionDescriptor version = pkg.version;
            final String name = format("%s %s", pkg.containerClassName, version.getKubernetesVersion());
            return DynamicTest.dynamicTest(name, () -> {
                assertStartsCorrectVersion(factory, version);
            });
        });
    }

    private void assertStartsCorrectVersion(
            final Supplier<? extends KubernetesContainer<?>> factory,
            final KubernetesVersionDescriptor version
    ) {
        final KubernetesContainer<?> k8s = factory.get();
        try {
            k8s.start();
            runWithClient(k8s, client -> {
                final String gitVersion = client.getKubernetesVersion().getGitVersion();  // e.g. v1.21.9+k3s1
                final String k8sVersion = gitVersion.replaceAll("\\+.*", ""); // e.g. v1.21.9
                assertEquals(version.getKubernetesVersion(), k8sVersion);
            });
        } catch (final Exception e) {
            throw new AssertionError("Failed to launch kubernetes with version " + version.getKubernetesVersion(), e);
        } finally {
            k8s.stop();
        }
    }
}
