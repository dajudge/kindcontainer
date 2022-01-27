/*
Copyright 2020-2021 Alex Stockinger

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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NetworkingTest extends BaseCommonTest {

    public NetworkingTest(final KubernetesContainer<?> k8s) {
        super(k8s);
    }

    @Test
    public void can_connect_internally() {
        try (final GenericContainer<?> curl = createContainer()) {
            curl.start();
            try {
                final String url = String.format("https://%s:%d", k8s.getInternalHostname(), k8s.getInternalPort());
                final Container.ExecResult result = curl.execInContainer("curl", "-vk", url);
                assertEquals(0, result.getExitCode());
            } catch (final IOException | InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private GenericContainer<?> createContainer() {
        return new GenericContainer<>("curlimages/curl:7.81.0")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sh", "-c", "trap 'echo signal;exit 0' SIGTERM; while : ; do sleep 1 ; done");
                })
                .withNetwork(k8s.getNetwork());
    }
}
