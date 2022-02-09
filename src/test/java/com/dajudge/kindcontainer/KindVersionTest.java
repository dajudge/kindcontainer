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
