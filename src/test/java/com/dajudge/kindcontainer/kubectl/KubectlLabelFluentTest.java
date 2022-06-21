package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.fabric8.kubernetes.api.model.Namespace;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class KubectlLabelFluentTest {
    @Container
    public final ApiServerContainer<?> k8s = new ApiServerContainer<>()
            .withKubectl(kubectl -> kubectl.label
                    .with("label1", "value1")
                    .with(new HashMap<String, String>() {{
                        put("label2", "value2");
                    }})
                    .run("namespace", "default"));

    @Test
    public void creates_docker_secret() {
        runWithClient(k8s, client -> {
            final Namespace namespace = client.namespaces().withName("default").get();
            assertNotNull(namespace);
            new HashMap<String, String>() {{
                put("label1", "value1");
                put("label2", "value2");
            }}.forEach((k, v) -> {
                assertEquals(v, namespace.getMetadata().getLabels().get(k));
            });
        });
    }
}
