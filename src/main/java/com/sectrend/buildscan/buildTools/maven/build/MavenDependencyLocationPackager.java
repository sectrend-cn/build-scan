package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.base.graph.MutableDependencyGraph;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.maven.model.MavenResult;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.utils.WildcardFilter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyLocationPackager {

    private static final List<String> KNOWN_SCOPES = Arrays.asList(new String[]{"compile", "import", "provided", "runtime", "system","test"});

    private static final Logger logger = LoggerFactory.getLogger(MavenDependencyLocationPackager.class);

    private final Pattern endOfTreePattern = Pattern.compile("^-*< .* >-*$");

    private final ForeignIdFactory foreignIdFactory;

    private List<MavenResult> dependencyLocations = new ArrayList<>();

    private MavenResult currentProject = null;

    private Stack<Dependency> dependencyParentStack = new Stack<>();

    private final List<Dependency> orphans = new ArrayList<>();

    private boolean isAnalyzingProjectSection;

    private int level;

    private boolean inOutOfScopeTree = false;
    private MutableDependencyGraph dependencyGraph = null;


    public MavenDependencyLocationPackager(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    public List<MavenResult> extractDependencyLocations(String sourcePath, String mavenOutputText, String excludedScopes, String includedScopes, String excludedModules, String includedModules) {
        // 初始化必要的过滤器和数据结构
        WildcardFilter moduleFilter = new WildcardFilter(excludedModules, includedModules);
        WildcardFilter scopeFilter = new WildcardFilter(excludedScopes, includedScopes);
        this.dependencyLocations = new ArrayList<>();
        this.dependencyParentStack = new Stack<>();
        this.isAnalyzingProjectSection = false;
        this.dependencyGraph = (MutableDependencyGraph) new MutableMapDependencyGraph();
        this.level = 0;

        // 逐行处理Maven的输出文本
        for (String line : mavenOutputText.split(System.lineSeparator())) {
            String trimmedLine = line.trim();

            // 跳过不需要处理的行
            if (skipLine(trimmedLine)) continue;

            // 处理可选的文本信息
            trimmedLine = cleanOptionalText(trimmedLine);

            // 如果在项目部分，初始化当前项目
            if (this.isAnalyzingProjectSection && this.currentProject == null) {
                initializeCurrentProject(moduleFilter, sourcePath, trimmedLine);
                continue;
            }

            // 处理项目结束的标志
            if (isProjectFinished(trimmedLine)) {
                finalizeProject();
            } else {
                processDependencyLine(trimmedLine, scopeFilter);
            }
        }

        // 处理并添加孤立的模块到图
        addOrphansToGraph(this.dependencyGraph, this.orphans);

        return this.dependencyLocations;
    }

    // 清理包含 "optional" 的行
    private String cleanOptionalText(String line) {
        if (line.endsWith("(optional)")) {
            return line.replace("(optional)", "").trim();
        }
        return line;
    }

    // 判断是否到达项目部分结束
    private boolean isProjectFinished(String line) {
        return line.contains("--------") || this.endOfTreePattern.matcher(line).matches();
    }

    // 完成当前项目的分析并清理
    private void finalizeProject() {
        this.currentProject = null;
        this.dependencyParentStack.clear();
        this.isAnalyzingProjectSection = false;
        this.level = 0;
    }

    // 处理每一行的依赖信息
    private void processDependencyLine(String line, WildcardFilter scopeFilter) {
        int previousLevel = this.level;
        String cleanedLine = calculateCurrentLevelAndCleanLine(line);
        ScopedDependency dependency = parseDependencyFromText(cleanedLine);
        if (dependency != null && this.currentProject != null) {
            populateGraphDependencies(scopeFilter, dependency, previousLevel);
        }
    }

    /**
     * Maven解析本地结果文件
     * @param sourceFile
     * @param excludedScopes
     * @param includedScopes
     * @param excludedModules
     * @param includedModules
     * @return
     */
    public List<MavenResult> extractLocalResult(File sourceFile, String excludedScopes, String includedScopes, String excludedModules, String includedModules) {
        // 创建过滤器
        WildcardFilter moduleFilter = new WildcardFilter(excludedModules, includedModules);
        WildcardFilter scopeFilter = new WildcardFilter(excludedScopes, includedScopes);

        // 初始化数据结构
        this.dependencyLocations = new ArrayList<>();
        this.currentProject = null;
        this.dependencyParentStack = new Stack<>();
        this.dependencyGraph = new MutableMapDependencyGraph();
        this.level = 0;

        // 读取文件并处理内容
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                this.logger.debug(line);

                // 跳过空行
                if (StringUtils.isBlank(line)) {
                    continue;
                }

                // 处理项目依赖或子依赖
                if (isProject(line.trim())) {
                    processProject(line.trim(), moduleFilter, sourceFile.getPath());
                } else {
                    processDependency(line, scopeFilter);
                }
            }
        } catch (IOException e) {
            logAndHandleFileReadError(sourceFile, e);
        }

        // 处理孤立的依赖
        addOrphansToGraph(this.dependencyGraph, this.orphans);

        return this.dependencyLocations;
    }

    // 处理项目部分的初始化
    private void processProject(String line, WildcardFilter moduleFilter, String sourcePath) {
        initializeCurrentProject(moduleFilter, sourcePath, line);
        resetForNewProject();
    }

    // 处理依赖部分
    private void processDependency(String line, WildcardFilter scopeFilter) {
        int previousLevel = this.level;

        // 获取并处理子依赖
        String cleanedLine = calculateCurrentLevelAndCleanLine(line);
        if (this.level != 0) {
            ScopedDependency dependency = parseDependencyFromText(cleanedLine);
            if (dependency != null && this.currentProject != null) {
                populateGraphDependencies(scopeFilter, dependency, previousLevel);
            }
        } else {
            resetForNewProject();
        }
    }

    // 重置数据以处理新的项目
    private void resetForNewProject() {
        this.currentProject = null;
        this.level = 0;
        this.dependencyParentStack.clear();
    }

    // 记录并处理文件读取错误
    private void logAndHandleFileReadError(File sourceFile, Exception e) {
        String errorMsg = String.format("读取依赖结果文件失败: %s", sourceFile.getAbsolutePath());
        this.logger.debug(errorMsg, e);
        this.logger.error(errorMsg);
    }


    /**
     * 判断当前是否是项目
     * @param line
     * @return
     */
    private boolean isProject(String line){
        //Pattern pattern = Pattern.compile("[\\\\|+ ]+");
        //校验字符中是否有非数字和非字母 以及.:-符号
        Pattern pattern = Pattern.compile("[^\\w.:-]+");
        Matcher matcher = pattern.matcher(line);
        if(!matcher.find() && line.split(":").length == 4){
            this.isAnalyzingProjectSection = true;
            return true;
        }
        return false;
    }

    private boolean skipLine(String line) {
        // 如果该行与当前分析无关，跳过
        int index = indexOfTrailing(line, new String[] { "[", "INFO", "]" });
        if (index == -1)
            return true;
        String trimmedLine = line.substring(index);
        if (StringUtils.isBlank(trimmedLine) || trimmedLine.contains("Downloaded") || trimmedLine.contains("Downloading"))
            return true;

        // 去除日志级别信息后的行内容
        line = trimmedLine.replaceFirst("^\\s", "");

        // 如果行为空，跳过
        if (StringUtils.isBlank(line)) return true;

        // 如果是项目部分的开始，切换到分析项目部分
        if (indexOfTrailing(line, new String[] { "---", "dependency", ":", "tree" }) != -1) {
            this.isAnalyzingProjectSection = true;
            return true;
        }

        // 如果当前不在分析项目部分，跳过
        if (!this.isAnalyzingProjectSection) return true;

        // 如果是依赖树更新的行，跳过
        return line.contains("checking for updates");
    }

    private void initializeCurrentProject(WildcardFilter modulesFilter, String sourcePath, String line) {
        this.dependencyGraph = new MutableMapDependencyGraph();

        Dependency dependency = parseProjectFromText(line);

        // 如果解析到依赖项，则调整路径并更新 Maven 项目
        if (dependency != null) {
            String dependencyLocationSourcePath = sourcePath.endsWith(dependency.getName())
                    ? sourcePath
                    : sourcePath + "/pom.xml";

            DependencyLocation dependencyLocation = new DependencyLocation(
                    this.dependencyGraph,
                    dependency.getForeignId(),
                    new File(dependencyLocationSourcePath)
            );

            MavenResult mavenProject = new MavenResult(dependency.getName(), dependency.getVersion(), dependencyLocation);

            // 处理 Maven 项目，更新当前项目或清理状态
            if (modulesFilter.shouldInclude(mavenProject.getProjectName())) {
                logger.trace("Project: {}", mavenProject.getProjectName());
                this.currentProject = mavenProject;
                this.dependencyLocations.add(mavenProject);
            }
        } else {
            logger.trace("Project: unknown");
            resetProjectState();
        }
    }

    private void resetProjectState() {
        this.currentProject = null;
        this.dependencyParentStack.clear();
        this.isAnalyzingProjectSection = false;
        this.level = 0;
    }


    private void populateGraphDependencies(WildcardFilter scopeFilter, ScopedDependency dependency, int previousLevel) {
        if (this.level == 1) {
            if (scopeFilter.shouldInclude(dependency.scope)) {
                logger.trace(
                        String.format("Level 1 component %s:%s:%s:%s is in scope; adding it to hierarchy root", new Object[] { dependency.getForeignId().getGroup(), dependency.getForeignId().getName(), dependency.getForeignId().getVersion(), dependency.scope }));
                this.dependencyGraph.addChildToRoot(dependency);
                this.inOutOfScopeTree = false;
            } else {
                logger.trace(String.format("Level 1 component %s:%s:%s:%s is a top-level out-of-scope component; entering non-scoped tree", new Object[] { dependency.getForeignId().getGroup(), dependency.getForeignId().getName(), dependency
                        .getForeignId().getVersion(), dependency.scope }));
                this.inOutOfScopeTree = true;
            }
            this.dependencyParentStack.clear();
            this.dependencyParentStack.push(dependency);
        } else if (this.level == previousLevel) {
            this.dependencyParentStack.pop();
            addDependencyIfInScope(this.dependencyGraph, this.orphans, scopeFilter, this.inOutOfScopeTree, this.dependencyParentStack.peek(), dependency);
            this.dependencyParentStack.push(dependency);
        } else if (this.level > previousLevel) {
            addDependencyIfInScope(this.dependencyGraph, this.orphans, scopeFilter, this.inOutOfScopeTree, this.dependencyParentStack.peek(), dependency);
            this.dependencyParentStack.push(dependency);
        } else {
            for (int i = previousLevel; i >= this.level; i--)
                this.dependencyParentStack.pop();
            addDependencyIfInScope(this.dependencyGraph, this.orphans, scopeFilter, this.inOutOfScopeTree, this.dependencyParentStack.peek(), dependency);
            this.dependencyParentStack.push(dependency);
        }
    }

    private void addOrphansToGraph(MutableDependencyGraph graph, List<Dependency> orphansList) {
        int orphanCount = orphansList.size();
        logger.trace("# orphans: {}", orphanCount);

        if (orphanCount > 0) {
            ForeignId foreignId = this.foreignIdFactory.createMavenForeignId("none", "Additional_Components", "none");
            Dependency orphanParent = new Dependency("Additional_Components", "none", foreignId);
            logger.trace("Adding orphan list parent dependency: {}", orphanParent.getForeignId());

            orphansList.forEach(orphan -> {
                logger.trace("Adding orphan: {}", orphan.getForeignId());
                graph.addParentWithChild(orphanParent, orphan);
            });
        }
    }


    private void addDependencyIfInScope(MutableDependencyGraph currentGraph, List<Dependency> orphans, WildcardFilter scopeFilter, boolean inOutOfScopeTree, Dependency parent, ScopedDependency dependency) {
        if (scopeFilter.shouldInclude(dependency.scope))
            if (inOutOfScopeTree) {
                logger.trace(
                        String.format("component %s:%s:%s:%s is in scope but in a nonScope tree; adding it to orphans", new Object[] { dependency.getForeignId().getGroup(), dependency.getForeignId().getName(), dependency.getForeignId().getVersion(), dependency.scope }));
                orphans.add(dependency);
            } else {
                logger.trace(String.format("component %s:%s:%s:%s is in scope and in an in-scope tree; adding it to hierarchy", new Object[] { dependency.getForeignId().getGroup(), dependency.getForeignId().getName(), dependency
                        .getForeignId().getVersion(), dependency.scope }));
                currentGraph.addParentWithChild(parent, dependency);
            }
    }

    public String calculateCurrentLevelAndCleanLine(String line) {
        this.level = 0;
        String cleanedLine = line;

        // 定义所有的缩进模式
        List<String> indentationStrings = Arrays.asList("+- ", "|  ", "\\- ", "   ");
        StringBuilder regexPattern = new StringBuilder();

        // 构建正则表达式模式
        for (String pattern : indentationStrings) {
            if (regexPattern.length() > 0) {
                regexPattern.append("|");  // 使用“|”连接多个模式
            }
            regexPattern.append(Pattern.quote(pattern));
        }

        // 使用正则表达式一次性替换并计算缩进级别
        Pattern pattern = Pattern.compile(regexPattern.toString());
        Matcher matcher = pattern.matcher(cleanedLine);

        while (matcher.find()) {
            this.level++;
        }

        // 替换所有缩进模式
        cleanedLine = matcher.replaceAll("");

        return cleanedLine;
    }

    public ScopedDependency parseDependencyFromText(String dependencyText) {
        // 如果不是有效的 GAV 格式，直接返回 null
        if (BooleanUtils.isFalse(isGav(dependencyText))) {
            return null;
        }

        String[] gavText = dependencyText.split(":");
        if (gavText.length < 4) {
            return null;
        }

        // 提取 GAV 部分
        String group = gavText[0];
        String artifact = gavText[1];
        String version = gavText[gavText.length - 2];
        String scope = gavText[gavText.length - 1];

        // 检查是否为已知的 scope
        Set<String> knownScopeSet = new HashSet<>(KNOWN_SCOPES); // 转换为 Set 提高查找效率
        boolean isRecognizedScope = knownScopeSet.stream().anyMatch(scope::startsWith);

        // 构建版本信息
        String fullVersion = version + "_|_" + scope;

        // 如果 scope 未识别，记录警告日志
        if (BooleanUtils.isFalse(isRecognizedScope)) {
            logger.warn("This line could not be parsed correctly due to unknown dependency format: " + dependencyText);
        }

        if (group.startsWith("[INFO] ")) {
            group = group.replace("[INFO] ", "").trim();
        }

        // 创建 ForeignId 和 ScopedDependency
        ForeignId foreignId = this.foreignIdFactory.createMavenForeignId(group, artifact, fullVersion);
        return new ScopedDependency(artifact, fullVersion, foreignId, scope);
    }

    public Dependency parseProjectFromText(String dependencyText) {
        // 如果不是有效的 GAV 格式，直接返回 null
        if (BooleanUtils.isFalse(isGav(dependencyText))) {
            return null;
        }

        String[] gavText = dependencyText.split(":");
        if (gavText.length < 4 || gavText.length > 5) {
            // GAV 格式不正确，返回 null，并记录日志
            logger.debug("{} does not look like a dependency we can parse", dependencyText);
            return null;
        }

        // 提取 GAV 部分
        String group = gavText[0];
        String artifact = gavText[1];
        String version = gavText[gavText.length - 1];

        if (group.startsWith("[INFO] ")) {
            group = group.replace("[INFO] ", "").trim();
        }
        // 创建 ForeignId 和 Dependency 对象
        ForeignId foreignId = this.foreignIdFactory.createMavenForeignId(group, artifact, version);
        return new Dependency(artifact, version, foreignId);
    }

    public boolean isGav(String dependencyText) {
        String[] gavText = dependencyText.split(":");
        if (gavText.length < 4) {
            logger.debug("{} does not look like a GAV we recognize", dependencyText);
            return false;
        }

        for (String gav : gavText) {
            if (StringUtils.isBlank(gav)) {
                logger.debug("{} does not look like a GAV we recognize", dependencyText);
                return false;
            }
        }

        return true;
    }

    public int indexOfTrailing(String line, String... segments) {
        int endOfSegments = 0;

        for (String segment : segments) {
            int index = line.indexOf(segment, endOfSegments);
            if (index == -1) {
                return -1;
            }
            endOfSegments = index + segment.length();
        }

        return endOfSegments;
    }
}
