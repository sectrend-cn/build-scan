package com.sectrend.buildscan.base.graph;

import com.sectrend.buildscan.model.Dependency;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:54
 */
public interface MutableDependencyGraph extends DependencyGraph {
    void addGraphAsChildrenToRoot(DependencyGraph paramDependencyGraph);

    void addGraphAsChildrenToParent(Dependency paramDependency, DependencyGraph paramDependencyGraph);

    void addParentWithChild(Dependency paramDependency1, Dependency paramDependency2);

    void addParentWithChildren(Dependency paramDependency, List<Dependency> paramList);

    void addParentWithChildren(Dependency paramDependency, Set<Dependency> paramSet);

    void addParentWithChildren(Dependency paramDependency, Dependency... paramVarArgs);

    void addChildWithParent(Dependency paramDependency1, Dependency paramDependency2);

    void addChildWithParents(Dependency paramDependency, List<Dependency> paramList);

    void addChildWithParents(Dependency paramDependency, Set<Dependency> paramSet);

    void addChildWithParents(Dependency paramDependency, Dependency... paramVarArgs);

    void addChildToRoot(Dependency paramDependency);

    void addChildrenToRoot(List<Dependency> paramList);

    void addChildrenToRoot(Set<Dependency> paramSet);

    void addChildrenToRoot(Dependency... paramVarArgs);
}
