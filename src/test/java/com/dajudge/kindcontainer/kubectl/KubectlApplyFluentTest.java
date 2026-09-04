package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class KubectlApplyFluentTest {

    @Test
    public void multiple_invocations_work() {
        /*
        There was a bug where adding parameters via "fromXXX()" would not be cleared with the actual
        invocation of the command via "run()".
        This test reproduces this issue by applying 2 different manifests in separate "run()" invocations
        while deleting some object of the first manifest in between. If the object pop up again due to
        the unwanted repeated application of the first manifest we've captured the bug.
         */
        withK8s(k8s -> k8s.withKubectl(kubectl -> {
            // First manifest
            kubectl.apply
                    .fileFromClasspath(
                            "manifests/interpolation-configmap.yaml",
                            bytes -> new String(bytes, UTF_8)
                                    .replace("{{ my-value }}", "hello, world!")
                                    .replace("{{ name }}", "lolcats")
                                    .getBytes(UTF_8)
                    ).run();
        }), (k8s, client) -> {
            try {
                // Delete sth from the first manifest
                client.inNamespace("default").configMaps().withName("lolcats").delete();
                // Apply 2nd manifest
                k8s.kubectl().apply.fileFromClasspath("manifests/serviceaccount1.yaml").run();
                // Make sure the deleted object remains gone
                assertNull(client.inNamespace("default").configMaps().withName("lolcats").get());
            } catch (final IOException | ExecutionException | InterruptedException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    public void can_apply_from_string_param() {
        withK8s(k8s -> k8s.withKubectl(kubectl -> {
            kubectl.copyFileToContainer(forClasspathResource("manifests/serviceaccount1.yaml"), "/tmp/sa.yaml");
            kubectl.apply.from("/tmp/sa.yaml").run();
        }), (k8s, client) -> assertTrue(serviceAccountExists(client)));
    }

    @Test
    public void can_interpolate_static_value() {
        final String staticValue = UUID.randomUUID().toString();
        final BiFunction<String, KubernetesContainer<?>, KubernetesContainer<?>> config = (name, k8s) -> k8s.withKubectl(kubectl ->
                kubectl.apply.fileFromClasspath("manifests/interpolation-configmap.yaml", bytes ->
                                new String(bytes, UTF_8)
                                        .replace("{{ my-value }}", staticValue)
                                        .replace("{{ name }}", name)
                                        .getBytes(UTF_8))
                        .run());
        assertInterpolates(config, k8s -> staticValue);
    }

    @Test
    public void can_interpolate_with_container_info() {
        final BiFunction<String, KubernetesContainer<?>, KubernetesContainer<?>> config = (name, k8s) -> k8s.withKubectl(kubectl ->
                kubectl.apply.fileFromClasspath("manifests/interpolation-configmap.yaml", (innerK8s, bytes) ->
                                new String(bytes, UTF_8)
                                        .replace("{{ my-value }}", innerK8s.getContainerInfo().getId())
                                        .replace("{{ name }}", name)
                                        .getBytes(UTF_8))
                        .run());
        assertInterpolates(config, k8s -> k8s.getContainerInfo().getId());
    }

    private boolean serviceAccountExists(final NamespacedKubernetesClient client) {
        return null != client.inNamespace("my-namespace").serviceAccounts()
                .withName("my-service-account")
                .get();
    }

    private void assertInterpolates(
            final BiFunction<String, KubernetesContainer<?>, KubernetesContainer<?>> config,
            final Function<KubernetesContainer<?>, String> value
    ) {
        final String name = UUID.randomUUID().toString();
        withK8s(k8s -> config.apply(name, k8s), (k8s, client) -> {
            final ConfigMap configMap = await()
                    .timeout(10, SECONDS)
                    .until(
                            () -> client.configMaps().inNamespace("default").withName(name).get(),
                            Objects::nonNull
                    );
            assertEquals(value.apply(k8s), configMap.getData().get("my-key"));
        });
    }

    private void withK8s(
            final Function<KubernetesContainer<?>, KubernetesContainer<?>> config,
            final BiConsumer<KubernetesContainer<?>, NamespacedKubernetesClient> consumer
    ) {
        try (final KubernetesContainer<?> k8s = config.apply(new ApiServerContainer<>())) {
            k8s.start();
            runWithClient(k8s, client -> {
                consumer.accept(k8s, client);
            });
        }
    }

}
