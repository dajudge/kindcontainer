package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import org.junit.Rule;
import org.junit.Test;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.Assert.assertNotNull;

public class KubectlCreateNamespaceFluentTest {
    @Rule
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
