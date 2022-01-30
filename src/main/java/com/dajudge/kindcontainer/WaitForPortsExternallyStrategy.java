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

import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class WaitForPortsExternallyStrategy extends AbstractWaitStrategy {

    @Override
    public void waitUntilReady() {
        final Set<Integer> ports = waitStrategyTarget.getExposedPorts()
                .stream()
                .map(waitStrategyTarget::getMappedPort)
                .collect(toSet());
        final ExternalPortListeningCheck check = new ExternalPortListeningCheck(waitStrategyTarget, ports);
        Awaitility.await()
                .pollInSameThread()
                .pollInterval(Duration.ofMillis(100))
                .pollDelay(Duration.ZERO)
                .ignoreExceptions()
                .forever()
                .until(check);
    }
}

