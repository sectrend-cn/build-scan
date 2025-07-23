package com.sectrend.buildscan.buildTools.pipenv.nonbuild;

import com.google.gson.Gson;
import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.model.PipfileLockDependencyEnv;
import com.sectrend.buildscan.utils.EnumUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipfileLockScanExecutor {
    private final Gson gson;
    private final PipfileLockDependencyConverter pipfileLockDependencyConverter;

    public PipfileLockScanExecutor(
            Gson gson,
            PipfileLockDependencyConverter pipfileLockDependencyConverter
    ) {
        this.gson = gson;
        this.pipfileLockDependencyConverter = pipfileLockDependencyConverter;
    }

    public ScanResults scanExecute(File pipfileLockFile, EnumUtil<PipenvDependencyType> dependencyTypeFilter, File directory, String providedProjectName){
        ScanResults scanResultsResult;

        try {
            String projectName = StringUtils.isNotBlank(providedProjectName) ? providedProjectName : directory.getName();
            String pipfileLockText = FileUtils.readFileToString(pipfileLockFile, StandardCharsets.UTF_8);
            Map<String,Integer> dependencyLineIndex = pipfileLockLineAnalyzer(pipfileLockText);
            PipfileLockDependencyEnv pipfileLockDependencyEnv = gson.fromJson(pipfileLockText, PipfileLockDependencyEnv.class);
            DependencyGraph dependencyGraph = pipfileLockDependencyConverter.convert(pipfileLockDependencyEnv,dependencyTypeFilter,dependencyLineIndex);
            DependencyLocation dependencyLocation = new DependencyLocation(dependencyGraph);

            dependencyLocation.setSourcePath(pipfileLockFile);
            if (dependencyLocation.getDependencyGraph() == null) {
                scanResultsResult = (new ScanResults.Builder()).failure("The Pipenv tree parse failed to produce output.").build();
            } else {
                scanResultsResult = (new ScanResults.Builder()).success(dependencyLocation).scanProjectName(projectName).scanProjectVersion(null).build();
            }
        } catch (Exception e) {
            scanResultsResult = (new ScanResults.Builder()).exception(e).build();
        }
        return scanResultsResult;
    }

    private Map<String,Integer> pipfileLockLineAnalyzer(String pipfileLockText) {
        String[] lines = pipfileLockText.split("\n");
        Map<String, Integer> dependencyLineIndex = new HashMap<>();
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (StringUtils.isNotBlank(line)) {
                Pattern pattern = Pattern.compile("\"([^\"]*)\"");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    dependencyLineIndex.put(matcher.group(1), lineNumber);
                }
            }
        }
        return dependencyLineIndex;
    }
}