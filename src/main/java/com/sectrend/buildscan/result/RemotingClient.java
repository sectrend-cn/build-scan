package com.sectrend.buildscan.result;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sectrend.buildscan.buildTools.scanner.Scanner;
import com.sectrend.buildscan.buildTools.scanner.ScannerConf;
import com.sectrend.buildscan.buildTools.scanner.model.BinaryFilterParam;
import com.sectrend.buildscan.compress.CompressExtractor;
import com.sectrend.buildscan.enums.*;
import com.sectrend.buildscan.factory.SerialLicenseFactory;
import com.sectrend.buildscan.model.*;
import com.sectrend.buildscan.utils.FileUploader;
import com.sectrend.buildscan.utils.OkHttpUtils;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import com.sectrend.buildscan.workflow.maintenance.MaintenanceSystem;
import lombok.Data;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Data
public class RemotingClient implements InitializingBean {

    private final static Logger logger = LoggerFactory.getLogger(RemotingClient.class);

    /**
     * 登录之后返回的cookie的session
     */
    private static String session;

    /**
     * 接口访问前缀
     */
    private final static String defaultServerUrl = "https://cleansource-ce.sectrend.com.cn";

    /**
     * 接口访问前缀
     */
    private final static String serverPrefix = "/cleansourcesca-community";

    /**
     * 创建任务的接口路径
     */
    private final static String uploadUrl = "/task/create";

    /**
     * 登录的接口路径
     */
    private final static String loginUrl = "/api/v2/user/login";

    /**
     * 邮箱登录的接口路径
     */
    private final static String emailLoginUrl = "/email_user/login";

    private final static String ldapLoginUrl = "/api/v2/user/ldap/login";

    /**
     * 验证license是否存在校验接口路径
     */
    private final static String verifyLicenseUrl = "/api/v2/license_knowledge/verify";


    /**
     * 验证Email是否绑定发送邮箱
     */
    private final static String verifyEmailUrl = "/api/v2/config/email/get";

    /**
     * 验证token是否可用的api
     */
    private final static String verifyToken = "/api/v2/token/list";


    /**
     * 创建分片接口
     */
    private final static String shardingUrl = "/file/multipart_upload/create";

    /**
     * 合并接口
     */
    private final static String mergeUrl = "/file/multipart_upload/complete";

    /**
     * 文件分片上传时采用的默认分片大小
     */
    public static final long DEFAULT_FILE_CHUNK_SIZE = 10 * 1024 * 1024;

    /**
     * 指纹文件的对象
     */
    private static File wfpFile;

    /**
     * npm自动编译按钮
     */
    private static Integer npmAutopiler;


    /**
     * npm 需要排除的字段
     */
    private static String npmExclude;

    private static String tokenKeyName = "token";

    private static String passwordKeyName = "password";

    private static String securityReplaceValue = "******";

    private static Integer MAX_FILE_HEADER = 16384;

    private static Integer MAX_HEADER_LINES = 30;

    private static String SPDX_LICENCE_IDENTIFIER = "SPDX-License-Identifier:";

    private static Integer MAX_ARG_LEN = 1024;

    private static Integer LICENSE_SERIAL_LEN = 4096;



/*    @Value("${scan.uploadInfo.uploadUrl}")
    private String getUploadUrl;

    @Value("${scan.uploadInfo.loginUrl}")
    private String getLoginUrl;

    @Value("${scan.uploadInfo.dockerUrl}")
    private String getDockerUrl;

    @Value("${scan.uploadInfo.verifyTaskUrl}")
    private String getVerifyTaskUrl;*/


    public static boolean isPath(String str) {
        String pattern = "^(?:[a-zA-Z]\\:|\\\\\\\\[\\w\\.]+\\\\[\\w.$]+)\\\\(?:[\\w]+\\\\)*\\w([\\w.])+$";
        return str.matches(pattern);
    }

