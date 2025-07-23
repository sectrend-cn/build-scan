package com.sectrend.buildscan.buildTools.go.govendor.nonbuild;

import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;


public class GoVendorScanExecutor {
    private final ForeignIdFactory foreignIdFactory;

    public GoVendorScanExecutor(ForeignIdFactory foreignIdFactory) {
        this.foreignIdFactory = foreignIdFactory;
    }

    public ScanResults scanExecute(File vendorJsonFile) {
        try {
            GoVendorJsonAnalyzer vendorJsonAnalyzer = new GoVendorJsonAnalyzer(foreignIdFactory);
            String vendorJsonContents = FileUtils.readFileToString(vendorJsonFile, StandardCharsets.UTF_8);

            DependencyGraph dependencyGraph = vendorJsonAnalyzer.analyzeVendorJson(vendorJsonContents);
            DependencyLocation dependencyLocation = new DependencyLocation(dependencyGraph);
            return new ScanResults.Builder().success(dependencyLocation).build();
        } catch (Exception e) {
            return new ScanResults.Builder().exception(e).build();
        }
    }
}
