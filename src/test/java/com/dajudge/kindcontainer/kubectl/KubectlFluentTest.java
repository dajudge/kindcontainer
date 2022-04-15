package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.K3sContainer;
import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class KubectlFluentTest {

    @Test
    public void multiple_invocations_work() {
        withK8s(k8s -> k8s.withKubectl(kubectl -> {
            kubectl.apply
                    .fileFromClasspath(
                            "manifests/interpolation-configmap.yaml",
                            bytes -> new String(bytes, UTF_8)
                                    .replace("{{ my-value }}", "hello, world!")
                                    .replace("{{ name }}", "lolcats")
                                    .getBytes(UTF_8)
                    ).run();
        }), k8s -> {
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(k8s.getKubeconfig()))) {
                client.inNamespace("default").configMaps().withName("lolcats").delete();
                k8s.kubectl().apply.fileFromClasspath("manifests/serviceaccount1.yaml").run();
                assertNull(client.inNamespace("default").configMaps().withName("lolcats").get());
            } catch (final IOException | ExecutionException | InterruptedException e) {
                throw new AssertionError(e);
            }
        });
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