    /**
     * 转换args中的构建器业务参数到NewScanInfo
     *
     * @author: Jimmy
     * @date: 2025-01-20 14:31:58
     */
    private static void resolveArgs(NewScanInfo newScanInfo, ApplicationArguments args) {

        Set<String> optionNames = args.getOptionNames();
        Set<String> strings = DetectBusinessParams.allParameterKey();

        if (CollUtil.isEmpty(CollUtil.removeNull(strings))) {
            return;
        }

        HashSet<String> intersection = new HashSet<>();
        intersection.addAll(optionNames);
        intersection.retainAll(strings);

        intersection.forEach(v -> {
            try {
                if (CollectionUtils.isNotEmpty(args.getOptionValues(v))) {
                    String value = args.getOptionValues(v).get(0);
                    Field declaredField = NewScanInfo.class.getDeclaredField(StrUtil.toCamelCase(v.replace("scan.", ""), '.'));
                    declaredField.setAccessible(true);
                    declaredField.set(newScanInfo, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 初始化启动参
     *
     * @param args
     * @return
     */
    public static NewScanInfo initNewScanInfo(ApplicationArguments args) {
        NewScanInfo newScanInfo = new NewScanInfo();
        DefaultParamInfo defaultParamInfo = new DefaultParamInfo();

        newScanInfo.setFormatEnable(false);


        if (CollectionUtils.isNotEmpty(args.getOptionValues("outputPath"))) {
            newScanInfo.setOutputPath(args.getOptionValues("outputPath").get(0));
        }


        npmAutopiler = 0;
        newScanInfo.setNpmAutopiler(0);

        newScanInfo.setCustomVersion("");

        newScanInfo.setQueuePriority("4");

        newScanInfo.setLoginType(1);

        newScanInfo.setCustomProject("");

        newScanInfo.setCustomProduct("");

        newScanInfo.setProjectExplain("");

        newScanInfo.setProductExplain("");

        newScanInfo.setVersionExplain("");

        newScanInfo.setVersionOwner("");

        newScanInfo.setProjectOwner("");

        newScanInfo.setProductOwner("");


        if (CollectionUtils.isNotEmpty(args.getOptionValues("taskDir"))) {
            newScanInfo.setTaskDir(args.getOptionValues("taskDir").get(0) == null ? "" : args.getOptionValues("taskDir").get(0));
        } else {
            newScanInfo.setTaskDir("");
        }

        newScanInfo.setBuildDepend(true);

        newScanInfo.setBuildType("");

        newScanInfo.setDistribution("");

        newScanInfo.setStage("");


        if (CollectionUtils.isNotEmpty(args.getOptionValues("serverUrl"))) {
            String serverUrl = args.getOptionValues("serverUrl").get(0);
            if (StringUtils.isNotBlank(serverUrl)) {
                if (serverUrl.endsWith("/")) {
                    serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                }

                newScanInfo.setServerUrl(serverUrl + serverPrefix);
            } else {
                newScanInfo.setServerUrl(defaultServerUrl + serverPrefix);
            }
        } else {
            newScanInfo.setServerUrl(defaultServerUrl + serverPrefix);
        }

        newScanInfo.setToken("");

        if (CollectionUtils.isNotEmpty(args.getOptionValues("password"))) {
            newScanInfo.setPassword(args.getOptionValues("password").get(0) == null ? "" : args.getOptionValues("password").get(0));
        } else {
            newScanInfo.setPassword("");
        }

        if (CollectionUtils.isNotEmpty(args.getOptionValues("username"))) {
            newScanInfo.setUsername(args.getOptionValues("username").get(0) == null ? "" : args.getOptionValues("username").get(0));
        } else {
            newScanInfo.setUsername("");
        }

        newScanInfo.setBuildTreeFile("");

        newScanInfo.setThreadNum("30");

        newScanInfo.setCallerType(SourceType.SOURCE_CLI_TYPE.getValue());

        if (CollectionUtils.isNotEmpty(args.getOptionValues("logLevel"))) {
            newScanInfo.setLogLevel(args.getOptionValues("logLevel").get(0) == null ? null : args.getOptionValues("logLevel").get(0));
        }

        initDefaultParam(defaultParamInfo, args);
        if (StringUtils.isNotBlank(newScanInfo.getBuildTreeFile())) {
            defaultParamInfo.setIsBuild(2);
        } else if (newScanInfo.getBuildDepend()) {
            defaultParamInfo.setIsBuild(1);
        } else {
            defaultParamInfo.setIsBuild(0);
        }
        if (BuildScanTypeEnum.NO_EXECUTE.equals(defaultParamInfo.getBuildScanType())) {
            newScanInfo.setBuildDepend(false);
        }

        // 社区版参数限制
        // 仅源码扫描
        newScanInfo.setScanType(ScanType.SOURCE_SCAN_TYPE.getValue());
        // 仅任务扫描
        newScanInfo.setTaskType(TaskType.TASK_SCAN_TYPE.getValue());

        //调用初始化默认参数方法
        newScanInfo.setDefaultParamInfo(defaultParamInfo);
        return newScanInfo;

    }

    private static void initDefaultParam(DefaultParamInfo defaultParamInfo, ApplicationArguments args) {

        defaultParamInfo.setSnippetFlag(1);

        defaultParamInfo.setMatched("50");

        defaultParamInfo.setMatchingAutoConfirm(0);

        defaultParamInfo.setLicenseFlag(1);

        defaultParamInfo.setAttributionFlag(0);

        defaultParamInfo.setSensitiveInformationFlag(0);

        defaultParamInfo.setCopyrightFlag(1);

        defaultParamInfo.setCryptographyFlag(1);

        defaultParamInfo.setVulnerabilityFlag(1);

        defaultParamInfo.setIsSaveSourceFile(0);

        defaultParamInfo.setDepth(0);

        defaultParamInfo.setThreadNum(30);

        defaultParamInfo.setIsOpenCandidatePool(0);

        defaultParamInfo.setScanWay(1);

        defaultParamInfo.setIsIncrement(1);

        defaultParamInfo.setIsBuildWithGradlew(1);

        defaultParamInfo.setIsUnzip(0);
    }

    private static void printErrorLog(String param, String logDate) {
        logger.error("Parameter abnormality：{}:{}", param, logDate);
        System.exit(7);
    }

    /**
     * 判断值是否为数字类型的
     *
     * @param str
     */
    public static void isNotNumber(String str, String param) {
        char[] ch = str.toCharArray();
        //将字符用数组封装
        for (int i = 0; i < ch.length; i++) {
            if (!(ch[i] >= '0' && ch[i] <= '9')) {
                logger.error("Parameter abnormality：{}:{}", param, str);
                System.exit(7);
            }
        }
    }

    /**
     * 验证开关按钮的方法 （值 0关，1开）
     *
     * @param str 传入值
     */
    private static Boolean isConfineToSwitch(String str, String param) {
        isNotNumber(str, param);
        if ("0".equals(str) || "1".equals(str)) {
            return true;
        }
        return false;
    }

    /**
     * @param str    传入值
     * @param minNum 最小值
     * @param maxNum 最大值
     */
    private static Boolean isConfineToNum(String str, String param, int minNum, int maxNum) {
        isNotNumber(str, param);
        int i = 0;
        try {
            i = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            logger.error("Parameter abnormality{}：{}", param, str);
        }
        if (minNum <= i && i <= maxNum) {
            return true;
        }
        return false;
    }

    /**
     * 调用登录接口异常处理
     */
    public static Boolean login(NewScanInfo newScanInfo) {

        try {
            String password = RsaEncryptedUtils.passwordEncrypt(newScanInfo.getPassword());
            newScanInfo.setPassword(password);

            Response response = null;
            if (newScanInfo.getLoginType().equals(2)) {

                response = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + ldapLoginUrl)
                        .addRequestParam("user_name", newScanInfo.getUsername())
                        .addRequestParam("password", newScanInfo.getPassword())
                        .post(false).sync();
            } else {
                response = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + emailLoginUrl)
                        .addRequestParam("email", newScanInfo.getUsername())
                        .addRequestParam("password", newScanInfo.getPassword())
                        .post(true).sync();
            }

            if (response == null || response.code() == 405) {
                logger.error("login error： Service link abnormality, please check if the IP address is abnormal");
                return false;
            }

            String str = response.body().string();
            JSONObject jsonObject = JSONObject.parseObject(str);
            String code = jsonObject.getString("code");
            if ("100000".equals(code)) {
                Headers headers = response.headers();
                List<String> cookies = headers.values("Set-Cookie");
                if (CollectionUtils.isNotEmpty(cookies)) {
                    String s = StringUtils.join(cookies, "; ");
                    session = s;
                }
                return true;
            } else {
                logger.error("login error： " + jsonObject.getString("message"));
                return false;
            }
        } catch (Exception e) {
            logger.error("login error： " + e);
            return false;
        }
    }

    /**
     * 获取 session
     *
     * @return
     */
    public static String getSession() {
        if (StringUtils.isBlank(session))
            return null;
        return session;
    }

    /**
     * 获取指纹文件对象
     *
     * @return
     */
    public static File getWfpFile() {
        return wfpFile;
    }


    /**
     * 获取指纹文件对象
     *
     * @return
     */
    public static Integer getNpmAutopiler() {
        return npmAutopiler;
    }


    public static String getNpmExclude() {
        return npmExclude;
    }


    public static Boolean generateWfpFile(NewScanInfo newScanInfo, String mappingPath, FilePathCollect filePathCollect, BinaryFilterParam binaryFilterParam) {
        Boolean aBoolean = executeGenerateWfpFile(newScanInfo, filePathCollect, binaryFilterParam);
        if (aBoolean && newScanInfo.getDefaultParamInfo().getIsUnzip() == 1) {
            writeMappingPath(mappingPath);
        }
        return aBoolean;
    }

    private static void writeMappingPath(String mappingPath) {
        try {
            if (StringUtils.isNotBlank(mappingPath)) {
                //获取解压映射数据写入文件
                Map<String, String> map = CompressExtractor.softWarePackagePathMap;
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                String scanResult = gson.toJson(map);
                //写入文件
                RemotingClient.writeFile(scanResult, mappingPath);
            }
        } catch (Exception e) {
            logger.debug("Compression mapping relationship processing failed");
        }
    }

    private static Boolean executeGenerateWfpFile(NewScanInfo newScanInfo, FilePathCollect filePathCollect, BinaryFilterParam binaryFilterParam) {
        String overrideAPIURL = System.getenv("CLEAN_SOURCE_API");
        String overrideAPIKEY = System.getenv("CLEAN_SOURCE_API");
        ScannerConf conf = ScannerConf.defaultConf();
        if (StringUtils.isNotEmpty(overrideAPIURL)) {
            conf = new ScannerConf(overrideAPIURL, overrideAPIKEY);
        }
        Scanner scanner = new Scanner(conf);
        try {
            logger.info("--------Generating wfp file--------");
            wfpFile = scanner.optimizationScanDirectory(newScanInfo, filePathCollect, binaryFilterParam);
            logger.info("--------wfp file generated--------");
            return true;
        } catch (Throwable e) {
            logger.error("--------wfp file generated  failed--------", e);
            return false;
        }
    }


    /**
     * 上传扫描文件和参数创建任务
     *
     * @param zipUrl      源码压缩路径
     * @param wfpFile     指纹文件
     * @param newScanInfo 启动参数
     * @param taskDirSize 源码目录大小
     */
    public static Boolean uploadData(File zipUrl, File wfpFile, String buildPath, NewScanInfo newScanInfo, Long taskDirSize) {

        FileTempDto fileTempDto = new FileTempDto();
        //判断文件是否存在
        if (zipUrl != null && zipUrl.exists()) {
            Boolean flag = true;
            try {
                logger.info("Uploading source code file -|");
                flag = uploadFile(newScanInfo, zipUrl, fileTempDto);
                logger.info("Uploading source code file completed -|");
            } catch (IOException e) {
                logger.error("Exception occurred while uploading source code file", e);
                flag = false;
            }
            if (flag) {
                newScanInfo.setObjectName(fileTempDto.getObjectName());
                newScanInfo.setFileName(fileTempDto.getFileName());
            } else {
                logger.error("Uploading source code failed");
            }
        }
        System.out.println();

        //判断文件是否存在
        if (StringUtils.isNotBlank(newScanInfo.getMappingPath())) {
            File file = new File(newScanInfo.getMappingPath());
            if (file.exists()) {
                Boolean flag = true;
                try {
                    logger.info("Uploading zip mapping file -|");
                    flag = uploadFile(newScanInfo, file, fileTempDto);
                    logger.info("Uploading zip mapping file completed -|");
                } catch (IOException e) {
                    logger.error("Exception occurred while uploading zip mapping file", e);
                    flag = false;
                }
                if (flag) {
                    newScanInfo.setMappingObjectName(fileTempDto.getObjectName());
                } else {
                    logger.error("Uploading zip mapping file failed");
                }
            }
        }

        System.out.println();
        //判断文件是否存在
        if (wfpFile != null && wfpFile.exists()) {
            Boolean flag = true;
            try {
                logger.info("Uploading wfp File -|");
                flag = uploadFile(newScanInfo, wfpFile, fileTempDto);
                logger.info("Uploading wfp file completed -|");
            } catch (IOException e) {
                logger.error("Exception occurred while uploading wfp file", e);
                flag = false;
            }
            if (flag) {
                newScanInfo.setWfpObjectName(fileTempDto.getObjectName());
                newScanInfo.setWfpFileName(fileTempDto.getFileName());
            } else {
                logger.error("Uploading wfp Upload failed");
                return false;
            }
        }

        System.out.println();
        //判断文件是否存在
        if (StringUtils.isNotBlank(buildPath)) {
            File file = new File(buildPath);
            if (file.exists()) {
                Boolean flag = true;
                try {
                    logger.info("Uploading Dependent result -|");
                    flag = uploadFile(newScanInfo, file, fileTempDto);
                    logger.info("Uploading Dependent result completed -|");
                } catch (IOException e) {
                    logger.error("Exception occurred while uploading dependent result", e);
                    flag = false;
                }
                if (flag) {
                    newScanInfo.setBuildFileObjectName(fileTempDto.getObjectName());
                    newScanInfo.setBuildFile(fileTempDto.getFileName());
                } else {
                    logger.error("Uploading dependent result failed");
                }
            }
        }

        //检测类型参数
        String sourceCode = "sourceCode";
        ScanConfigParam scanConfigParam = scanConfigTransition(newScanInfo);
        scanConfigParam.setRefer(newScanInfo.getServerUrl().replace(serverPrefix, Strings.EMPTY));
        List<FilterConditionParam> filterPath = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(newScanInfo.getAttachToPath())) {
            filterPath = newScanInfo.getAttachToPath().stream().map(s -> {
                FilterConditionParam filterConditionParam = new FilterConditionParam();
                filterConditionParam.setBuild_type(s.getBuildType());
                filterConditionParam.setDepth(s.getDepth());
                filterConditionParam.setPath(s.getPath());
                return filterConditionParam;
            }).collect(Collectors.toList());
        }
        List<FilterDependencyParam> filterDependencys = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(newScanInfo.getFilterDependency())) {
            filterDependencys = newScanInfo.getFilterDependency().stream().map(s -> {
                FilterDependencyParam filterDependency = new FilterDependencyParam();
                filterDependency.setInclude(s.getInclude());
                filterDependency.setExclude(s.getExclude());
                filterDependency.setBuild_type(s.getBuildType());
                return filterDependency;
            }).collect(Collectors.toList());
        }

        try (Response response = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + uploadUrl)
                .addRequestHeader(newScanInfo.getAuthType().getValue(), session)
                .addRequestParam("scanType", sourceCode)
                .addRequestParam("callerType", newScanInfo.getCallerType())
                .addRequestParam("objectName", newScanInfo.getObjectName())
                .addRequestParam("fileName", newScanInfo.getFileName())
                .addRequestParam("buildFile", newScanInfo.getBuildFile())
                .addRequestParam("buildFileObjectName", newScanInfo.getBuildFileObjectName())
                .addRequestParam("wfpFileName", newScanInfo.getWfpFileName())
                .addRequestParam("wfpObjectName", newScanInfo.getWfpObjectName())
                .addRequestParam("originProjectSize", taskDirSize)
                .post(true).sync()) {
            String str = response.body().string();
            logger.debug("response: {}", str);
            JSONObject jsonObject = JSONObject.parseObject(str);
            if ("100000".equals(jsonObject.getString("code"))) {
                JSONObject payload = JSONObject.parseObject(jsonObject.getString("data"));
                if (payload.get("increment_msg") != null && StringUtils.isNotBlank(payload.get("increment_msg").toString())) {
                    logger.warn("{}", payload.get("increment_msg"));
                }
                System.out.println();
                System.out.println();
                logger.info("====================");
                logger.info("Create task completed");
                logger.info("====================");
                logger.info("Scan data uploaded: taskInfo:{\"taskId\":\"{}\",\"taskInstanceId\":\"{}\",\"content\":\"{}\"}", payload.get("task_id"), payload.get("task_instance_id"), payload.get("content"));
                logger.info("Task scanning progress can be viewed in the scanning results");
                return true;
            } else {
                logger.error("Task creation failed： " + jsonObject.getString("message"));
                return false;
            }
        } catch (Exception e) {
            logger.error("Task creation failed： ", e);
            return false;
        }
    }


    /**
     * 上传docker压缩包
     *
     * @param newScanInfo
     */
    public static Boolean uploadAndCreateTask(NewScanInfo newScanInfo) {
        File file = new File(newScanInfo.getTaskFileDir());
        if (!file.exists()) {
            logger.error("{}文件路径不存在", newScanInfo.getScanType());
            return false;
        }

        //判断文件是否存在

        Boolean flag = true;

        FileTempDto fileTempDto = new FileTempDto();

        try {
            logger.info("{} file upload -|", newScanInfo.getScanType());
            flag = uploadFile(newScanInfo, file, fileTempDto);
            System.out.println();
            logger.info("{} File upload completed -|", newScanInfo.getScanType());
        } catch (IOException e) {
            logger.error("{} Data upload failed：", newScanInfo.getScanType(), e);
            flag = false;
        }
        if (flag) {
            newScanInfo.setObjectName(fileTempDto.getObjectName());
            newScanInfo.setFileName(fileTempDto.getFileName());
        } else {
            return false;
        }

        //参数转换
        ScanConfigParam scanConfigParam = scanConfigTransition(newScanInfo);
        try (Response response = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + uploadUrl)
                .addRequestHeader(newScanInfo.getAuthType().getValue(), session)
                .addRequestParam("scan_type", newScanInfo.getScanType())
                .addRequestParam("distribution", newScanInfo.getDistribution())
                .addRequestParam("stage", newScanInfo.getStage())
                .addRequestParam("custom_project", newScanInfo.getCustomProject())
                .addRequestParam("custom_product", newScanInfo.getCustomProduct())
                .addRequestParam("custom_version", newScanInfo.getCustomVersion())
                .addRequestParam("project_explain", newScanInfo.getProjectExplain())
                .addRequestParam("product_explain", newScanInfo.getProductExplain())
                .addRequestParam("version_explain", newScanInfo.getVersionExplain())
                .addRequestParam("version_owner", newScanInfo.getVersionOwner())
                .addRequestParam("project_owner", newScanInfo.getProjectOwner())
                .addRequestParam("product_owner", newScanInfo.getProductOwner())
                .addRequestParam("caller_type", newScanInfo.getCallerType())
                .addRequestParam("scan_way", newScanInfo.getDefaultParamInfo().getScanWay())
                .addRequestParam("scan_config", scanConfigParam)
                .addRequestParam("notification_email", newScanInfo.getNotificationEmail())
                .addRequestParam("strategy_id", newScanInfo.getStrategyId())
                .addRequestParam("license_name", newScanInfo.getLicenseName())
                .addRequestParam("object_name", newScanInfo.getObjectName())
                .addRequestParam("file_name", newScanInfo.getFileName())
                .addRequestParam("queue_priority", newScanInfo.getQueuePriority())
                .addRequestParam("department_id", newScanInfo.getDepartmentId())
                .addRequestParam("remarks", newScanInfo.getRemarks())
                .post(true).sync()) {

            String str = response.body().string();
            logger.debug("response: {}", str);
            JSONObject jsonObject = JSONObject.parseObject(str);
            if ("100000".equals(jsonObject.getString("code"))) {

                JSONObject payload = JSONObject.parseObject(jsonObject.getString("data"));
                System.out.println();
                System.out.println();
                logger.info("====================");
                logger.info("Create task completed");
                logger.info("====================");
                logger.info("Scan data uploaded: taskInfo:{\"taskId\":\"{}\",\"taskInstanceId\":\"{}\",\"content\":\"{}\"}", payload.get("task_id"), payload.get("task_instance_id"), payload.get("content"));
                logger.info("Task scanning progress can be viewed in the scanning results");
                return true;
            } else {
                System.out.println();
                logger.error("create {} task error：" + jsonObject.getString("message"), newScanInfo.getScanType());
                return false;
            }
        } catch (Exception e) {
            System.out.println();
            logger.error("create {} task error", newScanInfo.getScanType(), e);
            return false;
        }
    }

