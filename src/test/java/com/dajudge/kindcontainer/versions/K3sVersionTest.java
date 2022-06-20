package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.K3sContainer;
import com.dajudge.kindcontainer.K3sContainerVersion;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.descending;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.versionFilter;

public class K3sVersionTest extends AbstractVersionedTest {
    @Override
    protected Stream<KubernetesTestPackage<?>> testPackages() {
        return descending(K3sContainerVersion.class).stream()
                .filter(versionFilter(K3sContainer.class.getSimpleName(), "all"))
                .map(version -> new KubernetesTestPackage<K3sContainer<?>>(
                        K3sContainer.class.getSimpleName(),
                        () -> new K3sContainer<>(version),
                        version.descriptor()
                ));
    }
}
