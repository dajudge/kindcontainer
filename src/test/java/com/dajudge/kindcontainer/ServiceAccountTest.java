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

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class ServiceAccountTest {
    @ClassRule
    public static final KindContainer<?> k8s = new KindContainer<>()
            .withKubectl(kubectl -> {
                kubectl.apply
                        .withFile(forClasspathResource("manifests/serviceaccount1.yaml"), "/tmp/manifest.yaml")
                        .run("/tmp/manifest.yaml");
            });

    @Test
    public void creates_client_for_service_account() {
        k8s.runWithClient("my-namespace", "my-service-account", client -> {
            client.inNamespace("my-namespace").pods().list();
            try {
                client.inNamespace("my-namespace").secrets().list();
                fail("Should not be able to list secrets");
            } catch (final KubernetesClientException e) {
                // expected
            }
        });
    }
}
