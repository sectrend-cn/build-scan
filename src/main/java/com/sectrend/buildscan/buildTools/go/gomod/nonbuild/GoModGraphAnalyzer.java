package com.sectrend.buildscan.buildTools.go.gomod.nonbuild;

import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class GoModGraphAnalyzer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ForeignIdFactory foreignIdFactory;

    public GoModGraphAnalyzer(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    /**
     * 分割Go依赖 并追加值
     *
     * @param dependencyPart
     * @return
     */
    public Dependency analyzeDependency(String dependencyPart, Map<String, String> pathVersionMap) {
        if (dependencyPart.contains("@")) {
            String[] parts = dependencyPart.split("@");
            if (parts.length != 2) {
                this.logger.warn("Unknown graph dependency format, using entire line as name: " + dependencyPart);
                return new Dependency(dependencyPart, this.foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, dependencyPart, null));
            }
            String name = parts[0];
            String version = parts[1];
            if (Objects.nonNull(pathVersionMap)) {
                String replaceVersion = pathVersionMap.get(parts[0]);
                if (StringUtils.isNotBlank(replaceVersion)) {
                    version = replaceVersion;
                }
            }
            return new Dependency(name, version, this.foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, name, version));
        }
        return new Dependency(dependencyPart, this.foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, dependencyPart, null));
    }


}
