package com.dajudge.kindcontainer.readme.junit4;

import com.dajudge.kindcontainer.KindContainer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SomeKindTest {
    public static final KindContainer<?> KUBE = new KindContainer<>();

    @BeforeAll
    static void setUp() {
        KUBE.start();
    }

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(fromKubeconfig(KUBE.getKubeconfig())).build()) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }

    @AfterAll
    static void tearDown() {
        KUBE.stop();
    }

}