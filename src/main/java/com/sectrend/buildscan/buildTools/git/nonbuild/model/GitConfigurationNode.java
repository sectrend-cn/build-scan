package com.sectrend.buildscan.buildTools.git.nonbuild.model;


import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public class GitConfigurationNode {

    @Nullable
    private final String nodeName;

    private final Map<String, String> properties;

    private final String nodeType;

    public GitConfigurationNode(String nodeType, @Nullable String nodeName, Map<String, String> properties) {
        this.nodeType = nodeType;
        this.nodeName = nodeName;
        this.properties = properties;
    }

    public String getNodeType() {
        return this.nodeType;
    }

    public Optional<String> getProperty(String propertyKey) {
        return Optional.ofNullable(this.properties.get(propertyKey));
    }

    public Optional<String> getNodeName() {
        return Optional.ofNullable(this.nodeName);
    }

}
