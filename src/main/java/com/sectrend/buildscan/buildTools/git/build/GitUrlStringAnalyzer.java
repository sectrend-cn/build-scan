package com.sectrend.buildscan.buildTools.git.build;

import java.net.MalformedURLException;

public class GitUrlStringAnalyzer {

    public String getRepositoryName(String remoteUrl) throws MalformedURLException {
        // 使用正则表达式分割 URL，获取组织和仓库名
        String[] urlParts = remoteUrl.split("[/:]");
        if(urlParts.length < 2){
            // 如果无法解析 URL，抛出异常
            throw new MalformedURLException("Unable to extract repository name from URL.");
        }

        String organization = urlParts[urlParts.length - 2];
        String repository = urlParts[urlParts.length - 1];

        // 构造仓库名称，去除前后多余的斜杠和 .git 后缀
        String repositoryName = organization + "/" + repository;
        return repositoryName.replaceFirst("^/+", "").replaceAll("/+$", "").replaceAll("\\.git$", "");
    }
}
