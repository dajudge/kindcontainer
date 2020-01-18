package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.shaded.okhttp3.ResponseBody;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

public class SmokeTests {
    private static final HashSet<String> DEFAULT_NAMESPACES = new HashSet<>(asList(
            "kube-node-lease",
            "kube-public",
            "kube-system"
    ));

    @ClassRule
    public static final KindContainer K8S = new KindContainer()
            .withExposedPorts(30000)
            .waitingFor(NullWaitStrategy.INSTANCE)
            .withPodSubnet("10.245.0.0/16")
            .withServiceSubnet("10.112.0.0/12");

    @Test
    public void can_list_namespaces() {
        final Set<String> namespaces = K8S.client().namespaces().list().getItems().stream()
                .map(SmokeTests::toName)
                .collect(toSet());
        assertTrue(namespaces.containsAll(DEFAULT_NAMESPACES));
    }

    @Test
    public void can_start_pod() {
        final Pod pod = createTestPod();
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> isRunning(pod));
    }

    @Test
    public void exposes_node_port() {
        final Pod pod = createTestPod();
        K8S.client().services().create(new ServiceBuilder()
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
                .build());
        await("testpod")
                .timeout(ofSeconds(300))
                .until(http("http://localhost:" + K8S.getMappedPort(30000)));
    }

    private Callable<Boolean> http(final String url) {
        return () -> {
            try {
                final OkHttpClient client = new OkHttpClient();
                final Request request = new Request.Builder().url(url).build();
                final Response response = client.newCall(request).execute();
                try (final ResponseBody body = response.body()) {
                    return response.code() == 200;
                }
            } catch (final IOException e) {
                return false;
            }
        };
    }

    private Pod createTestPod() {
        final Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName(UUID.randomUUID().toString().replaceAll("-", ""))
                .endMetadata()
                .build();
        K8S.client().namespaces().create(namespace);
        final Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("testpod")
                .withNamespace(namespace.getMetadata().getName())
                .withLabels(new HashMap<String, String>() {{
                    put("app", "nginx");
                }})
                .endMetadata()
                .withNewSpec()
                .withContainers(new ContainerBuilder()
                        .withName("test")
                        .withImage("nginx")
                        .withPorts(new ContainerPortBuilder()
                                .withContainerPort(80)
                                .withProtocol("TCP")
                                .build())
                        .build())
                .endSpec()
                .build();
        K8S.client().pods().inNamespace(namespace.getMetadata().getName()).create(pod);
        return pod;
    }

    @NotNull
    private Boolean isRunning(final HasMetadata pod) {
        return "Running".equals(getPod(pod)
                .map(p -> p.getStatus().getPhase())
                .orElse(null));
    }

    private Optional<Pod> getPod(final HasMetadata pod) {
        return K8S.client().pods().list().getItems().stream()
                .filter(c -> isSame(pod, c))
                .findFirst();
    }

    private boolean isSame(final HasMetadata a, final HasMetadata b) {
        if (!b.getMetadata().getName().equals(a.getMetadata().getName())) {
            return false;
        }
        if (!b.getMetadata().getNamespace().equals(a.getMetadata().getNamespace())) {
            return false;
        }
        return true;
    }

    private String qname(final HasMetadata a) {
        return a.getMetadata().getNamespace() + "/" + a.getMetadata().getName();
    }

    private static String toName(final HasMetadata it) {
        return it.getMetadata().getName();
    }
}
