package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.K3sContainer;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

@RunWith(Parameterized.class)
public class K3sVersionTest extends AbstractVersionedTest {
    @Parameterized.Parameters
    public static Collection<K3sContainer.Version> apiServers() {
        return K3sContainer.Version.descending();
    }

    public K3sVersionTest(final K3sContainer.Version version) {
        super(() -> new K3sContainer<>(version), version.getDescriptor());
    }
}
