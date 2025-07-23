package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.maven.model.MavenAnalyzeParams;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.utils.WildcardFilter;
import lombok.extern.slf4j.Slf4j;

import javax.xml.parsers.SAXParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MavenAnalyzeScanExecutor {

    private final ForeignIdFactory foreignIdFactory;

    private final SAXParser saxParser;

    public MavenAnalyzeScanExecutor(ForeignIdFactory foreignIdFactory, SAXParser saxParser) {
        this.foreignIdFactory = foreignIdFactory;
        this.saxParser = saxParser;
    }

    public ScanResults scanExecute(List<ScannableEnvironment> scannableEnvironmentList, MavenAnalyzeParams mavenAnalyzeParams, Properties arguments) {
        List<DependencyLocation> dependencyLocations = new ArrayList<>();
        Map<String, String> dependencyManagementMap = new HashMap<>();
        Map<String, String> versionVariableMap = new HashMap<>();
        String excludedScopes= null;
        String includedScopes = null;
        String excludedModules = null;
        String includedModules = null;
        if(arguments != null && arguments.get("mavenExcludedScopes") != null){
            excludedScopes = arguments.get("mavenExcludedScopes").toString();
        }
        if(arguments != null && arguments.get("mavenIncludedScopes") != null){
            includedScopes = arguments.get("mavenIncludedScopes").toString();
        }
        if(arguments != null && arguments.get("mavenExcludedModules") != null){
            excludedModules = arguments.get("mavenExcludedModules").toString();
        }
        if(arguments != null && arguments.get("mavenIncludedModules") != null){
            includedModules = arguments.get("mavenIncludedModules").toString();
        }
        WildcardFilter modulesFilter = new WildcardFilter(excludedModules, includedModules);
        WildcardFilter scopeFilter = new WildcardFilter(excludedScopes, includedScopes);
        for (ScannableEnvironment scannableEnvironment : scannableEnvironmentList) {
            File pomXmlFile = new File(scannableEnvironment.getDirectory() + File.separator + "pom.xml");
            log.info("find：" + pomXmlFile.getAbsolutePath());
            MutableMapDependencyGraph dependencyGraph = new MutableMapDependencyGraph();
            try (InputStream pomXmlInputStream = new FileInputStream(pomXmlFile)) {
                PomDependenciesHandler pomDependenciesHandler = new PomDependenciesHandler(this.foreignIdFactory, mavenAnalyzeParams.isIncludePlugins(), dependencyManagementMap, versionVariableMap);
                this.saxParser.parse(pomXmlInputStream, pomDependenciesHandler);
                pomDependenciesHandler.replaceDependenciesVariable();
                List<Dependency> dependencies = pomDependenciesHandler.getDependencies();
                List<Dependency> dependencyList = dependencies.stream().filter(d -> !d.getName().contains("$")).collect(Collectors.toList());
                List<Dependency> filterScopeList = new ArrayList<>();
                // includedScope excludedScope
                for (Dependency dependency : dependencyList) {
                    String[] split = dependency.getVersion().split("\\|_");
                    if (split.length > 1) {
                        // 获取 |_ 后面的值
                        String result = split[1];
                        if (scopeFilter.shouldInclude(result)) {
                            filterScopeList.add(dependency);
                        }
                    }
                }
                dependencyGraph.addChildrenToRoot(filterScopeList);
                DependencyLocation dependencyLocation = new DependencyLocation(dependencyGraph, pomXmlFile);
                String[] parts;
                if (File.separator.equals("/")) {
                    parts = dependencyLocation.getSourcePath().toString().split("/");
                } else {
                    parts = dependencyLocation.getSourcePath().toString().split("\\\\");
                }
                if (parts.length > 1) {
                    String result = parts[parts.length - 2];
                    if(modulesFilter.shouldInclude(result)){
                        dependencyLocations.add(dependencyLocation);
                    }
                }
            } catch (Exception e) {
                return (new ScanResults.Builder()).exception(e).build();
            }
        }
        return (new ScanResults.Builder()).success(dependencyLocations).build();
    }

}
