package com.dajudge.kindcontainer;

import org.junit.Test;

import static com.dajudge.kindcontainer.TestUtils.stringResource;
import static org.junit.Assert.assertTrue;

public class CertificatesTest {
    @Test
    public void adds_custom_certificate() {
        final String allCerts = StaticContainers.kind().copyFileFromContainer(
                "/etc/ssl/certs/ca-certificates.crt",
                TestUtils::readString
        );
        assertTrue(allCerts.contains(stringResource("test.crt")));
    }
}
