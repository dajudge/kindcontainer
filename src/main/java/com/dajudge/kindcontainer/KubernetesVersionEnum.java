package com.dajudge.kindcontainer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public interface KubernetesVersionEnum<T extends KubernetesVersionEnum<T>> {

    KubernetesVersionDescriptor descriptor();

    /**
     * Returns the list of available versions in ascending order (earliest is first).
     *
     * @param versions the enum type.
     * @return the list of available versions in ascending order (earliest is first).
     */
    static <T extends KubernetesVersionEnum<T>> List<T> ascending(final Class<T> versions) {
        return sorted(versions, comparing(KubernetesVersionEnum::descriptor));
    }

    /**
     * Returns the list of available versions in descending order (latest is first).
     *
     * @param versions the enum type.
     * @return the list of available versions in descending order (latest is first).
     */
    static <T extends KubernetesVersionEnum<T>> List<T> descending(final Class<T> versions) {
        final Comparator<T> comparing = comparing(KubernetesVersionEnum::descriptor);
        return sorted(versions, comparing.reversed());
    }

    static <T extends KubernetesVersionEnum<T>> List<T> sorted(
            final Class<T> versionsEnum,
            final Comparator<T> comparator
    ) {
        return Stream.of(versionsEnum.getEnumConstants())
                .sorted(comparator)
                .collect(toList());
    }


    /**
     * Returns the latest supported version.
     *
     * @param versions the enum type.
     * @return the latest supported version.
     */
    static <T extends KubernetesVersionEnum<T>> T latest(final Class<T> versions) {
        return descending(versions).get(0);
    }
}
