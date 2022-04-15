package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.Test;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.Assert.assertNotNull;

public class KubeconfigTest extends BaseAllContainersTest {

    public KubeconfigTest(final KubernetesContainer<?> k8s) {
        super(k8s.withKubeconfig(kubeconfig -> {
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(kubeconfig))) {
                client.inNamespace("default").configMaps().create(new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName("test")
                        .endMetadata()
                        .build());
            }
        }));
    }

    @Test
    public void with_kubeconfig_gets_executed() {
        runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("default").configMaps().withName("test").get());
        });
    }
}
