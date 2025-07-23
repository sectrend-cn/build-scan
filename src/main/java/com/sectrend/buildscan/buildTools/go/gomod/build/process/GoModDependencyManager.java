package com.sectrend.buildscan.buildTools.go.gomod.build.process;

import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoListUJsonData;
import com.sectrend.buildscan.buildTools.go.gomod.build.model.SubstituteData;
import com.sectrend.buildscan.constant.CommonConstants;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.Supplier;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoModDependencyManager {
    private final Map<String, Dependency> modulesDependencyMap;

    private final ForeignIdFactory foreignIdFactory;

    public GoModDependencyManager(List<GoListUJsonData> allModules, ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
        modulesDependencyMap = convertModulesToDependencies(allModules);
    }

    private Map<String, Dependency> convertModulesToDependencies(List<GoListUJsonData> allModules) {
        Map<String, Dependency> dependencyMap = new HashMap<>();

        allModules.forEach(mod -> {
            String dependencyName = Optional.ofNullable(mod.getReplace())
                    .map(SubstituteData::getPath)
                    .orElse(mod.getPath());
            if (StringUtils.isBlank(dependencyName)) {
                return;
            }
            String dependencyVersion = Optional.ofNullable(mod.getReplace())
                    .map(SubstituteData::getVersion)
                    .orElse(mod.getVersion());
            if (StringUtils.isNotBlank(dependencyVersion)) {
                String finalVersion = dependencyVersion;
                dependencyVersion = getVersionFromPattern(dependencyVersion, CommonConstants.SHA1_VERSION_PATTERN)
                        .orElseGet(() ->
                                getVersionFromPattern(finalVersion, CommonConstants.SHORT_SHA1_VERSION_PATTERN)
                                        .orElse(finalVersion)
                        );
                if (dependencyVersion.endsWith(CommonConstants.INCOMPATIBLE_SUFFIX)) {
                    // incompatible的后缀需要被移除
                    dependencyVersion = dependencyVersion.substring(0, dependencyVersion.length() - CommonConstants.INCOMPATIBLE_SUFFIX.length());
                }
            }
            dependencyMap.put(mod.getPath(), convertToDependency(dependencyName, dependencyVersion));
        });
        return dependencyMap;
    }

    private Optional<String> getVersionFromPattern(String version, Pattern versionPattern) {
        Matcher matcher = versionPattern.matcher(version);
        if (matcher.matches()) {
            return Optional.ofNullable(StringUtils.trim(matcher.group(1)));
        }
        return Optional.empty();
    }

    public Dependency getDependencyForModule(String moduleName) {
        return Optional.ofNullable(moduleName)
                .map(name -> modulesDependencyMap.computeIfAbsent(name, key -> convertToDependency(key, null)))
                .orElseThrow(() -> new IllegalArgumentException("Module name cannot be null"));
    }

    private Dependency convertToDependency(String name, String version) {
        return new Dependency(name, version, foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, name, version));
    }

}
