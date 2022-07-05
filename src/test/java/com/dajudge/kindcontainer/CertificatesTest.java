package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.utility.MountableFile;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kindContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.stringResource;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CertificatesTest {
    @TestFactory
    public Stream<DynamicTest> adds_custom_certificate() {
        return kindContainers(this::assertAddsCertificate);
    }

    private void assertAddsCertificate(final KindContainer<?> container) {
        runWithK8s(container.withCaCert(MountableFile.forClasspathResource("test.crt")), k8s -> {
            final String allCerts = k8s.copyFileFromContainer(
                    "/etc/ssl/certs/ca-certificates.crt",
                    TestUtils::readString
            );
            assertTrue(allCerts.contains(stringResource("test.crt")));
        });
    }

}
