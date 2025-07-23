package com.sectrend.buildscan.finder;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Lists;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.compress.CompressExtractor;
import com.sectrend.buildscan.enums.BuildType;
import com.sectrend.buildscan.exception.DetectorFinderDirectoryListException;
import com.sectrend.buildscan.model.ScannerEvaluationTree;
import com.sectrend.buildscan.model.ExecutionDir;
import com.sectrend.buildscan.model.FilePathCollect;
import com.sectrend.buildscan.model.FilterCondition;
import com.sectrend.buildscan.utils.FileFilterUtils;
import com.sectrend.buildscan.utils.GlobalUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 查找检测器
 */
public class DetectorFinder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FileFinder fileFinder;

    private final String buildTreeType;

    private static final String KEY_LINKER = "___";

    private final Map<String,String> excludeFilePaths = new HashMap<>();

    // 包管理器 项目模块归类
    public static final List<String> CLASSIFICATION_LIST = Arrays.asList("mvn", "gradle", "goMod","xcode");

    private final List<String> packageManagerTypes;

    public List<File> referenceFileList = new ArrayList<>();

    private final List<FilterCondition> filterConditions;

    public DetectorFinder(FileFinder fileFinder, String buildTreeType, List<FilterCondition> filterConditions, List<String> packageManagerTypes) {
        this.fileFinder = fileFinder;
        this.buildTreeType = buildTreeType;
        this.filterConditions = filterConditions;
        this.packageManagerTypes = packageManagerTypes;
    }


    /**
     * 查找项目对应检测器 并且将依赖文件归类
     * @param taskDir
     * @param filePathCollect
     * @return
     * @throws DetectorFinderDirectoryListException
     */
    public HashMap<String, List<ScannableEnvironment>> findDetectorAndProjects(File taskDir, FilePathCollect filePathCollect) throws DetectorFinderDirectoryListException {

        HashMap<String, List<ScannableEnvironment>> scannableMap = new HashMap<>();

        List<ExecutionDir> executionPath = new ArrayList<>();
        //判断条件是否存在，并判断条件是否再同目录下
        if (CollectionUtils.isNotEmpty(filterConditions)) {
            for (FilterCondition filterCondition : filterConditions) {
                ExecutionDir executionFile = new ExecutionDir();
                if (StringUtils.isBlank(filterCondition.getPath()) || filterCondition.getPath().equals(File.separator)
                        || "\\".equals(filterCondition.getPath()) || "/".equals(filterCondition.getPath())) {
                    executionFile.setFile(taskDir);
                } else {
                    executionFile.setFile(new File(filterCondition.getPath()));
                }
                executionFile.setBuildType(filterCondition.getBuildType());
                executionFile.setDepth(filterCondition.getDepth());
                executionPath.add(executionFile);
            }


            List<ExecutionDir> list = executionPath.stream().filter(executionDir -> {
                if (!executionDir.getFile().getAbsolutePath().contains(taskDir.getAbsolutePath())) {
                    logger.warn("The specified path was not found in the detection project{}", executionDir.getFile().getPath());
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(list)) {
                for (ExecutionDir executionDir : list) {
                    if (filePathCollect == null) {
                        Optional<ScannerEvaluationTree> detectors = findScanners(executionDir.getFile(), executionDir.getDepth(), executionDir.getBuildType());
                        scannableMap.putAll(integrationPath(detectors));
                    } else {
                        fileFinder.setFilePaths(filePathCollect.getFiles().stream().filter(s -> s.startsWith(executionDir.getFile().getAbsolutePath())).collect(Collectors.toList()));
                        List<ScannerEvaluationTree> detectors = findDetectorsOnce(executionDir.getBuildType(), executionDir.getFile(), filePathCollect);
                        scannableMap.putAll(integrationPath(detectors));
                    }
                }
            }
        } else {
            if (filePathCollect == null) {
                Optional<ScannerEvaluationTree> detectors = findScanners(taskDir, 0, null);
                scannableMap.putAll(integrationPath(detectors));
            } else {
                List<ScannerEvaluationTree> detectors = findDetectorsOnce(null, taskDir, filePathCollect);
                scannableMap.putAll(integrationPath(detectors));

            }
        }

        // 循环递归匹配依赖文件
        return scannableMap;
    }


    private HashMap<String, List<ScannableEnvironment>> integrationPath(List<ScannerEvaluationTree> detectors) {
        HashMap<String, List<ScannableEnvironment>> scannableMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(detectors)) {
            // 将依赖文件归类
            classificationDetectors(detectors, scannableMap);
        } else {
            logger.info("not found dependency file");
        }
        return scannableMap;
    }

    private void classificationDetectors(List<ScannerEvaluationTree> trees, Map<String, List<ScannableEnvironment>> scannableMap) {
        List<ScannerEvaluationTree> sortedTrees = trees.stream().sorted(new Comparator<ScannerEvaluationTree>() {
            @Override
            public int compare(ScannerEvaluationTree o1, ScannerEvaluationTree o2) {
                String absolutePath1 = o1.getDirectory().getAbsolutePath();
                String absolutePath2 = o2.getDirectory().getAbsolutePath();
                String[] split1 = absolutePath1.split("[/\\\\]");
                String[] split2 = absolutePath2.split("[/\\\\]");
                return split1.length - split2.length;
            }
        }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(sortedTrees)) {
            for (ScannerEvaluationTree tree : sortedTrees) {
                String absolutePath = tree.getDirectory().getAbsolutePath();
                for (String buildType : tree.getBuildTypes()) {
                    String key = findRootProjectKey(scannableMap, absolutePath, buildType);
                    ScannableEnvironment environment = new ScannableEnvironment(
                            tree.getDirectory(),
                            buildType,
                            null
                    );
                    List<ScannableEnvironment> list = scannableMap.getOrDefault(key, new ArrayList<>());
                    list.add(environment);
                    scannableMap.put(key, list);

                }
            }
        }
    }


    private HashMap<String, List<ScannableEnvironment>> integrationPath(Optional<ScannerEvaluationTree> detectors) {
        HashMap<String, List<ScannableEnvironment>> scannableMap = new HashMap<>();

        if (detectors.isPresent()) {
            // 将依赖文件归类
            this.classification(detectors.get(), scannableMap);
            //logger.debug(JSON.toJSONString(scannableMap));
        } else {
            logger.info("not found dependency file");
        }
        return scannableMap;
    }


    public Optional<ScannerEvaluationTree> findScanners(File initialDirectory, Integer maxDepth, String buildType) throws DetectorFinderDirectoryListException {

        return findScanners(initialDirectory, 0, maxDepth, buildType);
    }


    private List<ScannerEvaluationTree> findDetectorsOnce(String buildType, File taskDir, FilePathCollect filePathCollect) {
        if (filePathCollect.isEmpty()) {
            return new ArrayList<>();
        }
        List<ScannerEvaluationTree> trees = new ArrayList<>();
        Map<String, Set<String>> buildTypeMaps = new HashMap<>();
        // 匹配依赖文件
        if (StringUtils.isNotBlank(buildType)) {
            BuildType typeByValue = BuildType.getTypeByValue(buildType);
            if (typeByValue != null) {
                findFileByFilePath(typeByValue, taskDir, buildTypeMaps);
            }
        } else {
            findBuildTypesByFilePath(taskDir,filePathCollect, buildTypeMaps);
        }
        for (Map.Entry<String, Set<String>> entry : buildTypeMaps.entrySet()) {
            trees.add(new ScannerEvaluationTree(new File(entry.getKey()), 0, new ArrayList<>(entry.getValue()), null));
        }
        return trees;
    }



    /**
     * 循环递归查找文件 并构建成树结构
     *
     * @param directory
     * @param depth     项目深度
     * @return
     * @throws DetectorFinderDirectoryListException
     */
    private Optional<ScannerEvaluationTree> findScanners(File directory, int depth, Integer maxDepth, String buildType) throws DetectorFinderDirectoryListException {
        // 后续可以自定义深度
        if (maxDepth != null && maxDepth != 0 && depth > maxDepth) {
            this.logger.trace("跳过超过最大深度的目录: " + directory.toString());
            return Optional.empty();
        }

        if (null == directory || Files.isSymbolicLink(directory.toPath()) || !directory.isDirectory()) {
            return Optional.empty();
        }

        this.logger.debug("Traverse directory: " + directory.getPath());
        // 匹配依赖文件
        List<String> buildTypeList = new ArrayList<>();
        if (StringUtils.isNotBlank(buildType)) {

            BuildType typeByValue = BuildType.getTypeByValue(buildType);
            if (typeByValue != null) {
                if (findFile(directory, typeByValue)) {
                    buildTypeList.add(typeByValue.getBuildType());
                }
            }
        } else {
            List<String> buildTypes = findBuildTypes(directory);
            if (CollectionUtils.isNotEmpty(buildTypes)) {
                buildTypeList.addAll(buildTypes);
            }
        }

        Set<ScannerEvaluationTree> children = new HashSet<>();
        // 查找目录下文件夹
        List<File> subDirectories = findFilteredSubDirectories(directory, buildTypeList);
        if (Objects.nonNull(subDirectories)) {
            for (File subDirectory : subDirectories) {
                Optional<ScannerEvaluationTree> childEvaluationSet = findScanners(subDirectory, depth + 1, maxDepth, buildType);
                childEvaluationSet.ifPresent(children::add);
            }
        }
        // 依赖文件为空 并且 子级元素为0 则直接返回空对象
        if (CollectionUtils.isEmpty(buildTypeList) && children.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ScannerEvaluationTree(directory, depth, buildTypeList, children));
    }


    private List<String> findBuildTypes(File directory) {
        File[] allFiles = directory.listFiles();
        if (allFiles == null) {
            return null;
        }

        List<String> buildTypeList = new ArrayList<>();
        for (File file : allFiles) {
            if (file.getName().endsWith(".gradle") || "gradle.properties".equals(file.getName())) {
                referenceFileList.add(file);
            }
            List<String> buildTypes = Arrays.stream(BuildType.values())
                    .filter(buildTypeInfo -> {
                        if (buildTreeType.equals(BuildType.MVN_TEXT_BUILD.getBuildType())) {
                            if (buildTypeInfo.getBuildType().equals(BuildType.MVN_BUILD.getBuildType())) {
                                return false;
                            }
                        }
//                        if (buildTreeType.equals(BuildType.GRADLE_TEXT_BUILD.getBuildType())) {
//                            if (buildTypeInfo.getBuildType().equals(BuildType.GRADLE_BUILD.getBuildType())) {
//                                return false;
//                            }
//                        }
                        return CollectionUtils.isNotEmpty(packageManagerTypes) && packageManagerTypes.contains(buildTypeInfo.getBuildType());
                    })
                    .filter(buildTypeInfo -> {
                        String[] buildFileNames = buildTypeInfo.getBuildFileNames().split("\\|");
                        if (buildFileNames.length > 0) {
                            int count = 0;
                            for (int i = 0; i < buildFileNames.length; i++) {
                                if (buildFileNames[i].startsWith("*")) {
                                    String buildFileName = buildFileNames[i].substring(1);
                                    if (file.getName().endsWith(buildFileName)) {
                                        return true;
                                    }
                                } else {
                                    if (file.getName().equals(buildFileNames[i])) {
                                        if (BooleanUtils.isFalse(buildTypeInfo.isMatchingAll())) {
                                            return true;
                                        }
                                        count++;
                                    }
                                }
                            }
                            if (buildTypeInfo.isMatchingAll() && buildFileNames.length == count) {
                                return true;
                            }
                        }
                        return false;
                    }).map(e -> e.getBuildType())
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(buildTypes)) {
                buildTypeList.addAll(buildTypes);
            }
        }
        return buildTypeList;
    }

    //fix bug:package. json and yarn. lock coexist, but the build type is npm but not yarn
    private void findBuildTypesByFilePath(File taskDir ,FilePathCollect filePathCollect, Map<String, Set<String>> buildTypeMap) {
        if (filePathCollect.isEmpty()) {
            return;
        }
        boolean haveTraveledFiles = false;

        Map<String, Map<BuildType, Set<String>>> builds = new HashMap<>();
        for(BuildType buildTypeInfo : BuildType.values()) {
            int flag = 0;
            int xcodeFlag = 0;
            if (buildTreeType.equals(BuildType.MVN_TEXT_BUILD.getBuildType()) && buildTypeInfo.getBuildType().equals(BuildType.MVN_BUILD.getBuildType())) {
                continue;
            }

//            if (buildTreeType.equals(BuildType.GRADLE_TEXT_BUILD.getBuildType()) && buildTypeInfo.getBuildType().equals(BuildType.GRADLE_BUILD.getBuildType())) {
//                continue;
//            }
            if (CollectionUtils.isNotEmpty(packageManagerTypes) && !packageManagerTypes.contains(buildTypeInfo.getBuildType())) {
                continue;
            }

            String[] buildFileNames = buildTypeInfo.getBuildFileNames().split("\\|");
            Set<String> buildFilesSet = new HashSet<>(Arrays.asList(buildFileNames));
            if (buildFilesSet.size() > 0) {
                List<String> filePathCollectFiles = filePathCollectFilter(taskDir,filePathCollect);
                for (String path : filePathCollectFiles) {
                    if (haveTraveledFiles == false) {
                        File sourceCodeFile = new File(path);
                        if (!sourceCodeFile.exists()) {
                            continue;
                        }
                        if (path.endsWith(".gradle") || "gradle.properties".equals(sourceCodeFile.getName())) {
                            referenceFileList.add(sourceCodeFile);
                        }
                    }
                    String[] split = path.split("[/\\\\]");
                    String name = split[split.length -1];

                    Boolean rubygemsFlag = false;
                    for(String buildFile : buildFilesSet){
                        if(!"".equals(buildFile) && name.contains(buildFile)){
                            rubygemsFlag = true;
                            break;
                        }
                    }


                    if (buildFilesSet.contains(name) || rubygemsFlag) {
                        String dirPath = getDirPath(path);
                        Map<BuildType, Set<String>> buildTypes = builds.getOrDefault(dirPath, new HashMap<BuildType, Set<String>>());
                        Set<String> buildFileHit = buildTypes.getOrDefault(buildTypeInfo, new HashSet<String>());
                        buildFileHit.add(name);
                        buildTypes.put(buildTypeInfo, buildFileHit);
                        builds.put(dirPath, buildTypes);
                    }

//                    if (buildTypeInfo.getBuildType().equals(BuildType.BITBAKE_BUILD.getBuildType())){
//                        if (path.endsWith(buildTypeInfo.getBuildFileNames())) {
//                            flag ++ ;
//                        }
//                        if (flag == 1){
//                            HashSet<String> buildTypeSet = new HashSet<>();
//                            buildTypeSet.add(BuildType.BITBAKE_BUILD.getBuildType());
//                            buildTypeMap.put(taskDir.getPath() , buildTypeSet);
//                        }
//                    }
//
//                    // Xcode 特殊处理: 特征文件夹识别
//                    if (buildTypeInfo.getBuildType().equals(BuildType.XCODE_BUILD.getBuildType())){
//                        boolean anyMatch = buildFilesSet.stream().anyMatch(file -> path.contains(file));
//                        if (anyMatch) {
//                            xcodeFlag ++ ;
//                        }
//                        if (xcodeFlag == 1){
//                            HashSet<String> buildTypeSet = new HashSet<>();
//                            buildTypeSet.add(BuildType.XCODE_BUILD.getBuildType());
//                            buildTypeMap.put(taskDir.getPath(), buildTypeSet);
//                        }
//                    }
                }
                //文件遍历过至少一次，做个已遍历标记
                haveTraveledFiles = true;
            }
        }

        // npm和yarn都有共同构建文件名称package.json,需要在同一个路径下正确识别两者其一或者两者都有
        for (Map.Entry<String, Map<BuildType, Set<String>>> entry : builds.entrySet()) {
            Map<BuildType, Set<String>> buildTypes = entry.getValue();
            String buildPath = entry.getKey(); // 当前路径下的构建类型判断
//            Set<String> yarnFiles = buildTypes.get(BuildType.YARN_BUILD);
//            Set<String> npmFiles = buildTypes.get(BuildType.NPM_BUILD);
//
//            //有yarn类型存在，需要判断yarn是否全部匹配，并且判断是否和npm有冲突
//            if ( yarnFiles != null) {
//                String[] yarnFileNames = BuildType.YARN_BUILD.getBuildFileNames().split("\\|");
//                //构建文件格式匹配不全，删除当前buildPath下的yuar构建类型
//                if (yarnFiles.size() != yarnFileNames.length) {
//                    buildTypes.remove(BuildType.YARN_BUILD);
//                } else {
//                    if (npmFiles != null) {
//                        //npm只匹配package.json一种类型，并且已经存在yarn类型的情况下，不能当作npm类型
//                        String[] npmFileNames = BuildType.NPM_BUILD.getBuildFileNames().split("\\|");
//                        Set<String> npmFileNamesSet = new HashSet<>(Arrays.asList(npmFileNames));
//                        if (npmFiles.size() == 1 && npmFileNamesSet.contains("package.json")) {
//                            buildTypes.remove(BuildType.NPM_BUILD);
//                        }
//                    }
//                }
//            }

            Set<String> buildTypeSet = buildTypeMap.getOrDefault(buildPath, new HashSet<>());
            Iterator<Map.Entry<BuildType, Set<String>>> it = buildTypes.entrySet().iterator();
            // 判断构建类型是否真正匹配
            while (it.hasNext()) {
                Map.Entry<BuildType, Set<String>> item = it.next();
                BuildType bt = item.getKey();
                // 判断构建类型完全匹配
                if (bt.isMatchingAll()) {
                    String[] files = bt.getBuildFileNames().split("\\|");
                    // 构建类型没有完全匹配，则要删除该构建类型
                    if (item.getValue().size() != files.length) {
                        it.remove();
                        continue;
                    }
                }
                buildTypeSet.add(bt.getBuildType());
            }

            if (FileFilterUtils.filterDirectory(buildPath, buildTypeSet)) {
                buildTypeMap.put(buildPath, buildTypeSet);
            }
        }
    }

    private String getDirPath(String path){
        int index = path.lastIndexOf("/");
       if(index < 0){
           index = path.lastIndexOf("\\");
       }
       if(index < 0){
           return path;
       }
       return path.substring(0, index);
    }

    /**
     * 查找目录下文件夹
     *
     * @param directory
     * @return
     */
    private List<File> findFilteredSubDirectories(File directory, List<String> buildTypeList) {
        try {
            Stream<Path> pathStream = Files.list(directory.toPath());
            if (pathStream != null) {
                List<File> fileList = pathStream.<File>map(Path::toFile).collect(Collectors.toList());

                List<File> compressFileList = Lists.newArrayList();
                for (File file : fileList) {
                    String compressPath = CompressExtractor.rootCompressPathMap.get(file.getAbsolutePath());
                    if (StringUtils.isNotBlank(compressPath)) {
                        File compressFile = new File(compressPath);
                        if (compressFile.exists()) {
                            compressFileList.add(compressFile);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(compressFileList)) {
                    fileList.addAll(compressFileList);
                }
                return fileList.stream()
                        .filter(File::isDirectory)
                        .filter(it -> FileFilterUtils.filterDirectory(it, buildTypeList))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            //throw new DetectorFinderDirectoryListException(String.format("无法获得的子目录 %s. %s", new Object[] { directory.getAbsolutePath(), e.getMessage() }), e);
            logger.error(String.format("Unable to obtain sub items %s. %s", directory.getAbsolutePath(), e));
        }
        return null;
    }

    /**
     * 查找依赖文件
     *
     * @param directory
     * @param buildType
     * @return
     */
    private boolean findFile(File directory, BuildType buildType) {

        if (buildTreeType.equals(BuildType.MVN_TEXT_BUILD.getBuildType())) {
            if (buildType.getBuildType().equals(BuildType.MVN_BUILD.getBuildType())) {
                return false;
            }
        }

//        if (buildTreeType.equals(BuildType.GRADLE_TEXT_BUILD.getBuildType())) {
//            if (buildType.getBuildType().equals(BuildType.GRADLE_BUILD.getBuildType())) {
//                return false;
//            }
//        }
        if (CollectionUtils.isNotEmpty(packageManagerTypes) && !packageManagerTypes.contains(buildType.getBuildType())) {
            return false;
        }

        String[] buildFileNames = buildType.getBuildFileNames().split("\\|");
        if (buildFileNames.length > 0) {
            int count = 0;
            for (int i = 0; i < buildFileNames.length; i++) {
                if (this.fileFinder.findFile(directory, buildFileNames[i]) != null) {
                    if (BooleanUtils.isFalse(buildType.isMatchingAll())) {
                        return true;
                    }
                    count++;
                }
            }
            if (buildType.isMatchingAll() && buildFileNames.length == count) {
                return true;
            }
        }
        return false;
    }

    private void findFileByFilePath(BuildType buildType, File taskDir, Map<String, Set<String>> buildTypeMap) {

        if (buildTreeType.equals(BuildType.MVN_TEXT_BUILD.getBuildType()) && buildType.getBuildType().equals(BuildType.MVN_BUILD.getBuildType())) {
            return;
        }

//        if (buildTreeType.equals(BuildType.GRADLE_TEXT_BUILD.getBuildType()) && buildType.getBuildType().equals(BuildType.GRADLE_BUILD.getBuildType())) {
//            return;
//        }
        if (CollectionUtils.isNotEmpty(packageManagerTypes) && !packageManagerTypes.contains(buildType.getBuildType())) {
            return;
        }

        String[] buildFileNames = buildType.getBuildFileNames().split("\\|");
        Map<String, Integer> fileMatchCount = new HashMap<>();
        if (buildFileNames.length > 0) {
            for (int i = 0; i < buildFileNames.length; i++) {
                List<File> files = fileFinder.findFiles(taskDir, buildFileNames[i]);
                for (File file : files) {
                    Integer matchCount = fileMatchCount.getOrDefault(file.getAbsolutePath(), 0);
                    matchCount += 1;
                    fileMatchCount.put(file.getAbsolutePath(), matchCount);
                }
            }
            for (Map.Entry<String, Integer> entry : fileMatchCount.entrySet()) {
                String dir = getDirPath(entry.getKey());
                Set<String> types = buildTypeMap.getOrDefault(dir, new HashSet<>());
                if(buildType.isMatchingAll()) {
                    if (entry.getValue() == buildFileNames.length) {
                        types.add(buildType.getBuildType());
                        buildTypeMap.put(dir, types);
                    }
                } else {
                    types.add(buildType.getBuildType());
                    buildTypeMap.put(dir, types);
                }
            }
        }
    }


    /**
     * 将依赖文件归类
     *
     * @param tree
     * @param detectableMap
     */
    private void classification(ScannerEvaluationTree tree, Map<String, List<ScannableEnvironment>> detectableMap) {
        if (CollectionUtils.isNotEmpty(tree.getBuildTypes())) {

            String absolutePath = tree.getDirectory().getAbsolutePath();
            // 判断是否是根项目下面的子项目 如果是则和根项目放同一个容器
            // 以项目文件夹路径 + 构建类型为key
            for (String buildType : tree.getBuildTypes()) {
                String key = findRootProjectKey(detectableMap, absolutePath, buildType);
                ScannableEnvironment scannableObj = new ScannableEnvironment(
                        tree.getDirectory(),
                        buildType,
                        null
                );
                List<ScannableEnvironment> envList = detectableMap.getOrDefault(key, new ArrayList<>());
                envList.add(scannableObj);
                detectableMap.put(key, envList);
            }
        }
        // 判断是否有子级  如果有则递归查找
        if (CollectionUtils.isNotEmpty(tree.getChildren())) {
            for (ScannerEvaluationTree tree2 : tree.getChildren()) {
                classification(tree2, detectableMap);
            }
        }
    }

    /**
     * 查找根项目key，如果没查找则返回当前项目为根项目
     * key = 目录 + 构建类型
     *
     * @param detectableMap
     * @param absolutePath
     * @param buildType
     * @return
     */
    private String findRootProjectKey(Map<String, List<ScannableEnvironment>> detectableMap, String absolutePath, String buildType) {

        String key = absolutePath + KEY_LINKER + buildType;

        // 判断当前包管理器是否在归类名单中， 如果不在名单中则直接返回新key
        if (!CLASSIFICATION_LIST.contains(buildType)) {
            return key;
        }
        for (Map.Entry<String, List<ScannableEnvironment>> entry : detectableMap.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()))
                continue;
            String[] keys = entry.getKey().split(KEY_LINKER);
            if (keys.length >= 2) {
                String rootProjectPath = GlobalUtils.replaceLast(entry.getKey(), KEY_LINKER + keys[keys.length - 1], "");

                // 判断 是否有解压文件  如果当前是解压文件则不进行归类处理
                if (CollectionUtil.isNotEmpty(CompressExtractor.rootCompressPathMap) && this.isUnCompressFile(absolutePath)) {
                    return key;
                }
                if (absolutePath.startsWith(rootProjectPath + File.separatorChar) && buildType.equals(keys[keys.length - 1])) {
                    return entry.getKey();
                }
            }
        }
        return key;
    }

    /**
     * 判断 是否是解压文件
     *
     * @param projectPath
     * @return
     */
    private boolean isUnCompressFile(String projectPath) {
        if (StringUtils.isBlank(projectPath)) {
            return false;
        }
        for (Map.Entry<String, String> entry : CompressExtractor.rootCompressPathMap.entrySet()) {
            // 判断当前是否是解压文件 或者 当前文件是在解压文件下面
            if (projectPath.equals(entry.getValue()) || projectPath.startsWith(entry.getValue() + File.separatorChar)) {
                return true;
            }
        }
        return false;
    }



    public List<String> filePathCollectFilter(File taskDir ,FilePathCollect filePathCollect){
        if (filePathCollect==null || taskDir ==null) {// 判断是否存在目录
            return null;
        }
        List<String> fileList = filePathCollect.getFiles();// 读取目录下的所有目录文件信息
        if(CollectionUtils.isEmpty(fileList)){
            return fileList;
        }
        List<File> list = Arrays.asList(taskDir.listFiles());
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        Map<String,Boolean> map = new HashMap<>();
        // 删除node_modules文件
        map.put("flag",false);
        list.stream().filter(f->f!=null).forEach(f->{
            String name = f.getName();
            if(name.equals("pnpm-lock.yaml") || name.equals("package-lock.json")
                            || name.equals("package.json") || name.equals("yarn.lock")
            ){
                map.put("flag",true);
            }
        });

        if(map.get("flag")){
            List<String> cancelList = new ArrayList<>();
            fileList.stream().forEach(f->{
                if(f.contains("/node_modules/")){
                    cancelList.add(f);

                }
                if(f.contains("\\node_modules\\")){
                    cancelList.add(f);

                }
            });
            fileList.removeAll(cancelList);
        }

        // 如果npm和yarn同时存在保留package.json 否则移除，为后面代码做铺垫
        map.put("yarnFlag",false);
        list.stream().filter(f-> f!= null && (
                f.getName().equals("yarn.lock")
        ) ).forEach(f->{
            map.put("yarnFlag",true);
        });

        map.put("npmFlag",false);
        list.stream().filter(f->f!=null).forEach(f->{
            String name = f.getName();
            if(name.equals("pnpm-lock.yaml") || name.equals("package-lock.json")
            ){
                map.put("npmFlag",true);
            }
        });

        if(map.get("npmFlag") && (!map.get("yarnFlag"))){
            List<String> cancelList = new ArrayList<>();
            fileList.stream().forEach(f->{
                if(f.contains("/package.json")){
                    cancelList.add(f);

                }
                if(f.contains("\\package.json")){
                    cancelList.add(f);

                }
            });
            fileList.removeAll(cancelList);
        }

        // bower 文件进行排除
        map.put("bowerFlag",false);
        list.stream().filter(f-> f!= null && f.getName().equals("bower.json") ).forEach(f->{
            map.put("bowerFlag",true);
        });
        if(map.get("bowerFlag")){

            String bowerExcludeFilePath = StringUtils.isEmpty(this.excludeFilePaths.get("bower"))?"components":this.excludeFilePaths.get("bower");
            List<String> cancelList = new ArrayList<>();
            fileList.stream().forEach(f->{
                if(f.contains("/"+bowerExcludeFilePath+"/")){
                    cancelList.add(f);

                }
                if(f.contains("\\"+bowerExcludeFilePath+"\\")){
                    cancelList.add(f);

                }
            });
            fileList.removeAll(cancelList);
        }

        // lerna 文件进行排除
        map.put("lernaFlag",false);
        list.stream().filter(f-> f!= null && f.getName().equals("lerna.json") ).forEach(f->{
            map.put("lernaFlag",true);
        });
        if(map.get("lernaFlag")){

            List<String> cancelList = new ArrayList<>();
            fileList.stream().forEach(f->{
                if(f.contains("/node_modules/")){
                    cancelList.add(f);

                }
                if(f.contains("\\node_modules\\")){
                    cancelList.add(f);

                }
                if(f.contains("/packages/")){
                    cancelList.add(f);

                }
                if(f.contains("\\packages\\")){
                    cancelList.add(f);

                }
                if(f.contains("/package-lock.json")){
                    cancelList.add(f);

                }
                if(f.contains("/yarn.lock")){
                    cancelList.add(f);

                }
                if(f.contains("\\package-lock.json")){
                    cancelList.add(f);

                }
                if(f.contains("\\yarn.lock")){
                    cancelList.add(f);

                }
                if(f.contains("/package.json")){
                    cancelList.add(f);

                }
                if(f.contains("\\package.json")){
                    cancelList.add(f);

                }
            });
            fileList.removeAll(cancelList);
        }

        return fileList;
    }

    public void addExcludeFilePath(String buildType,String excludePath){
        this.excludeFilePaths.put(buildType,excludePath);
    }

}
