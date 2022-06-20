package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.KIND_CONTAINER;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.stringResource;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class CertificatesTest {
    @ParameterizedTest
    @MethodSource(KIND_CONTAINER)
    public void adds_custom_certificate(final Supplier<KindContainer<?>> factory) {
        runWithK8s(createContainer(factory), k8s -> {
            final String allCerts = k8s.copyFileFromContainer(
                    "/etc/ssl/certs/ca-certificates.crt",
                    TestUtils::readString
            );
            assertTrue(allCerts.contains(stringResource("test.crt")));
        });
    }

    private KubernetesWithKubeletContainer<?> createContainer(final Supplier<KindContainer<?>> factory) {
        return factory.get()
                .withCaCert(MountableFile.forClasspathResource("test.crt"));
    }
}
