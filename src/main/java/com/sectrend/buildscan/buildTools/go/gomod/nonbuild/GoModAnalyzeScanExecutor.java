package com.sectrend.buildscan.buildTools.go.gomod.nonbuild;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author huishun.yi
 * @Date 2023/3/17 14:46
 */
public class GoModAnalyzeScanExecutor {

    private final Logger logger = LoggerFactory.getLogger(GoModAnalyzeScanExecutor.class);

    private static final String REQUIRE_START_CHARACTER = "require(";

    private static final String REQUIRE_SINGLE = "require ";

    private static final String REPLACE_START_CHARACTER = "replace(";

    private static final String REPLACE_SINGLE = "replace ";

    private static final String END_CHARACTER = ")";

    private static final String DEPENDENCY_SPLIT = "\\s+";

    Map<String, String> dependentData = new HashMap<>();

    private final GoModGraphAnalyzer goModGraphAnalyzer;

    public GoModAnalyzeScanExecutor(GoModGraphAnalyzer goModGraphAnalyzer) {
        this.goModGraphAnalyzer = goModGraphAnalyzer;
    }

    /**
     * 解析 go—mod 文件
     *
     * @return
     */
    public ScanResults scanExecute(List<ScannableEnvironment> scannableEnvironments) {

        List<DependencyLocation> dependencyLocations = new ArrayList<>();
        scannableEnvironments.forEach(scannableEnvironment -> {
            File file = new File(scannableEnvironment.getDirectory().getAbsolutePath() + File.separator + GoModAnalyzeScannable.GOMOD_FILENAME_PATTERN);
            try {
                this.analyze(file);
                DependencyLocation codeLocation = parseGoModGraph();
                if (codeLocation != null) {
                    codeLocation.setSourcePath(file);
                }
                dependencyLocations.add(codeLocation);
            } catch (Exception e) {
                logger.error("Go mod non build analyzing exception! ", e);
            }
        });
        return (new ScanResults.Builder()).success(dependencyLocations).build();
    }


    public void analyze(File file) throws IOException {
        List<String> readAllLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        //gomod的依赖非构建解析分为[require]和[replace]，以各自特定的格式来判断一个依赖的开始与结束
        boolean isRequirePiece = false;
        boolean isReplacePiece = false;
        for (String line : readAllLines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            //去掉所有双引号
            line = replaceComment(line).replaceAll("\"", "").trim();
            //去掉所有空格
            String notBlankLine = line.replaceAll(" ", "");

            //--------------------------------------------RequirePiece的情况--------------------------------------------
            if (notBlankLine.startsWith(REQUIRE_START_CHARACTER)) {
                isRequirePiece = true;
                continue;
            }
            if (isRequirePiece && notBlankLine.startsWith(END_CHARACTER)) {
                isRequirePiece = false;
                continue;
            }
            if (isRequirePiece) {
                addRequireDependent(line);
                continue;
            }

            if (line.trim().startsWith(REQUIRE_SINGLE)) {
                line = line.replaceAll(REQUIRE_SINGLE, "").trim();
                addRequireDependent(line);
                continue;
            }

            //--------------------------------------------ReplacePiece的情况--------------------------------------------
            if (notBlankLine.startsWith(REPLACE_START_CHARACTER)) {
                isReplacePiece = true;
                continue;
            }
            if (isReplacePiece && notBlankLine.startsWith(END_CHARACTER)) {
                isReplacePiece = false;
                continue;
            }
            if (isReplacePiece) {
                addReplaceDependent(line);
                continue;
            }
            if (line.trim().startsWith(REPLACE_SINGLE)) {
                line = line.replaceAll(REPLACE_SINGLE, "").trim();
                addReplaceDependent(line);
            }
        }

    }

    /**
     * 替换注释部分
     *
     * @param line
     * @return
     */
    public String replaceComment(String line) {
        int index = line.indexOf("//");
        if (index != -1) {
            return line.substring(0, index);
        }
        return line;
    }


    private void addRequireDependent(String line) {
        String[] dependent = line.split(DEPENDENCY_SPLIT);
        if (dependent.length == 2) {
            String path = dependent[0];
            String version = dependent[1];
            dependentData.put(String.format("%s@%s", path, version), null);
        }
    }


    private void addReplaceDependent(String line) {
        String[] dependents = line.split("=>");
        if (dependents.length != 2) {
            return;
        }
        String[] requireDependent = dependents[0].trim().split(DEPENDENCY_SPLIT);
        String[] replaceDependent = dependents[1].trim().split(DEPENDENCY_SPLIT);
        if (requireDependent.length != 2 || replaceDependent.length != 2) {
            return;
        }

        String requireDependentStr = String.format("%s@%s", requireDependent[0].trim(), requireDependent[1].trim());
        String replaceDependentStr = String.format("%s@%s", replaceDependent[0].trim(), replaceDependent[1].trim());
        if (dependentData.containsKey(requireDependentStr)) {
            dependentData.put(requireDependentStr, replaceDependentStr);
        }
    }


    public DependencyLocation parseGoModGraph() {
        MutableMapDependencyGraph mutableMapDependencyGraph = new MutableMapDependencyGraph();
        dependentData.forEach((k, v) -> {
            if (StringUtils.isNotBlank(v)) {
                mutableMapDependencyGraph.addChildToRoot(goModGraphAnalyzer.analyzeDependency(v, null));
            } else {
                mutableMapDependencyGraph.addChildToRoot(goModGraphAnalyzer.analyzeDependency(k, null));
            }
        });
        return new DependencyLocation(mutableMapDependencyGraph);
    }


}
