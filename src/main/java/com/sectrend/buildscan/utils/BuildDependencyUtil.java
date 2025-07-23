package com.sectrend.buildscan.utils;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.configuration.RunBeanConfiguration;
import com.sectrend.buildscan.enums.DetectBusinessParams;
import com.sectrend.buildscan.exception.DetectorFinderDirectoryListException;
import com.sectrend.buildscan.executable.impl.SimpleExecutableFinder;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.executable.impl.SimpleLocalExecutableFinder;
import com.sectrend.buildscan.executable.impl.SimpleSystemExecutableFinder;
import com.sectrend.buildscan.factory.ScannableFactory;
import com.sectrend.buildscan.factory.OptionOperation;
import com.sectrend.buildscan.finder.DetectorFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.finder.impl.ScanFileFinder;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import com.sectrend.buildscan.handler.ExtractHandler;
import com.sectrend.buildscan.model.*;
import com.sectrend.buildscan.result.ResultTransform;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sectrend.buildscan.enums.BuildType.*;

/**
 * 生成构建信息工具类
 */
public class BuildDependencyUtil {

    private static final Logger logger = LoggerFactory.getLogger(BuildDependencyUtil.class);
    private static SimpleExecutableResolver simpleExecutableResolver;

    //private static List<String> SPLIT_LIST = Arrays.asList(MAKEFILE_BUILD.getBuildType(), GOMOD_BUILD.getBuildType());
//    private static List<String> SPLIT_LIST = Arrays.asList(GOMOD_BUILD.getBuildType());

    static {
        ScanFileFinder bean = (ScanFileFinder) RunBeanConfiguration.detectFileFinder;
        SimpleLocalExecutableFinder simpleLocalExecutableFinder = new SimpleLocalExecutableFinder(SimpleExecutableFinder.forCurrentOperatingSystem(bean));
        SimpleSystemExecutableFinder simpleSystemExecutableFinder = new SimpleSystemExecutableFinder(SimpleExecutableFinder.forCurrentOperatingSystem(bean));
        simpleExecutableResolver = new SimpleExecutableResolver(null,
                simpleLocalExecutableFinder,
                simpleSystemExecutableFinder
        );
    }

    public static SimpleExecutableResolver getSimpleExecutableResolver() {
        return simpleExecutableResolver;
    }

    public static List<DependencyRoot> buildDependencyRoot(NewScanInfo newScanInfo, ScannableEnvironment scannableEnvironment) {
        return buildDependencyRoot(newScanInfo, scannableEnvironment, null);
    }


