package com.dajudge.kindcontainer.readme.junit4;

import com.dajudge.kindcontainer.K3sContainer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SomeK3sTest {

    public static final K3sContainer<?> K3S = new K3sContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(fromKubeconfig(K3S.getKubeconfig())).build()) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }

    @BeforeAll
    static void setUp() {
        K3S.start();
    }

    @AfterAll
    static void tearDown() {
        K3S.stop();
    }
}