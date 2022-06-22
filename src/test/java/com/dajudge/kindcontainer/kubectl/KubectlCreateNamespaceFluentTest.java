package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class KubectlCreateNamespaceFluentTest {
    @Container
    public final ApiServerContainer<?> k8s = new ApiServerContainer<>()
            .withKubectl(kubectl -> {
                kubectl.create.namespace.run("my-namespace");
            });

    @Test
    public void creates_namespace() {
        runWithClient(k8s, client -> {
            assertNotNull(client.namespaces().withName("my-namespace").get());
        });
    }
}
