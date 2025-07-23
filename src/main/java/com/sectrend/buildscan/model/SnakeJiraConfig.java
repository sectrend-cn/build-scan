package com.sectrend.buildscan.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/7/26 9:58
 **/
@Data
public class SnakeJiraConfig {

    private String jira_project_id;

    private String jira_project_key;

    private String jira_project_name;



    private String username;

    private Integer issue_limit;

    private String issue_type;

    private String issue_type_name;


    private Map<String, String> risk_id_map = new HashMap<>();

    private Map<String, String> risk_name_map = new HashMap<>();


    private List<String> license_risks = new ArrayList<>();

    private List<String> vul_risks = new ArrayList<>();
}
