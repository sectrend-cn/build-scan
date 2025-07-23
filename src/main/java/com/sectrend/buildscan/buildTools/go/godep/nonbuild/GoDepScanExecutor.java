package com.sectrend.buildscan.buildTools.go.godep.nonbuild;

import com.moandjiezana.toml.Toml;
import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.go.godep.nonbuild.model.GoLockEntry;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.model.Supplier;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

public class GoDepScanExecutor {

    private final ForeignIdFactory foreignIdFactory;

    public GoDepScanExecutor(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    public ScanResults scanExecute(InputStream goLockInputStream, File goLock) {
        DependencyGraph graph = this.analyzeDepLock(goLockInputStream);
        DependencyLocation dependencyLocation = new DependencyLocation(graph, goLock);
        return (new ScanResults.Builder()).success(dependencyLocation).build();
    }

    public DependencyGraph analyzeDepLock(InputStream depLockInputStream) {
        MutableMapDependencyGraph mutableMapDependencyGraph = new MutableMapDependencyGraph();
        Toml toml = new Toml();
        GoLockEntry goLockEntry = toml.read(depLockInputStream).to(GoLockEntry.class);
        if (CollectionUtils.isEmpty(goLockEntry.goDepProjects)) {
            return mutableMapDependencyGraph;
        }
        goLockEntry.goDepProjects.forEach(project -> {
            if (project != null) {
                String projectName = project.getName();
                String projectVersion = Optional.ofNullable(StringUtils.stripToNull(project.getVersion())).orElse(project.getRevision());
                project.getPackages().stream()
                        .map(packageName -> {
                            String dependencyName = projectName;
                            if (!packageName.equals(".")) {
                                dependencyName = dependencyName + "/" + packageName;
                            }
                            if (dependencyName.startsWith("golang.org/x/")) {
                                dependencyName = dependencyName.replace("golang.org/x/", "");
                            }
                            ForeignId foreignId = this.foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, dependencyName, projectVersion);
                            return new Dependency(dependencyName, projectVersion, foreignId);
                        })
                        .forEach(mutableMapDependencyGraph::addChildToRoot);
            }
        });
        return mutableMapDependencyGraph;
    }
}
