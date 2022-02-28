package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ConfigMap;
import org.junit.Test;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class KubectlTest {
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

    private void assertInterpolates(
            final BiFunction<String, KubernetesContainer<?>, KubernetesContainer<?>> config,
            final Function<KubernetesContainer<?>, String> value
    ) {
        final String name = UUID.randomUUID().toString();
        withK8s(k8s -> config.apply(name, k8s), k8s -> runWithClient(k8s, client -> {
            final ConfigMap configMap = await()
                    .timeout(10, SECONDS)
                    .until(
                            () -> client.configMaps().inNamespace("default").withName(name).get(),
                            Objects::nonNull
                    );
            assertEquals(value.apply(k8s), configMap.getData().get("my-key"));
        }));
    }

    private void withK8s(
            final Function<KubernetesContainer<?>, KubernetesContainer<?>> config,
            final Consumer<KubernetesContainer<?>> consumer
    ) {
        try (final KubernetesContainer<?> k8s = config.apply(new ApiServerContainer<>())) {
            k8s.start();
            consumer.accept(k8s);
        }
    }

}