    /**
     * 构建
     *
     * @param newScanInfo
     * @param scannableEnvironment
     * @param filePathCollect
     */
    public static List<DependencyRoot> buildDependencyRoot(NewScanInfo newScanInfo, ScannableEnvironment scannableEnvironment, FilePathCollect filePathCollect) {
        //返回依赖数据对象
        DependencyRoot dependencyRoot = new DependencyRoot();
        //返回构建扫描的数据集
        List<DependencyRoot> resultList = new ArrayList<>();

        //判断是否需要扫描
        if (newScanInfo.getBuildDepend() != null && newScanInfo.getBuildDepend()) {
            //校验扫描路径和项目类型
            CheckUtils.checkLocationAndType(newScanInfo);
            scannableEnvironment.setNpmExclude(newScanInfo.getNpmExclude());
            try {
                resultList = buildScan(scannableEnvironment, newScanInfo, filePathCollect);
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        } else {
            initDependencyRoot(dependencyRoot);
            resultList.add(dependencyRoot);
        }
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        if (newScanInfo.getBuildResultFlag() != null && newScanInfo.getBuildResultFlag() == 0){
            List<DependencyRoot> resultLogList = new ArrayList<>();
            resultList = filterDependencyType(resultList, newScanInfo.getFilterDependency());

            resultList.forEach(result -> {
                DependencyRoot resultLog = new DependencyRoot();
                List<DependencyInfo> dependencyInfoLogList = new ArrayList<>();
                result.getDependencyInfoList().forEach(dependencyInfo -> {
                    DependencyInfo dependencyInfoLog = new DependencyInfo();
                    dependencyInfoLog.setProjectName(dependencyInfo.getProjectName());
                    dependencyInfoLog.setRootMap(new HashMap<>());
                    dependencyInfoLog.setRelationshipsMap(new HashMap<>());
                    dependencyInfoLogList.add(dependencyInfoLog);
                });
                resultLog.setDependencyInfoList(dependencyInfoLogList);
                resultLog.setBuildSource(result.getBuildSource());
                resultLog.setBuildFlag(result.isBuildFlag());
                resultLog.setSourcePath(result.getSourcePath());
                resultLogList.add(resultLog);
            });
            logger.info("build result size is : " + gson.toJson(resultLogList));
        } else {
            logger.info("build result: " + gson.toJson(resultList));
        }
        return resultList;
    }


    public static List<DependencyRoot> filterDependencyType(List<DependencyRoot> resultList, List<FilterDependencyDto> filterDependencyDto) {


        List<String> gradleInclude = null;
        List<String> mvnInclude = new ArrayList<>();
        List<String> npmExclude = new ArrayList<>();
        List<DependencyRoot> list = new ArrayList<>();

        if (CollectionUtils.isEmpty(filterDependencyDto) || CollectionUtils.isEmpty(resultList)) {
            list = resultList;
            return list;
        }

        for (FilterDependencyDto dependencyDto : filterDependencyDto) {
            if (CollectionUtils.isEmpty(dependencyDto.getExclude()) && CollectionUtils.isEmpty(dependencyDto.getInclude())) {
                continue;
            }
//
//            if (dependencyDto.getBuildType().equals(NPM_BUILD.getBuildType())) {
//                npmExclude = dependencyDto.getExclude();
//            }

            if (dependencyDto.getBuildType().equals(MVN_BUILD.getBuildType())) {
                mvnInclude = dependencyDto.getInclude();
            }
//
//            if (dependencyDto.getBuildType().equals(GRADLE_BUILD.getBuildType())) {
//                gradleInclude = dependencyDto.getInclude().stream().map(includeStr -> includeStr.replace("*", ".*")).collect(Collectors.toList());
//            }

        }

        if (CollectionUtils.isEmpty(gradleInclude) && CollectionUtils.isEmpty(npmExclude) && CollectionUtils.isEmpty(mvnInclude)) {
            list = resultList;
            return list;
        }

        for (DependencyRoot r : resultList) {
            DependencyRoot dependencyRoot = new DependencyRoot();
            List<DependencyInfo> dependencyList = new ArrayList<>();
            List<DependencyInfo> dependencyInfoList = r.getDependencyInfoList();
            for (DependencyInfo dependencyInfo : dependencyInfoList) {
                DependencyInfo dependency = new DependencyInfo();
                dependency.setProjectName(dependencyInfo.getProjectName());
                Map<String, List<String>> resultRoot = new HashMap<>();
                Map<String, List<String>> resultRelationships = new HashMap<>();
//
//                if (r.getBuildSource().equals(NPM_BUILD.getBuildType()) && CollectionUtils.isNotEmpty(npmExclude)) {
//                    List<String> finalNpmExclude1 = npmExclude;
//                    resultRoot = dependencyInfo.getRootMap().entrySet().stream()
//                            .filter(d -> {
//                                String[] split = d.getKey().split("_\\|_");
//                                String scope = getScope(split);
//                                return split.length == 1 || !finalNpmExclude1.contains(scope);
//                            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//
//                    List<String> finalNpmExclude = npmExclude;
//                    resultRelationships = dependencyInfo.getRelationshipsMap().entrySet().stream()
//                            .filter(d -> {
//                                String[] split = d.getKey().split("_\\|_");
//                                String scope = getScope(split);
//                                return split.length == 1 || !finalNpmExclude.contains(scope);
//                            })
//                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//                } else
                if (r.getBuildSource().equals(MVN_BUILD.getBuildType()) && CollectionUtils.isNotEmpty(mvnInclude)) {
                    List<String> finalMvnInclude1 = mvnInclude;
                    resultRoot = dependencyInfo.getRootMap().entrySet().stream()
                            .filter(d -> {
                                String[] split = d.getKey().split("_\\|_");
                                String scope = getScope(split);
                                return split.length == 1 || finalMvnInclude1.contains(scope);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    List<String> finalMvnInclude = mvnInclude;
                    resultRelationships = dependencyInfo.getRelationshipsMap().entrySet().stream()
                            .filter(d -> {
                                String[] split = d.getKey().split("_\\|_");
                                String scope = getScope(split);
                                return split.length == 1 || finalMvnInclude.contains(scope);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//                } else if ((r.getBuildSource().equals(GRADLE_BUILD.getBuildType()) || r.getBuildSource().equals(GRADLE_TEXT_BUILD.getBuildType()))
//                        && CollectionUtils.isNotEmpty(gradleInclude)) {
//                    List<String> finalGradleInclude = gradleInclude;
//                    resultRoot = dependencyInfo.getRootMap().entrySet().stream()
//                            .filter(d -> {
//                                String[] split = d.getKey().split("_\\|_");
//                                String scope = getScope(split);
//                                return split.length == 2 && finalGradleInclude.stream().anyMatch(scope::matches);
//                            })
//                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//                    resultRelationships = dependencyInfo.getRelationshipsMap().entrySet().stream()
//                            .filter(d -> {
//                                String[] split = d.getKey().split("_\\|_");
//                                String scope = getScope(split);
//                                return split.length == 2 && finalGradleInclude.stream().anyMatch(scope::matches);
//                            })
//                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                } else {
                    resultRoot = dependencyInfo.getRootMap();
                    resultRelationships = dependencyInfo.getRelationshipsMap();

                }


                dependency.setRootMap(resultRoot);
                dependency.setRelationshipsMap(resultRelationships);
                dependencyList.add(dependency);
            }
            //遍历过滤
            dependencyRoot.setDependencyInfoList(dependencyList);
            dependencyRoot.setSourcePath(r.getSourcePath());
            dependencyRoot.setBuildSource(r.getBuildSource());
            dependencyRoot.setBuildFlag(r.isBuildFlag());
            list.add(dependencyRoot);
        }
        return list;
    }

    private static String getScope(String[] split) {
        String scope = split.length == 2 ? split[1] : "";
        if (StringUtils.isNotBlank(scope) && scope.contains("%%")) {
            int indexOf = scope.indexOf("%%");
            scope = scope.substring(0, indexOf);
        }
        return scope;
    }


    /**
     * 执行构建扫描
     *
     * @param scannableEnvironment
     * @return
     */
    public static List<DependencyRoot> buildScan(ScannableEnvironment scannableEnvironment, NewScanInfo newScanInfo, FilePathCollect filePathCollect) throws DetectorFinderDirectoryListException {

        //返回构建扫描的数据集
        List<DependencyRoot> resultList = new ArrayList<>();

        DependencyRoot dependencyRoot;

        // todo 处理newScanInfo2DetectProperties
        DetectProperties detectProperties = new DetectProperties();
//        高级参数禁用
//        newScanInfo2DetectProperties(newScanInfo, detectProperties);

        if (StringUtils.isNotBlank(newScanInfo.getBuildType())) {

            if (!(MVN_TEXT_BUILD.getBuildType().equals(newScanInfo.getBuildType())
//                    || GRADLE_TEXT_BUILD.getBuildType().equals(newScanInfo.getBuildType())
            )) {
                logger.error("not found buildTree type: buildType:{}", newScanInfo.getBuildType());
                System.exit(11);
            }

            if (!CheckUtils.checkTxtDir(newScanInfo.getBuildTreeFile())) {
                System.exit(11);
            }

            //依赖树解析
            try {
                //此处接收DetectProperties参数,来自NewScanInfo透传，需在NewScanInfo侧进行,数据处理
                scannableEnvironment.setArguments(OptionOperation.getProperties(newScanInfo.getBuildType(), detectProperties));
                scannableEnvironment.setBuildScanType(newScanInfo.getDefaultParamInfo().getBuildScanType());
                ScanResults scanResults = executeHandler(ScannableFactory.textHandlerMap.get(newScanInfo.getBuildType()), scannableEnvironment, scannableEnvironment.getDirectory().getPath(), simpleExecutableResolver);
                dependencyRoot = toDependencyRoot(scanResults);
                dependencyRoot.setSourcePath(scannableEnvironment.getDirectory().getPath());
                dependencyRoot.setBuildSource(newScanInfo.getBuildType());
                resultList.add(dependencyRoot);
            } catch (Exception e) {
                logger.error("buildTree file scan error", e);
            }
        }
        FileFinder simpleFileFinder = null;
        if (filePathCollect != null && !filePathCollect.isEmpty()) {
            simpleFileFinder = new SimpleFileFinder();
            simpleFileFinder.setFilePaths(filePathCollect.getFiles());
        } else {
            simpleFileFinder = RunBeanConfiguration.simpleFileFinder;
        }
        List<String> packageManagerTypes = analyzePackageManagerTypes(newScanInfo);
        DetectorFinder detectorFinder = new DetectorFinder(simpleFileFinder, newScanInfo.getBuildType(), newScanInfo.getAttachToPath(), packageManagerTypes);
//        detectorFinder.addExcludeFilePath("bower", newScanInfo.getBowerDependencyPathExcluded());
        File scanRootDir = scannableEnvironment.getDirectory();
        //System.out.println("scanRootDir ============" + scanRootDir);
        /**
         * 多项目多包 自动识别
         */
        HashMap<String, List<ScannableEnvironment>> detectorProjectMap = detectorFinder.findDetectorAndProjects(scanRootDir, filePathCollect);
        String parent = scannableEnvironment.getDirectory().getParent();
        String buildScanRootPath = parent.endsWith(File.separator) ? parent : parent + File.separator;
        List<DependencyLocation> dependencyLocations;

        for (Map.Entry<String, List<ScannableEnvironment>> detectorProjectEntry : detectorProjectMap.entrySet()) {
            List<ScannableEnvironment> scannableEnvironmentList = detectorProjectEntry.getValue();
            if (scannableEnvironmentList.isEmpty()) {
                continue;
            }
            // 获取项目根目录文件
            scannableEnvironment = scannableEnvironmentList.get(0);
            // 项目多模块集合
            scannableEnvironment.setScannableEnvironmentList(scannableEnvironmentList);
            scannableEnvironment.setReferenceFileList(detectorFinder.referenceFileList);
            scannableEnvironment.setUseGradlewFirst(newScanInfo.getDefaultParamInfo().getIsBuildWithGradlew() == 1);
            try {
                String buildType = scannableEnvironment.getBuildType();

                // TODO 此处接收DetectProperties参数,来自NewScanInfo透传，需在NewScanInfo侧进行,数据处理
                scannableEnvironment.setArguments(OptionOperation.getProperties(buildType, detectProperties));
                scannableEnvironment.setBuildScanType(newScanInfo.getDefaultParamInfo().getBuildScanType());

                ScanResults scanResults = executeHandler(ScannableFactory.handlerMap.get(buildType), scannableEnvironment, scannableEnvironment.getDirectory().getPath(), simpleExecutableResolver);
                if (scanResults == null) {
                    continue;
                }
                String sourcePath;
                // 是否拆分构建结果 如果是 非构建-并且多模块项目 则拆分
                if (isSplit(scanResults, buildType)) {
                    dependencyLocations = new ArrayList<>(scanResults.getDependencyLocations());
                    for (DependencyLocation dependencyLocation : dependencyLocations) {
                        // 重置 DependencyLocation 值
                        scanResults.resetCodeLocations(dependencyLocation);
                        sourcePath = dependencyLocation.getSourcePath().isPresent() ? dependencyLocation.getSourcePath().get().getAbsolutePath() : "";
                        addDependencyRoot(scanResults, buildType, sourcePath, resultList, buildScanRootPath, newScanInfo);
                    }
                } else {
                    //sourcePath = StringUtils.isNotBlank(scannableEnvironment.getDirectory().getAbsolutePath()) ? scannableEnvironment.getDirectory().getAbsolutePath() : "";
                    if (scanResults.getDependencyLocations() != null && scanResults.getDependencyLocations().size() > 0) {
                        DependencyLocation dependencyLocation = scanResults.getDependencyLocations().get(0);
                        sourcePath = dependencyLocation.getSourcePath().isPresent() ? dependencyLocation.getSourcePath().get().getAbsolutePath() : "";
                    } else {
                        sourcePath = StringUtils.isNotBlank(scannableEnvironment.getDirectory().getAbsolutePath()) ? scannableEnvironment.getDirectory().getAbsolutePath() : "";
                    }
                    addDependencyRoot(scanResults, buildType, sourcePath, resultList, buildScanRootPath, newScanInfo);
                }
            } catch (Exception e) {
                logger.error(scannableEnvironment.getBuildType() + ": ", e);
            }
        }
        return resultList;
    }

    @NotNull
    private static List<String> analyzePackageManagerTypes(NewScanInfo newScanInfo) {
        List<String> packageManagerTypes;
        if (StringUtils.isNotBlank(newScanInfo.getPackageManagerTypes())) {
            String[] packageManagerTypeArr = newScanInfo.getPackageManagerTypes().split(",|，");
            packageManagerTypes = Arrays.asList(packageManagerTypeArr);
        } else {
            packageManagerTypes = new ArrayList<>();
        }
        return packageManagerTypes;
    }

    private static void newScanInfo2DetectProperties(NewScanInfo newScanInfo, DetectProperties detectProperties) {

        Field[] declaredFields = newScanInfo.getClass().getDeclaredFields();

        Set<String> attributeNames = DetectBusinessParams.allAttributeName();

        Multimap<String, HashMap<String, Object>> multimap;
        try (Stream<Field> detect = Arrays.stream(declaredFields).filter(v -> {
            try {
                v.setAccessible(true);
                return attributeNames.contains(v.getName()) && v.get(newScanInfo) != null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        })) {
            multimap = ArrayListMultimap.create();
            detect.forEach(v -> {
                v.setAccessible(true);
                String buildType = StringUtil.getStrBeforeUpperCase(v.getName());
                HashMap<String, Object> map = new HashMap<>();
                try {
                    map.put(v.getName(), v.get(newScanInfo));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (!map.isEmpty()) {
                    multimap.put(buildType, map);
                }
            });
        }

        if (!multimap.isEmpty()) {
            multimap.keys().forEach(key -> OptionOperation.setProperties(key, maps2Properties(multimap.get(key)), detectProperties));
        }
    }

    private static Properties maps2Properties(Collection<HashMap<String, Object>> maps) {
        Properties properties = new Properties();
        for (HashMap<String, Object> map : maps) {
            properties.putAll(map);
        }
        return properties;
    }

    public static boolean isProperSubCollection(List<DependencyInfo> list, Map<String, List<String>> relationshipsMap, Map<String, List<String>> rootMap) {
        return !relationshipsMap.isEmpty() || !rootMap.isEmpty() || list.size() > 1;
    }

    public static void initDependencyRoot(DependencyRoot dependencyRoot) {
        //DependencyRoot dependencyRoot = new DependencyRoot();
        DependencyInfo dependencyInfo = new DependencyInfo();
        dependencyInfo.setRootMap(new HashMap<>());
        dependencyInfo.setRelationshipsMap(new HashMap<>());
        dependencyInfo.setProjectName("");
        List<DependencyInfo> dependencyInfoList = new ArrayList<>();
        dependencyInfoList.add(dependencyInfo);
        dependencyRoot.setDependencyInfoList(dependencyInfoList);
        dependencyRoot.setBuildFlag(false);
        // return dependencyRoot;
    }


    /**
     * 执行 handler, 并处理抛出的异常
     *
     * @param extractHandler
     * @param scannableEnvironment
     * @param taskDir
     * @param simpleExecutableResolver
     * @return
     */
    public static ScanResults executeHandler(ExtractHandler extractHandler, ScannableEnvironment scannableEnvironment,
                                             String taskDir, SimpleExecutableResolver simpleExecutableResolver) {
        ScanResults handler = null;
        try {
            logger.info("build scan path {}", scannableEnvironment.getDirectory());
            handler = extractHandler.handler(scannableEnvironment, taskDir, simpleExecutableResolver);
        } catch (Throwable e) {
            logger.error("", e);
        }
        return handler;
    }

    /**
     * 构建结果结果转换
     *
     * @param handler
     * @return
     */
    public static DependencyRoot toDependencyRoot(ScanResults handler) {
        DependencyRoot dependencyRoot = new DependencyRoot();
        List<DependencyInfo> dependencyInfoList = new ArrayList<>();

        if (handler == null || CollectionUtils.isEmpty(handler.getDependencyLocations())) {
            initDependencyRoot(dependencyRoot);
            return dependencyRoot;
        }

        for (DependencyLocation dependencyLocation : handler.getDependencyLocations()) {
            DependencyInfo dependencyInfo = new DependencyInfo();
            if (dependencyLocation.getDependencyGraph() != null && dependencyLocation.getDependencyGraph().getRelationships() != null) {
                dependencyInfo.setRelationshipsMap(ResultTransform.transformRelationshipsResult(dependencyLocation));
            } else {
                dependencyInfo.setRelationshipsMap(new HashMap<>());
            }

            if (dependencyLocation.getDependencyGraph() != null && dependencyLocation.getDependencyGraph().getRootDependencies() != null) {
                dependencyInfo.setRootMap(ResultTransform.transformRootResult(dependencyLocation));
            } else {
                dependencyInfo.setRootMap(new HashMap<>());
            }

            if (dependencyLocation.getForeignId().isPresent()) {
                dependencyInfo.setProjectName(dependencyLocation.getForeignId().get().getName());
            }

            if (StringUtils.isEmpty(dependencyInfo.getProjectName())) {
                dependencyInfo.setProjectName(handler.getScanProjectName());
            }

            if (dependencyLocation.getForeignId().isPresent() && "pypi".equals(dependencyLocation.getForeignId().get().getSupplier().getName())) {
                dependencyInfo.setProjectName(handler.getScanProjectName());
            }

            dependencyInfoList.add(dependencyInfo);
        }
        //是否是构建
        dependencyRoot.setBuildFlag(handler.getBuildFlag());
        dependencyRoot.setDependencyInfoList(dependencyInfoList);

        return dependencyRoot;
    }

    public static void addDependencyRoot(ScanResults scanResults, String buildType, String sourcePath, List<DependencyRoot> resultList) {
        addDependencyRoot(scanResults, buildType, sourcePath, resultList, null);
    }


    public static void addDependencyRoot(ScanResults scanResults, String buildType, String sourcePath, List<DependencyRoot> resultList, String buildScanRootPath, NewScanInfo newScanInfo) {
        addDependencyRoot(scanResults, buildType, sourcePath, resultList, sourcePath.startsWith(buildScanRootPath) ? buildScanRootPath : newScanInfo.getDecompressionParentDirectory());
    }

    /**
     * 添加 DependencyRoot
     */
    public static void addDependencyRoot(ScanResults scanResults, String buildType, String sourcePath, List<DependencyRoot> resultList, String buildScanRootPath) {

        DependencyRoot dependencyRoot = toDependencyRoot(scanResults);
        if (CollectionUtils.isNotEmpty(dependencyRoot.getDependencyInfoList())
                && isProperSubCollection(
                dependencyRoot.getDependencyInfoList(), dependencyRoot.getDependencyInfoList().get(0).getRelationshipsMap(),
                dependencyRoot.getDependencyInfoList().get(0).getRootMap())) {
            dependencyRoot.setBuildSource(buildType);

            if (StringUtils.isNotBlank(buildScanRootPath) && StringUtils.isNotBlank(sourcePath)) {
                sourcePath = sourcePath.substring(buildScanRootPath.length());
            }
            dependencyRoot.setSourcePath(sourcePath);
            resultList.add(dependencyRoot);
        }
    }


    /**
     * 是否拆分结果 如果是 非构建-并且多模块项目 则拆分
     *
     * @param buildType  构建类型
     * @param scanResults 构建结果
     * @return
     */
    public static boolean isSplit(ScanResults scanResults, String buildType) {
        boolean flag = scanResults != null && CollectionUtils.isNotEmpty(scanResults.getDependencyLocations());
        //新版本sbt需要扫描子目录的.dot文件，这里也加进来
        return (flag && BooleanUtils.isFalse(scanResults.getBuildFlag()) && DetectorFinder.CLASSIFICATION_LIST.contains(buildType))
//                || (flag && SPLIT_LIST.contains(buildType)) || SBT_BUILD.getBuildType().equals(buildType)
                ;
    }


    public static void buildProjectInfo(NewScanInfo newScanInfo, ScannableEnvironment scannableEnvironment) {

        ScanResults handler = null;
        try {
            logger.info("Detection project path {}", scannableEnvironment.getDirectory());
            handler = ScannableFactory.gitExtractHandler.handler(scannableEnvironment, scannableEnvironment.getDirectory().getPath(), simpleExecutableResolver);
        } catch (Throwable e) {
            logger.info("Detection project path error", e);
        }

        if (handler != null && StringUtils.isNotBlank(handler.getScanProjectName())) {

            String[] split = handler.getScanProjectName().split("/");
            if (StringUtils.isBlank(newScanInfo.getCustomProject()) && StringUtils.isNotBlank(split[0])) {
                newScanInfo.setCustomProject(split[0]);
            }

            if (StringUtils.isBlank(newScanInfo.getCustomProduct()) && split.length > 1 && StringUtils.isNotBlank(split[1])) {
                newScanInfo.setCustomProduct(split[1]);
            }

        } else {

            if (StringUtils.isBlank(newScanInfo.getCustomProject())) {
                newScanInfo.setCustomProject(scannableEnvironment.getDirectory().getName());
            }

            if (StringUtils.isBlank(newScanInfo.getCustomProduct())) {
                newScanInfo.setCustomProduct(scannableEnvironment.getDirectory().getName());
            }
        }

        if (handler != null && StringUtils.isNotBlank(handler.getScanProjectVersion()) && StringUtils.isBlank(newScanInfo.getCustomVersion())) {
            newScanInfo.setCustomVersion(handler.getScanProjectVersion());
        } else if (StringUtils.isBlank(newScanInfo.getCustomVersion())) {
            newScanInfo.setCustomVersion("defaultVersion");
        }

        logger.info("customProject: {}, customProduct: {}, customVersion: {}", newScanInfo.getCustomProject(), newScanInfo.getCustomProduct(), newScanInfo.getCustomVersion());
    }
}
