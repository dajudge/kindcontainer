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

public class KindVersionTest extends AbstractVersionedTest {
    @Parameterized.Parameters
    public static Collection<KindContainer.Version> apiServers() {
        return KindContainer.Version.descending();
    }

    public KindVersionTest(final KindContainer.Version version) {
        super(() -> createKindContainer(version), version.descriptor);
    }

    private static KindContainer<?> createKindContainer(final KindContainer.Version version) {
        return new KindContainer<>(version);
    }
}
