package com.sectrend.buildscan.result;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.MutableDependencyGraph;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * </p>
 * @author yhx
 * @date 2022/6/13 17:16
 */
public class ResultTransform {
    public static Map transformRootResult(DependencyLocation dependencyLocation) {
        Map<String, List<String>> result = new HashMap<>();
        MutableDependencyGraph dependencyGraph = (MutableDependencyGraph) dependencyLocation.getDependencyGraph();
        Set<Dependency> rootDependencies = dependencyGraph.getRootDependencies();
        // 获取直接依赖的子依赖
        for (Dependency dependency : rootDependencies) {
            Set<Dependency> childrenForParent = dependencyGraph.getChildrenForParent(dependency);
            result.put(dependency.toStringForeignIds(),
                    childrenForParent
                            .stream()
                            .map(Dependency::getForeignId)
                            .map(ForeignId::toStringForeignIds)
                            .collect(Collectors.toList()));
        }
        return result;
    }

    public static Map transformRelationshipsResult(DependencyLocation dependencyLocation) {
        Map<String, List<String>> result = new HashMap<>();
        DependencyGraph dependencyGraph = dependencyLocation.getDependencyGraph();
        Map<ForeignId, Set<ForeignId>> relationships = dependencyGraph.getRelationships();
        // 转换依赖关系
        for (Map.Entry entry : relationships.entrySet()) {
            ForeignId key = (ForeignId) entry.getKey();
            Set<ForeignId> foreignIds = (Set<ForeignId>) entry.getValue();
            result.put(key.toStringForeignIds(), foreignIds.stream().map(ForeignId::toStringForeignIds).collect(Collectors.toList()));
        }
        return result;
    }
}
