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

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.Test;

import java.util.Objects;

import static com.dajudge.kindcontainer.StaticContainers.apiServer;
import static com.dajudge.kindcontainer.StaticContainers.kind;
import static com.dajudge.kindcontainer.TestUtils.createSimplePod;
import static com.dajudge.kindcontainer.TestUtils.isRunning;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest {
    @Test
    public void can_start_pod_in_kind() {
        final Namespace namespace = kind().createNamespace();
        final Pod pod = kind().runWithClient(client -> {
            return createSimplePod(client, namespace.getMetadata().getName());
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> kind().runWithClient(client -> {
                    return isRunning(client, pod);
                }));
    }

    @Test
    public void can_create_pod_in_apiserver() {
        final Namespace namespace = apiServer().createNamespace();
        final Pod pod = apiServer().runWithClient(client -> {
            return createSimplePod(client, namespace.getMetadata().getName());
        });
        await("testpod")
                .until(() -> apiServer().runWithClient(client -> {
                    return client.pods()
                            .inNamespace(namespace.getMetadata().getName())
                            .withName(pod.getMetadata().getName())
                            .get();
                }), Objects::nonNull);
    }
}
