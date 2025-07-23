package com.sectrend.buildscan.model;


import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DependencyInfo {


   private Map<String, List<String>> rootMap  = new HashMap<>();

   private Map<String, List<String>> relationshipsMap = new HashMap<>();

   private String projectName = "";

}
