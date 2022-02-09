package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;

public interface RepoUpdateFluent {
    void run() throws IOException, InterruptedException, ExecutionException;
}
