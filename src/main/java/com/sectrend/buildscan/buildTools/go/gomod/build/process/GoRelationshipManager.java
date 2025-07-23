package com.sectrend.buildscan.buildTools.go.gomod.build.process;

import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoGraph;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GoRelationshipManager {
    private final Map<String, List<GoGraph>> goGraphRelationMap;
    private final Set<String> excludedModules;

    public GoRelationshipManager(List<GoGraph> goGraphs, Set<String> excludedModules) {
        this.excludedModules = excludedModules;
        this.goGraphRelationMap = buildRelationshipMap(goGraphs);
    }

    public boolean isNotUsedByMainModule(String moduleName) {
        return excludedModules.contains(moduleName);
    }

    private Map<String, List<GoGraph>> buildRelationshipMap(List<GoGraph> relationships) {
        return relationships.stream().collect(
                Collectors.groupingBy(rel -> rel.getParent().getName(), Collectors.toCollection(LinkedList::new))
        );
    }

    public List<GoGraph> getRelationships(String moduleName) {
        return goGraphRelationMap.getOrDefault(moduleName, Collections.emptyList());
    }

    public boolean containsRelationships(String moduleName) {
        return goGraphRelationMap.containsKey(moduleName);
    }

}
