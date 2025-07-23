package com.sectrend.buildscan.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/7/26 9:55
 **/
@Data
public class ScanJiraConfig {

    private String jiraProjectId;

    private String jiraProjectKey;

    private String jiraProjectName;

    private String issueTypeName;

    private Map<String, String> riskNameMap = new HashMap<>();


    private String username;

    private Integer issueLimit;

    private String issueType;

    private Map<String, String> riskIdMap = new HashMap<>();

    private List<String> licenseRisks = new ArrayList<>();

    private List<String> vulRisks = new ArrayList<>();
}
