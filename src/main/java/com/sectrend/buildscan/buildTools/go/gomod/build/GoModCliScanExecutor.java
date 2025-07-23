package com.sectrend.buildscan.buildTools.go.gomod.build;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoGraph;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoListUJsonData;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoVersion;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.SubstituteData;
import com.sectrend.buildscan.buildTools.go.gomod.build.analyze.GoGraphAnalyzer;
import com.sectrend.buildscan.buildTools.go.gomod.build.process.GoModDependencyManager;
import com.sectrend.buildscan.buildTools.go.gomod.build.process.GoModGraphGenerator;
import com.sectrend.buildscan.buildTools.go.gomod.build.process.GoRelationshipManager;
import com.sectrend.buildscan.executable.ExeRunnerException;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GoModCliScanExecutor {


    private final ExecutableRunner executableRunner;

    private final Gson gson;
    private final GoGraphAnalyzer goGraphAnalyzer;
    private final ForeignIdFactory foreignIdFactory;
    private final GoModGraphGenerator goModGraphGenerator;


    private static final String[] UNUSED_MODULE_PREFIXES = new String[]{
            "(main module does not need module",
            "(main module does not need to vendor module"
    };

    private static final String[] UNUSED_MODULE_REPLACEMENTS = new String[]{"", ""};

    public GoModCliScanExecutor(ExecutableRunner executableRunner, Gson gson,
                                GoGraphAnalyzer goGraphAnalyzer, ForeignIdFactory foreignIdFactory, GoModGraphGenerator goModGraphGenerator) {
        this.executableRunner = executableRunner;
        this.gson = gson;
        this.goGraphAnalyzer = goGraphAnalyzer;
        this.foreignIdFactory = foreignIdFactory;
        this.goModGraphGenerator = goModGraphGenerator;
    }

    public ScanResults scanExecute(ScannableEnvironment scanEnvironment, File goExe) {
        try {
            File directory = scanEnvironment.getDirectory();

            Properties arguments = scanEnvironment.getArguments();

            GoVersion goVersion = getGoVersion(directory, goExe);

            // 获取需要排除的依赖
            Set<String> excludeList = getExcludeList(goExe, arguments, directory);

            // go list -m json
            List<String> listUJsonOutputPre = goListUJsonOutput(directory, goExe);
            List<SubstituteData> goListUJsonDate = analyzeGoListJsonToClass(listUJsonOutputPre, SubstituteData.class);
            // go list -m json all
            List<String> listUJsonOutputPreAll = goListUJsonOutputAll(directory, goExe, goVersion);
            List<GoListUJsonData> goListUJsonDataAll = analyzeGoListJsonToClass(listUJsonOutputPreAll,GoListUJsonData.class);

            List<GoGraph> goGraphs = getGraphRelationships(directory, goExe, goVersion);
            GoRelationshipManager goRelationshipManager = new GoRelationshipManager(goGraphs, excludeList);
            GoModDependencyManager goModDependencyManager = new GoModDependencyManager(goListUJsonDataAll, foreignIdFactory);

            List<DependencyLocation> codeLocations = goListUJsonDate.stream()
                    .map(goListModule -> goModGraphGenerator.generateGraph(goListModule, goRelationshipManager, goModDependencyManager))
                    .collect(Collectors.toList());

            return (new ScanResults.Builder()).success(codeLocations).build();
        } catch (Exception e) {
            return (new ScanResults.Builder()).exception(e).build();
        }
    }

    private <T> List<T> analyzeGoListJsonToClass(List<String> listJsonOutput, Class<T> classOfT) throws JsonSyntaxException {
        List<T> listEntries = new LinkedList<>();

        StringBuilder jsonEntry = new StringBuilder();
        for (String line : listJsonOutput) {
            jsonEntry.append(line);
            if (line.startsWith("}")) {
                T data = gson.fromJson(jsonEntry.toString(), classOfT);
                listEntries.add(data);
                jsonEntry = new StringBuilder();
            }
        }
        return listEntries;
    }

    private List<GoGraph> getGraphRelationships(File directory, File goExe, GoVersion goVersion) throws ScannableException, ExeRunnerException {
        List<String> modGraphOutput = execute(directory, goExe, "Querying for the go mod graph failed:", "mod", "graph");

        // 生成此图的实际主模块
        String mainMod = "";
        // 获取直接依赖项列表，然后使用主模块名称和列表从需求图创建依赖关系图
        List<String> directs;
        //go的1.14版本开始有大改动，以这个为基准进行区分
        if (goVersion.getMajorVersion() > 1 || goVersion.getMinorVersion() >= 14){
            mainMod = execute(directory, goExe, "Querying go for a json list of module data failed:", "list", "-mod=readonly", "-m", "-f", "{{if (.Main)}}{{.Path}}{{end}}", "all").get(0);
            directs = execute(directory, goExe, "Querying go for a json list of module data failed:", "list", "-mod=readonly", "-m", "-f", "{{if not (or .Indirect .Main)}}{{.Path}}@{{.Version}}{{end}}", "all");
        } else {
            mainMod = execute(directory, goExe, "Querying go for a json list of module data failed:", "list", "-m", "-f", "{{if (.Main)}}{{.Path}}{{end}}", "all").get(0);
            directs = execute(directory, goExe, "Querying go for a json list of module data failed:", "list", "-m", "-f", "{{if not (or .Indirect .Main)}}{{.Path}}@{{.Version}}{{end}}", "all");
        }
        List<String> whyModuleList = new ArrayList<>(execute(directory, goExe, "Querying for the go mod why -m all failed: ", "mod", "why", "-m", "all"));

        Set<String> actualDependencyList = computeDependencies(mainMod, directs, whyModuleList, modGraphOutput);
        return goGraphAnalyzer.analyzeRelationshipsFromGoModGraph(actualDependencyList);
    }

    /**
     * 对结果进行修正
     *
     * @param main    - 主模块名称
     * @param directs - 主模块的直接依赖列表
     * @param whyList - 所有分模块和主模块的依赖关系列表
     * @param graph   - go mod graph列表
     * @return
     */
    public Set<String> computeDependencies(String main, List<String> directs, List<String> whyList, List<String> graph) {
        Set<String> goModGraph = new HashSet<>();
        List<String> correctedDependencies = new ArrayList<>();
        Map<String, List<String>> whyMap = convertListToMap(whyList);
        for (String grphLine : graph) {
            String[] splitLine = grphLine.split(" ");
            String parentModule = splitLine[0];
            String childModule = splitLine[1];

            // 跳过版本信息为 go@ 开头的子模块
            if (childModule.startsWith("go@")) {
                continue;
            }

            // 判断当前行是否包含直接依赖
            boolean containsDirect = parentModule.equals(main) && directs.stream().anyMatch(grphLine::contains);

            // 需要进行处理的条件
            boolean needsRedux = !containsDirect && parentModule.equals(main);

            // 如果 parentModule 包含版本信息，且已被 correctedDependencies 包含，则跳过
            if (parentModule.startsWith(main) && parentModule.contains("@")) {
                boolean isDuplicate = correctedDependencies.stream().anyMatch(childModule::startsWith);
                if (isDuplicate) {
                    continue;
                }
            }

            if (needsRedux) {
                String childModulePath = stripVersion(childModule);
                correctedDependencies.add(childModulePath);

                List<String> trackPath = whyMap.get(childModulePath);
                if (trackPath != null && !trackPath.isEmpty()) {
                    for (String tp : trackPath) {
                        String parent = directs.stream()
                                .filter(directMod -> tp.contains(stripVersion(directMod)))
                                .findFirst()
                                .orElse(null);
                        if (parent != null) {
                            grphLine = grphLine.replace(parentModule, parent);
                            break;
                        }
                    }
                }
            }
            goModGraph.add(grphLine);
        }
        return goModGraph;
    }
    /**
     * 去除版本号，类似 "module@v1.2.3" => "module"
     */
    private String stripVersion(String module) {
        int atIndex = module.indexOf('@');
        return atIndex == -1 ? module : module.substring(0, atIndex);
    }

    public HashMap<String, List<String>> convertListToMap(List<String> whyList) {
        HashMap<String, List<String>> moduleToHierarchyList = new HashMap<>();
        List<String> shortList = new LinkedList<>();
        String key = "";
        for (String whyModuleLine : whyList) {
            if (whyModuleLine.isEmpty()) continue;
            if (whyModuleLine.startsWith("#")) {
                // 处理前一个模块的记录
                if (!key.isEmpty()) {
                    moduleToHierarchyList.put(key, new ArrayList<>(shortList)); // 使用新实例避免共享
                    shortList.clear();
                }
                key = whyModuleLine.substring(2);  // 提取模块名称
            } else {
                shortList.add(whyModuleLine);
            }
        }
        // 最后一个模块需要手动添加
        if (!key.isEmpty()) {
            moduleToHierarchyList.put(key, new ArrayList<>(shortList)); // 防止引用共享
        }
        return moduleToHierarchyList;
    }

    private List<String> goListUJsonOutput(File directory, File goExe) throws ScannableException, ExeRunnerException {
        return execute(directory, goExe, "Querying go for a json list of module data failed:", "list", "-m", "-json");
    }

    @NotNull
    private Set<String> getExcludeList(File goExe, Properties arguments, File directory) throws ScannableException, ExeRunnerException {
        String goModDependencyTypesExcluded = null;
        if (arguments != null) {
            goModDependencyTypesExcluded = (String) arguments.get("goModDependencyTypesExcluded");
        }
        List<String> unusedList = new ArrayList<>();
        if ("UNUSED".equals(goModDependencyTypesExcluded)) {
            unusedList = new ArrayList<>(execute(directory, goExe, "Querying for the go mod why -m all failed: ", "mod", "why", "-m", "all"));
        } else if ("VENDORED".equals(goModDependencyTypesExcluded)) {
            unusedList = new ArrayList<>(execute(directory, goExe, "Querying for the go mod why -m all failed: ", "mod", "why", "-m", "-vendor", "all"));
        }

        Set<String> excludeList = new HashSet<>();
        if (CollectionUtils.isNotEmpty(unusedList)) {
            excludeList = unusedList.stream()
                    .map(String::trim)
                    .filter(line -> StringUtils.startsWithAny(line, UNUSED_MODULE_PREFIXES))
                    .map(line -> StringUtils.replaceEach(line, UNUSED_MODULE_PREFIXES, UNUSED_MODULE_REPLACEMENTS))
                    .map(line -> StringUtils.removeEnd(line, ")"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        return excludeList;
    }

    // 执行指令
    private List<String> execute(File directory, File goExe, String failureMessage, String... arguments) throws ScannableException, ExeRunnerException {
        ExecutionOutput output = this.executableRunner.execute(directory, goExe, arguments);
        if (output.getExitCode() == 0)
            return output.getStandardOutputAsList();
        throw new ScannableException(failureMessage + output.getExitCode());
    }


    /**
     * 查看go版本并根据版本执行不同脚本
     *
     * @param directory
     * @param goExe
     * @return
     * @throws ExeRunnerException
     * @throws ScannableException
     */
    private List<String> goListUJsonOutputAll(File directory, File goExe, GoVersion goVersion) throws ExeRunnerException, ScannableException {
        if (goVersion.getMajorVersion() > 1 || goVersion.getMinorVersion() >= 14)
            return execute(directory, goExe, "Querying go for a json list of module data failed:", new String[]{"list", "-mod=readonly", "-m", "-json", "all"});
        return execute(directory, goExe, "Querying go for a json list of module data failed:", new String[]{"list", "-m", "-json", "all"});
    }


    private GoVersion getGoVersion(File directory, File goExe) throws ScannableException, ExeRunnerException {
        List<String> versionOutput = execute(directory, goExe, "Querying for the version failed: ", new String[]{"version"});
        Pattern pattern = Pattern.compile("\\d+\\.[\\d.]+");
        Matcher matcher = pattern.matcher(versionOutput.get(0));
        if (matcher.find()) {
            String version = matcher.group();
            String[] parts = version.split("\\.");
            return new GoVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        throw new ScannableException("Failed to find Go version in the output: " + versionOutput.get(0));
    }


}
