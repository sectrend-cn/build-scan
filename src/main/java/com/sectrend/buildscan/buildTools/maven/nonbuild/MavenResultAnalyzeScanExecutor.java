package com.sectrend.buildscan.buildTools.maven.nonbuild;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.maven.build.MavenDependencyLocationPackager;
import com.sectrend.buildscan.buildTools.maven.model.MavenResult;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MavenResultAnalyzeScanExecutor {

    private final MavenDependencyLocationPackager mavenDependencyLocationPackager;

    public MavenResultAnalyzeScanExecutor(MavenDependencyLocationPackager mavenDependencyLocationPackager) {
        this.mavenDependencyLocationPackager = mavenDependencyLocationPackager;
    }

    public ScanResults scanExecute(File reportFile) {

        //File reportFile = fileFinder.findFile(directory, "maven-tree.txt");

        //过滤配置
        String excludedScopes = null;
        String includedScopes = null;
        String excludedModules = null;
        String includedModules = null;

        List<MavenResult> mavenResults = mavenDependencyLocationPackager.extractLocalResult(reportFile, excludedScopes, includedScopes, excludedModules, includedModules);

        List<DependencyLocation> dependencyLocations = (List<DependencyLocation>) mavenResults.stream().map(mavenResult -> mavenResult.getDependencyLocation()).collect(Collectors.toList());
        Optional<MavenResult> firstWithName = mavenResults.stream().filter(it -> StringUtils.isNoneBlank(new CharSequence[]{it.getProjectName()})).findFirst();

        ScanResults.Builder builder = (new ScanResults.Builder()).success(dependencyLocations);
        if (firstWithName.isPresent()) {
            builder.scanProjectName(((MavenResult) firstWithName.get()).getProjectName());
            builder.scanProjectVersion(((MavenResult) firstWithName.get()).getProjectVersion());
        }
        return builder.build();

    }



}
