package com.sectrend.buildscan.model;

import lombok.Data;

import java.util.Properties;

/**
 * 构建业务参数
 * 命名遵循如下规则
 * 根据调研出来的参数--scan.gradle.build.command 取业务参数项中构建器的命名 gradle + Argument
 * 如：
 * --scan.pip.only.project.tree 是 pipArgument
 * --scan.pipenv.path 是 pipenvArgument
 *
 * @author: Jimmy
 * @date: 2025-01-15 16:33:03
 */
@Data
public class DetectProperties {

//    /**
//     * gradle 构建参数
//     */
//    private Properties gradleArgument;


    /**
     * maven 构建参数
     */
    private Properties mavenArgument;
//    /**
//     * yarn 构建参数
//     */
//    private Properties yarnArgument;
//
//
//    private Properties npmArgument;
//
//    private Properties goArgument;
//
//    private Properties rubyArgument;
//
//    /**
//     * conan 构建参数
//     */
//    private Properties conanArgument;
//    /**
//     * pub 构造参数
//     */
//    private Properties pubArgument;
//    /**
//     * PHP Packagist composer 构造参数
//     */
//    private Properties packagistArgument;
//    /**
//     * sbt构建参数
//     */
//    private Properties sbtArgument;
    /**
     * pip 参数
     */
    private Properties pipArgument;

    /**
     * pipenv 参数
     */
    private Properties pipenvArgument;
//
//    private Properties pnpmArgument;
//
//    /**
//     * bazel 参数
//     */
//    private Properties bazelArgument;
//
//    /**
//     * cpan 参数
//     */
//    private Properties cpanArgument;
//
//    private Properties bowerArgument;
//
//    private Properties lernaArgument;
//
//    /**
//     * hex
//     */
//    private Properties hexArgument;
//
//    private Properties bitbakeArgument;
//    private Properties condaArgument;
//
//
//    /**
//     * nuget 参数
//     */
//    private Properties nugetArgument;
//    /**
//     * pear 参数
//     */
//    private Properties pearArgument;
//
//    /**
//     * clang 参数
//     */
//    private Properties clangArgument;

}
