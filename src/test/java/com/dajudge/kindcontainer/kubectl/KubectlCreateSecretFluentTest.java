package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.APISERVER_CONTAINER;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlCreateSecretFluentTest {

    @ParameterizedTest
    @MethodSource(APISERVER_CONTAINER)
    public void creates_docker_secret(final Supplier<ApiServerContainer<?>> factory) {
        runWithK8s(createContainer(factory), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("default").secrets().withName("pull-secret").get());
        }));
    }

    private ApiServerContainer<?> createContainer(Supplier<ApiServerContainer<?>> factory) {
        return factory.get().withKubectl(kubectl -> kubectl.create.secret.dockerRegistry
                .namespace("default")
                .dockerEmail("tstark@example.com")
                .dockerServer("https://registry.example.com")
                .dockerUsername("tony")
                .dockerPassword("p3pp3er")
                .run("pull-secret"));
    }
}
