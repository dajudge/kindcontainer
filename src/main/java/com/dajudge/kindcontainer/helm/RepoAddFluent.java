package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;

public interface RepoAddFluent {
    void run(final String name, final String repo) throws IOException, InterruptedException, ExecutionException;
}
