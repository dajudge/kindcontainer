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

import org.junit.ClassRule;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

public abstract class BaseCommonTest extends BaseKindContainerTest {
    @ClassRule
    public static ApiServerContainer<?> API_SERVER = new ApiServerContainer<>();

    @Parameterized.Parameters
    public static Collection<Object[]> apiServers() {
        return Arrays.asList(new Object[][]{{API_SERVER}, {KIND}});
    }

    protected final KubernetesContainer<?> k8s;

    protected BaseCommonTest(final KubernetesContainer<?> k8s) {
        this.k8s = k8s;
    }
}
