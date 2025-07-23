package com.sectrend.buildscan.model;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScanConfigParam {

    /**
     * 是否开启代码片段扫描（0, 1） “code”
     */
    private int snippet_flag;

    /**
     * 许可证是否检测（0, 1）
     */
    private int license_flag;

    /**
     * 版权是否检测（0, 1）
     */
    private int copyright_flag;

    /**
     * 敏感信息是否检测（0, 1）默认关闭
     */
    private int sensitive_information_flag;

    private List<String> excluding_scan_path_rules = new ArrayList<>();

    /**
     * 加密算法是否检测（0, 1）
     */
    private int cryptography_flag;

    /**
     * 漏洞给是否检测（0, 1）
     */
    private int vulnerability_flag;

    /**
     * attribution文件解析开关 (0, 1)
     */
    private int attribution_flag;

    /**
     * 是否保存源码（0, 1）
     */
    private int is_save_source_file;

    /**
     * 扫描代码深度
     */
    private int depth;

    /**
     * 扫描线程数
     */
    private int thread_num;

    /**
     * 匹配度
     */
    private String matched;

    /**
     * 是否开启候选池 (0, 1)
     */
    private int is_open_candidate_pool;

    /**
     * 扫描方式 (默认：1 (full_scan：全面扫描)，2 (quick_scan：快速扫描))
     */
    private int scan_way;

    /**
     * 扫描方式 (0:未开启构建扫描，1：开启构建扫描，2：开启依赖文件构建扫描)
     */
    private int is_build;

    /**
     * 是否开启增量扫描 (0:未开启增量扫描，1：开启增量扫描)
     */
    private int is_increment;

    /**
     * 增量基础版本信息（项目ID/产品ID/版本ID）
     */
    private String inherit_version;

    /**
     * 增量基础版本信息（项目名称///产品名称///版本名称）
     */
    private String inherited_version_name;

    /**
     * 多基线增量基础版本信息（项目名称///产品名称///版本名称）
     */
    private List<SnakeInheritConfig> inherit_configs;

    /**
     * 是否解压
     */
    private Integer is_unzip = 0;

    /**
     * 是否自动确认
     */
    private Integer matching_auto_confirm;


    /**
     * 是否做漏洞可达<0,1>
     */
    private Integer detect_reachable = 0;

    private SnakeJiraConfig scan_jira_config;


    /**
     * 组件依赖层级 (0-1)
     */
    private Integer com_dependency_level = 1;

    private Integer dependency_level = 4;

    private Integer build_scan_type;

    private String package_manager_types;

    private Integer mixed_binary_scan_flag = 0;

    private List<String> mixed_binary_scan_file_paths = new ArrayList<>();

    private String refer;

    /**
     * 备注
     */
    private String remarks;

    private String build_tree_file;


    /**
     * ================================= Gradle 构建器业务参数 =================================
     */
    private String gradle_build_command;
    private String gradle_pre_build_command;
    private String gradle_configuration_types_excluded;
    private String gradle_path;
    private String gradle_excluded_configurations;
    private String gradle_included_configurations;
    private String gradle_excluded_projects;
    private String gradle_included_projects;
    private String gradle_excluded_project_paths;
    private String gradle_included_projectPaths;
    private String gradle_root_only;
    private String gradle_global_log_level;

    /**
     * ================================= Maven 构建器业务参数 =================================
     */
    private String maven_excluded_scopes;
    private String maven_included_scopes;
    private String maven_path;
    private String maven_included_modules;
    private String maven_excluded_modules;
    private String maven_build_command;
    private String maven_pre_build_command;
    /**
     * ================================= Yarn 构建器业务参数 =================================
     */
    private String yarn_ignore_all_workspaces;
    private String yarn_excluded_workspaces;
    private String yarn_included_workspaces;
    private String yarn_dependency_types_excluded;
    /**
     * ================================= goMod 构建器业务参数 =================================
     */
    private String go_path;
    private String go_mod_dependency_types_excluded;


    /**
     * ================================= Npm 构建器业务参数 =================================
     */
    private String npm_path;
    private String npm_arguments;
    private String npm_dependency_types_excluded;

    /**
     * ======================== pip 构建器业务参数 ========================================================
     */
    private String pip_python_path;
    private String pip_project_name;
    private String pip_path;
    private String pip_requirements_path;

    /**
     * ======================== pipenv 构建器业务参数 ========================================================
     */
    private String pipenv_path;
    private String pipenv_python_path;
    private String pipenv_dependency_types_excluded;
    private String pipenv_only_project_tree;
    private String pipenv_project_name;
    private String pipenv_project_version_name;

    /**
     * ================================= Bazel 构建器业务参数 =================================
     */
    private String bazel_cquery_options;
    private String bazel_path;
    private String bazel_target;
    private String bazel_workspace_rules;

    /**
     * ================================= Pnpm 构建器业务参数 =================================
     */
    private String pnpm_dependency_types_excluded;

    /**
     * ================================= Conan 构建器业务参数 =================================
     */
    private String conan_arguments;
    private String conan_attempt_package_revision_match;
    private String conan_dependency_types_excluded;
    private String conan_path;
    private String conan_lockfile_path;
    private String conan_check_type;

    /**
     * ================================= Sbt 构建器业务参数 =================================
     */
    private String sbt_arguments;
    private String sbt_path;


    /**
     * ================================= Bower 构建器业务参数 =================================
     */
    private String bower_dependency_types_excluded;
    private String bower_dependency_path_excluded;

    /**
     * ================================= Lerna 构建器业务参数 =================================
     */
    private String lerna_path;
    private String lerna_package_types_excluded;
    private String lerna_excluded_packages;
    private String lerna_included_packages;


    /**
     * ================================= Hex 构建器业务参数 =================================
     */
    private String hex_rebar3_path;

    /**
     * ================================= Bitbake 构建器业务参数 =================================
     */
    private String bitbake_dependency_types_excluded;
    private String bitbake_build_env_name;
    private String bitbake_package_names;
    private String bitbake_search_depth;
    private String bitbake_source_arguments;
    /**
     * ================================= Nuget 构建器业务参数 =================================
     */
    private String nuget_config_path;
    private String nuget_dependency_types_excluded;
    private String nuget_packages_repoUrl;
    private String nuget_ignore_failure;
    private String nuget_excluded_modules;
    private String nuget_included_modules;


    /**
     * ================================== conda 构建器业务参数 ===============================
     */
    private String conda_environment_name;
    private String conda_path;


    /**
     * ================================= pear 构建器业务参数 =================================
     */
    private String pear_dependency_types_excluded;
    private String pear_path;

    /**
     * ================================= clang 构建器业务参数 =================================
     */
    private String clang_cleanup;

    /**
     * ================================= Pub 构建器业务参数 =================================
     */
    private String pub_dart_path;
    private String pub_flutter_path;
    private String pub_dependency_types_excluded;

    /**
     * ================================= Packagist 构建器业务参数 =================================
     */
    private String packagist_dependency_types_excluded;


    /**
     * ================================= Cpan 构建器业务参数 =================================
     */
    private String cpan_path;
    private String cpan_cpanm_path;

    /**
     * ================================= rubygems 构建器业务参数 =================================
     */
    private String ruby_dependency_types_excluded;

}
