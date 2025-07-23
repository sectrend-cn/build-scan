package com.sectrend.buildscan.buildTools.go.govendor.nonbuild;

import com.google.gson.Gson;
import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.buildTools.go.govendor.nonbuild.model.GoVendorJson;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.model.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoVendorJsonAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ForeignIdFactory foreignIdFactory;
    private final Gson gson = new Gson();

    public GoVendorJsonAnalyzer(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    public DependencyGraph analyzeVendorJson(String vendorJsonContents) {
        MutableMapDependencyGraph graph = new MutableMapDependencyGraph();
        GoVendorJson goVendorJsonData = gson.fromJson(vendorJsonContents, GoVendorJson.class);
        logger.info("The analyzed goVendor json is: " + goVendorJsonData);
        goVendorJsonData.getPackages().forEach(packageData -> {
            if (StringUtils.isBlank(packageData.getPath())) {
                logger.warn("One of the package data of the goVendor json misses path, the revision is: " + packageData.getRevision());
                return;
            }
            if (StringUtils.isBlank(packageData.getRevision())) {
                logger.warn("One of the package data of the goVendor json misses revision, the path is: " + packageData.getPath());
                return;
            }
            ForeignId dependencyForeignId = foreignIdFactory.createNameVersionForeignId(Supplier.GOLANG, packageData.getPath(), packageData.getRevision());
            Dependency dependency = new Dependency(packageData.getPath(), packageData.getRevision(), dependencyForeignId);
            logger.info("dependency: " +  dependency.getForeignId().toString());
            //govendor的依赖都是平铺的，没有父子依赖关系。这里只需要全部添加进去就行
            graph.addChildToRoot(dependency);
        });
        return graph;
    }
}
