
package com.sectrend.buildscan.base.graph;

import com.sectrend.buildscan.model.Dependency;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:56
 */

public class DependencyGraphCombiner {
    public void addGraphAsChildrenToRoot(MutableDependencyGraph destinationGraph, DependencyGraph sourceGraph) {
        Set<Dependency> encountered = new HashSet<>();
        for (Dependency dependency : sourceGraph.getRootDependencies()) {
            destinationGraph.addChildToRoot(dependency);
            copyDependencyFromGraph(destinationGraph, dependency, sourceGraph, encountered);
        }
    }

    public void addGraphAsChildrenToParent(MutableDependencyGraph destinationGraph, Dependency parent, DependencyGraph sourceGraph) {
        Set<Dependency> encountered = new HashSet<>();
        for (Dependency dependency : sourceGraph.getRootDependencies()) {
            destinationGraph.addChildWithParent(dependency, parent);
            copyDependencyFromGraph(destinationGraph, dependency, sourceGraph, encountered);
        }
    }

    public void copyDependencyFromGraph(MutableDependencyGraph destinationGraph, Dependency parentDependency, DependencyGraph sourceGraph, Set<Dependency> encountered) {
        for (Dependency dependency : sourceGraph.getChildrenForParent(parentDependency)) {
            if (!encountered.contains(dependency)) {
                encountered.add(dependency);
                copyDependencyFromGraph(destinationGraph, dependency, sourceGraph, encountered);
            }
            destinationGraph.addChildWithParent(dependency, parentDependency);
        }
    }
}
