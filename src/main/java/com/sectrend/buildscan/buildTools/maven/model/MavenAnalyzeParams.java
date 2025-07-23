package com.sectrend.buildscan.buildTools.maven.model;

public class MavenAnalyzeParams {

    private final boolean includePlugins;

    public MavenAnalyzeParams(boolean includePlugins) {
        this.includePlugins = includePlugins;
    }

    public boolean isIncludePlugins() {
        return this.includePlugins;
    }
}
