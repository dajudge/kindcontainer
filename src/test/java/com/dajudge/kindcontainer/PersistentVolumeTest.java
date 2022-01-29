/*
Copyright 2020-2022 Alex Stockinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static com.dajudge.kindcontainer.StaticContainers.KIND;
import static com.dajudge.kindcontainer.TestUtils.isRunning;
import static com.dajudge.kindcontainer.TestUtils.randomIdentifier;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PersistentVolumeTest {

    private String namespace;

    @Before
    public void before() {
        namespace = KIND.runWithClient(TestUtils::createNewNamespace);
    }

    @Test
    public void can_start_pod_with_pvc() {
        final PersistentVolumeClaim claim = KIND.runWithClient(client -> {
            return client.persistentVolumeClaims().inNamespace(namespace).create(buildClaim());
        });
        final Pod pod = KIND.runWithClient(client -> {
            return client.pods().inNamespace(namespace).create(buildPod(claim));
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> KIND.runWithClient(client -> {
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
