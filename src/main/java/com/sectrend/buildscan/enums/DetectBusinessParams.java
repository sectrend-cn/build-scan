package com.sectrend.buildscan.enums;

import com.sectrend.buildscan.annotation.detect.business.param.ParamMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 构建器业务参数
 *
 * @author: Jimmy
 * @date: 2025-01-20 11:35:30
 *
 * @see com.sectrend.buildscan.annotation.detect.business.param.ParamMeta
 * @see ParamMetaHelper
 */
@Getter
@AllArgsConstructor
public enum DetectBusinessParams {

    //=======================参数枚举开始=============================

    /**
     * GO_MOD
     */
    @ParamMeta(category = "go_mod")
    GO_PATH("goPath", "--scan.go.path"),
    @ParamMeta(category = "go_mod")
    GO_MOD_DEPENDENCY_TYPES_EXCLUDED("goModDependencyTypesExcluded", "--scan.go.mod.dependency.types.excluded"),

    /**
     * PIP
     */
    @ParamMeta(category = "pip")
    PIP_PATH("pipPath","--scan.pip.path"),
    @ParamMeta(category = "pip")
    PIP_PROJECT_NAME("pipProjectName","--scan.pip.project.name"),
    @ParamMeta(category = "pip")
    PIP_REQUIREMENTS_PATH("pipRequirementsPath","--scan.pip.requirements.path"),
    @ParamMeta(category = "pip")
    PIP_PYTHON_PATH("pipPythonPath","--scan.pip.python.path"),

    /**
     * PIPENV
     */
    @ParamMeta(category = "pip_env")
    PIPENV_PATH( "pipenvPath", "--scan.pipenv.path"),
    @ParamMeta(category = "pip_env")
    PIPENV_DEPENDENCY_TYPES_EXCLUDED("pipenvDependencyTypesExcluded", "--scan.pipenv.dependency.types.excluded"),
    @ParamMeta(category = "pip_env")
    PIPENV_ONLY_PROJECT_TREE("pipenvOnlyProjectTree", "--scan.pipenv.only.project.tree"),
    @ParamMeta(category = "pip_env")
    PIPENV_PROJECT_NAME( "pipenvProjectName", "--scan.pipenv.project.name"),
    @ParamMeta(category = "pip_env")
    PIPENV_PROJECT_VERSION_NAME("pipenvProjectVersionName", "--scan.pipenv.project.version.name"),
    @ParamMeta(category = "pip_env")
    PIPENV_PYTHON_PATH("pipenvPythonPath", "--scan.pipenv.python.path"),


    /**
     * MAVEN
     */
    @ParamMeta(category = "maven")
    MAVEN_EXCLUDED_SCOPES("mavenExcludedScopes", "--scan.maven.excluded.scopes"),
    @ParamMeta(category = "maven")
    MAVEN_INCLUDED_SCOPES("mavenIncludedScopes", "--scan.maven.included.scopes"),
    @ParamMeta(category = "maven")
    MAVEN_PATH("mavenPath", "--scan.maven.path"),
    @ParamMeta(category = "maven")
    MAVEN_MODULES_INCLUDED("mavenIncludedModules", "--scan.maven.included.modules"),
    @ParamMeta(category = "maven")
    MAVEN_MODULES_EXCLUDED("mavenExcludedModules", "--scan.maven.excluded.modules"),
    @ParamMeta(category = "maven")
    MAVEN_PRE_BUILD_COMMAND("mavenPreBuildCommand", "--scan.maven.pre.build.command"),
    @ParamMeta(category = "maven")
    MAVEN_BUILD_COMMAND("mavenBuildCommand", "--scan.maven.build.command"),

    //=======================参数枚举结束=============================
    ;


    /**
     * 和NewScanInfo中的属性名称保持一致
     */
    private String attributeName;
    /**
     * 和业务调研参数保持一致
     */
    private String parameterKey;



    public static Set<String> allParameterKey() {
        Set<String> keys = new HashSet<>();
        DetectBusinessParams[] values = DetectBusinessParams.values();
        for (DetectBusinessParams value : values) {
            keys.add(value.getParameterKey().replace("--", ""));
        }
        return keys;
    }


    public static Set<String> allAttributeName() {
        Set<String> attributeNames = new HashSet<>();
        DetectBusinessParams[] values = DetectBusinessParams.values();
        for (DetectBusinessParams value : values) {
            attributeNames.add(value.getAttributeName());
        }
        return attributeNames;
    }


    /**
     *  <p>构建器动态参数元信息帮助工具</p>
     * @since 20250218
     * @author yue.geng
     */
    public static class ParamMetaHelper{


        /**
         *  <p>构建器动态参数元信息帮助工具</p>
         * @since 20250218
         * @author yue.geng
         * @param category 动态参数分类
         * @param detectBusinessParams 当前动态参数枚举类字节码
         */
        public static List<Field> allParamsWithCategory(Class<? extends DetectBusinessParams> detectBusinessParams,String category){
            if(null==detectBusinessParams){
                return Collections.unmodifiableList(new ArrayList<>(0));
            }
            if(category==null){
                return Collections.unmodifiableList(new ArrayList<>(0));
            }
           if(category.isEmpty()){
               return Collections.unmodifiableList(new ArrayList<>(0));
           }
           List<Field> fieldList = new ArrayList<>(10);
          Arrays.stream(DetectBusinessParams.class.getFields()).
                   forEach(field -> {
                           field.setAccessible(true);
                           ParamMeta annotation = field.getAnnotation(ParamMeta.class);
                       if(null!=annotation&&!annotation.category().isEmpty() && annotation.category().equals(category)){
                           fieldList.add(field);
                       };
                   });
           return Collections.unmodifiableList(fieldList);
        }

    }



}
