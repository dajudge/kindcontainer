package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;

public class RepoFluent {
    private final BaseSidecarContainer.ExecInContainer c;

    RepoFluent(final BaseSidecarContainer.ExecInContainer c) {
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
