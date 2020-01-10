package com.dajudge.kindcontainer;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Volume;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.testcontainers.containers.BindMode.READ_ONLY;

public class KindContainer extends GenericContainer<KindContainer> {
    private static final Logger LOG = LoggerFactory.getLogger(KindContainer.class);
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;
    private static final Yaml YAML = new Yaml();
    private static final String CONTAINER_NAME = "kindcontainer-control-plane";
    private boolean initialized = false;
    private KubernetesClient client;
    private String podSubnet = "10.244.0.0/16";
    private String serviceSubnet = "10.97.0.0/12";


    public KindContainer() {
        super("kindest/node:v1.16.3");
        this
                .withCreateContainerCmdModifier(cmd -> {
                    final Volume varVolume = new Volume("/var/lib/containerd");
                    cmd.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init")
                            .withName(CONTAINER_NAME)
                            .withHostName("kindcontainer-control-plane")
                            .withVolumes(varVolume)
                            .withBinds(new Bind("kindcontainer-volume", varVolume, true));
                })
                .withPrivilegedMode(true)
                .withFileSystemBind("/lib/modules", "/lib/modules", READ_ONLY)
                .withTmpFs(new HashMap<String, String>() {{
                    put("/run", "rw");
                    put("/tmp", "rw");
                    put("/var", "rw");
                }})
                .withExposedPorts(6443)
                .withStartupTimeout(ofSeconds(300));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        initialize();
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        final Runnable r = () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                final String containerIpAddress = getInternalIpAddress(this);
                LOG.info("Container IP address: {}", containerIpAddress);
                final Map<String, String> params = new HashMap<String, String>() {{
                    put("NODE_IP", containerIpAddress);
                    put("POD_SUBNET", podSubnet);
                    put("SERVICE_SUBNET", serviceSubnet);
                }};
                LOG.info("Writing /kind/kubeadm.conf...");
                copyFileToContainer(kubeadmConfigFor(params), "/kind/kubeadm.conf");
                copyFileToContainer(Transferable.of(defaultCni(params)), "/kind/default-cni.conf");
                kubeadm("init");
                kubectl("apply", asList("-f", "/kind/default-cni.conf"));
                kubectl("taint", asList("node", CONTAINER_NAME, "node-role.kubernetes.io/master:NoSchedule-"));
            } catch (final Exception e) {
                LOG.error("Failed to initialize node.", e);
            }
        };
        new Thread(r).start();
    }

    private void kubectl(final String cmd, final List<String> params) throws IOException, InterruptedException {
        final List<String> exec = new ArrayList<>(asList(
                "--kubeconfig", "/etc/kubernetes/admin.conf"
        ));
        exec.addAll(params);
        exec("kubectl", cmd, exec);
    }

    private byte[] defaultCni(final Map<String, String> replacements) {
        return template(Utils.loadResource("default-cni.yml"), replacements).getBytes(UTF_8);
    }

    private void kubeadm(final String cmd) throws IOException, InterruptedException {
        exec("kubeadm", cmd, asList(
                // preflight errors are expected, in particular for swap being enabled
                "--ignore-preflight-errors=all",
                // specify our generated config file
                "--config=/kind/kubeadm.conf",
                // increase verbosity for debugging
                "--v=6"
        ));
    }

    private void exec(final String bin, final String cmd, final List<String> params) throws IOException, InterruptedException {
        LOG.info("Running {} {}...", bin, cmd);
        final List<String> exec = new ArrayList<>(asList(bin, cmd));
        exec.addAll(params);
        final ExecResult execResult = execInContainer(exec.toArray(new String[exec.size()]));
        final int exitCode = execResult.getExitCode();
        if (exitCode == 0) {
            LOG.info("{} {} exited with status code {}", bin, cmd, exitCode);
            LOG.debug("{}", execResult.getStdout().replaceAll("(?m)^", "STDOUT: "));
            LOG.debug("{}", execResult.getStderr().replaceAll("(?m)^", "STDERR: "));
        } else {
            LOG.error("{}", execResult.getStdout().replaceAll("(?m)^", "STDOUT: "));
            LOG.error("{}", execResult.getStderr().replaceAll("(?m)^", "STDERR: "));
            throw new IllegalStateException(bin + " " + cmd + " exited with status code " + execResult);
        }
    }

    private static Transferable kubeadmConfigFor(final Map<String, String> replacements) {
        return Transferable.of(template(Utils.loadResource("kubeadm.conf"), replacements).getBytes(UTF_8));
    }

    private static String template(String string, final Map<String, String> replacements) {
        return replacements.entrySet().stream()
                .map(r -> ((Function<String, String>) (s -> s.replace("${" + r.getKey() + "}", r.getValue()))))
                .reduce(Function.identity(), Function::andThen)
                .apply(string);
    }

    @NotNull
    private static String getInternalIpAddress(final KindContainer container) {
        return waitUntilNotNull(() -> {
                    final Map<String, ContainerNetwork> networks = container.getContainerInfo()
                            .getNetworkSettings().getNetworks();
                    if (!networks.containsKey("bridge")) {
                        return null;
                    }
                    return networks.get("bridge").getIpAddress();
                },
                CONTAINER_IP_TIMEOUT_MSECS,
                () -> "Failed to determine container IP address"
        );
    }

    private static <T> T waitUntilNotNull(
            final Supplier<T> check,
            final int timeout,
            final Supplier<String> errorMessage
    ) {
        final long start = currentTimeMillis();
        while ((currentTimeMillis() - start) < timeout) {
            final T result = check.get();
            if (result != null) {
                return result;
            }
            Thread.yield();
        }
        throw new IllegalStateException(errorMessage.get());
    }

    public KubernetesClient client() {
        return client;
    }

    @NotNull
    private KubernetesClient createClient() {
        try {
            return new DefaultKubernetesClient(fromKubeconfig(kubeconfig()));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to extract kubeconfig from test container", e);
        }
    }

    public String kubeconfig() {
        final String adminKubeConfig = copyFileFromContainer(
                "/etc/kubernetes/admin.conf",
                Utils::readString
        );
        return patchKubeConfig(adminKubeConfig);
    }

    @SuppressWarnings("unchecked")
    private String patchKubeConfig(final String kubeConfig) {
        final Map<String, Object> kubeConfigMap = YAML.load(kubeConfig);
        final List<Map<String, Object>> clusters = (List<Map<String, Object>>) kubeConfigMap.get("clusters");
        final Map<String, Object> firstCluster = clusters.iterator().next();
        firstCluster.put("server", "https://" + getContainerIpAddress() + ":" + getMappedPort(6443));
        return YAML.dump(kubeConfigMap);
    }

    @Override
    public void start() {
        super.start();
        LOG.info("Waiting for a node to become ready...");
        client = createClient();
        final Node readyNode = waitUntilNotNull(() -> client.nodes().list().getItems().stream().filter(
                node -> node.getStatus().getConditions().stream().anyMatch(cond ->
                        "Ready".equals(cond.getType()) && "True".equals(cond.getStatus())
                )).findAny().orElse(null), 300000, () -> "No node became ready");
        LOG.info("Node ready: {}", readyNode.getMetadata().getName());
    }

    public KindContainer withPodSubnet(final String cidr) {
        podSubnet = cidr;
        return this;
    }

    public KindContainer withServiceSubnet(final String cidr) {
        serviceSubnet = cidr;
        return this;
    }
}
