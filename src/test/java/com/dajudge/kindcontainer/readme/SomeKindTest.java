package com.dajudge.kindcontainer.readme;

import com.dajudge.kindcontainer.KindContainer;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.ClassRule;
import org.junit.Test;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.Assert.assertEquals;

public class SomeKindTest {
    @ClassRule
    public static final KindContainer<?> KUBE = new KindContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try(final KubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(KUBE.getKubeconfig()))) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }
}