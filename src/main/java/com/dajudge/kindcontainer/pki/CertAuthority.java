package com.dajudge.kindcontainer.pki;

import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import org.testcontainers.shaded.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.*;
import org.testcontainers.shaded.org.bouncycastle.cert.CertIOException;
import org.testcontainers.shaded.org.bouncycastle.cert.X509CertificateHolder;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.testcontainers.shaded.org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;
import org.testcontainers.shaded.org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class CertAuthority {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration CERT_VALIDITY = Duration.ofDays(365);
    private final KeyStoreWrapper caKeyStore;
    private final Supplier<Long> clock;
    private final String issuerDn;

    public CertAuthority(final Supplier<Long> clock, final String dn) {
        this.clock = clock;
        this.caKeyStore = createCaKeyStore(dn, clock);
        this.issuerDn = dn;
    }

    public KeyStoreWrapper getCaKeyStore() {
        return caKeyStore;
    }

    public KeyStoreWrapper newKeyPair(final String dn, final List<GeneralName> sans) {
        final char[] keyPassword = UUID.randomUUID().toString().toCharArray();
        final String keyAlias = UUID.randomUUID().toString();
        return new KeyStoreWrapper(createJks(keyStore -> {
            final KeyPair keyPair = randomKeyPair();
            final X509Certificate cert = sign(
                    dn,
                    issuerDn,
                    caKeyStore.getPrivateKey(),
                    "SHA256withRSA",
                    keyPair.getPublic(),
                    now(clock),
                    plus(now(clock), CERT_VALIDITY),
                    singletonList(addSan(sans))
            );
            keyStore.setKeyEntry(
                    keyAlias,
                    keyPair.getPrivate(),
                    keyPassword,
                    new Certificate[]{cert}
            );
        }), keyPassword, keyAlias);
    }


    private static KeyStoreWrapper createCaKeyStore(final String dn, final Supplier<Long> clock) {
        final char[] keyPassword = UUID.randomUUID().toString().toCharArray();
        final String keyAlias = UUID.randomUUID().toString();
        return new KeyStoreWrapper(createJks(keyStore -> {
            final KeyPair keyPair = randomKeyPair();
            final X509Certificate cert = selfSignedCert(
                    dn,
                    keyPair,
                    now(clock),
                    plus(now(clock), CERT_VALIDITY),
                    "SHA256withRSA",
                    singletonList(caKeystore()));
            keyStore.setKeyEntry(
                    keyAlias,
                    keyPair.getPrivate(),
                    keyPassword,
                    new Certificate[]{cert}
            );
        }), keyPassword, keyAlias);
    }

    private static ThrowingConsumer<X509v3CertificateBuilder, CertIOException> caKeystore() {
        return certGenerator -> {
            certGenerator.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), false, new BasicConstraints(true));
        };
    }

    private static ThrowingConsumer<X509v3CertificateBuilder, CertIOException> addSan(final List<GeneralName> sans) {
        return certGenerator -> {
            if (!sans.isEmpty()) {
                certGenerator.addExtension(
                        X509Extensions.SubjectAlternativeName,
                        false,
                        new GeneralNames(sans.toArray(new GeneralName[0]))
                );
            }
        };
    }

    public static X509Certificate selfSignedCert(
            final String dn,
            final KeyPair pair,
            final Date notBefore,
            final Date notAfter,
            final String algorithm,
            final List<ThrowingConsumer<X509v3CertificateBuilder, CertIOException>> mods
    ) {
        return sign(dn, dn, pair.getPrivate(), algorithm, pair.getPublic(), notBefore, notAfter, mods);
    }

    public static X509Certificate sign(
            final String ownerDn,
            final String issuerDn,
            final PrivateKey signingKey,
            final String algorithm,
            final PublicKey publicKey,
            final Date notBefore,
            final Date notAfter,
            final List<ThrowingConsumer<X509v3CertificateBuilder, CertIOException>> mods
    ) {
        try {
            final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                    .find(algorithm);
            final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder()
                    .find(sigAlgId);

            final X509v3CertificateBuilder certGenerator = new X509v3CertificateBuilder(
                    new X500Name(issuerDn),
                    BigInteger.valueOf(Math.abs(SECURE_RANDOM.nextInt())),
                    notBefore,
                    notAfter,
                    new X500Name(ownerDn),
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
            );
            for (final ThrowingConsumer<X509v3CertificateBuilder, CertIOException> mod : mods) {
                mod.accept(certGenerator);
            }
            final ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                    .build(PrivateKeyFactory.createKey(signingKey.getEncoded()));
            final X509CertificateHolder holder = certGenerator.build(sigGen);
            final CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");

            try (final InputStream stream = new ByteArrayInputStream(holder.toASN1Structure().getEncoded())) {
                return (X509Certificate) cf.generateCertificate(stream);
            }
        } catch (final CertificateException | IOException | OperatorCreationException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to sign certificate", e);
        }
    }

    public static KeyPair randomKeyPair() {
        return Helpers.call(() -> {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        });
    }

    private static KeyStore createJks(final ThrowingConsumer<KeyStore, Exception> withKeyStore) {
        return Helpers.call(() -> {
            final KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null, null);
            withKeyStore.accept(keystore);
            return keystore;
        });
    }

    private static Date now(final Supplier<Long> clock) {
        return new Date(clock.get());
    }

    private static Date plus(final Date now, final TemporalAmount amount) {
        return new Date(now.toInstant().plus(amount).toEpochMilli());
    }
}
