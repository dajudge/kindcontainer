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

import org.junit.runners.Parameterized;

import java.util.Collection;

public class ApiServerVersionTest extends AbstractVersionedTest {
    @Parameterized.Parameters
    public static Collection<ApiServerContainer.Version> apiServers() {
        return ApiServerContainer.Version.descending();
    }

    public ApiServerVersionTest(final ApiServerContainer.Version version) {
        super(() -> createApiServerContainer(version), version.descriptor);
    }

    private static ApiServerContainer<?> createApiServerContainer(final ApiServerContainer.Version version) {
        return new ApiServerContainer<>(version);
    }
}
