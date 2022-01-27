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
package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;

public class RepoFluent {
    private final ExecInContainer c;

    RepoFluent(final ExecInContainer c) {
        this.c = c;
    }

    public final RepoAddFluent add = new RepoAddFluent() {
        @Override
        public void run(final String name, final String repo) throws IOException, InterruptedException, ExecutionException {
            c.safeExecInContainer("helm", "repo", "add", name, repo);
        }
    };

    public RepoUpdateFluent update = new RepoUpdateFluent() {
        @Override
        public void run() throws IOException, InterruptedException, ExecutionException {
            c.safeExecInContainer("helm", "repo", "update");
        }
    };
}
