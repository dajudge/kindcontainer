package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.pki.CertAuthority;
import com.dajudge.kindcontainer.pki.KeyStoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.trilead.ssh2.Connection;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class AdmissionControllerManager {
    private static final Logger LOG = LoggerFactory.getLogger(AdmissionControllerManager.class);
    private static final CertAuthority CA = new CertAuthority(System::currentTimeMillis, "CN=kindcontainer-webhooks");
    public static final KeyStoreWrapper WEBHOOK_CERTS = CA.newKeyPair("CN=webhook", singletonList(new GeneralName(GeneralName.dNSName, "localhost")));
    private final List<Webhook> webhooks = new ArrayList<>();
    private final KubernetesContainer<?> k8s;
    private final int internalWebhookPort;
    private final Supplier<DockerImageName> nginxImage;
    private int nextTunnelPort;
    private GenericContainer<?> sshd;
    private GenericContainer<?> nginx;
    private Connection ssh;

    public AdmissionControllerManager(
            final KubernetesContainer<?> k8s,
            final int basePort,
            final Supplier<DockerImageName> nginx
    ) {
        this.k8s = k8s;
        this.internalWebhookPort = basePort;
        this.nextTunnelPort = basePort + 1;
        this.nginxImage = nginx;
    }

    public String mapWebhook(final String config, final String webhook, final int localPort) {
        webhooks.add(new Webhook(config, webhook, localPort, nextTunnelPort++));
        return String.format("https://localhost:%d/webhook/%s/%s", internalWebhookPort, config, webhook);
    }

    public void start() {
        if (webhooks.isEmpty()) {
            return;
        }
        final String sshdConfig = sshdConfig();
        LOG.debug("Admission controller SSH tunnel config: {}", sshdConfig);
        sshd = new GenericContainer<>("linuxserver/openssh-server")
                .withNetworkMode("container:" + k8s.getContainerId())
                .withEnv("PASSWORD_ACCESS", "true")
                .withEnv("USER_NAME", "t0ny")
                .withEnv("USER_PASSWORD", "p3pp3r")
                .withCopyToContainer(Transferable.of(sshdConfig), "/etc/ssh/sshd_config");
        final String nginxConfig = nginxConfig();
        LOG.debug("Admission controller reverse proxy nginx config: {}", nginxConfig);
        nginx = new GenericContainer<>(nginxImage.get())
                .withNetworkMode("container:" + k8s.getContainerId())
                .withCopyToContainer(Transferable.of(WEBHOOK_CERTS.getCertificatePem()), "/tmp/server.crt")
                .withCopyToContainer(Transferable.of(WEBHOOK_CERTS.getPrivateKeyPem()), "/tmp/server.key")
                .withCopyToContainer(Transferable.of(nginxConfig), "/etc/nginx/conf.d/default.conf");
        sshd.start();
        nginx.start();

        ssh = sshConnect(k8s);
        try {
            ssh.authenticateWithPassword("t0ny", "p3pp3r");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        webhooks.forEach(webhook -> {
            try {
                LOG.debug("Tunneling admission controller: {} -> {}", webhook.tunnelPort, webhook.localPort);
                ssh.requestRemotePortForwarding("localhost", webhook.tunnelPort, "localhost", webhook.localPort);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Connection sshConnect(final KubernetesContainer<?> container) {
        return Awaitility.await().ignoreExceptions().until(() -> {
            final Connection ssh = new Connection(container.getHost(), container.getMappedPort(getExposedPort()));
            ssh.connect();
            return ssh;
        }, Objects::nonNull);
    }

    public int getExposedPort() {
        return 2222; // Default of linuxserver/openssh-server
    }

    public void stop() {
        if (!webhooks.isEmpty()) {
            ssh.close();
            nginx.stop();
            sshd.stop();
        }
    }

    private String nginxConfig() {
        final List<String> lines = new ArrayList<>(asList(
                "server {",
                "    listen " + internalWebhookPort + " ssl;",
                "    server_name localhost;",
                "    ssl_certificate /tmp/server.crt;",
                "    ssl_certificate_key /tmp/server.key;"
        ));
        webhooks.forEach(webhook -> {
            final String path = "/webhook/" + webhook.config + "/" + webhook.webhook;
            lines.addAll(asList(
                    "    location " + path + " {",
                    "        rewrite " + path + "(.*) /$1  break;",
                    "        proxy_pass http://localhost:" + webhook.tunnelPort + ";",
                    "    }"
            ));
        });
        lines.add("}");
        return String.join("\n", lines);
    }

    private static String sshdConfig() {
        return "HostKeyAlgorithms ssh-rsa\n" +
                "KexAlgorithms diffie-hellman-group1-sha1\n" +
                "PasswordAuthentication yes\n" +
                "AllowTcpForwarding yes\n";
    }

    public String getCaCertPem() {
        return CA.getCaKeyStore().getCertificatePem();
    }

    private static class Webhook {
        private final String config;
        private final String webhook;
        private final int localPort;
        private final int tunnelPort;

        private Webhook(final String config, final String webhook, final int localPort, final int tunnelPort) {
            this.config = config;
            this.webhook = webhook;
            this.localPort = localPort;
            this.tunnelPort = tunnelPort;
        }
    }
}
