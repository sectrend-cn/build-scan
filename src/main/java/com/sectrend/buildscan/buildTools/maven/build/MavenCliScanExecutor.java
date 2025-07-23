package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.maven.model.MavenResult;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.executable.ExecutableRunner;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MavenCliScanExecutor {
    private final ExecutableRunner executableRunner;
    private final MavenDependencyLocationPackager mavenDependencyLocationPackager;

    public MavenCliScanExecutor(ExecutableRunner executableRunner, MavenDependencyLocationPackager mavenDependencyLocationPackager) {
        this.executableRunner = executableRunner;
        this.mavenDependencyLocationPackager = mavenDependencyLocationPackager;
    }

    public ScanResults scanExecute(ScannableEnvironment scannableEnvironment, File directory, File mavenExe) {
        try {
            Properties argument = scannableEnvironment.getArguments();
            //   Maven Exe
            if(argument != null && argument.get("mavenPath") != null){
                mavenExe = new File(argument.get("mavenPath").toString());
            }
            List<String> arguments = new ArrayList<>();

            if(argument != null && argument.get("mavenPreBuildCommand") != null) {
                String[] args = argument.get("mavenPreBuildCommand").toString().trim().split(" ");
                arguments.addAll(Arrays.asList(args));
            }
            arguments.add("dependency:tree");
            // Maven Build Command
            if(argument != null && argument.get("mavenBuildCommand") != null) {
                String[] args = argument.get("mavenBuildCommand").toString().trim().split(" ");
                arguments.addAll(Arrays.asList(args));
            }
            ExecutionOutput mvnOutput = this.executableRunner.execute(directory, mavenExe, arguments);
            if (mvnOutput.getExitCode() == 0) {
                String excludedScopes= null;
                String includedScopes = null;
                String excludedModules = null;
                String includedModules = null;
                // Dependency Scope Excluded  Dependency Scope Included  Maven Modules Included  Maven Modules Excluded
                if(argument != null && argument.get("mavenExcludedScopes") != null){
                   excludedScopes = argument.get("mavenExcludedScopes").toString();
                }
                if(argument != null && argument.get("mavenIncludedScopes") != null){
                    includedScopes = argument.get("mavenIncludedScopes").toString();
                }
                if(argument != null && argument.get("mavenExcludedModules") != null){
                    excludedModules = argument.get("mavenExcludedModules").toString();
                }
                if(argument != null && argument.get("mavenIncludedModules") != null){
                    includedModules = argument.get("mavenIncludedModules").toString();
                }
                List<MavenResult> mavenResults = this.mavenDependencyLocationPackager.extractDependencyLocations(directory.toString(), mvnOutput.getStandardOutput(), excludedScopes, includedScopes, excludedModules, includedModules);
//                List<MavenResult> mavenResults = this.mavenDependencyLocationPackager.extractCodeLocations(directory.toString(), mvnOutput.getStandardOutput(), excludedScopes, includedScopes, excludedModules, includedModules);
                List<DependencyLocation> dependencyLocations = (List<DependencyLocation>) mavenResults.stream().map(mavenResult -> mavenResult.getDependencyLocation()).collect(Collectors.toList());
                Optional<MavenResult> firstWithName = mavenResults.stream().filter(it -> StringUtils.isNoneBlank(new CharSequence[]{it.getProjectName()})).findFirst();

                ScanResults.Builder builder1 = (new ScanResults.Builder()).success(dependencyLocations);
                if (firstWithName.isPresent()) {
                    builder1.scanProjectName(((MavenResult) firstWithName.get()).getProjectName());
                    builder1.scanProjectVersion(((MavenResult) firstWithName.get()).getProjectVersion());
                }
                return builder1.build();
            }
            ScanResults.Builder builder = (new ScanResults.Builder()).failure(String.format("Executing command '%s' returned a non-zero exit code %s", new Object[]{String.join(" ", (Iterable) arguments), Integer.valueOf(mvnOutput.getExitCode())}));
            return builder.build();
        } catch (Exception e) {
            return (new ScanResults.Builder()).exception(e).build();
        }
    }
}
