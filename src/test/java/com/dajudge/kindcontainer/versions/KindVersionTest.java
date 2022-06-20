package com.dajudge.kindcontainer.versions;

import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.descending;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.versionFilter;

public class KindVersionTest extends AbstractVersionedTest {
    @Override
    protected Stream<KubernetesTestPackage<?>> testPackages() {
        return descending(KindContainerVersion.class).stream()
                .filter(versionFilter(KindContainer.class.getSimpleName(), "all"))
                .map(version -> new KubernetesTestPackage<KindContainer<?>>(
                        KindContainer.class.getSimpleName(),
                        () -> new KindContainer<>(version),
                        version.descriptor()
                ));
    }
}
