package com.dajudge.kindcontainer.client.ssl;

import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;
import org.testcontainers.shaded.com.google.common.io.ByteStreams;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;

public final class SslUtil {

    private static final KeyFactory RSA_KEY_FACTORY = createKeyFactory("RSA");
    private static final CertificateFactory CERT_FACTORY = createCertFactory();

    private SslUtil() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    public static KeyManager[] createKeyManager(ByteArrayInputStream certStream, ByteArrayInputStream keyStream, char[] passphrase) throws CertificateException, InvalidKeySpecException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        final Collection<? extends Certificate> certificates = CERT_FACTORY.generateCertificates(certStream);
        final PrivateKey key = getPrivateKey(keyStream);

        final KeyStore keyStore = newKeyStore();
        keyStore.setKeyEntry("key", key, passphrase, certificates.toArray(new Certificate[0]));
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, passphrase);
        return kmf.getKeyManagers();
    }

    private static PrivateKey getPrivateKey(final ByteArrayInputStream keyStream) throws InvalidKeySpecException, IOException {
        final byte[] keyBytes = parsePem(keyStream);
        try {
            return RSA_KEY_FACTORY.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (final InvalidKeySpecException e) {
            return RSA_KEY_FACTORY.generatePrivate(decodePKCS1(keyBytes));
        }
    }

    public static TrustManager[] createTrustManagers(ByteArrayInputStream certStream) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        final X509Certificate cert = (X509Certificate) createCertFactory().generateCertificate(certStream);
        final KeyStore trustStore = newKeyStore();
        trustStore.setCertificateEntry("ca", cert);

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }

    private static KeyStore newKeyStore() throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        return keyStore;
    }

    private static CertificateFactory createCertFactory() {
        try {
            return CertificateFactory.getInstance("X509");
        } catch (final CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyFactory createKeyFactory(final String algorithm) {
        try {
            return KeyFactory.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    static byte[] parsePem(final InputStream pem) throws IOException {
        final byte[] pemBytes = ByteStreams.toByteArray(pem);
        final StringBuilder base64 = new StringBuilder();
        final List<String> lines = new ArrayList<>(asList(new String(pemBytes, US_ASCII).split("\n")));
        String endLine = null;
        while (!lines.isEmpty()) {
            final String line = lines.remove(0).trim();
            if (endLine == null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("-----BEGIN ")) {
                    endLine = "-----END " + line.substring("-----BEGIN ".length());
                    continue;
                }
            }
            if (line.equals(endLine)) {
                return Base64.getDecoder().decode(base64.toString());
            }
            base64.append(line);
        }
        throw new IllegalArgumentException("Unterminated PEM");
    }


    private static RSAPrivateCrtKeySpec decodePKCS1(final byte[] keyBytes) throws IOException {
        final DerParser outer = new DerParser(new ByteArrayInputStream(keyBytes));
        final DerParser parser = new DerParser(new ByteArrayInputStream(outer.read().getValue()));
        parser.read();
        return new RSAPrivateCrtKeySpec(
                parser.read().getBigInteger(),
                parser.read().getBigInteger(),
                parser.read().getBigInteger(),
                parser.read().getBigInteger(),
                parser.read().getBigInteger(),
                parser.read().getBigInteger(),
                parser.read().getBigInteger(),
                parser.read().getBigInteger()
        );
    }
}
