package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.maven.build.MavenDependencyLocationPackager;
import com.sectrend.buildscan.buildTools.maven.model.MavenResult;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * maven构建输出结果转换
 * </p>
 *
 * @author yhx
 * @date 2022/6/20 10:10
 */
public class MavenBuildInputTransform {

    private ForeignIdFactory foreignIdFactory = new ForeignIdFactory();
    private static final List<String> indentationStrings = Arrays.asList(new String[]{"+- ", "|  ", "\\- ", "   "});


    public static ScanResults inputTransform(String mavenInput) {
        ExecutionOutput mvnOutput = new ExecutionOutput(null, 0, mavenInput, null);
        MavenDependencyLocationPackager mavenDependencyLocationPackager = new MavenDependencyLocationPackager(new ForeignIdFactory());

        List<MavenResult> mavenResults = mavenDependencyLocationPackager.extractDependencyLocations(" ", mvnOutput.getStandardOutput(), null, null, null, null);

//                List<MavenResult> mavenResults = this.mavenDependencyLocationPackager.extractCodeLocations(directory.toString(), mvnOutput.getStandardOutput(), excludedScopes, includedScopes, excludedModules, includedModules);
        List<DependencyLocation> dependencyLocations = (List<DependencyLocation>) mavenResults.stream().map(mavenResult -> mavenResult.getDependencyLocation()).collect(Collectors.toList());
        Optional<MavenResult> firstWithName = mavenResults.stream().filter(it -> StringUtils.isNoneBlank(new CharSequence[]{it.getProjectName()})).findFirst();

        ScanResults.Builder builder1 = (new ScanResults.Builder()).success(dependencyLocations);
        return builder1.build();
    }

    public static void main(String[] args) throws IOException {
        File file = new File("E:/Downloads/maven-tree.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        StringBuilder sb = new StringBuilder();
        while (br.ready()) {
            sb.append(br.readLine() + "\r" + "\n");
        }
        ScanResults scanResults = MavenBuildInputTransform.inputTransform(sb.toString());

    }

}
