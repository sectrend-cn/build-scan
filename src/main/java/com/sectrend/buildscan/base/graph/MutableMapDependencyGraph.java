package com.sectrend.buildscan.base.graph;

import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.utils.MutableMapDependencyGraphUtil;

import java.util.*;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:55
 */

public class MutableMapDependencyGraph implements MutableDependencyGraph {
    private final Set<ForeignId> rootDependencies = new HashSet<>();

    private final Map<ForeignId, Dependency> dependencies = new HashMap<>();

    private final Map<ForeignId, Set<ForeignId>> relationships = new HashMap<>();

    private final DependencyGraphCombiner dependencyGraphCombiner = new DependencyGraphCombiner();

    public void addGraphAsChildrenToRoot(DependencyGraph sourceGraph) {
        this.dependencyGraphCombiner.addGraphAsChildrenToRoot(this, sourceGraph);
    }

    public void addGraphAsChildrenToParent(Dependency parent, DependencyGraph sourceGraph) {
        this.dependencyGraphCombiner.addGraphAsChildrenToParent(this, parent, sourceGraph);
    }

    public boolean hasDependency(ForeignId dependency) {
        return this.dependencies.containsKey(dependency);
    }

    public boolean hasDependency(Dependency dependency) {
        return this.dependencies.containsKey(dependency.getForeignId());
    }

    public Dependency getDependency(ForeignId dependency) {
        if (this.dependencies.containsKey(dependency))
            return this.dependencies.get(dependency);
        return null;
    }

    public Set<Dependency> getChildrenForParent(ForeignId parent) {
        Set<ForeignId> childIds = getChildrenForeignIdsForParent(parent);
        return dependenciesFromForeignIds(childIds);
    }

    public Set<Dependency> getParentsForChild(ForeignId child) {
        Set<ForeignId> parentIds = getParentForeignIdsForChild(child);
        return dependenciesFromForeignIds(parentIds);
    }

    public Set<ForeignId> getChildrenForeignIdsForParent(ForeignId parent) {
        Set<ForeignId> children = new HashSet<>();
        if (this.relationships.containsKey(parent))
            children.addAll(this.relationships.get(parent));
        return children;
    }

    public Set<ForeignId> getParentForeignIdsForChild(ForeignId child) {
        Set<ForeignId> parents = new HashSet<>();
        for (Map.Entry<ForeignId, Set<ForeignId>> foreignIdSetEntry : this.relationships.entrySet()) {
            ForeignId parentId = foreignIdSetEntry.getKey();
            for (ForeignId childId : foreignIdSetEntry.getValue()) {
                if (childId.equals(child))
                    parents.add(parentId);
            }
        }
        return parents;
    }

    public Set<Dependency> getChildrenForParent(Dependency parent) {
        return getChildrenForParent(parent.getForeignId());
    }

    public Set<Dependency> getParentsForChild(Dependency child) {
        return getParentsForChild(child.getForeignId());
    }

    public Set<ForeignId> getChildrenForeignIdsForParent(Dependency parent) {
        return getChildrenForeignIdsForParent(parent.getForeignId());
    }

    public Set<ForeignId> getParentForeignIdsForChild(Dependency child) {
        return getParentForeignIdsForChild(child.getForeignId());
    }

    @Override
    public void addParentWithChild(Dependency parent, Dependency child) {
        ensureDependencyAndRelationshipExists(parent);
        ensureDependencyExists(child);
        addRelationship(parent, child);
    }

    public void addChildWithParent(Dependency child, Dependency parent) {
        addParentWithChild(parent, child);
    }

    public void addParentWithChildren(Dependency parent, List<Dependency> children) {
        ensureDependencyAndRelationshipExists(parent);
        for (Dependency child : children) {
            ensureDependencyExists(child);
            addRelationship(parent, child);
        }
    }

    public void addChildWithParents(Dependency child, List<Dependency> parents) {
        ensureDependencyExists(child);
        for (Dependency parent : parents) {
            ensureDependencyAndRelationshipExists(parent);
            addRelationship(parent, child);
        }
    }

    public void addParentWithChildren(Dependency parent, Set<Dependency> children) {
        ensureDependencyAndRelationshipExists(parent);
        for (Dependency child : children) {
            ensureDependencyExists(child);
            addRelationship(parent, child);
        }
    }

    public void addChildWithParents(Dependency child, Set<Dependency> parents) {
        ensureDependencyExists(child);
        for (Dependency parent : parents) {
            ensureDependencyAndRelationshipExists(parent);
            addRelationship(parent, child);
        }
    }

    public void addParentWithChildren(Dependency parent, Dependency... children) {
        addParentWithChildren(parent, Arrays.asList(children));
    }

    public void addChildWithParents(Dependency child, Dependency... parents) {
        addChildWithParents(child, Arrays.asList(parents));
    }

    public Set<ForeignId> getRootDependencyForeignIds() {
        HashSet<ForeignId> copy = new HashSet<>();
        copy.addAll(this.rootDependencies);
        return copy;
    }

    public Set<Dependency> getRootDependencies() {
        return dependenciesFromForeignIds(getRootDependencyForeignIds());
    }

    @Override
    public Map<ForeignId, Set<ForeignId>> getRelationships() {
        return this.relationships;
    }

    public void addChildToRoot(Dependency child) {
        ensureDependencyExists(child);
        this.rootDependencies.add(child.getForeignId());
    }

    public void addChildrenToRoot(List<Dependency> children) {
        for (Dependency child : children)
            addChildToRoot(child);
    }

    public void addChildrenToRoot(Set<Dependency> children) {
        for (Dependency child : children)
            addChildToRoot(child);
    }

    public void addChildrenToRoot(Dependency... children) {
        for (Dependency child : children)
            addChildToRoot(child);
    }

    private void ensureDependencyExists(Dependency dependency) {
        if (!this.dependencies.containsKey(dependency.getForeignId()))
            this.dependencies.put(dependency.getForeignId(), dependency);
    }

    private void ensureDependencyAndRelationshipExists(Dependency dependency) {
        ensureDependencyExists(dependency);
        if (!this.relationships.containsKey(dependency.getForeignId()))
            this.relationships.put(dependency.getForeignId(), new HashSet<>());
    }

    private void addRelationship(Dependency parent, Dependency child) {
        ((Set<ForeignId>) this.relationships.get(parent.getForeignId())).add(child.getForeignId());
    }

    private Set<Dependency> dependenciesFromForeignIds(Set<ForeignId> ids) {
        Set<Dependency> foundDependencies = new HashSet<>();
        for (ForeignId id : ids) {
            if (this.dependencies.containsKey(id))
                foundDependencies.add(this.dependencies.get(id));
        }
        return foundDependencies;
    }

    public void addDirectDependency(Dependency child){
        ensureDependencyExists(child);
        this.rootDependencies.add(child.getForeignId());
    }

    public void copyGraphToRoot(DependencyGraph sourceGraph) {
        MutableMapDependencyGraphUtil.copyRootMutableMapDependencies(this, sourceGraph);
    }
}

