
package com.sectrend.buildscan.base.graph;

import com.sectrend.buildscan.model.ForeignId;

import java.io.File;
import java.util.Optional;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:58
 */
public class DependencyLocation {
    private File sourcePath;

    private final ForeignId foreignId;

    private final DependencyGraph dependencyGraph;

    public DependencyLocation(DependencyGraph dependencyGraph) {
        this(dependencyGraph, null, null);
    }

    public DependencyLocation(DependencyGraph dependencyGraph, File sourcePath) {
        this(dependencyGraph, null, sourcePath);
    }

    public DependencyLocation(DependencyGraph dependencyGraph, ForeignId foreignId) {
        this(dependencyGraph, foreignId, null);
    }

    public DependencyLocation(DependencyGraph dependencyGraph, ForeignId foreignId, File sourcePath) {
        this.sourcePath = sourcePath;
        this.foreignId = foreignId;
        this.dependencyGraph = dependencyGraph;
    }

    public Optional<File> getSourcePath() {
        return Optional.ofNullable(this.sourcePath);
    }

    public void setSourcePath(File sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Optional<ForeignId> getForeignId() {
        return Optional.ofNullable(this.foreignId);
    }

    public DependencyGraph getDependencyGraph() {
        return this.dependencyGraph;
    }
}
