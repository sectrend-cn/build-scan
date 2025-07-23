package com.sectrend.buildscan.executable.impl;

public class CachedExecutableResolverOptions {
    private final boolean python3;

    public CachedExecutableResolverOptions(boolean python3) {
        this.python3 = python3;
    }

    public boolean isPython3() {
        return this.python3;
    }
}
