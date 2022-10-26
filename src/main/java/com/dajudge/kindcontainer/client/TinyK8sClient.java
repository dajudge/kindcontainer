package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.config.ClusterSpec;
import com.dajudge.kindcontainer.client.config.ContextSpec;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.UserSpec;
import com.dajudge.kindcontainer.client.http.TinyHttpClient;
import com.dajudge.kindcontainer.client.ssl.SslUtil;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class TinyK8sClient {
    private final HttpSupport support;

    public TinyK8sClient(final TinyHttpClient client, final String masterUrl) {
        this.support = new HttpSupport(client, masterUrl);
    }

    public static TinyK8sClient fromKubeconfig(final String kubeconfig) {
        return fromKubeconfig(KubeConfigUtils.parseKubeConfig(kubeconfig));
    }

    public static TinyK8sClient fromKubeconfig(final KubeConfig kubeconfig) {
        try {
            final ContextSpec currentContext = kubeconfig.getContexts().stream()
                    .filter(it -> it.getName().equals(kubeconfig.getCurrentContext()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("kubeconfig invalid")).getContext();
            final ClusterSpec cluster = kubeconfig.getClusters().stream()
                    .filter(it -> it.getName().equals(currentContext.getCluster()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("kubeconfig invalid")).getCluster();
            final UserSpec user = kubeconfig.getUsers().stream()
                    .filter(it -> it.getName().equals(currentContext.getUser()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("kubeconfig invalid")).getUser();
            final byte[] caCert = Base64.getDecoder().decode(cluster.getCertificateAuthorityData());
            final byte[] clientCert = Base64.getDecoder().decode(user.getClientCertificateData());
            final byte[] clientKey = Base64.getDecoder().decode(user.getClientKeyData());
            final TrustManager[] trustManagers = SslUtil.createTrustManagers(new ByteArrayInputStream(caCert));
            final ByteArrayInputStream certStream = new ByteArrayInputStream(clientCert);
            final ByteArrayInputStream keyStream = new ByteArrayInputStream(clientKey);
            final KeyManager[] keyManagers = SslUtil.createKeyManager(certStream, keyStream, UUID.randomUUID().toString().toCharArray());
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            final TinyHttpClient httpClient = TinyHttpClient.newHttpClient()
                    .withSslSocketFactory(sslContext.getSocketFactory())
                    .build();
            return new TinyK8sClient(httpClient, cluster.getServer());
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create client", e);
        }
    }

    public HttpSupport http() {
        return support;
    }

    public com.dajudge.kindcontainer.client.model.v1.Fluent v1() {
        return new com.dajudge.kindcontainer.client.model.v1.Fluent(support);
    }

    public com.dajudge.kindcontainer.client.model.apps.Fluent apps() {
        return new com.dajudge.kindcontainer.client.model.apps.Fluent(support);
    }

    public com.dajudge.kindcontainer.client.model.reflection.Fluent reflection() {
        return new com.dajudge.kindcontainer.client.model.reflection.Fluent(support);
    }
    public com.dajudge.kindcontainer.client.model.admission.Fluent admissionRegistration() {
        return new com.dajudge.kindcontainer.client.model.admission.Fluent(support);
    }
}
