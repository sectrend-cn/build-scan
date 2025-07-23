package com.sectrend.buildscan.buildTools.go.govendor.nonbuild;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

/**
 * @Author nike.xie
 * @Date 2024/11/13
 */
public class GoVendorScannable extends Scannable {

    private static final String VENDOR_JSON_FILENAME = "vendor.json";

    private final FileFinder fileFinder;
    private final GoVendorScanExecutor goVendorScanExecutor;

    private File vendorJson;

    public GoVendorScannable(ScannableEnvironment environment, FileFinder fileFinder, GoVendorScanExecutor goVendorScanExecutor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.goVendorScanExecutor = goVendorScanExecutor;
    }

    @Override
    public ScannableResult exeFind() throws ScannableException {
        return new PassedScannableResult();
    }

    @Override
    public ScannableResult fileFind() {
        this.vendorJson = this.fileFinder.findFile(this.environment.getDirectory(), VENDOR_JSON_FILENAME);
        if(vendorJson == null)
            return (ScannableResult) new FailedScannableResult();
        return new PassedScannableResult();
    }

    @Override
    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return goVendorScanExecutor.scanExecute(vendorJson);
    }


}
