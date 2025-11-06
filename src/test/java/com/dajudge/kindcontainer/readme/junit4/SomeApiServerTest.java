package com.dajudge.kindcontainer.readme.junit4;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SomeApiServerTest {
    public static final ApiServerContainer<?> KUBE = new ApiServerContainer<>();

    @BeforeAll
    static void setUp() {
        KUBE.start();
    }

    @Test
    public void verify_no_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(fromKubeconfig(KUBE.getKubeconfig())).build()) {
            assertTrue(client.nodes().list().getItems().isEmpty());
        }
    }

    @AfterAll
    static void tearDown() {
        KUBE.stop();
    }

}
