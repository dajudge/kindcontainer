package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.pki.CertAuthority;
import com.dajudge.kindcontainer.pki.KeyStoreWrapper;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

class EtcdContainer extends BaseGenericContainer<EtcdContainer> {
    private static final String DOCKER_BASE_PATH = "/docker";
    private static final String SERVER_CERT_PATH = DOCKER_BASE_PATH + "/server.crt";
    private static final String SERVER_KEY_PATH = DOCKER_BASE_PATH + "/server.key";
    private static final String SERVER_CACERTS_PATH = DOCKER_BASE_PATH + "/ca.crt";
    private static final String STARTUP_SIGNAL_PATH = DOCKER_BASE_PATH + "/startup";
    private static final String[] CMD = buildCommand();

    EtcdContainer(final DockerImageName image, final CertAuthority etcdCa, final String targetContainerId) {
        super(image);
        final KeyStoreWrapper etcdKeypair = etcdCa.newKeyPair(
                "CN=etcd",
                singletonList(new GeneralName(GeneralName.dNSName, "localhost"))
        );
        this
                .withNetworkAliases("etcd")
                .withNetworkMode("container:" + targetContainerId)
                .withEnv("STARTUP_SIGNAL", STARTUP_SIGNAL_PATH)
                .withEnv("SERVER_CERT_PATH", SERVER_CERT_PATH)
                .withEnv("SERVER_KEY_PATH", SERVER_KEY_PATH)
                .withEnv("SERVER_CACERTS_PATH", SERVER_CACERTS_PATH)
                .waitingFor(new WaitForPortsExternallyStrategy())
                .withCommand(CMD)
                .withCopyAsciiToContainer(etcdKeypair.getCertificatePem(), SERVER_CERT_PATH)
                .withCopyAsciiToContainer(etcdKeypair.getPrivateKeyPem(), SERVER_KEY_PATH)
                .withCopyAsciiToContainer(etcdCa.getCaKeyStore().getCertificatePem(), SERVER_CACERTS_PATH);
    }

    private static String[] buildCommand() {
        final Map<String, String> params = new HashMap<String, String>() {{
            put("advertise-client-urls", "https://localhost:2379");
            put("cert-file", SERVER_CERT_PATH);
            put("key-file", SERVER_KEY_PATH);
            put("trusted-ca-file", SERVER_CACERTS_PATH);
            put("peer-cert-file", SERVER_CERT_PATH);
            put("peer-key-file", SERVER_KEY_PATH);
            put("peer-trusted-ca-file", SERVER_CACERTS_PATH);
            put("peer-client-cert-auth", "true");
            put("client-cert-auth", "true");
            put("data-dir", "/var/lib/etcd");
            put("initial-advertise-peer-urls", "https://localhost:2380");
            put("initial-cluster", "control-plane=https://localhost:2380");
            put("listen-client-urls", "https://localhost:2379");
            put("listen-metrics-urls", "http://localhost:2381");
            put("listen-peer-urls", "https://localhost:2380");
            put("name", "control-plane");
            put("snapshot-count", "10000");
        }};
        final List<String> args = new ArrayList<>(singletonList("etcd"));
        args.addAll(params.entrySet().stream().map(e -> format("--%s=%s", e.getKey(), e.getValue())).collect(toList()));
        return args.toArray(new String[0]);
    }
}
