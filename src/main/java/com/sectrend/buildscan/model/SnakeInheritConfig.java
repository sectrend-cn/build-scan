package com.sectrend.buildscan.model;

import lombok.Data;

@Data
public class SnakeInheritConfig {
    /**
     * 增量基础版本信息，格式为"项目id/产品id/版本id"，如“216/255/350”
     */
    private String inherited_version;

    /**
     * 增量基础版本名称，格式为"项目名称///产品名称///版本名称"，如“sca///sca_backend///cleansourcesca”。该字段与inheritVersion二选一，优先使用inheritVersion
     */
    private String inherited_version_name;

    /**
     * 增量继承的任务ID
     */
    private Long inherited_task_id;

    /**
     * 增量继承的任务扫描实例ID
     */
    private Long inherited_task_instance_id;

    public SnakeInheritConfig(){}

    public SnakeInheritConfig(Long taskId, Long taskInstanceId, String version, String versionName) {
        this.inherited_task_id=taskId;
        this.inherited_task_instance_id=taskInstanceId;
        this.inherited_version=version;
        this.inherited_version_name=versionName;
    }
}