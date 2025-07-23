package com.sectrend.buildscan.buildTools.pipenv.nonbuild;

import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.model.PipfileLockDependencyEnv;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.model.PipfileLockDependencyVersion;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.Supplier;
import com.sectrend.buildscan.utils.EnumUtil;

import java.util.Map;
import java.util.Set;

public class PipfileLockDependencyConverter {
    private final ForeignIdFactory foreignIdFactory = new ForeignIdFactory();

    public DependencyGraph convert(PipfileLockDependencyEnv pipfileLockDependencyEnv, EnumUtil<PipenvDependencyType> dependencyTypeFilter, Map<String,Integer> dependencyLineIndex) {
        MutableMapDependencyGraph dependencyGraph = new MutableMapDependencyGraph();

        convertEntriesToDependency(pipfileLockDependencyEnv.defaultDependencies,dependencyLineIndex,dependencyGraph);
        if (dependencyTypeFilter.include(PipenvDependencyType.DEV)) {
            convertEntriesToDependency(pipfileLockDependencyEnv.developDependencies,dependencyLineIndex,dependencyGraph);
        }
        return dependencyGraph;
    }

    private void convertEntriesToDependency(Map<String, PipfileLockDependencyVersion> dependencyEntries, Map<String,Integer> dependencyLineIndex, MutableMapDependencyGraph dependencyGraph) {
        Set<Map.Entry<String, PipfileLockDependencyVersion>> entries = dependencyEntries.entrySet();
        for (Map.Entry<String, PipfileLockDependencyVersion> entry : entries) {
            String version = entry.getValue().pipfileLockDependencyVersion;
            if (version != null) {
                version = version.replace("==", "");
            }
            String name = entry.getKey();
            Dependency dependencyHasLine = new Dependency(name, version, this.foreignIdFactory.createNameVersionForeignId(Supplier.PYPI, name, version));
            dependencyHasLine.setLine(dependencyLineIndex.get(name));
            dependencyGraph.addChildToRoot(dependencyHasLine);
        }
    }
}
