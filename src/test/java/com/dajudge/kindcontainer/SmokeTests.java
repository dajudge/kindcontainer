package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    public static final KindContainer K8S = new KindContainer();

    @Test
    public void can_list_namespaces() {
        final Set<String> namespaces = K8S.client().namespaces().list().getItems().stream()
                .map(SmokeTests::toName)
                .collect(toSet());
        assertTrue(namespaces.containsAll(DEFAULT_NAMESPACES));
    }

    @Test
    public void can_start_pod() {
        final Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName("podtest")
                .endMetadata()
                .build();
        K8S.client().namespaces().create(namespace);
        final Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("testpod")
                .withNamespace(namespace.getMetadata().getName())
                .endMetadata()
                .withNewSpec()
                .withContainers(new ContainerBuilder()
                        .withName("test")
                        .withImage("nginx")
                        .build())
                .endSpec()
                .build();
        K8S.client().pods().inNamespace(namespace.getMetadata().getName()).create(pod);
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> isRunning(pod));
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
