package com.sectrend.buildscan.model;

import lombok.Data;

@Data
public class InheritConfig {
    /**
     * 增量基础版本信息，格式为"项目id/产品id/版本id"，如“216/255/350”
     */
    private String inheritedVersion;

    /**
     * 增量基础版本名称，格式为"项目名称///产品名称///版本名称"，如“sca///sca_backend///cleansourcesca”。该字段与inheritVersion二选一，优先使用inheritVersion
     */
    private String inheritedVersionName;

    /**
     * 增量继承的任务ID
     */
    private Long inheritedTaskId;

    /**
     * 增量继承的任务扫描实例ID
     */
    private Long inheritedTaskInstanceId;

    public InheritConfig(){}

    public InheritConfig(Long taskId, Long taskInstanceId, String version, String versionName) {
        this.inheritedTaskId=taskId;
        this.inheritedTaskInstanceId=taskInstanceId;
        this.inheritedVersion=version;
        this.inheritedVersionName=versionName;
    }
}