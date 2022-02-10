package com.dajudge.kindcontainer;

import org.junit.ClassRule;
import org.junit.Test;

import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static org.junit.Assert.assertNotNull;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class KubectlTest {

    @ClassRule
    public static ApiServerContainer<?> K8S = new ApiServerContainer<>()
            .withKubectl(kubectl -> kubectl.apply
                    .withFile(forClasspathResource("manifests/configmap1.yaml"), "/tmp/configmap1.yaml")
                    .run("/tmp/configmap1.yaml"));

    @Test
    public void can_apply_manifest() throws Exception {
        runWithClient(K8S, client -> {
            assertNotNull(client.configMaps().inNamespace("configmap1").withName("configmap1").get());
        });
    }
}
