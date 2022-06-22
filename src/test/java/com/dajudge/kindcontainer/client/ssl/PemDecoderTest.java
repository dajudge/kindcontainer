package com.dajudge.kindcontainer.client.ssl;

import com.dajudge.kindcontainer.util.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.dajudge.kindcontainer.client.ssl.SslUtil.parsePem;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PemDecoderTest {
    private static final byte[] PEM = TestUtils.readResource("pem.txt");

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


}