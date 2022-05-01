package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

public class ConfigurableNodePortRangeTest extends BaseFullContainersTest {
    private DefaultKubernetesClient client;

    public ConfigurableNodePortRangeTest(final KubernetesWithKubeletContainer<?> k8s) {
        super(k8s.withNodePortRange(20000, 20010));
    }

    @Before
    public void before() {
        client = new DefaultKubernetesClient(fromKubeconfig(k8s.getKubeconfig()));
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void can_expose_in_valid_range() {
        createService("valid-port-service-min", 20000);
        createService("valid-port-service-max", 20010);
    }

    @Test(expected = KubernetesClientException.class)
    public void cannot_expose_outside_of_valid_range() {
        createService("invalid-port-service", 20011);
    }

    private void createService(String name, int nodePort) {
        client.services().create(new ServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace("default")
                .endMetadata()
                .withSpec(new ServiceSpecBuilder()
                        .withSelector(new HashMap<String, String>(){{
                            put("app", "doesNotExist");
                        }})
                        .withType("NodePort")
                        .withPorts(new ServicePortBuilder()
                                .withName("port")
                                .withPort(80)
                                .withNodePort(nodePort)
                                .build())
                        .build())
                .build());
    }
}