    private static ScanConfigParam scanConfigTransition(NewScanInfo newScanInfo) {
        DefaultParamInfo defaultParamInfo = newScanInfo.getDefaultParamInfo();
        ScanConfigParam scanConfigParam = new ScanConfigParam();
        scanConfigParam.setCopyright_flag(defaultParamInfo.getCopyrightFlag());
        scanConfigParam.setVulnerability_flag(defaultParamInfo.getVulnerabilityFlag());
        scanConfigParam.setSensitive_information_flag(defaultParamInfo.getSensitiveInformationFlag());
        scanConfigParam.setThread_num(defaultParamInfo.getThreadNum());
        scanConfigParam.setSnippet_flag(defaultParamInfo.getSnippetFlag());
        scanConfigParam.setScan_way(defaultParamInfo.getScanWay());
        scanConfigParam.setMatching_auto_confirm(defaultParamInfo.getMatchingAutoConfirm());
        scanConfigParam.setMatched(defaultParamInfo.getMatched());
        scanConfigParam.setLicense_flag(defaultParamInfo.getLicenseFlag());
        scanConfigParam.setAttribution_flag(defaultParamInfo.getAttributionFlag());
        scanConfigParam.setIs_unzip(defaultParamInfo.getIsUnzip());
        scanConfigParam.setIs_save_source_file(defaultParamInfo.getIsSaveSourceFile());
        scanConfigParam.setIs_open_candidate_pool(defaultParamInfo.getIsOpenCandidatePool());
        scanConfigParam.setIs_increment(defaultParamInfo.getIsIncrement());
        scanConfigParam.setIs_build(defaultParamInfo.getIsBuild());
        scanConfigParam.setInherit_version(defaultParamInfo.getInheritVersion());
        scanConfigParam.setInherited_version_name(defaultParamInfo.getInheritedVersionName());
        scanConfigParam.setMixed_binary_scan_flag(defaultParamInfo.getMixedBinaryScanFlag());
        scanConfigParam.setMixed_binary_scan_file_paths(defaultParamInfo.getMixedBinaryScanFilePaths());
        scanConfigParam.setExcluding_scan_path_rules(defaultParamInfo.getExcludingScanPathRules());
        scanConfigParam.setDepth(defaultParamInfo.getDepth());
        scanConfigParam.setCryptography_flag(defaultParamInfo.getCryptographyFlag());
        scanConfigParam.setCom_dependency_level(defaultParamInfo.getComDependencyLevel());
        scanConfigParam.setDependency_level(defaultParamInfo.getDependencyLevel());
        scanConfigParam.setBuild_scan_type(defaultParamInfo.getBuildScanType().getValue());
        scanConfigParam.setPackage_manager_types(newScanInfo.getPackageManagerTypes());

        scanConfigParam.setMaven_excluded_scopes(newScanInfo.getMavenExcludedScopes());
        scanConfigParam.setMaven_included_scopes(newScanInfo.getMavenIncludedScopes());
        scanConfigParam.setMaven_path(newScanInfo.getMavenPath());
        scanConfigParam.setMaven_included_modules(newScanInfo.getMavenIncludedModules());
        scanConfigParam.setMaven_excluded_modules(newScanInfo.getMavenExcludedModules());
        scanConfigParam.setMaven_pre_build_command(newScanInfo.getMavenPreBuildCommand());
        scanConfigParam.setMaven_build_command(newScanInfo.getMavenBuildCommand());

        ScanJiraConfig scanJiraConfig = defaultParamInfo.getScanJiraConfig();
        if (scanJiraConfig != null) {
            SnakeJiraConfig snakeJiraConfig = new SnakeJiraConfig();
            snakeJiraConfig.setJira_project_id(scanJiraConfig.getJiraProjectId());
            snakeJiraConfig.setJira_project_key(scanJiraConfig.getJiraProjectKey());
            snakeJiraConfig.setJira_project_name(scanJiraConfig.getJiraProjectName());
            snakeJiraConfig.setIssue_limit(scanJiraConfig.getIssueLimit());
            snakeJiraConfig.setUsername(scanJiraConfig.getUsername());
            snakeJiraConfig.setIssue_type(scanJiraConfig.getIssueType());
            snakeJiraConfig.setIssue_type_name(scanJiraConfig.getIssueTypeName());

            snakeJiraConfig.setLicense_risks(scanJiraConfig.getLicenseRisks());
            snakeJiraConfig.setRisk_id_map(scanJiraConfig.getRiskIdMap());
            snakeJiraConfig.setRisk_name_map(scanJiraConfig.getRiskNameMap());
            snakeJiraConfig.setVul_risks(scanJiraConfig.getVulRisks());
            scanConfigParam.setScan_jira_config(snakeJiraConfig);
        }
        List<SnakeInheritConfig> inheritConfigs = null;
        if (defaultParamInfo.getInheritConfigs() != null && CollectionUtils.isNotEmpty(defaultParamInfo.getInheritConfigs())) {
            inheritConfigs = new ArrayList<>();

            List<InheritConfig> inheritConfigList = defaultParamInfo.getInheritConfigs();
            for (InheritConfig inheritConfig : inheritConfigList) {
                SnakeInheritConfig snakeInheritConfig = new SnakeInheritConfig();
                snakeInheritConfig.setInherited_task_id(inheritConfig.getInheritedTaskId());
                snakeInheritConfig.setInherited_task_instance_id(inheritConfig.getInheritedTaskInstanceId());
                snakeInheritConfig.setInherited_version(inheritConfig.getInheritedVersion());
                snakeInheritConfig.setInherited_version_name(inheritConfig.getInheritedVersionName());
                inheritConfigs.add(snakeInheritConfig);
            }
        }
        scanConfigParam.setInherit_configs(inheritConfigs);

        scanConfigParam.setDetect_reachable(defaultParamInfo.getDetectReachable());
        scanConfigParam.setRemarks(defaultParamInfo.getRemarks());

        if (StringUtils.isNotBlank(newScanInfo.getBuildType())) {
            if (StringUtils.isNotBlank(scanConfigParam.getPackage_manager_types())) {
                scanConfigParam.setPackage_manager_types(scanConfigParam.getPackage_manager_types() + "," + newScanInfo.getBuildType());
            } else {
                scanConfigParam.setPackage_manager_types(newScanInfo.getBuildType());
            }
        }
        scanConfigParam.setBuild_tree_file(newScanInfo.getBuildTreeFile());
        return scanConfigParam;
    }


