package com.sectrend.buildscan.buildTools.pipenv.build;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.executable.finder.PipenvFinder;
import com.sectrend.buildscan.executable.finder.PythonFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

public class PipenvScannable extends Scannable {

    private final PipenvScannableParams pipenvScannableParams;

    private final FileFinder fileFinder;

    private final PythonFinder pythonFinder;

    private final PipenvFinder pipenvFinder;

    private final PipenvScanExecutor pipenvScanExecutor;

    private File pythonExe;

    private File pipenvExe;

    private File setupFile;
    private int depth;

    public PipenvScannable(ScannableEnvironment environment, PipenvScannableParams pipenvScannableParams, FileFinder fileFinder, PythonFinder pythonFinder, PipenvFinder pipenvFinder, PipenvScanExecutor pipenvScanExecutor) {
        super(environment);
        this.pipenvScannableParams = pipenvScannableParams;
        this.fileFinder = fileFinder;
        this.pipenvFinder = pipenvFinder;
        this.pipenvScanExecutor = pipenvScanExecutor;
        this.pythonFinder = pythonFinder;
    }

    public ScannableResult exeFind() throws ScannableException {
        this.pythonExe = this.pythonFinder.findPython(this.environment);
        if (this.pythonExe == null){
            return (ScannableResult) new FailedScannableResult();
        }
        this.pipenvExe = this.pipenvFinder.findPipenv(this.environment);
        if (this.pipenvExe == null){
            return (ScannableResult) new FailedScannableResult();
        }
        this.setupFile = this.fileFinder.findFile(this.environment.getDirectory(), "setup.py");
        return (ScannableResult)new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        File pipfile = this.fileFinder.findFile(this.environment.getDirectory(), "Pipfile");
        File pipfileDotLock = this.fileFinder.findFile(this.environment.getDirectory(), "Pipfile.lock");
        if (pipfile != null || pipfileDotLock != null){
            return (ScannableResult)new PassedScannableResult();
        }
        return (ScannableResult) new FailedScannableResult();
    }

    private void getDepth(File directory) {
        depth++;
        //获取目标文件下的文件夹或者文件对象
        File[] files = directory.listFiles();
        for (File file : files) {
            if(file.isDirectory()){
                getDepth(file);
            }
        }
    }


    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.pipenvScanExecutor.scanExecute(this.environment.getDirectory(), this.pythonExe, this.pipenvExe, this.setupFile, this.pipenvScannableParams.getPipenvProjectName().orElse(""), this.pipenvScannableParams.getPipenvProjectVersionName().orElse(""), this.pipenvScannableParams
                .isPipenvProjectTreeOnly());
    }
}
