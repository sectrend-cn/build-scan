package com.sectrend.buildscan.factory;

import com.google.gson.Gson;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.go.gomod.build.GoModCliScanExecutor;
import com.sectrend.buildscan.buildTools.go.gomod.build.GoModCliScannable;
import com.sectrend.buildscan.buildTools.go.gomod.nonbuild.GoModGraphAnalyzer;
import com.sectrend.buildscan.buildTools.go.gomod.nonbuild.GoModAnalyzeScanExecutor;
import com.sectrend.buildscan.buildTools.go.gomod.nonbuild.GoModAnalyzeScannable;
import com.sectrend.buildscan.buildTools.go.gomod.build.analyze.GoGraphAnalyzer;
import com.sectrend.buildscan.buildTools.go.gomod.build.process.GoModGraphGenerator;
import com.sectrend.buildscan.buildTools.maven.build.MavenCliScanExecutor;
import com.sectrend.buildscan.buildTools.maven.build.MavenDependencyLocationPackager;
import com.sectrend.buildscan.buildTools.maven.build.MavenPomScannable;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipfileLockDependencyConverter;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipfileLockScanExecutor;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipfileLockScannable;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipfileLockScannableParams;
import com.sectrend.buildscan.enums.BuildType;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.executable.finder.MavenFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import com.sectrend.buildscan.handler.ExtractHandler;
import com.sectrend.buildscan.handler.GitExtractHandler;
import com.sectrend.buildscan.handler.GoDepExtractHandler;
import com.sectrend.buildscan.handler.GoModExtractHandler;
import com.sectrend.buildscan.handler.GoVendorExtractHandler;
import com.sectrend.buildscan.handler.MavenExtractHandler;
import com.sectrend.buildscan.handler.MavenResultAnalyzeExtractHandler;
import com.sectrend.buildscan.handler.PipExtractHandler;
import com.sectrend.buildscan.handler.PipenvExtractHandler;
import com.sectrend.buildscan.utils.ExecutableVersionLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScannableFactory {
    private final FileFinder fileFinder;
    private final ExecutableRunner executableRunner;

    private final ForeignIdFactory foreignIdFactory;
    private final Gson gson;

    private final ExecutableVersionLogger executableVersionLogger;

    public static Map<String, ExtractHandler> handlerMap = new ConcurrentHashMap<>();
    public static Map<String, ExtractHandler> textHandlerMap = new ConcurrentHashMap<>();
    public static GitExtractHandler gitExtractHandler;
    static {

        handlerMap.put(BuildType.GOMOD_BUILD.getBuildType(), new GoModExtractHandler());
        handlerMap.put(BuildType.MVN_BUILD.getBuildType(),new MavenExtractHandler());
        handlerMap.put(BuildType.PIPENV_BUILD.getBuildType(),new PipenvExtractHandler());
        handlerMap.put(BuildType.PIP_BUILD.getBuildType(), new PipExtractHandler());

        handlerMap.put(BuildType.GODEP_BUILD.getBuildType(), new GoDepExtractHandler());

        handlerMap.put(BuildType.GO_VENDOR_BUILD_TYPE.getBuildType(), new GoVendorExtractHandler());

        textHandlerMap.put(BuildType.MVN_TEXT_BUILD.getBuildType(), new MavenResultAnalyzeExtractHandler());

        gitExtractHandler = new GitExtractHandler();
    }

    public ScannableFactory(FileFinder fileFinder, ExecutableRunner executableRunner, ForeignIdFactory foreignIdFactory, Gson gson) {
        this.fileFinder = fileFinder;
        this.executableRunner = executableRunner;
        this.foreignIdFactory = foreignIdFactory;
        this.gson = gson;
        this.executableVersionLogger = new ExecutableVersionLogger(executableRunner);
    }


    public MavenPomScannable createMavenPomScannable(ScannableEnvironment environment, MavenFinder mavenFinder) {
        return new MavenPomScannable(environment, this.fileFinder, mavenFinder, mavenCliScanExecutor());
    }

    private MavenCliScanExecutor mavenCliScanExecutor() {
        return new MavenCliScanExecutor(this.executableRunner, mavenCodeLocationPackager());
    }

    private MavenDependencyLocationPackager mavenCodeLocationPackager() {
        return new MavenDependencyLocationPackager(this.foreignIdFactory);
    }


    public PipfileLockScannable createPipfileLockScannable(ScannableEnvironment environment, PipfileLockScannableParams pipfileLockScannableParams) {
        return new PipfileLockScannable(environment,fileFinder,new PipfileLockScanExecutor(gson,new PipfileLockDependencyConverter()), pipfileLockScannableParams);
    }
    /**
     * 获取handler
     * @param scanTypeName
     * @return
     */
    public static ExtractHandler getExtractHandler(String scanTypeName){
        return textHandlerMap.get(scanTypeName);
    }

    /**
     * gomod构建
     * @param scannableEnvironment
     * @param simpleFileFinder
     * @param simpleExecutableResolver
     * @return
     */
    public Scannable createGoModScannable(ScannableEnvironment scannableEnvironment, SimpleFileFinder simpleFileFinder, SimpleExecutableResolver simpleExecutableResolver) {
    GoModGraphGenerator goModGraphGenerator = new GoModGraphGenerator(foreignIdFactory);
    return new GoModCliScannable(scannableEnvironment, simpleFileFinder, simpleExecutableResolver, new GoModCliScanExecutor(executableRunner, gson, new GoGraphAnalyzer(), foreignIdFactory, goModGraphGenerator));
    }

    /**
     * gomod非构建
     * @param scannableEnvironment
     * @param simpleFileFinder
     * @param goModGraphAnalyzer
     * @return
     */
    public Scannable createGoModAnalyzeScannable(ScannableEnvironment scannableEnvironment, SimpleFileFinder simpleFileFinder, GoModGraphAnalyzer goModGraphAnalyzer) {
        return new GoModAnalyzeScannable(scannableEnvironment, simpleFileFinder, new GoModAnalyzeScanExecutor(goModGraphAnalyzer));
    }

}
