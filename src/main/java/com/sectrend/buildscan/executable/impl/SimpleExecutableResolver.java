package com.sectrend.buildscan.executable.impl;

import cn.hutool.core.util.StrUtil;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.enums.DetectBusinessParams;
import com.sectrend.buildscan.executable.ExeResolverFunction;
import com.sectrend.buildscan.executable.finder.GitFinder;
import com.sectrend.buildscan.executable.finder.GoFinder;
import com.sectrend.buildscan.executable.finder.MavenFinder;
import com.sectrend.buildscan.executable.finder.PipFinder;
import com.sectrend.buildscan.executable.finder.PipenvFinder;
import com.sectrend.buildscan.executable.finder.PythonFinder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SimpleExecutableResolver implements MavenFinder, GitFinder, PythonFinder, PipenvFinder, PipFinder, GoFinder
{
    private final CachedExecutableResolverOptions executableResolverOptions;

    private final SimpleLocalExecutableFinder localExecutableFinder;

    private final SimpleSystemExecutableFinder systemExecutableFinder;

    private final Map<String, File> cached = new HashMap<>();

    private final Map<String, File> cachedExecutables = new HashMap<>();

    public SimpleExecutableResolver(CachedExecutableResolverOptions executableResolverOptions, SimpleLocalExecutableFinder localExecutableFinder, SimpleSystemExecutableFinder systemExecutableFinder) {
        this.executableResolverOptions = executableResolverOptions;
        this.localExecutableFinder = localExecutableFinder;
        this.systemExecutableFinder = systemExecutableFinder;
    }

    private File findLocalOrSystem(String localName, String systemName, ScannableEnvironment environment) {
        File local = this.localExecutableFinder.findExecutable(localName, environment.getDirectory());
        if (local != null)
            return local;
        return findCachedSystem(systemName);
    }

    private File findLocalOrSystem(String systemName, Path override) {
        if (override != null){
            File local = this.localExecutableFinder.findExecutable(systemName,override.toFile());
            if (local != null)
                return local;
        }
        return findCachedSystem(systemName);
    }

    private File findSystemOrLocal(String localName, String systemName, ScannableEnvironment environment) {
        File cachedSystem = findCachedSystem(systemName);
        if (cachedSystem != null) {
            return cachedSystem;
        }
        return this.localExecutableFinder.findExecutable(localName, environment.getDirectory());
    }

    private File findCachedSystem(String name) {
        if (!this.cached.containsKey(name)) {
            File found = this.systemExecutableFinder.findExecutable(name);
            this.cached.put(name, found);
        }
        return this.cached.get(name);
    }

    private File resolve(@Nullable String cacheKey, ExeResolverFunction... resolvers) throws ScannableException {
        File resolved = null;
        for (ExeResolverFunction resolver : resolvers) {
            resolved = resolver.resolve();
            if (resolved != null) {
                break;
            }
        }
        if (cacheKey != null) {
            cachedExecutables.put(cacheKey, resolved);
        }
        return resolved;
    }


    @Override
    public File findMaven(ScannableEnvironment paramScannableEnvironment) throws ScannableException {
        return findLocalOrSystem("mvnw", "mvn", paramScannableEnvironment);
    }

    @Override
    public File findPipenv(ScannableEnvironment paramScannableEnvironment) {
        Path path = null;
        String pathStr = paramScannableEnvironment.getArguments().getProperty(DetectBusinessParams.PIPENV_PATH.getAttributeName());
        if (StringUtils.isNotBlank(pathStr)) {
            path = Paths.get(pathStr);
        }
        return findLocalOrSystem("pipenv", path);
    }

    @Override
    public File findPython(ScannableEnvironment paramScannableEnvironment) {
        //String suffix = this.executableResolverOptions.isPython3() ? "3" : "";
        //String suffix = "3";
        Path path = null;
        String pathStr = paramScannableEnvironment.getArguments().getProperty(paramScannableEnvironment.getBuildType() + "PythonPath");
        if (StringUtils.isNotBlank(pathStr)) {
            path = Paths.get(pathStr);
        }
        return findLocalOrSystem("python",path);
    }


    @Override
    public File findGo(ScannableEnvironment paramScannableEnvironment) throws ScannableException {
        if (null != paramScannableEnvironment.getArguments()
                && !paramScannableEnvironment.getArguments().isEmpty()
                && paramScannableEnvironment.getArguments().containsKey(DetectBusinessParams.GO_PATH.getAttributeName())
                && !StrUtil.isEmpty(
                (String) paramScannableEnvironment.getArguments().getOrDefault(DetectBusinessParams.GO_PATH.getAttributeName(), ""))
        ) {
            Path path = null;
            String pathStr = paramScannableEnvironment.getArguments().getProperty(DetectBusinessParams.GO_PATH.getAttributeName());
            if (StringUtils.isNotBlank(pathStr)) {
                path = Paths.get(pathStr);
            }
            return findLocalOrSystem("go", path);
        }
        return findCachedSystem("go");
    }

    @Override
    public File findPip(ScannableEnvironment paramScannableEnvironment) {
        //String suffix = this.executableResolverOptions.isPython3() ? "3" : "";
        Path path = null;
        String pathStr = paramScannableEnvironment.getArguments().getProperty(DetectBusinessParams.PIP_PATH.getAttributeName());
        if (StringUtils.isNotBlank(pathStr)) {
            path = Paths.get(pathStr);
        }
        return findLocalOrSystem("pip",path);
    }

    @Override
    public File findGit() {
        return findCachedSystem("git");
    }

}