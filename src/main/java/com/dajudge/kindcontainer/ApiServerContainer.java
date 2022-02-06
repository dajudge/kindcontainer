package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.KubeConfigUtils;
import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.config.*;
import com.dajudge.kindcontainer.kubecontrollermanager.KubeControllerManager;
import com.dajudge.kindcontainer.client.config.Cluster;
import com.dajudge.kindcontainer.client.config.Context;
import com.dajudge.kindcontainer.pki.CertAuthority;
import com.dajudge.kindcontainer.pki.KeyStoreWrapper;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;
import org.testcontainers.shaded.com.google.common.io.Files;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.Utils.createNetwork;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.testcontainers.utility.MountableFile.forHostPath;

public class ApiServerContainer<T extends ApiServerContainer<T>> extends KubernetesContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ApiServerContainer.class);
    private static final String PKI_BASEDIR = "/etc/kubernetes/pki";
    private static final String ETCD_PKI_BASEDIR = PKI_BASEDIR + "/etcd";
    private static final String ETCD_CLIENT_KEY = ETCD_PKI_BASEDIR + "/etcd/apiserver-client.key";
    private static final String ETCD_CLIENT_CERT = ETCD_PKI_BASEDIR + "/etcd/apiserver-client.crt";
    private static final String ETCD_CLIENT_CA = ETCD_PKI_BASEDIR + "/etcd/ca.crt";
    private static final String API_SERVER_CA = PKI_BASEDIR + "/ca.crt";
    private static final String API_SERVER_CERT = PKI_BASEDIR + "/apiserver.crt";
    private static final String API_SERVER_KEY = PKI_BASEDIR + "/apiserver.key";
    private static final String API_SERVER_PUBKEY = PKI_BASEDIR + "/apiserver.pub";
    private static final String DOCKER_BASE_PATH = "/docker";
    private static final String IP_ADDRESS_PATH = DOCKER_BASE_PATH + "/ip.txt";
    private static final String ETCD_HOSTNAME_PATH = DOCKER_BASE_PATH + "/etcd.txt";
    private static final String INTERNAL_HOSTNAME = "apiserver";
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final File tempDir = Files.createTempDir();
    private final CertAuthority apiServerCa = new CertAuthority(System::currentTimeMillis, "CN=API Server CA");
    private final EtcdContainer etcd;
    private KeyStoreWrapper apiServerKeyPair;
    private KubeControllerManager controllerManager = new KubeControllerManager();

    /**
     * Constructs a new <code>ApiServerContainer</code> with the latest supported Kubernetes version.
     */
    public ApiServerContainer() {
        this(Version.getLatest());
    }

    /**
     * Constructs a new <code>KindContainer</code>.
     *
     * @param version the Kubernetes version to run.
     */
    public ApiServerContainer(final Version version) {
        super(getDockerImage(version));
        final Network network = createNetwork();
        etcd = new EtcdContainer(network);
        this
                .withCreateContainerCmdModifier(this::createContainerCmdModifier)
                .withEnv("ETCD_CLIENT_KEY", ETCD_CLIENT_KEY)
                .withEnv("ETCD_CLIENT_CERT", ETCD_CLIENT_CERT)
                .withEnv("ETCD_CLIENT_CA", ETCD_CLIENT_CA)
                .withEnv("API_SERVER_CA", API_SERVER_CA)
                .withEnv("API_SERVER_CERT", API_SERVER_CERT)
                .withEnv("API_SERVER_KEY", API_SERVER_KEY)
                .withEnv("API_SERVER_PUBKEY", API_SERVER_PUBKEY)
                .withEnv("IP_ADDRESS_PATH", IP_ADDRESS_PATH)
                .withEnv("ETCD_HOSTNAME_PATH", ETCD_HOSTNAME_PATH)
                .withNetwork(network)
                .withNetworkAliases(INTERNAL_HOSTNAME);
    }

    private static DockerImageName getDockerImage(final Version version) {
        return DockerImageName.parse(format("k8s.gcr.io/kube-apiserver:%s", version.descriptor.getKubernetesVersion()));
    }

    @Override
    public String getInternalHostname() {
        return INTERNAL_HOSTNAME;
    }

    @Override
    public int getInternalPort() {
        return INTERNAL_API_SERVER_PORT;
    }

    private void createContainerCmdModifier(final CreateContainerCmd cmd) {
        cmd.withEntrypoint();
        final List<String> params = new HashMap<String, String>() {{
            put("advertise-address", Utils.resolve(getHost()));
            put("allow-privileged", "true");
            put("authorization-mode", "Node,RBAC");
            put("enable-admission-plugins", "NodeRestriction");
            put("enable-bootstrap-token-auth", "true");
            put("client-ca-file", API_SERVER_CA);
            put("tls-cert-file", API_SERVER_CERT);
            put("tls-private-key-file", API_SERVER_KEY);
            put("kubelet-client-certificate", API_SERVER_CERT);
            put("kubelet-client-key", API_SERVER_KEY);
            put("proxy-client-key-file", API_SERVER_KEY);
            put("proxy-client-cert-file", API_SERVER_CERT);
            put("etcd-cafile", ETCD_CLIENT_CA);
            put("etcd-certfile", ETCD_CLIENT_CERT);
            put("etcd-keyfile", ETCD_CLIENT_KEY);
            put("etcd-servers", format("https://%s:2379", etcd.getEtcdIpAddress()));
            put("service-account-key-file", API_SERVER_PUBKEY);
            put("service-account-signing-key-file", API_SERVER_KEY);
            put("service-account-issuer", "https://kubernetes.default.svc.cluster.local");
            put("kubelet-preferred-address-types", "InternalIP,ExternalIP,Hostname");
            put("requestheader-allowed-names", "front-proxy-client");
            put("requestheader-client-ca-file", API_SERVER_CA);
            put("requestheader-extra-headers-prefix", "X-Remote-Extra-");
            put("requestheader-group-headers", "X-Remote-Group");
            put("requestheader-username-headers", "X-Remote-User");
            put("runtime-config", "");
            put("secure-port", String.format("%d", INTERNAL_API_SERVER_PORT));
            put("service-cluster-ip-range", "10.96.0.0/16");
        }}.entrySet().stream()
                .map(e -> format("--%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        final ArrayList<String> cmdLine = new ArrayList<>();
        cmdLine.add("kube-apiserver");
        cmdLine.addAll(params);
        cmd.withCmd(cmdLine.toArray(new String[]{}));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        waitForApiServer();
        super.containerIsStarting(containerInfo);
    }

    private void waitForApiServer() {
        LOG.info("Waiting for API server...");
        Awaitility.await()
                .pollInSameThread()
                .pollInterval(ofMillis(100))
                .pollDelay(ZERO)
                .ignoreExceptions()
                .forever()
                .until(() -> null != TinyK8sClient.fromKubeconfig(getExternalKubeconfig()).v1().nodes().list());
    }

    @Override
    public void start() {
        try {
            etcd.start();
            writeCertificates();
            super.start();
            if (controllerManager != null) {
                controllerManager.start(client());
            }
        } catch (final RuntimeException e) {
            etcd.close();
            throw e;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Disables the (active by default) fake controller manager.
     *
     * @return this
     */
    public T withoutControllerManager() {
        controllerManager = null;
        return self();
    }

    private KeyStoreWrapper writeCertificates() throws IOException {
        apiServerKeyPair = apiServerCa.newKeyPair("O=system:masters,CN=kubernetes-admin", asList(
                new GeneralName(GeneralName.iPAddress, Utils.resolve(getHost())),
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                new GeneralName(GeneralName.dNSName, INTERNAL_HOSTNAME)
        ));
        final KeyStoreWrapper etcdClientKeyPair = etcd.newClientKeypair("CN=API Server");
        final Path apiServerCert = writeTempFile("apiServer.crt", apiServerKeyPair.getCertificatePem());
        final Path apiServerKey = writeTempFile("apiServer.key", apiServerKeyPair.getPrivateKeyPem());
        final Path apiServerPubkey = writeTempFile("apiServer.pub", apiServerKeyPair.getPublicKeyPem());
        final Path apiServerCaCert = writeTempFile("apiServer.ca.crt", apiServerCa.getCaKeyStore().getCertificatePem());
        final Path etcdCert = writeTempFile("etcd.crt", etcdClientKeyPair.getCertificatePem());
        final Path etcdKey = writeTempFile("etcd.key", etcdClientKeyPair.getPrivateKeyPem());
        final Path etcdCaCert = writeTempFile("etcd.ca.crt", etcd.getCaCertificatePem());

        withCopyFileToContainer(forHostPath(apiServerCert), API_SERVER_CERT);
        withCopyFileToContainer(forHostPath(apiServerKey), API_SERVER_KEY);
        withCopyFileToContainer(forHostPath(apiServerCaCert), API_SERVER_CA);
        withCopyFileToContainer(forHostPath(apiServerPubkey), API_SERVER_PUBKEY);
        withCopyFileToContainer(forHostPath(etcdCert), ETCD_CLIENT_CERT);
        withCopyFileToContainer(forHostPath(etcdKey), ETCD_CLIENT_KEY);
        withCopyFileToContainer(forHostPath(etcdCaCert), ETCD_CLIENT_CA);
        return apiServerKeyPair;
    }

    private Path writeTempFile(final String filename, final String data) throws IOException {
        final File file = new File(tempDir, filename);
        Files.write(data.getBytes(US_ASCII), file);
        return file.toPath();
    }

    private String getKubeconfig(final String url) {
        final Cluster cluster = new Cluster();
        cluster.setName("apiserver");
        cluster.setCluster(new ClusterSpec());
        cluster.getCluster().setServer(url);
        cluster.getCluster().setCertificateAuthorityData((base64(apiServerCa.getCaKeyStore().getCertificatePem())));
        final User user = new User();
        user.setName("apiserver");
        user.setUser(new UserSpec());
        user.getUser().setClientKeyData(base64(apiServerKeyPair.getPrivateKeyPem()));
        user.getUser().setClientCertificateData(base64(apiServerKeyPair.getCertificatePem()));
        final Context context = new Context();
        context.setName("apiserver");
        context.setContext(new ContextSpec());
        context.getContext().setCluster(cluster.getName());
        context.getContext().setUser(user.getName());
        final KubeConfig config = new KubeConfig();
        config.setUsers(singletonList(user));
        config.setClusters(singletonList(cluster));
        config.setContexts(singletonList(context));
        config.setCurrentContext(context.getName());
        return KubeConfigUtils.serializeKubeConfig(config);
    }


    private String base64(final String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(US_ASCII));
    }

    @Override
    public String getInternalKubeconfig() {
        return getKubeconfig(format("https://%s:%d", INTERNAL_HOSTNAME, INTERNAL_API_SERVER_PORT));
    }

    @Override
    public String getExternalKubeconfig() {
        return getKubeconfig(format("https://%s:%d", getHost(), getMappedPort(INTERNAL_API_SERVER_PORT)));
    }

    @Override
    public void stop() {
        try {
            if (controllerManager != null) {
                controllerManager.close();
            }
        } finally {
            LOG.info("Stopping {}", ApiServerContainer.class.getSimpleName());
            super.stop();
            etcd.stop();
        }
    }

    /**
     * The available Kubernetes versions.
     */
    public enum Version {
        VERSION_1_21_2(new KubernetesVersionDescriptor(1, 21, 2)),
        VERSION_1_22_4(new KubernetesVersionDescriptor(1, 22, 4)),
        VERSION_1_23_4(new KubernetesVersionDescriptor(1, 23, 3));

        private static final Comparator<Version> COMPARE_ASCENDING = comparing(a -> a.descriptor);
        private static final Comparator<Version> COMPARE_DESCENDING = COMPARE_ASCENDING.reversed();
        @VisibleForTesting
        final KubernetesVersionDescriptor descriptor;

        Version(final KubernetesVersionDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        /**
         * Returns the latest supported version.
         *
         * @return the latest supported version.
         */
        public static Version getLatest() {
            return descending().get(0);
        }

        /**
         * Returns the list of available versions in descending order (latest is first).
         *
         * @return the list of available versions in descending order (latest is first).
         */
        public static List<Version> descending() {
            return Stream.of(Version.values())
                    .sorted(COMPARE_DESCENDING)
                    .collect(toList());
        }


        @Override
        public String toString() {
            return format("%d.%d.%d", descriptor.getMajor(), descriptor.getMinor(), descriptor.getPatch());
        }
    }
}
