package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.http.Response;
import com.dajudge.kindcontainer.client.http.TinyHttpClient;
import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import com.github.dockerjava.api.command.InspectContainerResponse;
import io.fabric8.kubernetes.api.model.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kubeletContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.createNewNamespace;
import static com.dajudge.kindcontainer.util.TestUtils.createSimplePod;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.awaitility.Awaitility.await;

public class NodePortTest {

    private static final Logger LOG = LoggerFactory.getLogger(NodePortTest.class);

    @TestFactory
    public Stream<DynamicTest> exposes_node_port() {
        return kubeletContainers(this::assertExposesNodePort);
    }

    private void assertExposesNodePort(final KubernetesTestPackage<? extends KubernetesWithKubeletContainer<?>> testPkg) {
        runWithK8s(configureContainer(testPkg.newContainer()), k8s -> {
            final Pod pod = runWithClient(k8s, client -> createSimplePod(client, createNewNamespace(client)));
            final Service service = runWithClient(k8s, client -> client.services().create(new ServiceBuilder()
                    .withNewMetadata()
                    .withName("nginx")
                    .withNamespace(pod.getMetadata().getNamespace())
                    .endMetadata()
                    .withNewSpec()
                    .withType("NodePort")
                    .withSelector(new HashMap<String, String>() {{
                        put("app", "nginx");
                    }})
                    .withPorts(new ServicePortBuilder()
                            .withNodePort(30000)
                            .withPort(80)
                            .withTargetPort(new IntOrString(80))
                            .withProtocol("TCP")
                            .build())
                    .endSpec()
                    .build()));

            final AtomicReference<String> lastHostProbeResult = new AtomicReference<>("not attempted");
            try {
                waitForPodAndServiceEndpoint(k8s, pod, service);
                final String url = "http://localhost:" + k8s.getMappedPort(30000);
                await("testpod answers on node port")
                        .timeout(1, TimeUnit.MINUTES)
                        .until(httpWithDiagnostics(url, lastHostProbeResult));
            } catch (final RuntimeException e) {
                LOG.warn("NodePort diagnostics - last host probe result: {}", lastHostProbeResult.get());
                logNodePortDiagnostics(k8s, pod, service);
                throw e;
            }
        });
    }

    private Callable<Boolean> httpWithDiagnostics(
            final String url,
            final AtomicReference<String> lastResult
    ) {
        return () -> {
            try {
                final TinyHttpClient client = TinyHttpClient.newHttpClient().build();
                try (final Response response = client.request().url(url).execute()) {
                    lastResult.set("HTTP " + response.code());
                    return response.code() == 200;
                }
            } catch (final IOException e) {
                lastResult.set(e.getClass().getSimpleName() + ": " + e.getMessage());
                return false;
            }
        };
    }

    private void waitForPodAndServiceEndpoint(
            final KubernetesWithKubeletContainer<?> k8s,
            final Pod pod,
            final Service service
    ) {
        runWithClient(k8s, client -> {
            await("test pod is ready")
                    .ignoreExceptions()
                    .timeout(1, TimeUnit.MINUTES)
                    .until(() -> client.pods()
                                    .inNamespace(pod.getMetadata().getNamespace())
                                    .withName(pod.getMetadata().getName())
                                    .get(),
                            NodePortTest::isReady);

            await("service has a ready endpoint")
                    .ignoreExceptions()
                    .timeout(1, TimeUnit.MINUTES)
                    .until(() -> client.endpoints()
                                    .inNamespace(service.getMetadata().getNamespace())
                                    .withName(service.getMetadata().getName())
                                    .get(),
                            NodePortTest::hasReadyEndpoint);
        });
    }

    private static boolean isReady(final Pod pod) {
        return pod != null
                && pod.getStatus() != null
                && pod.getStatus().getConditions() != null
                && pod.getStatus().getConditions().stream()
                .anyMatch(condition -> "Ready".equals(condition.getType()) && "True".equals(condition.getStatus()));
    }

    private static boolean hasReadyEndpoint(final Endpoints endpoints) {
        return endpoints != null
                && endpoints.getSubsets() != null
                && endpoints.getSubsets().stream()
                .filter(Objects::nonNull)
                .filter(subset -> subset.getAddresses() != null)
                .flatMap(subset -> subset.getAddresses().stream())
                .anyMatch(Objects::nonNull);
    }

    private void logNodePortDiagnostics(
            final KubernetesWithKubeletContainer<?> k8s,
            final Pod pod,
            final Service service
    ) {
        final String namespace = pod.getMetadata().getNamespace();
        final String serviceName = service.getMetadata().getName();

        runWithClient(k8s, client -> {
            final Pod currentPod = client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).get();
            final Service currentService = client.services().inNamespace(namespace).withName(serviceName).get();
            final Endpoints endpoints = client.endpoints().inNamespace(namespace).withName(serviceName).get();

            LOG.warn("NodePort diagnostics - mapped host port: {}", k8s.getMappedPort(30000));
            LOG.warn("NodePort diagnostics - pod: {}", currentPod);
            LOG.warn("NodePort diagnostics - service: {}", currentService);
            LOG.warn("NodePort diagnostics - endpoints: {}", endpoints);
            LOG.warn("NodePort diagnostics - nodes: {}", client.nodes().list().getItems());

            if (currentPod != null && currentPod.getStatus() != null && currentPod.getStatus().getPodIP() != null) {
                logExec(k8s, "pod IP probe", "wget -S -T 5 -O- http://" + currentPod.getStatus().getPodIP() + ":80/ || true");
            }
        });

        logDockerNetworkDiagnostics(k8s);
        logExec(k8s, "NodePort probe inside container", "wget -S -T 5 -O- http://127.0.0.1:30000/ || true");
        logExec(k8s, "KUBE-NODEPORTS rules", "iptables-save 2>&1 | grep -E 'KUBE-NODEPORTS|30000' || true");
        logExec(k8s, "nft rules mentioning NodePort", "nft list ruleset 2>&1 | grep -E '30000|KUBE-NODEPORTS' || true");
    }

    private void logDockerNetworkDiagnostics(final KubernetesWithKubeletContainer<?> k8s) {
        try {
            final InspectContainerResponse info = k8s.getContainerInfo();
            LOG.warn("NodePort diagnostics - Docker port bindings: {}", info.getNetworkSettings().getPorts());
            LOG.warn("NodePort diagnostics - Docker networks: {}", info.getNetworkSettings().getNetworks());
            LOG.warn("NodePort diagnostics - Docker container IP: {}", info.getNetworkSettings().getIpAddress());
        } catch (final RuntimeException e) {
            LOG.warn("NodePort diagnostics - failed to inspect Docker network state", e);
        }
    }

    private void logExec(
            final KubernetesWithKubeletContainer<?> k8s,
            final String description,
            final String command
    ) {
        try {
            final ExecResult result = k8s.execInContainer("sh", "-c", command);
            LOG.warn("NodePort diagnostics - {} (exit {}):\nstdout:\n{}\nstderr:\n{}",
                    description, result.getExitCode(), result.getStdout(), result.getStderr());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("NodePort diagnostics - interrupted while collecting {}", description, e);
        } catch (final IOException e) {
            LOG.warn("NodePort diagnostics - failed to collect {}", description, e);
        }
    }

    private KubernetesWithKubeletContainer<?> configureContainer(final KubernetesWithKubeletContainer<?> container) {
        return container.withExposedPorts(30000);
    }

}
