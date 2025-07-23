package com.sectrend.buildscan.buildTools.pipenv.build;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.pipenv.build.model.PipenvFreeze;
import com.sectrend.buildscan.buildTools.pipenv.build.model.PipenvFreezeDependencyEntry;
import com.sectrend.buildscan.buildTools.pipenv.build.model.PipenvGraphDependency;
import com.sectrend.buildscan.buildTools.pipenv.build.model.PipenvGraphDependencyEntry;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.ExeRunnerException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PipenvScanExecutor {

    private final ExecutableRunner executableRunner;

    private final PipenvConverter pipenvConverter;

    private final Gson gson;

    private static final Logger logger = LoggerFactory.getLogger(PipenvScanExecutor.class);

    public PipenvScanExecutor(ExecutableRunner executableRunner, PipenvConverter pipenvConverter, Gson gson) {
        this.executableRunner = executableRunner;
        this.pipenvConverter = pipenvConverter;
        this.gson = gson;
    }

    public ScanResults scanExecute(File directory, File pythonExe, File pipenvExe, File setupFile, String providedProjectName, String providedProjectVersionName, boolean includeOnlyProjectTree) {
        try {
            String projectName = resolveProjectName(directory, pythonExe, setupFile, providedProjectName);
            String projectVersionName = resolveProjectVersionName(directory, pythonExe, setupFile, providedProjectVersionName);
            ExecutionOutput pipFreezeOutput = this.executableRunner.execute(directory, pipenvExe, Arrays.asList(new String[] { "run", "pip", "freeze" }));
            ExecutionOutput graphOutput = this.executableRunner.execute(directory, pipenvExe, Arrays.asList(new String[] { "graph", "--bare", "--json-tree" }));
            PipenvFreeze pipenvFreeze = pipenvFreezeAnalyze(pipFreezeOutput.getStandardOutputAsList());
            PipenvGraphDependency pipenvGraphDependency = pipenvJsonGraphAnalyze(graphOutput.getStandardOutput());
            PipenvResult result = this.pipenvConverter.convert(projectName, projectVersionName, pipenvFreeze, pipenvGraphDependency, includeOnlyProjectTree);
            logger.info((new ScanResults.Builder()).success(result.getDependencyLocation()).scanProjectName(result.getProjectName()).scanProjectVersion(result.getProjectVersion()).build() + "");
             return (new ScanResults.Builder()).success(result.getDependencyLocation()).scanProjectName(result.getProjectName()).scanProjectVersion(result.getProjectVersion()).build();
        } catch (Exception e) {
            ScanResults scanResults = (new ScanResults.Builder()).exception(e).build();
            return scanResults;
        }
    }

    private String resolveProjectName(File directory, File pythonExe, File setupFile, String providedProjectName) throws ExeRunnerException {
        String projectName = providedProjectName;
        if (StringUtils.isBlank(projectName) && setupFile != null && setupFile.exists()) {
            List<String> arguments = Arrays.asList(new String[] { setupFile.getAbsolutePath(), "--name" });
            List<String> output = this.executableRunner.execute(directory, pythonExe, arguments).getStandardOutputAsList();
            projectName = ((String)output.get(output.size() - 1)).replace('_', '-').trim();
        }
        if(StringUtils.isBlank(projectName)){
            projectName = directory.getName();
        }
        return projectName;
    }

    private String resolveProjectVersionName(File directory, File pythonExe, File setupFile, String providedProjectVersionName) throws ExeRunnerException {
        String projectVersionName = providedProjectVersionName;
        if (StringUtils.isBlank(projectVersionName) && setupFile != null && setupFile.exists()) {
            List<String> arguments = Arrays.asList(new String[] { setupFile.getAbsolutePath(), "--version" });
            List<String> output = this.executableRunner.execute(directory, pythonExe, arguments).getStandardOutputAsList();
            projectVersionName = ((String)output.get(output.size() - 1)).trim();
        }
        return projectVersionName;
    }

    /**
     * 分析pipFreezeOutput获取包管理器输出依赖数据
     *
     * @param pipFreezeOutput pipFreezeOutput
     * @return 依赖集合
     */
    public PipenvFreeze pipenvFreezeAnalyze(List<String> pipFreezeOutput) {
        //1. 创建一个空的列表，用来存储转换后的依赖项
        List<PipenvFreezeDependencyEntry> entries = new ArrayList<>();

        //2. 遍历 pipFreezeOutput 中的每一行
        for (String line : pipFreezeOutput) {
            // 将每行按 "==" 分隔成两个部分
            String[] pieces = line.split("==");

            // 如果分隔后有两个部分，则将它们封装为 PipenvFreezeDependencyEntry 对象
            if (pieces.length == 2) {
                String packageName = pieces[0];
                String packageVersion = pieces[1];

                // 创建 PipenvFreezeDependencyEntry 对象并添加到列表中
                PipenvFreezeDependencyEntry entry = new PipenvFreezeDependencyEntry(packageName, packageVersion);
                entries.add(entry);
            }
        }

        //3. 使用解析后的条目创建一个新的 PipenvFreeze 对象
        return new PipenvFreeze(entries);
    }

    /**
     * 分析pipEnvGraphOutput
     *
     * @param pipEnvGraphOutput 输入的JSON字符串，表示Pipenv图的依赖关系
     * @return 返回一个包含有效依赖项的PipenvGraphDependency对象
     * @throws IllegalArgumentException 如果输入为null或JSON解析失败，抛出异常
     */
    public PipenvGraphDependency pipenvJsonGraphAnalyze(String pipEnvGraphOutput) {
        // 1. 检查输入参数pipEnvGraphOutput是否为null
        if (pipEnvGraphOutput == null) {
            // 如果为null，抛出IllegalArgumentException
            throw new IllegalArgumentException("输入的pipEnvGraphOutput不能为空");
        }

        // 2. 使用Gson将输入的JSON字符串反序列化为List<PipenvGraphDependencyEntry>类型
        List<PipenvGraphDependencyEntry> entries;
        try {
            // 反序列化操作，利用TypeToken获取泛型类型
            entries = this.gson.fromJson(pipEnvGraphOutput, new TypeToken<List<PipenvGraphDependencyEntry>>(){}.getType());
        } catch (JsonSyntaxException e) {
            // 如果JSON解析失败，抛出异常并附带详细的错误信息
            throw new IllegalArgumentException("解析JSON失败，检查输入的pipEnvGraphOutput格式是否正确", e);
        }

        // 3. 如果反序列化的结果为null（即空JSON或无效数据），则将entries设置为一个空列表
        if (entries == null) {
            entries = Collections.emptyList();
        }

        // 4. 使用流操作过滤掉null值的依赖项，确保返回的依赖项列表不包含无效数据
        List<PipenvGraphDependencyEntry> filteredEntries = entries.stream()
                .filter(Objects::nonNull)  // 过滤掉null元素
                .collect(Collectors.toList());  // 收集成一个新的列表

        // 5. 输出日志，记录过滤后的有效依赖项数量，方便调试
        System.out.println("过滤后的有效依赖项数量: " + filteredEntries.size());

        // 6. 返回包含有效依赖项的PipenvGraphDependency对象
        return new PipenvGraphDependency(filteredEntries);
    }

}
