package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.*;
import org.junit.Test;

import java.util.HashMap;

import static com.dajudge.kindcontainer.TestUtils.*;
import static com.dajudge.kindcontainer.StaticContainers.kind;
import static com.dajudge.kindcontainer.TestUtils.createSimplePod;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class NodePortTest {

    @Test
    public void exposes_node_port() {
        final Pod pod =runWithClient( kind(), client -> {
            final String namespace = kind().createNamespace();
            return createSimplePod(client, namespace);
        });
        runWithClient(kind(), client -> {
            client.services().create(new ServiceBuilder()
                    .withNewMetadata()
                    .withName("nginx")
                    .withNamespace(pod.getMetadata().getNamespace())
                    .endMetadata()
                    .withNewSpec()
                    .withType("NodePort")
                    .withSelector(new HashMap<String, String>() {{
                        put("app", "nginx");
                    }})
                    .withPorts(new ServicePortBuilder()
                            .withNodePort(30000)
                            .withPort(80)
                            .withTargetPort(new IntOrString(80))
                            .withProtocol("TCP")
                            .build())
                    .endSpec()
                    .build());
            await("testpod")
                    .timeout(ofSeconds(300))
                    .until(TestUtils.http("http://localhost:" + kind().getMappedPort(30000)));

        });
    }

}
