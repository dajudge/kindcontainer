package com.dajudge.kindcontainer.readme.junit5;

import com.dajudge.kindcontainer.K3sContainer;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class SomeK3sTest {
    @Container
    public static final K3sContainer<?> K3S = new K3sContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(K3S.getKubeconfig()))) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }
}