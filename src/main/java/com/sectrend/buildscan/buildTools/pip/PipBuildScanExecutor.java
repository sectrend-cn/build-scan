package com.sectrend.buildscan.buildTools.pip;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.pipenv.build.PipenvResult;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.ExeRunnerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PipBuildScanExecutor {



    private final ExecutableRunner executableRunner;

    private final PipBuildTreeAnalyzer pipBuildTreeAnalyzer;

    private final static Pattern PATTERN = Pattern.compile(".*[!@#$%&*()_+\\{\\}\\[\\]\",<>./;':\\\\|`~].*");

    private final static Set<String> dependencyVersionOps;
    static {
        dependencyVersionOps = new HashSet<>();
        dependencyVersionOps.add("==");
        dependencyVersionOps.add(">");
        dependencyVersionOps.add(">=");
        dependencyVersionOps.add("<");
        dependencyVersionOps.add("<=");
        dependencyVersionOps.add("!=");
        dependencyVersionOps.add("--"); //支持非法格式
        dependencyVersionOps.add("===");//支持非法格式
        dependencyVersionOps.add("~=");
    }

    public PipBuildScanExecutor(ExecutableRunner executableRunner, PipBuildTreeAnalyzer pipBuildTreeAnalyzer) {
        this.executableRunner = executableRunner;
        this.pipBuildTreeAnalyzer = pipBuildTreeAnalyzer;
    }

    public ScanResults scanExecute(File directory, File pythonExe, File pipBuilder, File setupFile, Set<Path> requirementFilePaths, String providedProjectName) {
        ScanResults scanResultsResult;
        try {
            String projectName = StringUtils.isBlank(providedProjectName)
                    ? (setupFile != null && setupFile.exists()
                    ? getProjectNameFromSetupFile(setupFile, directory, pythonExe)
                    : directory.getName())
                    : providedProjectName;

            List<DependencyLocation> dependencyLocations = new ArrayList<>();
            AtomicReference<String> projectVersion = new AtomicReference<>();
            List<Path> requirementsPaths = new ArrayList<>();
            if (requirementFilePaths == null || requirementFilePaths.isEmpty()) {
                requirementsPaths.add(null);
            } else {
                requirementsPaths.addAll(requirementFilePaths);
            }

            requirementsPaths.forEach(requirementFilePath -> {
                List<String> builderOutput = null;
                try {
                    builderOutput = runBuilder(directory, pythonExe, pipBuilder, projectName, requirementFilePath);
                } catch (ExeRunnerException e) {
                    throw new RuntimeException(e);
                }
                Optional<PipenvResult> result = this.pipBuildTreeAnalyzer.analyze(builderOutput, directory.toString());
                if (result.isPresent()) {
                    DependencyLocation dependencyLocation = ((PipenvResult)result.get()).getDependencyLocation();
                    dependencyLocation.setSourcePath(requirementFilePath.toFile());
                    dependencyLocations.add(dependencyLocation);
                    String potentialProjectVersion = ((PipenvResult)result.get()).getProjectVersion();
                    if (projectVersion.get() == null && StringUtils.isNotBlank(potentialProjectVersion))
                        projectVersion.set(potentialProjectVersion);
                }
            });

            if (dependencyLocations.isEmpty()) {
                scanResultsResult = (new ScanResults.Builder()).failure("The Pip Builder tree parse failed to produce output.").build();
            } else {
                scanResultsResult = (new ScanResults.Builder()).success(dependencyLocations).scanProjectName(projectName).scanProjectVersion(projectVersion.get()).build();
            }
        } catch (Exception e) {
            scanResultsResult = (new ScanResults.Builder()).exception(e).build();
        }
        return scanResultsResult;
    }

    private List<String> runBuilder(File sourceDirectory, File pythonExe, File builderScript, String projectName, Path requirementsFilePath) throws ExeRunnerException {
        List<String> builderArguments = new ArrayList<>();
        builderArguments.add(builderScript.getAbsolutePath());
        if (requirementsFilePath != null)
            builderArguments.add(String.format("--requirements=%s", new Object[] { requirementsFilePath.toAbsolutePath().toString() }));
        if (StringUtils.isNotBlank(projectName))
            builderArguments.add(String.format("--projectname=%s", new Object[] { projectName }));
        return this.executableRunner.execute(sourceDirectory, pythonExe, builderArguments).getStandardOutputAsList();
    }

    private String getProjectNameFromSetupFile(File setupFile, File directory, File pythonExe) throws ExeRunnerException {
        List<String> pythonArguments = Arrays.asList(new String[] { setupFile.getAbsolutePath(), "--name" });
        List<String> output = this.executableRunner.execute(directory, pythonExe, pythonArguments).getStandardOutputAsList();
        return ((String)output.get(output.size() - 1)).replace('_', '-').trim();
    }

    private String getUtfEncoding(File file) throws Exception  {
        InputStream inputStream = new FileInputStream(file);
        byte[] bom = new byte[4]; // 读取 BOM
        inputStream.mark(4); // 标记当前读的位置
        int bytesRead = inputStream.read(bom);
        inputStream.close();

        String encoding;
        if (bytesRead >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            encoding = "UTF-8"; // UTF-8 BOM
        } else if (bytesRead >= 2 && bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            encoding = "UTF-16LE"; // UTF-16 LE BOM
        } else if (bytesRead >= 2 && bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
            encoding = "UTF-16BE"; // UTF-16 BE BOM
        } else {
            // 如果没有 BOM，假设为 UTF-8，或可以抛出异常
            encoding = "UTF-8";
        }
        return encoding;
    }

    public ScanResults readTheFile(File directory, File setupFile, PipBuildScannableParams pipBuildScannableParams) {
        Set<Path> requirementsFilePaths = pipBuildScannableParams.getRequirementsPaths();
        ScanResults scanResultsResult;
        try {
            String projectName = StringUtils.isNotBlank(pipBuildScannableParams.getPipProjectName().orElse(""))? pipBuildScannableParams.getPipProjectName().get():directory.getName();
            List<DependencyLocation> dependencyLocations = new ArrayList<>();
            List<String> list = new ArrayList<>();
            String[] setupSplit = null;
            //List<String[]> splitList = new ArrayList<>();
            Map<File, String[]> splitMap = new HashMap<>();
            String projectVersion = null;
            if (setupFile != null && setupFile.exists()) {
                String enconding = getUtfEncoding(setupFile);
                String setup = FileUtils.readFileToString(setupFile, enconding);
                String s = "install_requires=[";
                int i = setup.indexOf("install_requires=[");
                String substring = "";
                int j = 0;
                if (i == -1) {
                    j = setup.indexOf("install_requires = [");
                }

                if (j != -1) {
                    if (i == -1) {
                        substring = setup.substring(setup.indexOf("install_requires = [") + "install_requires= [".length());
                    } else {
                        substring = setup.substring(setup.indexOf("install_requires=[") + s.length());
                    }
                    String str = substring.substring(0, substring.indexOf("]"));
                    if (str.contains("[") || str.contains("]") || str.contains("(") || str.contains(")")) {
                        return (new ScanResults.Builder()).build();
                    }

                    if (str.contains("#")) {
                        str = dislodgeAnnotation(str);
                    }
                    setupSplit = str.replaceAll("\\r?\\n", "").replace("'", "\"").trim().split("\",", -1);
                    //splitList.add(setupSplit);
                    splitMap.put(setupFile, setupSplit);
                }
            }
            if (CollectionUtils.isNotEmpty(requirementsFilePaths)) {
                for (Path filePath : requirementsFilePaths) {
                    File file = new File(filePath.toString());
                    String enconding = getUtfEncoding(file);
                    String setup = FileUtils.readFileToString(file, enconding);

                    String[] split = setup.trim().split("\\r?\\n");
                    //splitList.add(split);
                    splitMap.put(filePath.toFile(), split);
                }
            }
            //for (String[] strings : splitList) {
                //for (String line : strings) {
            for (Map.Entry<File, String[]> entry : splitMap.entrySet()) {
                List<String> listEachFile = new ArrayList<>();
                int lineNumber = 0;
                for (String line : entry.getValue()) {
                    lineNumber++;
                    if (StringUtils.isBlank(line)) {
                        continue;
                    }
                    String newLine = line.replace("\n", "").replace("\"", "").trim();
                    if (newLine.contains("#")) {
                        int i = newLine.indexOf("#");
                        if (i == 0) {
                            continue;
                        }
                        newLine = newLine.substring(0, i);
                    }
                    // 判断第一个字符是否是字母
                    if (!Character.isLetter(newLine.charAt(0))) {
                        if (newLine.contains(".whl") || newLine.contains(".tar.gz") || newLine.contains("/")) {
                            continue;
                        }
                        listEachFile.clear();
                        break;
                    }
                    if (StringUtils.isNotBlank(newLine) && !newLine.contains("http")) {
                        newLine=newLine+"%%"+lineNumber;
                        listEachFile.add(newLine);
                    }
                }
                list.addAll(listEachFile);
                //nike
                List<String> resultList = new ArrayList<>();
                for (String s1 : list) {
                    try {
                        String[] split = s1.split(";");
                        if (split.length > 1) {
                            String[] lineSplit = split[split.length -1].split("%%");
                            String dependencyStr = split[0] + "%%" + lineSplit[1];
                            analyticalDependence(dependencyStr, resultList);
                        } else {
                            analyticalDependence(s1, resultList);
                        }
                    } catch (Exception e) {
                        log.error(s1, e);
                        throw new RuntimeException(e);
                    }
                }

                Optional<PipenvResult> result = this.pipBuildTreeAnalyzer.analyze1(resultList, directory.toString());
                if (result.isPresent()) {
                    DependencyLocation dependencyLocation = ((PipenvResult)result.get()).getDependencyLocation();
                    dependencyLocation.setSourcePath(entry.getKey());
                    dependencyLocations.add(dependencyLocation);
                    String potentialProjectVersion = ((PipenvResult) result.get()).getProjectVersion();
                    if (projectVersion == null && StringUtils.isNotBlank(potentialProjectVersion))
                        projectVersion = potentialProjectVersion;
                }

            }

            if (dependencyLocations.isEmpty()) {
                scanResultsResult = (new ScanResults.Builder()).failure("The Pip Builder tree parse failed to produce output.").build();
            } else {
                scanResultsResult = (new ScanResults.Builder()).success(dependencyLocations).scanProjectName(projectName).scanProjectVersion(null).build();
            }
        } catch (Exception e) {
            scanResultsResult = (new ScanResults.Builder()).exception(e).build();
        }
        return scanResultsResult;
    }


    private String dislodgeAnnotation(String str) {

        String sub = str.substring(str.indexOf("#"));
        String su = "";
        int indexOne = sub.indexOf("\"");
        int indexTwo = sub.indexOf("\'");
        if(indexOne == -1 && indexTwo == -1){
            su = sub;
        }else if(indexOne == -1){
            su = sub.substring(0, sub.indexOf("\'"));
        }else {
            su = sub.substring(0, sub.indexOf("\""));
        }
        String replace = str.replace(su, "");
        if(replace.contains("#")){
            replace = dislodgeAnnotation(replace);
        }
        return replace;
    }

//    analyticalDependence解析依赖版本：兼容不合法的格式
//    apache-flink>=1.13.0<=1.13.3  //不合法
//    pandas>=1.0，<1.2.0   //全角逗号，不合法
//    xxxx>=1.0,<1.2.0
//    numpy<1.20
//    jupyter
//    py4j==0.10.8.1
//    cloudpickle--1.2.2   //不合法
//    scipy Rx
//    tqdm requests
//    deprecation
    private static void analyticalDependence(String s1, List<String> list) {
        String str = s1;
        String lineNumber="";

        // 提取行号
        String[] parts = str.split("%%");
        str = parts[0];
        s1=parts[0];
        lineNumber = parts[1];


        int index = s1.indexOf(",");
        if (index < 0) {
            index = s1.indexOf("，");
        }
        if (index > 0) {
            str = s1.substring(0,index);
        }

        Map<Integer, String> counts = new TreeMap<>();
        int count = 0;
        for(String op: dependencyVersionOps) {
            index = str.indexOf(op);
            if (index > 0) {
                counts.put(index, op);
            }
        }
        Iterator<Map.Entry<Integer, String>> iterator = counts.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<Integer, String> firstEntry = iterator.next();
            str = str.replace(firstEntry.getValue(), "==");

            if (iterator.hasNext()) {
                Map.Entry<Integer, String> secondEntry = iterator.next();
                str = str.substring(0, secondEntry.getKey());
            }
            list.add(str+"%%"+lineNumber);
        } else {
            list.add(str+"==undefined"+"%%"+lineNumber);
        }
    }


    public static void main(String[] args) {

        Matcher hasSpecialCharacters = PATTERN.matcher("123");
        System.out.println(hasSpecialCharacters.find());
    }
}
