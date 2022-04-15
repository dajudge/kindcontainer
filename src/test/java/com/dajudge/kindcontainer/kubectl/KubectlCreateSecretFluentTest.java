package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import org.junit.Rule;
import org.junit.Test;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.Assert.assertNotNull;

public class KubectlCreateSecretFluentTest {
    @Rule
    public final ApiServerContainer<?> k8s = new ApiServerContainer<>()
            .withKubectl(kubectl -> {
               kubectl.create.secret.dockerRegistry
                       .namespace("default")
                       .dockerEmail("tstark@example.com")
                       .dockerServer("https://registry.example.com")
                       .dockerUsername("tony")
                       .dockerPassword("p3pp3er")
                       .run("pull-secret");
            });

    @Test
    public void creates_docker_secret() {
        runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("default").secrets().withName("pull-secret").get());
        });
    }
}
