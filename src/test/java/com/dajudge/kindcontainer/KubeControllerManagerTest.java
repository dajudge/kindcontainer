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

import io.fabric8.kubernetes.api.model.Pod;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Objects;

import static com.dajudge.kindcontainer.TestUtils.createSimplePod;
import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static java.util.concurrent.TimeUnit.SECONDS;

public class KubeControllerManagerTest {
    @ClassRule
    public static final ApiServerContainer<?> API_SERVER = new ApiServerContainer<>();

    @Test
    public void creates_default_service_account() {
        // When a new namespace is created, the controller manager takes care of creating the default
        // ServiceAccount
        runWithClient(API_SERVER, client -> {
            final String namespace = API_SERVER.createNamespace();
            // Make sure default SA pop up
            Awaitility.await("default SA")
                    .pollInSameThread()
                    .timeout(10, SECONDS)
                    .until(
                            () -> client.serviceAccounts()
                                    .inNamespace(namespace)
                                    .withName("default")
                                    .get(),
                            Objects::nonNull
                    );
            // Should now be able to create pods
            final Pod pod = createSimplePod(client, namespace);
            Awaitility.await("pod")
                    .pollInSameThread()
                    .timeout(10, SECONDS)
                    .until(() -> client.pods()
                            .inNamespace(namespace)
                            .withName(pod.getMetadata().getName())
                            .get() != null);
        });
    }

    @Test
    public void finalizes_terminated_namespaces() {
        // Namespaces get a finalizer "kubernetes" when they're created. The controller manager
        // takes care of removing it when all related resources are destroyed.
        runWithClient(API_SERVER, client -> {
            final String namespace = API_SERVER.createNamespace();
            client.namespaces().withName(namespace).delete();
            // Namespace should be gone eventually
            Awaitility.await()
                    .pollInSameThread()
                    .timeout(10, SECONDS)
                    .until(() -> client.namespaces().withName(namespace).get() == null);
        });
    }
}
