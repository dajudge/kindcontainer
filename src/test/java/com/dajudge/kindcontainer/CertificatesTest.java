package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import static com.dajudge.kindcontainer.util.TestUtils.stringResource;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

public class CertificatesTest {

    @Rule
    public final KindContainer<?> k8s = new KindContainer<>()
            .withCaCerts(singletonList(stringResource("test.crt")));

    @Test
    public void adds_custom_certificate() {
        final String allCerts = k8s.copyFileFromContainer(
                "/etc/ssl/certs/ca-certificates.crt",
                TestUtils::readString
        );
        assertTrue(allCerts.contains(stringResource("test.crt")));
    }
}
