/*
Copyright 2020-2021 Alex Stockinger

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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;
import org.testcontainers.utility.Base58;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

abstract class BusyBoxContainer<T extends GenericContainer<T>> extends GenericContainer<T> {

    protected static final String[] COMMANDS = {"sleep", "ls", "cat", "grep", "awk", "nc", "bash", "sh"};

    protected BusyBoxContainer(final String image) {
        super(buildImageWithBusybox(image, COMMANDS));
    }

    private static String linkBusyBox(final List<String> sources) {
        return sources.stream()
                .map(s -> format("/bin/ln -s /bin/busybox /tmp/busybox/%s", s))
                .collect(joining(" && "));
    }

    private static ImageFromDockerfile buildImageWithBusybox(final String image, final String[] commands) {
        return new ImageFromDockerfile("localhost/apiservercontainer:"+ Base58.randomString(16).toLowerCase(), false)
                .withDockerfileFromBuilder(builder -> builder
                .withStatement(new SingleArgumentStatement("FROM", "busybox as builder"))
                .run("mkdir -p /tmp/busybox")
                .run(linkBusyBox(asList(commands)))
                .withStatement(new SingleArgumentStatement("FROM", image))
                .withStatement(new SingleArgumentStatement("COPY", "--from=builder /bin/busybox /bin/busybox"))
                .withStatement(new SingleArgumentStatement("COPY", "--from=builder /tmp/busybox/* /bin/"))
        );
    }
}
