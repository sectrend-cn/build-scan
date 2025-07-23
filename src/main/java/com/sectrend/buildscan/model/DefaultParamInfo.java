package com.sectrend.buildscan.model;

import com.sectrend.buildscan.enums.BuildScanTypeEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class DefaultParamInfo {

    /**
     * 是否开启代码片段扫描（0, 1） “code”
     */
    private int snippetFlag;

    /**
     * 许可证是否检测（0, 1）
     */
    private int licenseFlag;

    /**
     * 敏感信息是否检测（0, 1）默认关闭
     */
    private int sensitiveInformationFlag;

    /**
     * 版权是否检测（0, 1）
     */
    private int copyrightFlag;

    /**
     * 加密算法是否检测（0, 1）
     */
    private int cryptographyFlag;

    /**
     * 漏洞给是否检测（0, 1）
     */
    private int vulnerabilityFlag;

    /**
     * 是否保存源码（0, 1）
     */
    private int isSaveSourceFile;

    /**
     * 扫描代码深度
     */
    private int depth;

    /**
     * 扫描线程数
     */
    private int threadNum;

    /**
     * 匹配度
     */
    private String matched;

    /**
     * 是否开启候选池 (0, 1)
     */
    private int isOpenCandidatePool;

    /**
     * 扫描方式 (默认：1 (full_scan：全面扫描)，2 (quick_scan：快速扫描))
     */
    private int scanWay;

    /**
     * 扫描方式 (0:未开启构建扫描，1：开启构建扫描，2：开启依赖文件构建扫描)
     * todo 该字段可以考虑废弃，目前后端只有在生成pdf报告的时候用到
     */
    private int isBuild;

    /**
     * 构建扫描类型： 1.构建. 2.非构建. 3.先走构建,构建失败走非构建. 默认3
     */
    private BuildScanTypeEnum buildScanType = BuildScanTypeEnum.ALL;

    private int isBuildWithGradlew = 1;

    /**
     * 是否开启增量扫描 (0:未开启增量扫描，1：开启增量扫描)
     */
    private int isIncrement;

    /**
     * 增量基础版本信息（项目ID/产品ID/版本ID）
     */
    private String inheritVersion;

    /**
     * 增量基础版本信息（项目名称///产品名称///版本名称）
     */
    private String inheritedVersionName;

    /**
     * 多基线增量基础版本信息（项目名称///产品名称///版本名称）
     */
    private List<InheritConfig> inheritConfigs;

    private ScanJiraConfig scanJiraConfig;


    /**
     * 是否解压
     */
    private Integer isUnzip = 0;

    /**
     * 是否自动确认
     */
    private Integer matchingAutoConfirm;

    /**
     * 是否做漏洞可达<0,1>
     */
    private Integer detectReachable = 0;

    /**
     * 组件依赖层级 (0-1)
     */
    private Integer comDependencyLevel = 0;

    private Integer dependencyLevel = 4;

    /**
     * attribution文件解析开关 (0, 1)
     */
    private Integer attributionFlag = 0;

    private List<String> excludingScanPathRules = new ArrayList<>();

    private Integer mixedBinaryScanFlag = 0;

    /**
     * 控制开关是否打印构建日志
     */
    private Integer buildResultFlag = 1;

    private List<String> mixedBinaryScanFilePaths = new ArrayList<>();

    /**
     * 备注
     */
    private String remarks;

}
