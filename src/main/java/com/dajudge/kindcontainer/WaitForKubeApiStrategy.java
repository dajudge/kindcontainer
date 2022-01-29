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

import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

public class WaitForKubeApiStrategy extends HostPortWaitStrategy {
    private final WaitStrategy delegate;

    public WaitForKubeApiStrategy(final WaitStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public void waitUntilReady() {
        super.waitUntilReady();
        delegate.waitUntilReady(waitStrategyTarget);
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(singletonList(waitStrategyTarget.getMappedPort(6443)));
    }
}
