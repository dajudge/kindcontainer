package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static com.dajudge.kindcontainer.util.TestUtils.stringResource;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class CertificatesTest {
    @Container
    public final KindContainer<?> k8s = new KindContainer<>()
            .withCaCert(MountableFile.forClasspathResource("test.crt"));

    @Test
    public void adds_custom_certificate() {
        final String allCerts = k8s.copyFileFromContainer(
                "/etc/ssl/certs/ca-certificates.crt",
                TestUtils::readString
        );
        assertTrue(allCerts.contains(stringResource("test.crt")));
    }
}
