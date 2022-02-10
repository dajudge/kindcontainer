package com.dajudge.kindcontainer.client.config;

public class Context {
    private String name;
    private ContextSpec context;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContextSpec getContext() {
        return context;
    }

    public void setContext(ContextSpec context) {
        this.context = context;
    }
}
