package com.sectrend.buildscan.buildTools.pip;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.executable.finder.PipBuilderFinder;
import com.sectrend.buildscan.executable.finder.PipFinder;
import com.sectrend.buildscan.executable.finder.PythonFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PipBuildScannable extends Scannable {


    private final FileFinder fileFinder;

    private final PythonFinder pythonFinder;

    private final PipFinder pipFinder;

    private final PipBuilderFinder pipBuilderFinder;

    private final PipBuildScanExecutor pipBuildScanExecutor;

    private PipBuildScannableParams pipBuildScannableParams;

    private File pythonExe;

    private File pipBuilder;

    private File setupFile;

    private int depth;
    private Set<Path> requirementsFilePaths = new HashSet<>();

    public PipBuildScannable(ScannableEnvironment environment, FileFinder fileFinder, PythonFinder pythonFinder, PipFinder pipFinder, PipBuilderFinder pipBuilderFinder, PipBuildScanExecutor pipBuildScanExecutor, PipBuildScannableParams pipBuildScannableParams) {
        super(environment);
        this.fileFinder = fileFinder;
        this.pythonFinder = pythonFinder;
        this.pipFinder = pipFinder;
        this.pipBuilderFinder = pipBuilderFinder;
        this.pipBuildScanExecutor = pipBuildScanExecutor;
        this.pipBuildScannableParams = pipBuildScannableParams;
    }

    public ScannableResult exeFind() throws ScannableException {
        this.pythonExe = this.pythonFinder.findPython(this.environment);
        if (this.pythonExe == null)
            return (ScannableResult) new FailedScannableResult();
        File pipExe = this.pipFinder.findPip(this.environment);
        if (pipExe == null)
            return (ScannableResult) new FailedScannableResult();
        this.pipBuilder = this.pipBuilderFinder.findPipBuilder();
        if (this.pipBuilder == null)
            return null;
        return (ScannableResult)new PassedScannableResult();
    }

    public ScannableResult fileFind() {

       /* String fileName = "requirements.txt";
        getDepth(fileName, this.environment.getDirectory());*/
        //System.out.println("depth = " + depth);

        boolean present = pipBuildScannableParams.getPipProjectName().isPresent();
        String  pipProjectName = null;
        if (present) {
            pipProjectName = pipBuildScannableParams.getPipProjectName().get();
        }

        this.setupFile = this.fileFinder.findFile(this.environment.getDirectory(), "setup.py");

        if (CollectionUtils.isEmpty(pipBuildScannableParams.getRequirementsPaths())){
            File requirementsFile = this.fileFinder.findFile(this.environment.getDirectory(), "requirements.txt");
            if(requirementsFile != null && requirementsFile.exists()){
                this.requirementsFilePaths.add(Paths.get(requirementsFile.getPath()));
                this.pipBuildScannableParams = new PipBuildScannableParams(pipProjectName, requirementsFilePaths);
            }
        }

        boolean hasSetups = (this.setupFile != null);
        boolean hasRequirements = (this.pipBuildScannableParams.getRequirementsPaths() != null && this.pipBuildScannableParams.getRequirementsPaths().size() > 0);

        if (hasSetups || hasRequirements)
            return (ScannableResult)new PassedScannableResult();
        return (ScannableResult) new FailedScannableResult();


    }

    private void getDepth(String fileName, File directory) {
        depth++;
        //获取目标文件下的文件夹或者文件对象
        File[] files = directory.listFiles();
        for (File file : files) {
            if(file.isDirectory()){
                getDepth(fileName, file);
            }else {
                if(file.isFile() && file.getName().contains(fileName)){
                    requirementsFilePaths.add(Paths.get(file.getPath()));
                }
            }
        }
    }

    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.pipBuildScanExecutor.scanExecute(this.environment.getDirectory(), this.pythonExe, this.pipBuilder, this.setupFile, this.pipBuildScannableParams.getRequirementsPaths(), this.pipBuildScannableParams.getPipProjectName().orElse(""));
    }

        public ScanResults readTheFile(ScanEnvironment scanEnvironment) {
        return this.pipBuildScanExecutor.readTheFile(this.environment.getDirectory(), this.setupFile, this.pipBuildScannableParams);
    }
}
