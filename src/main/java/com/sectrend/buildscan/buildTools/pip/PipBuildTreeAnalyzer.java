package com.sectrend.buildscan.buildTools.pip;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.pipenv.build.PipenvResult;
import com.sectrend.buildscan.factory.DependencyDequeUtil;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.model.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipBuildTreeAnalyzer {

    private final Logger logger = LoggerFactory.getLogger(PipBuildTreeAnalyzer.class);

    private final ForeignIdFactory foreignIdFactory;

    private final static Pattern PATTERN = Pattern.compile(".*[!@#$%&*()+\\{\\}\\[\\]\"<>,/;':\\\\|`~].*");

    public PipBuildTreeAnalyzer(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    public Optional<PipenvResult> analyze(List<String> pipBuilderOutputAsList, String sourcePath) {
        PipenvResult parseResult = null;
        MutableMapDependencyGraph mutableMapDependencyGraph = new MutableMapDependencyGraph();
        DependencyDequeUtil history = new DependencyDequeUtil();
        Dependency projectDependency = null;
        int unResolvedPackageCount = 0;
        for (String line : pipBuilderOutputAsList) {
            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isEmpty(trimmedLine) || !trimmedLine.contains("==") || trimmedLine.startsWith("r?") || trimmedLine.startsWith("p?") || trimmedLine.startsWith("--")) {
                if (analyzeErrorsFromLine(trimmedLine)) {
                    unResolvedPackageCount++;
                }
                continue;
            }
            Dependency dependencyFromLine = analyzeDependencyFromLine(trimmedLine, sourcePath);
            int level = getLineLevel(line);
            // 仅在需要时处理异常
            try {
                history.clearDependenciesDepth(level);
            } catch (Exception e) {
                logger.warn("Problem analyzing line '{}': {}", line, e.getMessage());
            }
            if (projectDependency != null) {
                addDependencyToGraph(history, mutableMapDependencyGraph, dependencyFromLine, projectDependency);
            } else {
                projectDependency = dependencyFromLine;
            }
            history.add(dependencyFromLine);
        }

        // 若没有未解决的包，并且项目不为null，构造PipenvResult
        if (projectDependency != null && unResolvedPackageCount == 0) {
            DependencyLocation dependencyLocation = new DependencyLocation((DependencyGraph)mutableMapDependencyGraph, projectDependency.getForeignId());
            parseResult = new PipenvResult(projectDependency.getName(), projectDependency.getVersion(), dependencyLocation);
        }
        return Optional.ofNullable(parseResult);
    }

    private void addDependencyToGraph(DependencyDequeUtil history, MutableMapDependencyGraph mutableMapDependencyGraph, Dependency currentDependency, Dependency project) {
        if (project.equals(history.getLastDependency()) || history.isEmpty()) {
            mutableMapDependencyGraph.addChildToRoot(currentDependency);
        } else {
            mutableMapDependencyGraph.addChildWithParents(currentDependency, new Dependency[]{history.getLastDependency()});
        }
    }

    public Optional<PipenvResult> analyze1(List<String> pipBuilderOutputAsList, String sourcePath) {
        PipenvResult parseResult = null;
        MutableMapDependencyGraph mutableMapDependencyGraph = new MutableMapDependencyGraph();
        DependencyDequeUtil history = new DependencyDequeUtil();
        Dependency projectDependency = null;
        for (String line : pipBuilderOutputAsList) {
            int lineNumber = 0;
            String[] parts = line.split("%%");
            line = parts[0];
            lineNumber = Integer.parseInt(parts[1]);

            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isEmpty(trimmedLine) || !trimmedLine.contains("==") || trimmedLine.startsWith("r?") || trimmedLine.startsWith("p?") || trimmedLine.startsWith("--")) {
                analyzeErrorsFromLine(trimmedLine);
                continue;
            }

            String[] segments = line.split("==");
            String name = segments[0].trim();

            if(name.contains("../")){
                break;
            }

            Dependency dependencyFromLine = analyzeDependencyFromLine(trimmedLine, sourcePath);
            dependencyFromLine.setLine(lineNumber);
            int level = getLineLevel(line);
            try {
                history.clearDependenciesDepth(level);
            } catch (Exception e) {
                this.logger.warn(String.format("Problem parsing line '%s': %s", new Object[] { line, e.getMessage() }));
            }
            if (projectDependency == null) {
                projectDependency = dependencyFromLine;
            }

            if (StringUtils.isBlank(dependencyFromLine.getName())) {
                continue;
            }

            Matcher matcher = PATTERN.matcher(dependencyFromLine.getName());
            // 如果组件名称有特殊字符则不添加该组件
            if (matcher.find()) {
                continue;
            }
            if (projectDependency.equals(history.getLastDependency())) {
                mutableMapDependencyGraph.addChildToRoot(dependencyFromLine);
            } else if (history.isEmpty()) {
                mutableMapDependencyGraph.addChildToRoot(dependencyFromLine);
            } else {
                mutableMapDependencyGraph.addChildWithParents(dependencyFromLine, new Dependency[] { history.getLastDependency() });
            }
            history.add(dependencyFromLine);
        }
        if (projectDependency != null) {
            DependencyLocation dependencyLocation = new DependencyLocation((DependencyGraph)mutableMapDependencyGraph, projectDependency.getForeignId());
            parseResult = new PipenvResult(projectDependency.getName(), projectDependency.getVersion(), dependencyLocation);
        }
        return Optional.ofNullable(parseResult);
    }

    private boolean analyzeErrorsFromLine(String trimmedLine) {
        boolean unResolvedPackage = false;
        if (trimmedLine.startsWith("r?"))
            this.logger.warn(String.format("Pip Builder could not find requirements file @ %s", new Object[] { trimmedLine.substring("r?".length()) }));
        if (trimmedLine.startsWith("p?"))
            this.logger.warn(String.format("Pip Builder could not analyze requirements file @ %s", new Object[] { trimmedLine.substring("p?".length()) }));
        if (trimmedLine.startsWith("--")) {
            this.logger.warn(String.format("Pip Builder could not resolve the package: %s", new Object[]{trimmedLine.substring("--".length())}));
            unResolvedPackage = true;
        }
        return unResolvedPackage;
    }

    private Dependency analyzeDependencyFromLine(String line, String sourcePath) {
        String[] segments = line.split("==");
        String name = segments[0].trim();
        String version = segments[1].trim();
        char lastChar = version.charAt(version.length() - 1);
        boolean flag = false;
        if (lastChar == '/' || lastChar == '\\'){
            flag = true;
        }
        if(StringUtils.isNotBlank(version) && flag){
            version = version.replace("\\","");
        }
        char nameLastChar = name.charAt(name.length() - 1);
        if(nameLastChar == '~'){
            name = name.substring(0, name.length() - 1);
        }

        ForeignId foreignId = this.foreignIdFactory.createNameVersionForeignId(Supplier.PYPI, name, version);
        if (name.equals("n?") || version.equals("v?"))
            foreignId = this.foreignIdFactory.createPathForeignId(Supplier.PYPI, sourcePath);
        name = name.equals("n?") ? "" : name;
        version = version.equals("v?") ? "" : version;
        return new Dependency(name, version, foreignId);
    }

    private int getLineLevel(String line) {
        int level = 0;
        String tmpLine = line;
        while (tmpLine.startsWith("    ")) {
            tmpLine = tmpLine.replaceFirst("    ", "");
            level++;
        }
        return level;
    }



}
