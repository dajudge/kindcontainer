package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.model.v1.Node;
import com.dajudge.kindcontainer.client.model.v1.NodeCondition;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.TemplateHelpers.*;
import static com.dajudge.kindcontainer.Utils.waitUntilNotNull;
import static com.dajudge.kindcontainer.client.KubeConfigUtils.replaceServerInKubeconfig;
import static com.github.dockerjava.api.model.AccessMode.ro;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * A Testcontainer for testing against Kubernetes using
 * <a href="https://kind.sigs.k8s.io/docs/user/quick-start/">kind</a>.
 *
 * @param <T> SELF
 */
public class KindContainer<T extends KindContainer<T>> extends KubernetesContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(KindContainer.class);
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;
    private static final String CONTAINTER_WORKDIR = "/kindcontainer";
    private static final String KUBEADM_CONFIG = CONTAINTER_WORKDIR + "/kubeadmConfig.yaml";
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final String CACERTS_INSTALL_DIR = "/usr/local/share/ca-certificates";
    private static final Pattern PROVISIONING_TRIGGER_PATTERN = Pattern.compile(".*Reached target .*Multi-User System.*");
    private static final HashMap<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "rw");
        put("/tmp", "rw");
    }};
    private static final String KUBECONFIG_PATH = "/etc/kubernetes/admin.conf";
    private static final String NODE_NAME = "kind";
    private final CountDownLatch provisioningLatch = new CountDownLatch(1);
    private final String volumeName = "kindcontainer-" + UUID.randomUUID();
    private final Version version;
    private String podSubnet = "10.244.0.0/16";
    private String serviceSubnet = "10.245.0.0/16";
    private List<String> certs = emptyList();
    private Duration startupTimeout = Duration.ofSeconds(300);

    /**
     * Constructs a new <code>KindContainer</code> with the latest supported Kubernetes version.
     */
    public KindContainer() {
        this(Version.getLatest());
    }

    /**
     * Constructs a new <code>KindContainer</code>.
     *
     * @param version the Kubernetes version to run.
     */
    public KindContainer(final Version version) {
        super(getDockerImage(version));
        this.version = version;
        this.withStartupTimeout(ofSeconds(300))
                .withLogConsumer(outputFrame -> {
                    if (PROVISIONING_TRIGGER_PATTERN.matcher(outputFrame.getUtf8String()).matches()) {
                        provisioningLatch.countDown();
                    }
                })
                .withCreateContainerCmdModifier(cmd -> {
                    final Volume varVolume = new Volume("/var/lib/containerd");
                    final Volume modVolume = new Volume("/lib/modules");
                    cmd.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init")
                            .withVolumes(varVolume)
                            .withTty(true)
                            .withBinds(
                                    new Bind(volumeName, varVolume, true),
                                    new Bind("/lib/modules", modVolume, ro)
                            );

                })
                .withEnv("KUBECONFIG", "/etc/kubernetes/admin.conf")
                .withPrivilegedMode(true)
                .withTmpFs(TMP_FILESYSTEMS);

    }

    private static DockerImageName getDockerImage(final Version version) {
        return DockerImageName.parse(format("kindest/node:%s", version.descriptor.getKubernetesVersion()));
    }

    @Override
    public int getInternalPort() {
        return INTERNAL_API_SERVER_PORT;
    }

    /**
     * Sets the timeout applied when waiting for the Kubernetes node to become ready.
     *
     * @param startupTimeout the timeout
     * @return <code>this</code>
     */
    public T withNodeReadyTimeout(final Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
        return self();
    }

    /**
     * Adds certificates to the container's trust anchors.
     *
     * @param certs the PEM encoded certificates
     * @return <code>this</code>
     */
    public T withCaCerts(final Collection<String> certs) {
        this.certs = new ArrayList<>(certs);
        return self();
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        waitForProvisioningSignal();
        try {
            final Map<String, String> params = prepareTemplateParams();
            updateCaCertificates();
            kubeadmInit(params);
            installCni(params);
            installStorage();
            untaintMasterNode();
            waitForNodeReady();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize node", e);
        }
        super.containerIsStarting(containerInfo);
    }

    private void waitForProvisioningSignal() {
        try {
            // https://github.com/kubernetes-sigs/kind/pull/2421
            if (!provisioningLatch.await(60, SECONDS)) {
                throw new IllegalStateException("Container init does not seem to have started.");
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for provisioning signal.");
        }
    }

    private Map<String, String> prepareTemplateParams() throws IOException, InterruptedException {
        final String containerInternalIpAddress = getInternalIpAddress(this);
        LOG.info("Container internal IP address: {}", containerInternalIpAddress);
        LOG.info("Container external IP address: {}", getContainerIpAddress());
        final Set<String> subjectAlternativeNames = new HashSet<>(asList(
                containerInternalIpAddress,
                "127.0.0.1",
                "localhost",
                getContainerIpAddress()
        ));
        LOG.debug("SANs for Kube-API server certificate: {}", subjectAlternativeNames);
        final Map<String, String> params = new HashMap<String, String>() {{
            put(".NodeIp", containerInternalIpAddress);
            put(".PodSubnet", podSubnet);
            put(".ServiceSubnet", serviceSubnet);
            put(".CertSANs", subjectAlternativeNames.stream().map(san -> "\"" + san + "\"").collect(joining(",")));
            put(".KubernetesVersion", version.descriptor.getKubernetesVersion());
        }};
        exec("mkdir", "-p", CONTAINTER_WORKDIR);
        return params;
    }

    private void updateCaCertificates() throws IOException, InterruptedException {
        if (certs.isEmpty()) {
            return;
        }
        for (int i = 0; i < certs.size(); i++) {
            writeContainerFile(
                    this,
                    certs.get(i),
                    format("%s/custom-cert-%d.crt", CACERTS_INSTALL_DIR, i)
            );
        }
        exec(singletonList("update-ca-certificates"));
    }

    private void untaintMasterNode() throws IOException, InterruptedException {
        kubectl("taint", "node", NODE_NAME, "node-role.kubernetes.io/master:NoSchedule-");
    }

    private void kubeadmInit(final Map<String, String> params) throws IOException, InterruptedException {
        try {
            final String kubeadmConfig = templateResource(this, "kubeadm.yaml", params, KUBEADM_CONFIG);
            exec(asList(
                    "kubeadm", "init",
                    "--skip-phases=preflight",
                    // specify our generated config file
                    "--config=" + kubeadmConfig,
                    "--skip-token-print",
                    // Use predetermined node name
                    "--node-name=" + NODE_NAME,
                    // increase verbosity for debugging
                    "--v=6"
            ));
        } catch (final RuntimeException | IOException | InterruptedException e) {
            try {
                LOG.error("{}", Utils.prefixLines(execInContainer("journalctl").getStdout(), "JOURNAL: "));
            } catch (final IOException | InterruptedException ex) {
                LOG.error("Could not retrieve journal.", ex);
            }
            throw e;
        }
    }

    private void installStorage() throws IOException, InterruptedException {
        kubectl("apply", "-f", "/kind/manifests/default-storage.yaml");
    }

    private void installCni(final Map<String, String> params) throws IOException, InterruptedException {
        final String cniManifest = templateContainerFile(
                this,
                "/kind/manifests/default-cni.yaml",
                CONTAINTER_WORKDIR + "/cni.yaml",
                params
        );
        kubectl("apply", "-f", cniManifest);
    }

    private void kubectl(
            final String... params
    ) throws IOException, InterruptedException {
        final List<String> exec = new ArrayList<>(singletonList("kubectl"));
        exec.addAll(asList(params));
        exec(exec);
    }

    private void exec(final String... exec) throws IOException, InterruptedException {
        exec(asList(exec));
    }

    private void exec(final List<String> exec) throws IOException, InterruptedException {
        final String cmdString = join(" ", exec);
        LOG.info("Executing command: {}", cmdString);
        final ExecResult execResult = execInContainer(exec.toArray(new String[0]));
        final int exitCode = execResult.getExitCode();
        if (exitCode == 0) {
            LOG.debug("\"{}\" exited with status code {}", cmdString, exitCode);
            LOG.debug("{}", Utils.prefixLines(execResult.getStdout(), "STDOUT: "));
            LOG.debug("{}", Utils.prefixLines(execResult.getStderr(), "STDERR: "));
        } else {
            LOG.error("\"{}\" exited with status code {}", cmdString, exitCode);
            LOG.error("{}", Utils.prefixLines(execResult.getStdout(), "STDOUT: "));
            LOG.error("{}", Utils.prefixLines(execResult.getStderr(), "STDERR: "));
            throw new IllegalStateException(cmdString + " exited with status code " + execResult);
        }
    }

    private static String getInternalIpAddress(final KindContainer<?> container) {
        return waitUntilNotNull(() -> {
                    final Map<String, ContainerNetwork> networks = container.getContainerInfo()
                            .getNetworkSettings().getNetworks();
                    if (networks.isEmpty()) {
                        return null;
                    }
                    return networks.entrySet().iterator().next().getValue().getIpAddress();
                },
                CONTAINER_IP_TIMEOUT_MSECS,
                "Waiting for network to receive internal IP address...",
                () -> new IllegalStateException("Failed to determine internal IP address")
        );
    }

    @Override
    protected String getKubeconfig(final String server) {
        return replaceServerInKubeconfig(server, copyFileFromContainer(KUBECONFIG_PATH, Utils::readString));
    }

    @Override
    public void start() {
        createVolumes();
        super.start();
    }

    private void createVolumes() {
        dockerClient.createVolumeCmd()
                .withName(volumeName)
                .withLabels(DockerClientFactory.DEFAULT_LABELS)
                .exec();
        LOG.debug("Created volume: {}", volumeName);
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            try {
                dockerClient.removeVolumeCmd(volumeName).exec();
            } catch (final Exception e) {
                LOG.warn("Failed to remove volume: {}", volumeName, e);
            }
        }
    }

    private void waitForNodeReady() {
        LOG.info("Waiting for a node to become ready...");
        final Node readyNode = Awaitility.await("Ready node")
                .pollInSameThread()
                .pollDelay(0, SECONDS)
                .pollInterval(100, MILLISECONDS)
                .ignoreExceptions()
                .timeout(startupTimeout)
                .until(this::findReadyNode, Objects::nonNull);
        LOG.info("Node ready: {}", readyNode.getMetadata().getName());
    }

    private Node findReadyNode() {
        final Predicate<NodeCondition> isReadyStatus = cond ->
                "Ready".equals(cond.getType()) && "True".equals(cond.getStatus());
        final Predicate<Node> nodeIsReady = node -> node.getStatus().getConditions().stream()
                .anyMatch(isReadyStatus);
        final TinyK8sClient client = client();
        try {
            return client.v1().nodes().list().getItems().stream()
                    .peek(it -> LOG.trace("{} -> {}", it.getMetadata().getName(), it.getStatus().getConditions()))
                    .filter(nodeIsReady)
                    .findAny()
                    .orElse(null);
        } catch (final Exception e) {
            LOG.info("Failed to list ready nodes", e);
            return null;
        }
    }

    /**
     * Sets the pod subnet.
     *
     * @param cidr the subnet CIDR in the <code>0.0.0.0/0</code> format
     * @return <code>this</code>
     */
    public T withPodSubnet(final String cidr) {
        podSubnet = cidr;
        return self();
    }

    /**
     * Sets the service subnet.
     *
     * @param cidr the subnet CIDR in the <code>0.0.0.0/0</code> format
     * @return <code>this</code>
     */
    public T withServiceSubnet(final String cidr) {
        serviceSubnet = cidr;
        return self();
    }

    /**
     * The available Kubernetes versions.
     */
    public enum Version {
        VERSION_1_21_2(new KubernetesVersionDescriptor(1, 21, 2)),
        VERSION_1_22_4(new KubernetesVersionDescriptor(1, 22, 4)),
        VERSION_1_22_5(new KubernetesVersionDescriptor(1, 22, 5)),
        VERSION_1_23_3(new KubernetesVersionDescriptor(1, 23, 3));

        private static final Comparator<Version> COMPARE_ASCENDING = comparing(a -> a.descriptor);
        private static final Comparator<Version> COMPARE_DESCENDING = COMPARE_ASCENDING.reversed();
        @VisibleForTesting
        private final KubernetesVersionDescriptor descriptor;

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

        public KubernetesVersionDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public String toString() {
            return format("%d.%d.%d", descriptor.getMajor(), descriptor.getMinor(), descriptor.getPatch());
        }
    }
}
