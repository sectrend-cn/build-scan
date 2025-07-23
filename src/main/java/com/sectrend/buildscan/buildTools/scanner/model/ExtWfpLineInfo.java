package com.sectrend.buildscan.buildTools.scanner.model;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/1/2 9:33
 **/
@Data
public class ExtWfpLineInfo {
    AtomicInteger totalLineCount = new AtomicInteger(0);

    AtomicInteger matchedLineCount = new AtomicInteger(0);

    AtomicInteger fileCount = new AtomicInteger(0);

    String selfResearchRate;

    Integer matched = 50;

    ConcurrentHashMap<String, AtomicInteger> fileLineCountMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, AtomicInteger> dirLineCountMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, AtomicInteger> softWarePackageLineCountMap = new ConcurrentHashMap<>();

    // jar、aar类型的路径Hash对应关系
    ConcurrentHashMap<String, String> customComponentPackageHashMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, AtomicInteger> dirFileCountMap = new ConcurrentHashMap<>();

    public void incrDirFileCountBy(String key, int delta) {
        dirFileCountMap.computeIfAbsent(key, path -> new AtomicInteger(0)).getAndAdd(delta);
    }

    public void incrTotalLineCountBy(int delta) {
        totalLineCount.getAndAdd(delta);
    }

    public void incrTotalFileCountBy(int delta) {
        fileCount.getAndAdd(delta);
    }

    public void incrMatchedCountBy(int delta) {
        matchedLineCount.getAndAdd(delta);
    }

    public void incrSoftWarePackageLineCountBy(String key, int delta) {
        softWarePackageLineCountMap.computeIfAbsent(key, path -> new AtomicInteger(0)).getAndAdd(delta);
    }

}
