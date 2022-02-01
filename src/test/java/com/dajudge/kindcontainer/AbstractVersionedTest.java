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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public abstract class AbstractVersionedTest {

    protected final Supplier<KubernetesContainer<?>> k8sFactory;
    protected final KubernetesVersionDescriptor version;

    protected AbstractVersionedTest(
            final Supplier<KubernetesContainer<?>> k8sFactory,
            final KubernetesVersionDescriptor version
    ) {
        this.k8sFactory = k8sFactory;
        this.version = version;
    }

    @Test
    public void can_start() {
        final KubernetesContainer<?> k8s = k8sFactory.get();
        try {
            k8s.start();
            k8s.runWithClient(client -> {
                assertEquals(client.getKubernetesVersion().getGitVersion(), version.getKubernetesVersion());
            });
        } catch (final Exception e) {
            throw new AssertionError("Failed to launch kubernetes with version " + version.toString(), e);
        } finally {
            k8s.stop();
        }
    }
}
