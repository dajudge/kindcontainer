package com.dajudge.kindcontainer.client.ssl;

import org.junit.Test;
import org.testcontainers.shaded.com.google.common.io.Resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.dajudge.kindcontainer.client.ssl.SslUtil.parsePem;
import static org.junit.Assert.assertEquals;

public class PemDecoderTest {
    private static final byte[] PEM = readResource("pem.txt");

    @Test
    public void can_parse_pem() throws IOException {
        assertEquals(-265980, checksum(parsePem(new ByteArrayInputStream(PEM))));
    }

    private int checksum(final byte[] result) {
        int checksum = 0;
        for (int i = 0; i < result.length; i++) {
            checksum += (i + 1) * result[i];
        }
        return checksum;
    }


    private static byte[] readResource(final String resourceName) {
        try {
            return Resources.toByteArray(Resources.getResource(resourceName));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourceName, e);
        }
    }

}