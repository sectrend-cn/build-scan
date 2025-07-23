package com.sectrend.buildscan.buildTools.pipenv.build;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.pipenv.build.model.*;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.model.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class PipenvConverter {

    private static final Logger logger = LoggerFactory.getLogger(PipenvConverter.class);


   /* private final ForeignIdFactory externalIdFactory;
    public PipenvConverter(ForeignIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }*/

    public PipenvResult convert(String projectName, String projectVersionName, PipenvFreeze pipenvFreeze, PipenvGraphDependency pipenvGraphDependency, boolean includeOnlyProjectTree) {
        MutableMapDependencyGraph dependencyGraph = new MutableMapDependencyGraph();
        Map<String, PipenvFreezeDependencyEntry> freezeEntryMap = createFreezeEntryMap(pipenvFreeze);
        for (PipenvGraphDependencyEntry entry : pipenvGraphDependency.getEntries()) {
            Dependency entryDependency = nameVersionToDependency(entry.getPackageName(), entry.getInstalledVersion(),freezeEntryMap);
            List<Dependency> children = addDependenciesToGraph(entry.getDependencies(), dependencyGraph,freezeEntryMap);
            if (entryDependency.getName() != null && entryDependency.getVersion() != null && entryDependency.getName().equals(projectName) && entryDependency.getVersion().equals(projectVersionName)) {
                dependencyGraph.addChildrenToRoot(children);
                continue;
            }
            if (!includeOnlyProjectTree) {
                dependencyGraph.addChildToRoot(entryDependency);
                dependencyGraph.addParentWithChildren(entryDependency, children);
            }
        }
        ForeignId projectForeignId = this.createNameVersionExternalId(Supplier.PYPI, projectName, projectVersionName);
        DependencyLocation dependencyLocation = new DependencyLocation((DependencyGraph)dependencyGraph, projectForeignId);
        return new PipenvResult(projectName, projectVersionName, dependencyLocation);
    }

    private List<Dependency> addDependenciesToGraph(List<PipenvGraphDependencyNode> graphDependencies, MutableMapDependencyGraph graph, Map<String, PipenvFreezeDependencyEntry> freezeEntryMap) {
        List<Dependency> dependencies = new ArrayList<>();
        for (PipenvGraphDependencyNode graphDependency : graphDependencies) {
            Dependency dependency = nameVersionToDependency(graphDependency.getPackageName(), graphDependency.getInstalledVersion(),freezeEntryMap);
            List<Dependency> children = addDependenciesToGraph(graphDependency.getDependencies(), graph,freezeEntryMap);
            graph.addParentWithChildren(dependency, children);
            dependencies.add(dependency);
        }
        return dependencies;
    }

    private Map<String, PipenvFreezeDependencyEntry> createFreezeEntryMap(PipenvFreeze pipenvFreeze) {
        return pipenvFreeze.getEntries().stream()
                .collect(Collectors.toMap(entry -> entry.getName().toLowerCase(), entry -> entry));
    }

    private String findFrozenValue(String name, Map<String, PipenvFreezeDependencyEntry> freezeEntryMap, Function<PipenvFreezeDependencyEntry, String> valueExtractor, String defaultValue) {
        PipenvFreezeDependencyEntry entry = freezeEntryMap.get(name.toLowerCase());
        return entry != null ? valueExtractor.apply(entry) : defaultValue;
    }

    private Dependency nameVersionToDependency(String givenName, String givenVersion, Map<String, PipenvFreezeDependencyEntry> freezeEntryMap) {
        String version = findFrozenValue(givenName, freezeEntryMap, PipenvFreezeDependencyEntry::getVersion, givenVersion);
        String name = findFrozenValue(givenName, freezeEntryMap, PipenvFreezeDependencyEntry::getName, givenName);

        return new Dependency(name, version, this.createNameVersionExternalId(Supplier.PYPI, name, version));
    }

    public ForeignId createNameVersionExternalId(Supplier supplier, String name, String version) {
        ForeignId foreignId = new ForeignId(supplier);
        foreignId.setName(name);
        foreignId.setVersion(version);
        //checkForValidity(foreignId);
        return foreignId;
    }

   /* private void checkForValidity(ForeignId externalId) {
        externalId.createBdioId();
    }*/
}
