package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.KindContainer;

import java.util.stream.Stream;

public class KindVersionTest extends AbstractVersionedTest {
    @Override
    protected Stream<KubernetesTestPackage<?>> testPackages() {
        return KindContainer.Version.descending().stream()
                .map(version -> new KubernetesTestPackage<KindContainer<?>>(
                        KindContainer.class.getSimpleName(),
                        () -> new KindContainer<>(version),
                        version.getDescriptor()
                ));
    }
}
