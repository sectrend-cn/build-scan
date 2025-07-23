package com.sectrend.buildscan.base.graph;

import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:54
 */
public interface DependencyGraph {
    Set<Dependency> getRootDependencies();

    Map<ForeignId, Set<ForeignId>> getRelationships();

    Set<ForeignId> getRootDependencyForeignIds();

    boolean hasDependency(Dependency paramDependency);

    boolean hasDependency(ForeignId paramForeignId);

    Dependency getDependency(ForeignId paramForeignId);

    Set<Dependency> getChildrenForParent(Dependency paramDependency);

    Set<ForeignId> getChildrenForeignIdsForParent(Dependency paramDependency);

    Set<Dependency> getChildrenForParent(ForeignId paramForeignId);

    Set<ForeignId> getChildrenForeignIdsForParent(ForeignId paramForeignId);

    Set<ForeignId> getParentForeignIdsForChild(Dependency paramDependency);

    Set<Dependency> getParentsForChild(ForeignId paramForeignId);

    Set<Dependency> getParentsForChild(Dependency paramDependency);

    Set<ForeignId> getParentForeignIdsForChild(ForeignId paramForeignId);

    void addDirectDependency(Dependency paramDependency);

    void copyGraphToRoot(DependencyGraph sourceGraph);
}