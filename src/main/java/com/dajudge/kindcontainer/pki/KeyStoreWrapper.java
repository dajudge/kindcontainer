/*
Copyright 2020-2021 Alex Stockinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.dajudge.kindcontainer.pki;

import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemWriter;

import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static com.dajudge.kindcontainer.pki.Helpers.call;

public class KeyStoreWrapper {
    private KeyStore keyStore;
    private char[] keyPassword;
    private final String keyAlias;

    KeyStoreWrapper(final KeyStore keyStore, final char[] keyPassword, final String keyAlias) {
        this.keyStore = keyStore;
        this.keyPassword = keyPassword;
        this.keyAlias = keyAlias;
    }

    public PrivateKey getPrivateKey() {
        return call(() -> (PrivateKey) keyStore.getKey(keyAlias, keyPassword));
    }


    public X509Certificate getCertificate() {
        return (X509Certificate) call(() -> keyStore.getCertificate(keyAlias));
    }

    public String getPrivateKeyPem() {
        return toPem(getPrivateKey());
    }

    public String getPublicKeyPem() { return toPem(getCertificate().getPublicKey());}

    public String getCertificatePem() {
        return toPem(getCertificate());
    }

    private static String toPem(final Object object) {
        return call(() -> {
            final StringWriter writer = new StringWriter();
            final PemWriter pemWriter = new PemWriter(writer);
            pemWriter.writeObject(new JcaMiscPEMGenerator(object));
            pemWriter.flush();
            return writer.toString();
        });
    }
}
