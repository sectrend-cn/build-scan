package com.sectrend.buildscan.model;

import com.sectrend.buildscan.enums.AuthType;
import lombok.Data;

import java.util.List;

@Data
public class NewScanInfo {

    /**
     * customProject 项目
     */
    private String customProject;

    /**
     * customProduct 产品
     */
    private String customProduct;

    /**
     * customVersion 版本
     */
    private String customVersion;

    /**
     * projectExplain 自定义项目解释
     */
    private String projectExplain;

    /**
     * productExplain 自定义产品解释
     */
    private String productExplain;

    /**
     * versionExplain 自定义版本解释
     */
    private String versionExplain;

    /**
     * versionOwner 版本所有者
     */
    private String versionOwner;

    /**
     * projectOwner 项目所有者
     */
    private String projectOwner;

    /**
     * productOwner 产品所有者
     */
    private String productOwner;

    /**
     * taskDir 文件目录
     */
    private String taskDir;

    /**
     * buildDepend 是否构建依赖（默认构建）
     */
    private Boolean buildDepend = true;

    /**
     * buildType 依赖树构建类型
     */
    private String buildType;

    /**
     * distribution 分发方式
     */
    private String distribution;

    /**
     * stage 阶段
     */
    private String stage;


    /**
     * serverUrl 服务端地址
     */
    private String serverUrl;

    /**
     * 用户账号
     */
    private String password;

    /**
     * 用户密码
     */
    private String username;

    /**
     * 令牌
     */
    private String token;

    /**
     *  扫描类型
     */
    private String scanType;

    /**
     *  docker扫描上传路径
     */
    private String taskFileDir;

    /**
     * taskType 模块类型
     */
    private String taskType;

    /**
     * 需要生成指纹文件的路径
     */
    private String fromPath;

    /**
     * 生成指纹目录（默认用户目录）
     */
    private String toPath;


    /**
     * 构建依赖树文件路径
     */
    private String buildTreeFile;


    /**
     * 自定义输入的license名字
     */
    private String licenseName;


    /**
     * 线程数（默认是30，范围是1-60）
     */
    private String threadNum;

    /**
     * 来源类型: CLI:命令行 UI:客户端 jenkins: JENKINS
     */
    private String callerType;

    /**
     * 日志级别
     */
    private String logLevel;

    /**
     * 转换密码字段
     */
    private String transcoding;

    /**
     * 指纹核心线程数
     */
    private String wfpKernelThreadSize;

    /**
     * 指纹最大线程数
     */
    private String wfpMaxThreadSize;

    /**
     * 指纹等待队列
     */
    private String wfpQueueCapacity;

    /**
     * 通知邮箱
     */
    private String notificationEmail;

    /**
     * 策略组id
     */
    private String strategyId;

    /**
     *  默认参数
     */
    private DefaultParamInfo defaultParamInfo;

    /**
     * 指纹文件是否生成hpsm
     */
    private Boolean hpsm = true;

    /**
     * 登录类型
     */
    private Integer loginType;

    /**
     * 优先级
     */
    private String queuePriority;

    //------------------------------------------------------------------------------------------------------------------------------

    /**
     * 构建文件
     */
    private String buildFile;


    /**
     * 文件名 当scan_type为"source_code"时必传
     */
    private String objectName;


    /**
     * 文件名 当scan_type为"source_code"时必传
     */
    private String fileName;

    /**
     * 文件名 当scan_type为"source_code"，开启二进制扫描开关时必传
     */
    private String binaryName;

    /**
     * 上传二进制扫描文件的ojectName
     */
    private String binaryFileObjectName;

    private String binaryParamObjectName;

    /**
     * 扫描方式 快速扫描全量扫描(默认：1 (full_scan：全面扫描)，2 (quick_scan：快速扫描))
     */
    private Integer scanWay;

    /**
     * 上传构建依赖文件的bojectName
     */
    private String buildFileObjectName;


    /**
     * wfp文件名称
     */
    private String wfpFileName;


    /**
     * wfp objectName
     */
    private String wfpObjectName;

    /**
     * 构建条件
     */
    List<FilterCondition> attachToPath;

    /* */
    /**
     * npm构建需要排除的依赖类型
     */
    private String npmExclude;
    // mvnInclude  mvnExclude gradleInclude  gradleExclude

    /**
     * npmAutopiler npm自动编译
     */
    private Integer npmAutopiler;

    /**
     * 过滤条件dto
     */
    private List<FilterDependencyDto> filterDependency;


    private Integer departmentId;

    private String mappingPath;

    private String mappingObjectName;

    /**
     * 认证类型的请求头里面的key
     */
    private AuthType authType;

    private String decompressionDirectory;
    private String decompressionParentDirectory;

    private List<String> excludePaths;


    /**
     * 日志，解压文件等 输出路径
     * */
    private String outputPath;

    /**
     * 是否对扫描文件进行换行符格式转化
     * */
    private boolean formatEnable;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 控制开关是否打印构建日志
     */
    private Integer buildResultFlag = 1;

    /**
     * 包管理器类型
     */
    private String packageManagerTypes;

    /**
     * ================================= Maven 构建器业务参数 =================================
     */
    private String mavenExcludedScopes;
    private String mavenIncludedScopes;
    private String mavenPath;
    private String mavenIncludedModules;
    private String mavenExcludedModules;
    private String mavenPreBuildCommand;
    private String mavenBuildCommand;

    /**
     * ================================= pip 构建器业务参数 ==================================
     */
    private String pipPythonPath;
    private String pipProjectName;
    private String pipPath;
    private String pipRequirementsPath;

    /**
     * ================================ pipenv 构建器业务参数 ================================
     */
    private String pipenvPath;
    private String pipenvPythonPath;
    private String pipenvDependencyTypesExcluded;
    private String pipenvOnlyProjectTree;
    private String pipenvProjectName;
    private String pipenvProjectVersionName;

}
