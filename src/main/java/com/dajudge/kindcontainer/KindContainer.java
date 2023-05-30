package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.model.v1.Node;
import com.dajudge.kindcontainer.client.model.v1.Taint;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static com.dajudge.kindcontainer.TemplateHelpers.templateContainerFile;
import static com.dajudge.kindcontainer.TemplateHelpers.templateResource;
import static com.dajudge.kindcontainer.Utils.waitUntilNotNull;
import static com.dajudge.kindcontainer.client.KubeConfigUtils.replaceServerInKubeconfig;
import static com.github.dockerjava.api.model.AccessMode.ro;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Map.Entry.comparingByKey;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

/**
 * A Testcontainer for testing against Kubernetes using
 * <a href="https://kind.sigs.k8s.io/docs/user/quick-start/">kind</a>.
 *
 * @param <T> SELF
 */
public class KindContainer<T extends KindContainer<T>> extends KubernetesWithKubeletContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(KindContainer.class);
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;
    private static final String CONTAINTER_WORKDIR = "/kindcontainer";
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final String CACERTS_INSTALL_DIR = "/usr/local/share/ca-certificates";
    private static final Map<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "rw");
        put("/tmp", "rw");
    }};
    private static final Map<KubernetesVersionDescriptor, String> KUBEADM_CONFIGS = new HashMap<KubernetesVersionDescriptor, String>() {{
        put(new KubernetesVersionDescriptor(1, 21, 0), "kubeadm-1.21.0.yaml");
        put(new KubernetesVersionDescriptor(1, 24, 0), "kubeadm-1.24.0.yaml");
    }};
    private static final String KUBECONFIG_PATH = "/etc/kubernetes/admin.conf";
    private static final String NODE_NAME = "kind";
    private final CountDownLatch provisioningLatch = new CountDownLatch(1);
    private final KindContainerVersion version;
    private String volumeName = "kindcontainer-" + UUID.randomUUID();
    private String podSubnet = "10.244.0.0/16";
    private String serviceSubnet = "10.245.0.0/16";
    private List<Transferable> certs = new ArrayList<>();
    private int minNodePort = 30000;
    private int maxNodePort = 32767;

    /**
     * Constructs a new <code>KindContainer</code> with the latest supported Kubernetes version.
     */
    public KindContainer() {
        this(latest(KindContainerVersion.class));
    }


    /**
     * Constructs a new <code>KindContainer</code>.
     *
     * @param version the Kubernetes version to use.
     */
    public KindContainer(final KindContainerVersion version) {
        this(version.toImageSpec());
    }

    /**
     * Constructs a new <code>KindContainer</code>.
     *
     * @param imageSpec the Kubernetes image spec to use.
     */
    public KindContainer(final KubernetesImageSpec<KindContainerVersion> imageSpec) {
        super(imageSpec.getImage());
        this.version = imageSpec.getVersion();
        final StringBuffer log = new StringBuffer();
        this.withStartupTimeout(ofSeconds(300))
                .withLogConsumer(outputFrame -> {
                    if (provisioningLatch.getCount() != 0) {
                        log.append(outputFrame.getUtf8String());
                        if (log.toString().contains("Reached target Multi-User System.")) {
                            log.append(outputFrame.getUtf8String());
                            provisioningLatch.countDown();
                        }
                    }
                })
                .withCreateContainerCmdModifier(cmd -> {
                    final Volume varVolume = new Volume("/var/lib/containerd");
                    final Volume modVolume = new Volume("/lib/modules");

                    final List<Volume> volumes = new ArrayList<>(asList(cmd.getVolumes() == null ? new Volume[]{} : cmd.getVolumes()));
                    volumes.add(varVolume);
                    volumes.add(modVolume);

                    final List<Bind> binds = new ArrayList<>(asList(cmd.getBinds() == null ? new Bind[]{} : cmd.getBinds()));
                    binds.add(new Bind(volumeName, varVolume, true));
                    binds.add(new Bind("/lib/modules", modVolume, ro));

                    cmd.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init")
                            .withVolumes(volumes)
                            .withTty(true)
                            .withBinds(binds);

                })
                .withEnv("KUBECONFIG", "/etc/kubernetes/admin.conf")
                .withPrivilegedMode(true)
                .withTmpFs(TMP_FILESYSTEMS);
    }

    @Override
    public T withReuse(final boolean reuse) {
        if (reuse) {
            this.volumeName = "kindcontainer-reuse-default";
        }
        super.withReuse(reuse);
        return self();
    }

    public T withReuse(final boolean reuse, final String volumeName) {
        if (reuse) {
            this.volumeName = "kindcontainer-reuse-" + volumeName;
        }
        super.withReuse(reuse);
        return self();
    }

    @Override
    public int getInternalPort() {
        return INTERNAL_API_SERVER_PORT;
    }

    /**
     * Adds a certificate to the container's trust anchors.
     *
     * @param cert the PEM encoded certificate
     * @return <code>this</code>
     */
    public T withCaCert(final Transferable cert) {
        this.certs.add(cert);
        return self();
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo, final boolean reused) {
        if (!reused) {
            waitForProvisioningSignal();
            try {
                final Map<String, String> params = prepareTemplateParams();
                updateCaCertificates();
                kubeadmInit(params);
                installCni(params);
                installStorage();
                untaintNode();
            } catch (final Exception e) {
                throw new RuntimeException("Failed to initialize node", e);
            }
        }
        super.containerIsStarting(containerInfo, reused);
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
            put(".KubernetesVersion", version.descriptor().getKubernetesVersion());
            put(".MinNodePort", String.valueOf(minNodePort));
            put(".MaxNodePort", String.valueOf(maxNodePort));
        }};
        exec("mkdir", "-p", CONTAINTER_WORKDIR);
        return params;
    }

    private void updateCaCertificates() throws IOException, InterruptedException {
        if (certs.isEmpty()) {
            return;
        }
        for (int i = 0; i < certs.size(); i++) {
            copyFileToContainer(certs.get(i), format("%s/custom-cert-%d.crt", CACERTS_INSTALL_DIR, i));
        }
        exec(singletonList("update-ca-certificates"));
    }

    private void untaintNode() {
        client().v1().nodes().list().getItems().forEach(node -> {
            asList("master", "control-plane").forEach(role -> {
                final String key = format("node-role.kubernetes.io/%s", role);
                final String effect = "NoSchedule";
                final String removeTaint = String.format("%s:%s-", key, effect);
                if (hasTaint(node, key, null, effect)) {
                    try {
                        kubectl("taint", "node", node.getMetadata().getName(), removeTaint);
                    } catch (final IOException | InterruptedException e) {
                        throw new RuntimeException("Failed to untaint node", e);
                    }
                }
            });
        });
    }

    private boolean hasTaint(final Node node, final String key, final String value, final String effect) {
        return Optional.ofNullable(node.getSpec().getTaints()).orElse(emptyList()).stream()
                .anyMatch(t -> isTaint(t, key, value, effect));
    }

    private static boolean isTaint(final Taint t, final String key, final String value, final String effect) {
        if (!Objects.equals(t.getKey(), key)) {
            return false;
        }
        if (!Objects.equals(t.getValue(), value)) {
            return false;
        }
        return Objects.equals(t.getEffect(), effect);
    }

    private void kubeadmInit(final Map<String, String> params) throws IOException, InterruptedException {
        try {
            final String kubeadmResource = getKubeadmResource();
            final String kubeadmConfigPath = format("%s/%s", CONTAINTER_WORKDIR, kubeadmResource);
            final String kubeadmConfig = templateResource(this, kubeadmResource, params, kubeadmConfigPath);
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

    private String getKubeadmResource() {
        return KUBEADM_CONFIGS.entrySet().stream()
                .filter(v -> version.descriptor().compareTo(v.getKey()) >= 0)
                .max(comparingByKey())
                .orElseThrow(() -> new IllegalStateException(format("No kubeadm config available for Kubernetes version %s", version.descriptor())))
                .getValue();
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

    @Override
    public T withNodePortRange(final int minPort, final int maxPort) {
        this.minNodePort = minPort;
        this.maxNodePort = maxPort;
        return self();
    }
}
