package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class WebhookTest {

    @TestFactory
    public Stream<DynamicTest> obeys_namespace_selector() {
        return allContainers(this::assertObeysNamespaceSelector);
    }

    @TestFactory
    public Stream<DynamicTest> obeys_object_selector() {
        return allContainers(this::assertObeysObjectSelector);
    }

    private void assertObeysNamespaceSelector(final KubernetesTestPackage<? extends KubernetesContainer<?>> pkg) {
        runWithWebhooks(pkg, false, WebhookTest::asserObeysNamespaceSelector);
    }

    private void assertObeysObjectSelector(final KubernetesTestPackage<? extends KubernetesContainer<?>> pkg) {
        runWithWebhooks(pkg, true, WebhookTest::assertObeysObjectSelector);
    }

    private static void asserObeysNamespaceSelector(NamespacedKubernetesClient client) {
        awaitWebhooks(client);

        final ConfigMap created = client.configMaps().create(buildConfigMap(true, true));
        assertNotEquals("true", created.getMetadata().getAnnotations().get("mutated"));
    }

    private static void assertObeysObjectSelector(NamespacedKubernetesClient client) {
        awaitWebhooks(client);

        final ConfigMap created = client.configMaps().create(buildConfigMap(true, false));
        assertNotEquals("true", created.getMetadata().getAnnotations().get("mutated"));
    }

    private static void awaitWebhooks(NamespacedKubernetesClient client) {
        await().ignoreExceptions()
                .timeout(ofSeconds(30))
                .untilAsserted(() -> assertValidates(client.inNamespace("default")));
        await().ignoreExceptions()
                .timeout(ofSeconds(30))
                .untilAsserted(() -> assertMutates(client.inNamespace("default")));
    }

    private static void assertValidates(NamespacedKubernetesClient client) {
        try {
            client.configMaps().create(buildConfigMap(false, true));
        } catch (final KubernetesClientException e) {
            if (e.getCode() == 400) {
                return;
            }
            throw e;
        }
    }

    private static void assertMutates(NamespacedKubernetesClient client) {
        final ConfigMap created = client.configMaps().create(buildConfigMap(true, true));
        assertEquals("true", created.getMetadata().getAnnotations().get("mutated"));
    }

    private static void runWithWebhooks(
            final KubernetesTestPackage<? extends KubernetesContainer<?>> pkg,
            final boolean labelNamespace,
            final Consumer<NamespacedKubernetesClient> test
    ) {
        try (
                final AbstractWebhookServer validating = new ValidatingWebhookServer().start();
                final AbstractWebhookServer mutating = new MutatingWebhookServer().start();
                final KubernetesContainer<?> k8s = pkg.newContainer()
                        .withAdmissionController(admission -> {
                            validating.register(admission);
                            mutating.register(admission);
                        })
        ) {
            final String namespaceName = UUID.randomUUID().toString();
            k8s.start();
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(k8s.getKubeconfig()))) {
                final Namespace ns = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespaceName)
                        .endMetadata()
                        .build();
                if (labelNamespace) {
                    ns.getMetadata().setLabels(new HashMap<>());
                    ns.getMetadata().getLabels().put("mutate", "true");
                }
                client.namespaces().create(ns);
                client.namespaces().withName("default").edit(defaultNamespace -> {
                    final Map<String, String> labels = Optional.ofNullable(defaultNamespace.getMetadata().getLabels()).orElse(new HashMap<>());
                    labels.put("mutate", "true");
                    defaultNamespace.getMetadata().setLabels(labels);
                    return defaultNamespace;
                });
                // Admission controller webhook availability is unfortunately not exactly deterministic :-/
                test.accept(client.inNamespace(namespaceName));
            }
        }
    }

    private static ConfigMap buildConfigMap(final boolean allowed, final boolean labeled) {
        final ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(UUID.randomUUID().toString())
                .withLabels(new HashMap<String, String>() {{
                    put("testresource", "true");
                }})
                .endMetadata()
                .withData(new HashMap<String, String>() {{
                    put("allowed", String.valueOf(allowed));
                }})
                .build();
        if (labeled) {
            configMap.getMetadata().getLabels().put("mutate", "true");
        }
        return configMap;
    }
}
