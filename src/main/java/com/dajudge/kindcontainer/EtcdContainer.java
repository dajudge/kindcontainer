package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.pki.CertAuthority;
import com.dajudge.kindcontainer.pki.KeyStoreWrapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dajudge.kindcontainer.Utils.writeAsciiFile;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

class EtcdContainer extends GenericContainer<EtcdContainer> {
    private static final String DOCKER_BASE_PATH = "/docker";
    private static final String ENTRYPOINT_PATH = DOCKER_BASE_PATH + "/entrypoint-etcd.sh";
    private static final String SERVER_CERT_PATH = DOCKER_BASE_PATH + "/server.crt";
    private static final String SERVER_KEY_PATH = DOCKER_BASE_PATH + "/server.key";
    private static final String SERVER_CACERTS_PATH = DOCKER_BASE_PATH + "/ca.crt";
    private static final String STARTUP_SIGNAL_PATH = DOCKER_BASE_PATH + "/startup";
    private static final String ETCD_IMAGE = "k8s.gcr.io/etcd:3.4.13-0";
    private static final String[] CMD = buildCommand();
    private final CertAuthority etcdCa;

    EtcdContainer(final CertAuthority etcdCa, final String targetContainerId) {
        super(ETCD_IMAGE);
        this.etcdCa = etcdCa;
        this
                .withNetworkAliases("etcd")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint(ENTRYPOINT_PATH);
                    cmd.withCmd(CMD);
                })
                .withNetworkMode("container:" + targetContainerId)
                .withEnv("STARTUP_SIGNAL", STARTUP_SIGNAL_PATH)
                .withEnv("SERVER_CERT_PATH", SERVER_CERT_PATH)
                .withEnv("SERVER_KEY_PATH", SERVER_KEY_PATH)
                .withEnv("SERVER_CACERTS_PATH", SERVER_CACERTS_PATH)
                .withCopyFileToContainer(forClasspathResource("scripts/entrypoint-etcd.sh", 755), ENTRYPOINT_PATH)
                .waitingFor(new WaitForPortsExternallyStrategy())
                .withCommand(CMD);
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

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        final KeyStoreWrapper etcdKeypair = etcdCa.newKeyPair(
                "CN=etcd",
                singletonList(new GeneralName(GeneralName.dNSName, "localhost"))
        );
        writeAsciiFile(this, etcdKeypair.getCertificatePem(), SERVER_CERT_PATH);
        writeAsciiFile(this, etcdKeypair.getPrivateKeyPem(), SERVER_KEY_PATH);
        writeAsciiFile(this, etcdCa.getCaKeyStore().getCertificatePem(), SERVER_CACERTS_PATH);
        writeAsciiFile(this, "go go go!", STARTUP_SIGNAL_PATH);
    }
}
