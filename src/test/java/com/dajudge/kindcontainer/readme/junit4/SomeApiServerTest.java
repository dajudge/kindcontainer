package com.dajudge.kindcontainer.readme.junit4;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.ClassRule;
import org.junit.Test;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.Assert.assertTrue;

public class SomeApiServerTest {
    @ClassRule
    public static final ApiServerContainer<?> KUBE = new ApiServerContainer<>();

    @Test
    public void verify_no_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(KUBE.getKubeconfig()))) {
            assertTrue(client.nodes().list().getItems().isEmpty());
        }
    }
}
