package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import io.fabric8.kubernetes.api.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static com.dajudge.kindcontainer.util.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PersistentVolumeTest extends BaseFullContainersTest {

    private String namespace;

    public PersistentVolumeTest(final KubernetesWithKubeletContainer<?> k8s) {
        super(k8s);
    }

    @Before
    public void before() {
        namespace = runWithClient(k8s, TestUtils::createNewNamespace);
    }

    @Test
    public void can_start_pod_with_pvc() {
        final PersistentVolumeClaim claim = runWithClient(k8s, client -> {
            return client.persistentVolumeClaims().inNamespace(namespace).create(buildClaim());
        });
        final Pod pod = runWithClient(k8s, client -> {
            return client.pods().inNamespace(namespace).create(buildPod(claim));
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> runWithClient(k8s, client -> {
                    return isRunning(client, pod);
                }));
    }

    private PersistentVolumeClaim buildClaim() {
        return new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(randomIdentifier())
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests(new HashMap<String, Quantity>() {{
                    put("storage", new Quantity("1", "Gi"));
                }})
                .endResources()
                .endSpec()
                .build();
    }

    private Pod buildPod(final PersistentVolumeClaim claim) {
        return new PodBuilder()
                .withNewMetadata()
                .withName("testpod")
                .withNamespace(namespace)
                .withLabels(new HashMap<String, String>() {{
                    put("app", "nginx");
                }})
                .endMetadata()
                .withNewSpec()
                .withVolumes(new VolumeBuilder()
                        .withName("test-volume")
                        .withNewPersistentVolumeClaim(claim.getMetadata().getName(), false)
                        .build())
                .withContainers(new ContainerBuilder()
                        .withName("test")
                        .withImage("nginx")
                        .withVolumeMounts(new VolumeMountBuilder()
                                .withName("test-volume")
                                .withMountPath("/testVolume")
                                .build())
                        .withPorts(new ContainerPortBuilder()
                                .withContainerPort(80)
                                .withProtocol("TCP")
                                .build())
                        .build())
                .endSpec()
                .build();
    }
}
