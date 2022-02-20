package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;

public class RepoFluent<P> {
    private final BaseSidecarContainer.ExecInContainer c;
    private final P parent;

    RepoFluent(final BaseSidecarContainer.ExecInContainer c, final P parent) {
        this.c = c;
        this.parent = parent;
    }

    public final RepoAddFluent<P> add = new RepoAddFluent<P>() {
        @Override
        public P run(final String name, final String repo) throws IOException, InterruptedException, ExecutionException {
            c.safeExecInContainer("helm", "repo", "add", name, repo);
            return parent;
        }
    };

    public RepoUpdateFluent<P> update = new RepoUpdateFluent<P>() {
        @Override
        public P run() throws IOException, InterruptedException, ExecutionException {
            c.safeExecInContainer("helm", "repo", "update");
            return parent;
        }
    };
}
