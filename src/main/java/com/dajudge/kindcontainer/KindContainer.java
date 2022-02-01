/*
Copyright 2020-2022 Alex Stockinger

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
package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import com.dajudge.kindcontainer.Utils.ThrowingFunction;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Volume;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.TemplateHelpers.*;
import static com.dajudge.kindcontainer.Utils.createNetwork;
import static com.dajudge.kindcontainer.Utils.waitUntilNotNull;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.testcontainers.containers.BindMode.READ_ONLY;

/**
 * A Testcontainer for testing against Kubernetes using
 * <a href="https://kind.sigs.k8s.io/docs/user/quick-start/">kind</a>.
 *
 * @param <T> SELF
 */
public class KindContainer<T extends KindContainer<T>> extends KubernetesContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(KindContainer.class);
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;
    private static final Yaml YAML = new Yaml();
    private static final String CONTAINTER_WORKDIR = "/kindcontainer";
    private static final String KUBEADM_CONFIG = CONTAINTER_WORKDIR + "/kubeadmConfig.yaml";
    private static final String INTERNAL_HOSTNAME = "kindcontainer";
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final String CACERTS_INSTALL_DIR = "/usr/local/share/ca-certificates";
    private final String volumeName = "kindcontainer-" + UUID.randomUUID().toString();
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
        final Network.NetworkImpl network = createNetwork();
        this.withStartupTimeout(ofSeconds(300))
                .withCreateContainerCmdModifier(cmd -> {
                    final Volume varVolume = new Volume("/var/lib/containerd");
                    cmd.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init")
                            .withVolumes(varVolume)
                            .withBinds(new Bind(volumeName, varVolume, true));

                })
                .withEnv("KUBECONFIG", "/etc/kubernetes/admin.conf")
                .withPrivilegedMode(true)
                .withFileSystemBind("/lib/modules", "/lib/modules", READ_ONLY)
                .withTmpFs(new HashMap<String, String>() {{
                    put("/run", "rw");
                    put("/tmp", "rw");
                }})
                .withNetwork(network)
                .withNetworkAliases(INTERNAL_HOSTNAME);

    }

    private static DockerImageName getDockerImage(final Version version) {
        return DockerImageName.parse(format("kindest/node:%s", version.descriptor.getKubernetesVersion()));
    }

    @Override
    public String getInternalHostname() {
        return INTERNAL_HOSTNAME;
    }

    @Override
    public int getInternalPort() {
        return INTERNAL_API_SERVER_PORT;
    }

    @Override
    public String getInternalKubeconfig() {
        try {
            final Config config = KubeConfigUtils.parseConfigFromString(getExternalKubeconfig());
            final String url = format("https://%s:%d", INTERNAL_HOSTNAME, INTERNAL_API_SERVER_PORT);
            config.getClusters().get(0).getCluster().setServer(url);
            return Serialization.yamlMapper().writeValueAsString(config);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to serialize kubeconfig");
        }
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

    private Map<String, String> prepareTemplateParams() throws IOException, InterruptedException {
        final String containerInternalIpAddress = getInternalIpAddress(this);
        LOG.info("Container internal IP address: {}", containerInternalIpAddress);
        LOG.info("Container external IP address: {}", getContainerIpAddress());
        final Set<String> subjectAlternativeNames = new HashSet<>(asList(
                containerInternalIpAddress,
                "127.0.0.1",
                "localhost",
                getContainerIpAddress(),
                INTERNAL_HOSTNAME
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
        kubectl("taint", "node", INTERNAL_HOSTNAME, "node-role.kubernetes.io/master:NoSchedule-");
    }

    private void kubeadmInit(final Map<String, String> params) throws IOException, InterruptedException {
        final String kubeadmConfig = templateResource(this, "kubeadm.yaml", params, KUBEADM_CONFIG);
        exec(asList(
                "kubeadm", "init",
                "--skip-phases=preflight",
                // specify our generated config file
                "--config=" + kubeadmConfig,
                "--skip-token-print",
                // Use predetermined node name
                "--node-name=" + INTERNAL_HOSTNAME,
                // increase verbosity for debugging
                "--v=6"
        ));
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
            LOG.debug("{}", execResult.getStdout().replaceAll("(?m)^", "STDOUT: "));
            LOG.debug("{}", execResult.getStderr().replaceAll("(?m)^", "STDERR: "));
        } else {
            LOG.error("\"{}\" exited with status code {}", cmdString, exitCode);
            LOG.error("{}", execResult.getStdout().replaceAll("(?m)^", "STDOUT: "));
            LOG.error("{}", execResult.getStderr().replaceAll("(?m)^", "STDERR: "));
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
    public DefaultKubernetesClient getClient() {
        return client(getExternalKubeconfig());
    }

    private DefaultKubernetesClient client(final String kubeconfig) {
        return new DefaultKubernetesClient(fromKubeconfig(kubeconfig));
    }

    private String kubeconfig;

    @Override
    public synchronized String getExternalKubeconfig() {
        if (kubeconfig == null) {
            kubeconfig = patchKubeConfig(copyFileFromContainer(
                    "/etc/kubernetes/admin.conf",
                    Utils::readString
            ));
        }
        return kubeconfig;
    }

    @SuppressWarnings("unchecked")
    private String patchKubeConfig(final String kubeConfig) {
        final Map<String, Object> kubeConfigMap = YAML.load(kubeConfig);
        final List<Map<String, Object>> clusters = (List<Map<String, Object>>) kubeConfigMap.get("clusters");
        final Map<String, Object> firstCluster = clusters.iterator().next();
        final Map<String, Object> cluster = (Map<String, Object>) firstCluster.get("cluster");
        final String newServerEndpoint = format("https://%s:%s", getHost(), getMappedPort(INTERNAL_API_SERVER_PORT));
        final String server = cluster.get("server").toString();
        LOG.debug("Creating kubeconfig with server {} instead of {}", newServerEndpoint, server);
        cluster.put("server", newServerEndpoint);
        return YAML.dump(kubeConfigMap);
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
        final Node readyNode = waitUntilNotNull(
                findReadyNode(),
                startupTimeout.toMillis(),
                "Waiting for a node to become ready...",
                () -> {
                    dumpDebuggingInfo();
                    return new IllegalStateException("No node became ready");
                }
        );
        LOG.info("Node ready: {}", readyNode.getMetadata().getName());
    }

    private void dumpDebuggingInfo() {
        runWithClient(client -> {
            client.nodes().list().getItems().forEach(it -> LOG.info("{}", it));
            client.pods().list().getItems().forEach(it -> LOG.info("{}", it));
            client.pods().list().getItems().stream()
                    .filter(it -> it.getMetadata().getName().startsWith("kindnet-"))
                    .forEach(it -> {
                        final String podLog = client.pods()
                                .inNamespace(it.getMetadata().getNamespace())
                                .withName(it.getMetadata().getName()).getLog();
                        LOG.info("{}/{}:\n{}", it.getMetadata().getNamespace(), it.getMetadata().getName(), podLog);
                    });
        });
    }

    private Supplier<Node> findReadyNode() {
        final Predicate<NodeCondition> isReadyStatus = cond ->
                "Ready".equals(cond.getType()) && "True".equals(cond.getStatus());
        final Predicate<Node> nodeIsReady = node -> node.getStatus().getConditions().stream()
                .anyMatch(isReadyStatus);
        return () -> runWithClient(client -> {
            try {
                return client.nodes().list().getItems().stream()
                        .peek(it -> LOG.trace("{} -> {}", it.getMetadata().getName(), it.getStatus().getConditions()))
                        .filter(nodeIsReady)
                        .findAny()
                        .orElse(null);
            } catch (final KubernetesClientException e) {
                LOG.debug("Failed to list ready nodes", e);
                return null;
            }
        });
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

    /**
     * Provides a <code>DefaultKubernetesClient</code> for the specified <code>ServiceAccount</code> to a piece of code.
     *
     * @param serviceAccountNamespace the namespace of the <code>ServiceAccount</code> to impersonate
     * @param serviceAccountName      the name of the <code>ServiceAccount</code> to impersonate
     * @param consumer                the <code>ThrowingConsumer</code> to execute
     */
    public void runWithClient(
            final String serviceAccountNamespace,
            final String serviceAccountName,
            final ThrowingConsumer<DefaultKubernetesClient, Exception> consumer
    ) {
        runWithClient(serviceAccountNamespace, serviceAccountName, client -> {
            consumer.accept(client);
            return null;
        });
    }

    /**
     * Provides a <code>DefaultKubernetesClient</code> for the specified <code>ServiceAccount</code> to a piece of code.
     *
     * @param serviceAccountNamespace the namespace of the <code>ServiceAccount</code> to impersonate
     * @param serviceAccountName      the name of the <code>ServiceAccount</code> to impersonate
     * @param function                the <code>ThrowingFunction</code> to execute
     * @param <R>                     the return type
     * @return the return value of the function
     */
    public <R> R runWithClient(
            final String serviceAccountNamespace,
            final String serviceAccountName,
            final ThrowingFunction<DefaultKubernetesClient, R, Exception> function
    ) {
        try (final DefaultKubernetesClient client = getClient(serviceAccountNamespace, serviceAccountName)) {
            try {
                return function.apply(client);
            } catch (final Exception e) {
                throw new RuntimeException("Error running with client", e);
            }
        }
    }

    /**
     * Returns a fabric8 Kubernetes client for a given <code>ServiceAccount</code>.
     *
     * @param serviceAccountNamespace the namespace of the <code>ServiceAccount</code> to impersonate
     * @param serviceAccountName      the name of the <code>ServiceAccount</code> to impersonate
     * @return a <code>DefaultKubernetesClient</code> for the specified service account
     */
    public DefaultKubernetesClient getClient(final String serviceAccountNamespace, final String serviceAccountName) {
        try (final DefaultKubernetesClient client = getClient()) {
            final String token = getServiceAccountToken(serviceAccountNamespace, serviceAccountName, client);
            return impersonate(token);
        }
    }

    private DefaultKubernetesClient impersonate(final String token) {
        try {
            final Config kubeconfig = KubeConfigUtils.parseConfigFromString(getExternalKubeconfig());
            kubeconfig.getUsers().get(0).setUser(new AuthInfoBuilder()
                    .withToken(token)
                    .build());
            final String newKubeconfig = Serialization.yamlMapper().writeValueAsString(kubeconfig);
            return new DefaultKubernetesClient(fromKubeconfig(newKubeconfig));
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to create kubeconfig for ServiceAccount", e);
        }
    }

    private String getServiceAccountToken(
            final String serviceAccountNamespace,
            final String serviceAccountName,
            final DefaultKubernetesClient client
    ) {
        final ServiceAccount sa = client.inNamespace(serviceAccountNamespace).serviceAccounts().withName(serviceAccountName).get();
        final String saName = serviceAccountNamespace + "/" + serviceAccountName;
        if (sa == null) {
            throw new RuntimeException(format("ServiceAccount %s not found", saName));
        }
        if (sa.getSecrets().isEmpty()) {
            throw new RuntimeException(format("ServiceAccount %s has no secrets", saName));
        }
        final ObjectReference secretRef = sa.getSecrets().get(0);
        final String secretNamespace = Optional.ofNullable(secretRef.getNamespace()).orElse(serviceAccountNamespace);
        final String secretName = secretRef.getName();
        final Secret secret = client.inNamespace(secretNamespace).secrets().withName(secretName).get();
        if (secret == null) {
            throw new RuntimeException(format("Secret %s/%s not found", secretNamespace, secretName));
        }
        if (!"kubernetes.io/service-account-token".equals(secret.getType())) {
            throw new RuntimeException(format("Secret %s/%s is not of type kubernetes.io/service-account-token", secretNamespace, secretName));
        }
        return new String(Base64.getDecoder().decode(secret.getData().get("token")), UTF_8);
    }

}
