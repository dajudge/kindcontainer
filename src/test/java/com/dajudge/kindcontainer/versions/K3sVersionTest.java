package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.K3sContainer;

import java.util.stream.Stream;

public class K3sVersionTest extends AbstractVersionedTest {
    @Override
    protected Stream<KubernetesTestPackage<?>> testPackages() {
        return K3sContainer.Version.descending().stream()
                .map(version -> new KubernetesTestPackage<K3sContainer<?>>(
                        K3sContainer.class.getSimpleName(),
                        () -> new K3sContainer<>(version),
                        version.getDescriptor()
                ));
    }
}
