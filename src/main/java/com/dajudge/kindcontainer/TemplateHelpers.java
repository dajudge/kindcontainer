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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

import java.util.Map;
import java.util.function.Function;

import static com.dajudge.kindcontainer.Utils.loadResource;
import static java.nio.charset.StandardCharsets.UTF_8;

final class TemplateHelpers {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateHelpers.class);

    private TemplateHelpers() {
        throw new IllegalStateException("Do not instantiate");
    }

    static String templateContainerFile(
            final GenericContainer<?> container,
            final String sourceFileName,
            final String destFileName,
            final Map<String, String> params
    ) {
        final String template = readContainerFile(container, sourceFileName);
        final String interpolated = template(template, params);
        return writeContainerFile(container, interpolated, destFileName);
    }

    private static String readContainerFile(final GenericContainer<?> container, final String fname) {
        return container.copyFileFromContainer(fname, Utils::readString);
    }

    static String writeContainerFile(final GenericContainer<?> container, final String content, final String fname) {
        LOG.info("Writing container file: {}", fname);
        container.copyFileToContainer(Transferable.of(content.getBytes(UTF_8)), fname);
        return fname;
    }

    private static String template(String string, final Map<String, String> replacements) {
        return replacements.entrySet().stream()
                .map(r -> ((Function<String, String>) (s -> s.replace("{{ " + r.getKey() + " }}", r.getValue()))))
                .reduce(Function.identity(), Function::andThen)
                .apply(string);
    }

    static String templateResource(final String resource, final Map<String, String> replacements) {
        return template(loadResource(resource), replacements);
    }

    static String templateResource(
            final GenericContainer<?> container,
            final String resource, final Map<String, String> params,
            final String destFileName
    ) {
        return writeContainerFile(container, templateResource(resource, params), destFileName);
    }
}
