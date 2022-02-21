package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ConfigMap;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Objects;
import java.util.function.Function;

import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class KubectlTest {

    private static final Function<byte[], byte[]> APPLY_PLACEHOLDERS = bytes -> new String(bytes, UTF_8)
            .replace("{{ my-value }}", "Hello, world!")
            .getBytes(UTF_8);
    @ClassRule
    public static ApiServerContainer<?> K8S = new ApiServerContainer<>()
            .withKubectl(kubectl -> kubectl.apply
                    .fileFromClasspath("manifests/config_map_1.yaml", APPLY_PLACEHOLDERS)
                    .run());

    @Test
    public void can_apply_manifest() {
        runWithClient(K8S, client -> {
            final ConfigMap configMap = await()
                    .timeout(10, SECONDS)
                    .until(
                            () -> client.configMaps().inNamespace("configmap1").withName("configmap1").get(),
                            Objects::nonNull
                    );
            assertEquals("Hello, world!", configMap.getData().get("my-key"));
        });
    }
}
