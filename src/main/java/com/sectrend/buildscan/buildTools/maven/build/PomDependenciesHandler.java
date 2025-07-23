package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.enums.VariableUseType;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomDependenciesHandler extends DefaultHandler {

    private static final Set<String> ONLY_DEPENDENCIES = Sets.newHashSet("dependency");

    private static final String PROPERTIES = "properties";

    private static final String DEPENDENCY_MANAGEMENT = "dependencyManagement";

    private static final Set<String> WITH_PLUGINS = Sets.newHashSet("dependency", "plugin");

    private final ForeignIdFactory foreignIdFactory;

    private final boolean includePluginDependencies;

    private boolean parsingDependency;

    private boolean parsingGroup;

    private boolean parsingArtifact;

    private boolean parsingVersion;
    private boolean parsingProperties;
    private boolean parsingScope;

    private String group;

    private String artifact;

    private String version;

    private String scope;

    private Boolean isDependencyManagement = false;

    private String str = "\\$\\{(.+?)\\}";

    private static final String DEPENDENCY_SEPARATOR = "___";

    private final List<Dependency> dependencies = new ArrayList<>();

    private final Map<String, String> versionVariableMap;

    private final Map<String, String> dependencyManagementMap;

    private String qName;

    private Locator locator;
    private int line = 0;

    public PomDependenciesHandler(ForeignIdFactory foreignIdFactory, boolean includePluginDependencies, Map<String, String> dependencyManagementMap, Map<String, String> versionVariableMap) {
        this.foreignIdFactory = foreignIdFactory;
        this.includePluginDependencies = includePluginDependencies;
        this.dependencyManagementMap = dependencyManagementMap;
        this.versionVariableMap = versionVariableMap;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if (isDependencyManagementName(qName)) {
            this.isDependencyManagement = true;
        } else if (isDependencyQName(qName)) {
            this.parsingDependency = true;
        } else if (isPropertiesQName(qName)) {
            this.parsingProperties = true;
        } else if (this.parsingDependency && "groupId".equals(qName)) {
            parsingGroup();
        } else if (this.parsingDependency && "artifactId".equals(qName)) {
            parsingArtifact();
            line = locator.getLineNumber();
        } else if (this.parsingDependency && "version".equals(qName)) {
            parsingVersion();
        } else if (this.parsingDependency && "scope".equals(qName)) {
            parsingScope();
        } else if (this.parsingProperties) {
            parsingProperties(qName);
        }
    }

    public void replaceDependenciesVariable() {
        for (Dependency dep : dependencies) {
            String varuableType = dep.getVariableUseType();
            if (varuableType != null && varuableType.equals(VariableUseType.VERSION.getValue())) {
                String completeVariableName = dep.getCompleteVariableName();
                String versionValue = isRegularJudgment(dep.getCompleteVariableName());
                String undefined = "undefined";
                if (versionValue.contains("$")) {
                    //变量替换未成功
                    int index = versionValue.indexOf("_|");
                    if (index < 0) {
                        versionValue = undefined;
                    } else {
                        String str = versionValue.substring(0, index);
                        versionValue = versionValue.replace(str, undefined);
                    }
                }
                dep.setVersion(versionValue);
                dep.setCompleteVariableName(null);
                dep.setVariableUseType(null);
                ForeignId oldForeignId = dep.getForeignId();
                ForeignId foreignId = this.foreignIdFactory.createMavenForeignId(oldForeignId.getGroup(), oldForeignId.getName(), versionValue);
                dep.setForeignId(foreignId);
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (isDependencyManagementName(qName)) {
            this.isDependencyManagement = false;
        } else if (isDependencyQName(qName)) {
            //删除换行符和空格，以及制表符
            if (StringUtils.isNotBlank(group)) {
                group = group.replaceAll("[\\r\\n\\t ]+", "");
            }
            if (StringUtils.isNotBlank(artifact)) {
                artifact = artifact.replaceAll("[\\r\\n\\t ]+", "");
            }
            if (StringUtils.isNotBlank(version)) {
                version = version.replaceAll("[\\r\\n\\t ]+", "");
            }
            this.parsingDependency = false;
            group = isRegularJudgment(group);
            artifact = isRegularJudgment(artifact);

            String dependencyKey = group + DEPENDENCY_SEPARATOR + artifact;
            boolean isVersionVariable = false;
            String completeVariableName = null;
            if (StringUtils.isNotBlank(version)) {
                version = isRegularJudgment(version);
                if (version.startsWith("@")) {
                    version = "undefined";
                } else if (version.contains("$")) {
                    isVersionVariable = true;
                }
                if (version.contains("(") || version.contains(")") || version.contains("[") || version.contains("]")) {
                    String replace = version.replace("(", "").replace(")", "").replace("[", "").replace("]", "");
                    String[] split = replace.split(",");
                    String temp = "";
                    for (String s : split) {
                        if (StringUtils.isNotBlank(s) && StringUtils.isBlank(temp)) {
                            temp = s;
                            version = temp;
                        }
                    }
                }
            } else {
                String versionValue = dependencyManagementMap.get(dependencyKey);
                if (StringUtils.isNotBlank(versionValue)) {
                    version = versionValue;
                } else {
                    version = "undefined";
                }
            }
            if (isDependencyManagement) {
                dependencyManagementMap.put(dependencyKey, version);
            }
            if (StringUtils.isNotBlank(scope)) {
                version = version + "_|_" + scope;
            } else {
                version = version + "_|_compile";
            }

            if (StringUtils.isBlank(group) || group.startsWith("$") || group.startsWith("@")) {
                return;
            }
            if (StringUtils.isBlank(artifact) || artifact.startsWith("$") || artifact.startsWith("@")) {
                return;
            }

            ForeignId foreignId = this.foreignIdFactory.createMavenForeignId(this.group, this.artifact, this.version);
            Dependency dep = new Dependency(this.artifact, this.version, foreignId);
            if (isVersionVariable) {
                dep.setCompleteVariableName(version);
                dep.setVariableUseType(VariableUseType.VERSION.getValue());
            }
            dep.setLine(line);
            line = 0;
            this.dependencies.add(dep);
            reset();
        } else if (isPropertiesQName(qName)) {
            this.parsingProperties = false;
            for (Map.Entry<String, String> entry : versionVariableMap.entrySet()) {
                String value = entry.getValue();
                String key = entry.getKey();
                Pattern p = Pattern.compile(str);
                Matcher m = p.matcher(value);
                if (m.find()) {
                    String mm = m.group(0);
                    for (String s : versionVariableMap.keySet()) {
                        if (mm.contains(s)) {
                            try {
                                this.versionVariableMap.put(key, value.replaceAll(str, versionVariableMap.get(s)));
                            } catch (Exception e) {
                                this.versionVariableMap.put(key, value.replaceAll(str, "undefined"));
                            }
                        }
                    }
                }
            }
        } else {
            parsingNothingImportant();
        }
    }

    private void reset() {
        version = "";
        artifact = "";
        group = "";
        scope = "";
    }

    private String isRegularJudgment(String beforeStr) {
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(beforeStr);

        if (m.find()) {
            String mm = m.group(0);
            for (String s : versionVariableMap.keySet()) {
                if (mm.contains(s)) {
                    return beforeStr.replaceAll(str, versionVariableMap.get(s));
                }
            }
        }
        return beforeStr;
    }


    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        if (this.parsingGroup) {
            if (StringUtils.isBlank(group))
                this.group = new String(ch, start, length);
        } else if (this.parsingArtifact) {
            if (StringUtils.isBlank(artifact))
                this.artifact = new String(ch, start, length);
        } else if (this.parsingVersion) {
            if (StringUtils.isBlank(version))
                this.version = new String(ch, start, length);
        } else if (this.parsingScope) {
            if (StringUtils.isBlank(scope))
                this.scope = new String(ch, start, length);
        } else if (this.parsingProperties) {
            String s = new String(ch, start, length);
            if (!s.contains("\n") && !this.versionVariableMap.containsKey(this.qName)) {
                this.versionVariableMap.put(this.qName, s);
            }
        }
    }

    public List<Dependency> getDependencies() {
        return this.dependencies;
    }

    private boolean isDependencyQName(String qName) {
        if (this.includePluginDependencies)
            return WITH_PLUGINS.contains(qName);
        return ONLY_DEPENDENCIES.contains(qName);
    }

    private boolean isPropertiesQName(String qName) {
        return PROPERTIES.equals(qName);
    }


    private boolean isDependencyManagementName(String qName) {
        return DEPENDENCY_MANAGEMENT.equals(qName);
    }

    private void parsingNothingImportant() {
        this.parsingGroup = false;
        this.parsingArtifact = false;
        this.parsingVersion = false;
        this.parsingScope = false;
    }

    private void parsingGroup() {
        parsingNothingImportant();
        this.parsingGroup = true;
    }

    private void parsingArtifact() {
        parsingNothingImportant();
        this.parsingArtifact = true;
    }

    private void parsingVersion() {
        parsingNothingImportant();
        this.parsingVersion = true;
    }

    private void parsingScope() {
        parsingNothingImportant();
        this.parsingScope = true;
    }

    private void parsingProperties(String qName) {
        if (!isDependencyQName(qName)) {
            this.qName = qName;
        }
    }
}
