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

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class KubectlTest {

    @ClassRule
    public static ApiServerContainer<?> K8S = new ApiServerContainer<>()
            .withKubectl(kubectl -> kubectl.apply
                    .withFile(forClasspathResource("manifests/configmap1.yaml"), "/tmp/configmap1.yaml")
                    .run("/tmp/configmap1.yaml"));

    @Test
    public void can_apply_manifest() {
        K8S.runWithClient(client -> {
            assertNotNull(client.configMaps().inNamespace("configmap1").withName("configmap1").get());
        });
    }
}
