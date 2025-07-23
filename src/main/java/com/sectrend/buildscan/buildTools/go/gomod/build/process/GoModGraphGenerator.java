package com.sectrend.buildscan.buildTools.go.gomod.build.process;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoGraph;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.SubstituteData;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.model.Supplier;
import org.junit.platform.commons.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class GoModGraphGenerator {

    private final ForeignIdFactory foreignIdFactory;
    private final Set<String> fullyGraphedModules = new HashSet<>();

    public GoModGraphGenerator(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    public DependencyLocation generateGraph(SubstituteData projectModule, GoRelationshipManager goRelationshipManager, GoModDependencyManager goModDependencyManager) {
        MutableMapDependencyGraph graph = new MutableMapDependencyGraph();
        String moduleName = projectModule.getPath();
        if (goRelationshipManager.containsRelationships(moduleName)) {
            for (GoGraph relationship : goRelationshipManager.getRelationships(moduleName)) {
                String childName = relationship.getChild().getName();
                if (StringUtils.isBlank(childName)) {
                    continue;
                }
                processModuleAndDependencies(childName, null, graph, goRelationshipManager, goModDependencyManager);
            }
        }
        ForeignId foreignId = foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, projectModule.getPath(), projectModule.getVersion());
        return new DependencyLocation(graph, foreignId);
    }


    private void processModuleAndDependencies(String moduleName, Dependency parent, MutableMapDependencyGraph graph,
                                              GoRelationshipManager relationshipManager, GoModDependencyManager dependencyManager) {
        if (shouldSkipModule(moduleName, relationshipManager)) {
            return;
        }

        Dependency current = getAndRegisterDependency(moduleName, parent, graph, dependencyManager);

        if (shouldProcessDependencies(moduleName, relationshipManager)) {
            processChildDependencies(moduleName, current, graph, relationshipManager, dependencyManager);
        }
    }


    /**
     * 生成Dependency对象并且放入graph
     * @param moduleName
     * @param parent
     * @param graph
     * @param dependencyManager
     * @return
     */
    private Dependency getAndRegisterDependency(String moduleName, Dependency parent, MutableMapDependencyGraph graph, GoModDependencyManager dependencyManager) {
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        registerDependency(graph, parent, dependency);
        return dependency;
    }

    /**
     * 根据是否有父节点，判断如何处理子节点
     * @param graph
     * @param parent
     * @param current
     */
    private void registerDependency(MutableMapDependencyGraph graph, Dependency parent, Dependency current) {
        if (parent != null) {
            graph.addChildWithParent(current, parent);
        } else {
            graph.addDirectDependency(current);
        }
    }

    /**
     * 处理子节点
     * @param moduleName
     * @param parent
     * @param graph
     * @param relationshipManager
     * @param dependencyManager
     */
    private void processChildDependencies(String moduleName, Dependency parent, MutableMapDependencyGraph graph,
                                          GoRelationshipManager relationshipManager, GoModDependencyManager dependencyManager) {
        fullyGraphedModules.add(moduleName);
        relationshipManager.getRelationships(moduleName)
                .forEach(rel -> processChildRelationship(rel, parent, graph, relationshipManager, dependencyManager));
    }

    /**
     * 处理子节点和其他节点的关系
     * @param relationship
     * @param parent
     * @param graph
     * @param relationshipManager
     * @param dependencyManager
     */
    private void processChildRelationship(GoGraph relationship, Dependency parent, MutableMapDependencyGraph graph,
                                          GoRelationshipManager relationshipManager, GoModDependencyManager dependencyManager) {
        String childName = relationship.getChild().getName();
        processModuleAndDependencies(childName, parent, graph, relationshipManager, dependencyManager);
    }

    /**
     * 是否应该跳过该module
     * @param moduleName
     * @param manager
     * @return
     */
    private boolean shouldSkipModule(String moduleName, GoRelationshipManager manager) {
        return manager.isNotUsedByMainModule(moduleName);
    }

    /**
     * 是否满足处理这个依赖的条件
     * @param moduleName
     * @param manager
     * @return
     */
    private boolean shouldProcessDependencies(String moduleName, GoRelationshipManager manager) {
        return !fullyGraphedModules.contains(moduleName) && manager.containsRelationships(moduleName);
    }
}
