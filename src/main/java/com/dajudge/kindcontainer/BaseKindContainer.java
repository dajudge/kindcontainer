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
package com.dajudge.kindcontainer;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Volume;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.Utils.*;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.testcontainers.containers.BindMode.READ_ONLY;

public class BaseKindContainer<T extends BaseKindContainer<T>> extends GenericContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseKindContainer.class);
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;
    private static final Yaml YAML = new Yaml();
    private static final String CONTAINER_NAME = "kindcontainer-control-plane";
    private static final String CONTAINTER_WORKDIR = "/kindcontainer";
    private static final String DEFAULT_IMAGE = "kindest/node:v1.21.1";
    private String podSubnet = "10.244.0.0/16";
    private String serviceSubnet = "10.97.0.0/12";
    private int startupTimeoutSecs = 300;
    private List<String> certs = emptyList();

    public BaseKindContainer() {
        this(DEFAULT_IMAGE);
    }

    public BaseKindContainer(final String image) {
        super(image);
        this.withStartupTimeout(ofSeconds(300))
                .withCreateContainerCmdModifier(cmd -> {
                    final Volume varVolume = new Volume("/var/lib/containerd");
                    cmd.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init")
                            .withName(CONTAINER_NAME)
                            .withHostName("kindcontainer-control-plane")
                            .withVolumes(varVolume)
                            .withBinds(new Bind("kindcontainer-volume", varVolume, true));
                })
                .withEnv("KUBECONFIG", "/etc/kubernetes/admin.conf")
                .withPrivilegedMode(true)
                .withFileSystemBind("/lib/modules", "/lib/modules", READ_ONLY)
                .withTmpFs(new HashMap<String, String>() {{
                    put("/run", "rw");
                    put("/tmp", "rw");
                }})
                .withExposedPorts();
    }

    public T withNodeReadyTimeout(final int seconds) {
        startupTimeoutSecs = seconds;
        return self();
    }

    public T withCaCerts(final Collection<String> certs) {
        this.certs = new ArrayList<>(certs);
        return self();
    }

    @Override
    public T withExposedPorts(final Integer... ports) {
        final HashSet<Integer> exposedPorts = new HashSet<>(asList(ports));
        exposedPorts.add(6443);
        return super.withExposedPorts(exposedPorts.toArray(new Integer[0]));
    }

    @Override
    public T waitingFor(final WaitStrategy waitStrategy) {
        return super.waitingFor(new WaitForKubeApiStrategy(waitStrategy));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        try {
            updateCaCertificates();
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
            }};
            exec("mkdir", "-p", CONTAINTER_WORKDIR);
            kubeadmInit(params);
            installCni(params);
            installStorage();
            untaintMasterNode();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize node", e);
        }
    }

    private void updateCaCertificates() throws IOException, InterruptedException {
        if (certs.isEmpty()) {
            return;
        }
        for (int i = 0; i < certs.size(); i++) {
            writeContainerFile(certs.get(i), "/usr/local/share/ca-certificates/custom-cert-" + i + ".crt");
        }
        exec(singletonList("update-ca-certificates"));
    }

    private void untaintMasterNode() throws IOException, InterruptedException {
        kubectl("taint", "node", CONTAINER_NAME, "node-role.kubernetes.io/master:NoSchedule-");
    }

    private void kubeadmInit(final Map<String, String> params) throws IOException, InterruptedException {
        final String kubeadmConfig = writeContainerFile(
                kubeadmConfigFor(params),
                CONTAINTER_WORKDIR + "/kubeadmConfig.yaml"
        );
        exec(asList(
                "kubeadm", "init",
                "--skip-phases=preflight",
                // specify our generated config file
                "--config=" + kubeadmConfig,
                "--skip-token-print",
                // increase verbosity for debugging
                "--v=6"
        ));
    }

    private void installStorage() throws IOException, InterruptedException {
        kubectl("apply", "-f", "/kind/manifests/default-storage.yaml");
    }

    private void installCni(final Map<String, String> params) throws IOException, InterruptedException {
        final String cniManifest = templateContainerFile(
                "/kind/manifests/default-cni.yaml",
                CONTAINTER_WORKDIR + "/cni.yaml",
                params
        );
        kubectl("apply", "-f", cniManifest);
    }

    private String templateContainerFile(
            final String sourceFileName,
            final String destFileName,
            final Map<String, String> params
    ) {
        return writeContainerFile(template(readContainerFile(sourceFileName), params), destFileName);
    }

    private String readContainerFile(final String fname) {
        return copyFileFromContainer(fname, Utils::readString);
    }

    private String writeContainerFile(final String content, final String fname) {
        LOG.info("Writing container file: {}", fname);
        copyFileToContainer(Transferable.of(content.getBytes(UTF_8)), fname);
        return fname;
    }

    private void kubectl(
            final String... params
    ) throws IOException, InterruptedException {
        final List<String> exec = new ArrayList<>(asList("kubectl", "--kubeconfig", "/etc/kubernetes/admin.conf"));
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

    private static String kubeadmConfigFor(final Map<String, String> replacements) {
        return template(loadResource("kubeadm.yaml"), replacements);
    }

    private static String template(String string, final Map<String, String> replacements) {
        return replacements.entrySet().stream()
                .map(r -> ((Function<String, String>) (s -> s.replace("{{ " + r.getKey() + " }}", r.getValue()))))
                .reduce(Function.identity(), Function::andThen)
                .apply(string);
    }

    private static String getInternalIpAddress(final BaseKindContainer<?> container) {
        return waitUntilNotNull(() -> {
                    final Map<String, ContainerNetwork> networks = container.getContainerInfo()
                            .getNetworkSettings().getNetworks();
                    if (!networks.containsKey("bridge")) {
                        return null;
                    }
                    return networks.get("bridge").getIpAddress();
                },
                CONTAINER_IP_TIMEOUT_MSECS,
                "Waiting for bridge network to receive IP address...",
                () -> new IllegalStateException("Failed to determine container IP address")
        );
    }

    public KubernetesClient client() {
        return client(kubeconfig());
    }

    private KubernetesClient client(final String kubeconfig) {
        try {
            return new DefaultKubernetesClient(fromKubeconfig(kubeconfig));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to extract kubeconfig from test container", e);
        }
    }

    public void withClient(final Consumer<KubernetesClient> callable) {
        withClient(client -> {
            callable.accept(client);
            return null;
        });
    }

    public <R> R withClient(final Function<KubernetesClient, R> callable) {
        try (final KubernetesClient client = client()) {
            return callable.apply(client);
        }
    }

    private String kubeconfig;

    public synchronized String kubeconfig() {
        if (kubeconfig == null) {
            final String adminKubeConfig = copyFileFromContainer(
                    "/etc/kubernetes/admin.conf",
                    Utils::readString
            );
            if (tryKubeconfig(adminKubeConfig)) {
                kubeconfig = adminKubeConfig;
                LOG.info("Original kubeconfig works");
            } else {
                LOG.info("Original kubeconfig doesn't seem to work, creating patched version...");
                kubeconfig = patchKubeConfig(adminKubeConfig);
            }
        }
        return kubeconfig;
    }

    private boolean tryKubeconfig(final String kubeconfig) {
        try (final KubernetesClient client = client(kubeconfig)) {
            client.nodes().list();
            return true;
        } catch (final Exception e) {
            LOG.trace("Kubeconfig check failed", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String patchKubeConfig(final String kubeConfig) {
        final Map<String, Object> kubeConfigMap = YAML.load(kubeConfig);
        final List<Map<String, Object>> clusters = (List<Map<String, Object>>) kubeConfigMap.get("clusters");
        final Map<String, Object> firstCluster = clusters.iterator().next();
        final Map<String, Object> cluster = (Map<String, Object>) firstCluster.get("cluster");
        final String newServerEndpoint = "https://" + getContainerIpAddress() + ":" + getMappedPort(6443);
        final String server = cluster.get("server").toString();
        LOG.info("Creating kubeconfig with server {} instead of {}", newServerEndpoint, server);
        cluster.put("server", newServerEndpoint);
        return YAML.dump(kubeConfigMap);
    }

    @Override
    public void start() {
        super.start();
        final InspectContainerResponse containerInfo = getContainerInfo();
        LOG.debug("Bridge: {}", containerInfo.getNetworkSettings().getBridge());
        LOG.debug("Networks: ");
        containerInfo.getNetworkSettings().getNetworks().forEach((k, v) ->
                LOG.debug("  {}: {} gw {}", k, v.getIpAddress(), v.getGateway()));
        LOG.debug("Ports: ");
        containerInfo.getNetworkSettings().getPorts().getBindings().forEach((e, b) ->
                LOG.debug("  {} -> {}", e, asList(b)));
        final Node readyNode = waitUntilNotNull(
                findReadyNode(),
                startupTimeoutSecs * 1000,
                "Waiting for a node to become ready...",
                () -> {
                    dumpDebuggingInfo();
                    return new IllegalStateException("No node became ready");
                }
        );
        LOG.info("Node ready: {}", readyNode.getMetadata().getName());
    }

    private void dumpDebuggingInfo() {
        withClient(client -> {
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
        return () -> withClient(client -> {
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

    public T withPodSubnet(final String cidr) {
        podSubnet = cidr;
        return self();
    }

    public T withServiceSubnet(final String cidr) {
        serviceSubnet = cidr;
        return self();
    }

    public void pullImages(final String... images) {
        LOG.info("Pulling {} images...", images.length);
        new HashSet<>(Stream.of(images)
                .map(BaseKindContainer::fullNameOf).collect(toList()))
                .forEach(this::pullImage);
        LOG.info("Pull complete");
    }

    public void pullImage(final String imageName) {
        final String fullImageName = fullNameOf(imageName);
        LOG.info("Pulling image: {}", fullImageName);
        try {
            final Container.ExecResult result = execInContainer("ctr", "-n", "k8s.io",
                    "image", "pull", fullImageName
            );
            if (result.getExitCode() != 0) {
                LOG.error("Image pull returned non-zero exit code: {}", result.getExitCode());
                LOG.error("\n{}", indent("STDOUT: ", result.getStdout()));
                LOG.error("\n{}", indent("STDERR: ", result.getStderr()));
                throw new AssertionError("Image pull returned non-zero exit code: " + result.getExitCode());
            }
        } catch (final IOException | InterruptedException e) {
            throw new AssertionError("Failed to pull image " + fullImageName, e);
        }
    }

    private static String fullNameOf(final String image) {
        final StringBuilder builder = new StringBuilder(image);
        if (!image.contains(":")) {
            builder.append(":latest");
        }
        if (image.indexOf('/') < 0) {
            builder.insert(0, "library/");
        }
        if (image.indexOf('/') == image.lastIndexOf('/')) {
            builder.insert(0, "docker.io/");
        }
        return builder.toString();
    }
}
