package com.sectrend.buildscan.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.impl.*;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.factory.ScannableFactory;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.finder.impl.ScanFileFinder;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;

import java.util.Collections;


public class RunBeanConfiguration {

    public static GsonBuilder createDefaultGsonBuilder() {
        return (new GsonBuilder())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    }

    public static Gson createDefaultGson() {
        return createDefaultGsonBuilder().create();
    }

    public static FileFinder simpleFileFinder;

    public static ForeignIdFactory foreignIdFactory;

    public static FileFinder detectFileFinder;

    public static Gson gson;

    public static SimpleExecutableFinder simpleExecutableFinder;

    public static SimpleSystemExecutableFinder simpleSystemExecutableFinder;

    public static SimpleExecutableResolver simpleExecutableResolver;

    public static ExecutableRunner executableRunner;

    public static ScannableFactory scannableFactory;

    public static SimpleLocalExecutableFinder simpleLocalExecutableFinder;


    static {
        simpleFileFinder = new SimpleFileFinder();
        foreignIdFactory = new ForeignIdFactory();
        detectFileFinder = new ScanFileFinder(Collections.emptyList());
        gson = new Gson();
        simpleExecutableFinder = SimpleExecutableFinder.forCurrentOperatingSystem(simpleFileFinder);
        simpleLocalExecutableFinder = new SimpleLocalExecutableFinder(simpleExecutableFinder);
        simpleSystemExecutableFinder = new SimpleSystemExecutableFinder(simpleExecutableFinder);
        simpleExecutableResolver = new SimpleExecutableResolver(null, simpleLocalExecutableFinder, simpleSystemExecutableFinder);
        executableRunner = ScanExecutableRunner.newInfo();
        scannableFactory = new ScannableFactory(detectFileFinder, executableRunner, foreignIdFactory, createDefaultGson());

    }


}