    /**
     * 把扫描结果写入文件
     *
     * @param json
     * @param filePath
     */
    public static void writeFile(String json, String filePath) {

        try {
            File file = new File(filePath);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
       /*
            FileOutputStream fileOutputStream=new FileOutputStream(file);//实例化FileOutputStream
            OutputStreamWriter outputStreamWriter=new OutputStreamWriter(fileOutputStream,"utf-8");//将字符流转换为字节流
             FileWriter fileWritter = new FileWriter(fileOutputStream, false);
            */

            // true = append file
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8));
            bufferedWriter.write(json);
            bufferedWriter.close();
            logger.info("build file path：{}", filePath);
        } catch (IOException e) {
            logger.error("create build scan file error", e);
        }
    }


    /**
     * 验证license名称是否存在
     *
     * @param newScanInfo
     */
    public static void verifyLicenseUrl(NewScanInfo newScanInfo) {

        try {
            Response response = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + verifyLicenseUrl)
                    .addRequestParam("spdx_name", newScanInfo.getLicenseName()).addRequestHeader(newScanInfo.getAuthType().getValue(), session)
                    .post(false).sync();
            String str = response.body().string();
            JSONObject jsonObject = JSONObject.parseObject(str);
            if (!"100000".equals(jsonObject.getString("code"))) {
                logger.error("licenseName error {}", jsonObject.getString("message"));
            }
        } catch (Exception e) {
            logger.error("licenseName error ", e);
        }
    }

    /**
     * 验证Token是否能用(使用token请求任意接口能通就说明token生效)
     *
     * @param newScanInfo
     */
    public static boolean verifyToken(NewScanInfo newScanInfo) {
        try {

            String str = OkHttpUtils.builder()
                    .url(newScanInfo.getServerUrl() + verifyToken)
                    .addRequestHeader("Token", newScanInfo.getToken())
                    .addRequestParam("page_num", 1)
                    .addRequestParam("page_size", 50)
                    .post(true).sync().body().string();

            JSONObject jsonObject = JSONObject.parseObject(str);
            String code = jsonObject.getString("code");
            if (code.equals("100000")) {
                // 只要请求成功, 就说明token生效
                session = newScanInfo.getToken();
                return true;
            } else {
                logger.warn("exec verifyToken warn; code:{}", code);
                return false;
            }
        } catch (Exception e) {
            logger.error("exec verifyToken err:", e);
            return false;
        }
    }

    /**
     * 验证邮箱是否绑定发送邮箱
     *
     * @param newScanInfo
     */
    public static void verifyEmail(NewScanInfo newScanInfo) {
        try {

            String str = OkHttpUtils.builder()
                    .url(newScanInfo.getServerUrl() + verifyEmailUrl)
                    .addRequestHeader(newScanInfo.getAuthType().getValue(), session)
                    .get().sync().body().string();

            JSONObject jsonObject = JSONObject.parseObject(str);
            String code = jsonObject.getString("code");
            if (code.equals("100000")) {
                if (jsonObject.get("data") == null) {
                    logger.error("Please contact the administrator to configure the sending email");
                    System.exit(8);
                }
            } else {
                logger.error("Verification email exception {}", jsonObject.getString("message"));
                System.exit(9);
            }
        } catch (Exception e) {
            logger.error("Verification email exception", e);
            System.exit(9);
        }
    }

    public static void initLog(ApplicationArguments args) {

        String logLevel = "";
        if (CollectionUtils.isNotEmpty(args.getOptionValues("logLevel"))) {
            logLevel = args.getOptionValues("logLevel").get(0) == null ? null : args.getOptionValues("logLevel").get(0);
        }
        String outputPath = null;
        if (CollectionUtils.isNotEmpty(args.getOptionValues("outputPath"))) {
            outputPath = args.getOptionValues("outputPath").get(0);
        }
        DirectoryManager.getDirectoryManager(outputPath);
        //动态日志
        MaintenanceSystem.createDiagnosticSystem(logLevel);

        if (StringUtils.isNotBlank(outputPath)) {
            File outputFile = new File(outputPath);
            if (!outputFile.exists()) {
                logger.warn("outputPath does not exist! {}", outputPath);
            }
        }
    }


    private static Boolean getUploadUrls(String serverUrl, String objectName, Integer chunkSize, String[] uploadUrls) {
        String apiUrl = "/api/v2/file/multipart_upload/create";
        try {
            Response response = OkHttpUtils.builder().url(serverUrl + apiUrl)
                    .addRequestParam("object_name", objectName)
                    .addRequestParam("chunk_size", chunkSize)
                    .post(false).sync();
            String str = response.body().string();
            JSONObject jsonObject = JSONObject.parseObject(str);
            if (!"100000".equals(jsonObject.getString("code"))) {
                logger.error("Create shard upload failed {}", jsonObject.getString("message"));
                return false;
            }
        } catch (Exception e) {
            logger.error("Create shard upload failed", e);
            return false;
        }
        return true;
    }


    public static Boolean uploadFile(NewScanInfo newScanInfo, File file, FileTempDto fileTempDto) throws IOException {
        FileSliceResult fileSliceResult = FileUploader.calculateChunkNum(file, DEFAULT_FILE_CHUNK_SIZE);
        int chunkNumber = fileSliceResult.getChunkNum();
        long chunkSize = fileSliceResult.getChunkSize();

        String objectName = "/source-code" + "/" + "" + UUID.randomUUID().toString() + "/" + file.getName();
        logger.info("Uploading file, file:{}, OSS location:{}", file.getAbsolutePath(), objectName);

        fileTempDto.setObjectName(objectName);
        fileTempDto.setFileName(file.getName());
        String uploadId = "";
        String[] uploadUrls = new String[0];

        JSONObject jsonObject = null;
        try (Response response = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + shardingUrl)
                .addRequestParam("fileName", objectName)
                .addRequestParam("chunkSize", chunkNumber)
                .addRequestHeader(newScanInfo.getAuthType().getValue(), session)
                .addRequestHeader("Content-Type", "application/json")
                .post(true).sync()) {
            String str1 = response.body().string();
            jsonObject = JSONObject.parseObject(str1);
            if (!"100000".equals(jsonObject.getString("code"))) {
                logger.error("Generating object uploading urls failed, error:{}", jsonObject.getString("message"));
                return false;
            } else {
                JSONObject data = JSONObject.parseObject(jsonObject.getString("data"));
                uploadId = data.getString("uploadId");
                JSONArray uploadUrlList = data.getJSONArray("uploadUrlList");
                uploadUrls = (String[]) uploadUrlList.toArray(new String[0]);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while generating object uploading urls", e);
            return false;
        }

        FileUploader fileUploader = null;

        try {
            if (newScanInfo.getCallerType().equals(SourceType.SOURCE_CLI_TYPE.getValue())
                    || newScanInfo.getCallerType().equals(SourceType.SOURCE_JENKINS_TYPE.getValue())) {
                fileUploader = new FileUploader(file, chunkNumber, uploadUrls, true, chunkSize);
            } else {
                fileUploader = new FileUploader(file, chunkNumber, uploadUrls, false, chunkSize);
            }

            // 等待上传完成
            while (!fileUploader.isFinished()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("Exception occurred while awaiting file uploading completion", e);
                }
            }
            System.out.println();
            logger.info("Uploading result, chunkNum:{}, succeeded:{}, failed:{}", chunkNumber, fileUploader.getSucceededCount(), fileUploader.getFailedCount());
        } catch (Exception e) {
            logger.error("Exception occurred while uploading file", e);
        }

        if (fileUploader == null || !fileUploader.isSuccess()) {
            logger.error("Uploading file failed");
            return false;
        }

        //合并
        try (Response merge = OkHttpUtils.builder().url(newScanInfo.getServerUrl() + mergeUrl)
                .addRequestParam("uploadId", uploadId)
                .addRequestParam("fileName", objectName)
                .addRequestHeader(newScanInfo.getAuthType().getValue(), session)
                .post(true).sync()) {
            String mergeStr = merge.body().string();
            JSONObject mergeObject = JSONObject.parseObject(mergeStr);
            if (!"100000".equals(mergeObject.getString("code"))) {
                logger.error("Merging file objects failed, error:{}", jsonObject.getString("message"));
                return false;
            }
        } catch (IOException e) {
            logger.error("Exception occurred while merging file objects", e);
            return false;
        }
        return true;
    }

    public static String analyseSPDXLicense(String licenseFile) {
        try {
            List<String> lines = FileUtils.readLines(new File(licenseFile));
            if (lines.size() > MAX_HEADER_LINES) {
                lines = lines.subList(0, MAX_HEADER_LINES);
            }
            int byteSize = 0;
            for (String line : lines) {
                if (line.contains(SPDX_LICENCE_IDENTIFIER)) {
                    int index = line.indexOf(SPDX_LICENCE_IDENTIFIER);
                    String license = line.substring(index + SPDX_LICENCE_IDENTIFIER.length()).replaceAll("^[^A-Za-z]+", "");
                    if (license.length() > MAX_ARG_LEN) {
                        license = license.substring(0, license.length() - 2);
                    }
                    return license;
                }
                byteSize += line.length();
                if (byteSize > MAX_FILE_HEADER) {
                    return null;
                }
            }
        } catch (Throwable t) {
            logger.error("can not analyse project license file {}", licenseFile, t);
        }
        return null;
    }

    public static String analyseLicense(String licenseFile) {
        String licenseResult = null;
        try {
            licenseResult = analyseSPDXLicense(licenseFile);
            if (StringUtils.isBlank(licenseResult)) {
                String fileContent = FileUtils.readFileToString(new File(licenseFile));
                licenseResult = analyseFileHeadLicense(fileContent);
                if (StringUtils.isBlank(licenseResult)) {
                    if (fileContent.length() > LICENSE_SERIAL_LEN.intValue()) {
                        licenseResult = analyseFileHeadLicense(fileContent.substring(fileContent.length() - LICENSE_SERIAL_LEN));
                    } else {
                        licenseResult = analyseFileHeadLicense(fileContent);
                    }
                }
            }

        } catch (Throwable t) {
            logger.error("can not analyse project license file {}", licenseFile, t);
        }
        return licenseResult;
    }

    public static String analyseFileHeadLicense(String fileContent) {
        if (fileContent.length() > MAX_FILE_HEADER) {
            fileContent = fileContent.substring(0, MAX_FILE_HEADER);
        }
        List<SerialLicense> licenses = SerialLicenseFactory.getLicenses();
        StringBuilder serial = new StringBuilder(LICENSE_SERIAL_LEN);
        serialFileHead(fileContent, serial, fileContent.length(), LICENSE_SERIAL_LEN - 1);
        String serialStr = serial.toString();
        for (SerialLicense licens : licenses) {
            if (serialStr.contains(licens.getContent())) {
                return licens.getSpdxId();
            }
        }

        return null;
    }


    public static void serialFileHead(String fileHead, StringBuilder serial, int fhSize, int licSize) {
        int nPtr = 0;

        for (int i = 0; i < fhSize && i < fileHead.length(); i++) {
            char ch = fileHead.charAt(i);

            if (Character.isLetterOrDigit(ch)) {
                serial.append(Character.toLowerCase(ch));
                nPtr++;
            } else if (ch == '+') {
                serial.append(ch);
                nPtr++;
            }

            if (nPtr >= licSize) {
                break;
            }
        }
    }


    /**
     * 初始化参数
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() {
        /*RemotingClient.uploadUrl = this.getUploadUrl;
        RemotingClient.loginUrl = this.getLoginUrl;
        RemotingClient.dockerUrl = this.getDockerUrl;
        RemotingClient.verifyTaskUrl = this.getVerifyTaskUrl;*/
    }

    public static void printParamLog(NewScanInfo newScanInfo) {

        Class<? extends NewScanInfo> clazz = newScanInfo.getClass();
        Field[] fields = clazz.getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(newScanInfo);

                String fieldName = field.getName();
                if (ObjectUtils.anyNotNull(value) && !"".equals(value)) {
                    // token字段打印日志脱敏处理
                    if (tokenKeyName.equals(fieldName) || passwordKeyName.equals(fieldName)) {
                        logger.info("[{}]={}", fieldName, securityReplaceValue);
                        continue;
                    }
                    logger.info("[{}]={}", fieldName, value);

                }
            }
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
    }

    public static void main(String[] args) throws IOException {

        //File zipFile = ZipUtils.getZipFile(new File("F:\\e\\测试压缩\\hm"), "D:\\tmp\\scan");

        NewScanInfo newScanInfo = new NewScanInfo();


    }


    private static void printLog(FileUploader fileUploader, int num) {
/*
        System.out.print("Progress: ");
        int temp = 0;
        while (!(fileUploader.getNum() > (num - 1))) {

            // 计算当前进度百分比
            double progress = fileUploader.getUploadProgress();

            // 计算进度条长度（假设进度条长度为 50）
            int progressBarLength = 50;
            int filledLength = (int) (progressBarLength * progress);
            int remainingLength = progressBarLength - filledLength;

            // 构建进度条字符串
            StringBuilder progressBar = new StringBuilder("[");
            for (int j = 0; j < filledLength; j++) {
                progressBar.append("=");
            }
            progressBar.append(">");
            for (int j = 0; j < remainingLength; j++) {
                progressBar.append(" ");
            }
            progressBar.append("]");

            // 输出进度条
            System.out.print("\r" + progressBar + String.format(" %.2f%%", progress));

            // 模拟处理时间
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            temp = fileUploader.getNum();
        }

        System.out.println("\nProcessing completed.");*/


    }


}
