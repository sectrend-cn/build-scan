package com.sectrend.buildscan.buildTools.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

public class WfpThread implements Callable<StringBuilder> {

    private static final Logger log = LoggerFactory.getLogger(WfpThread.class);
    private String dir;
    private String taskDir;
    private Boolean hpsm;

    public WfpThread(String dir,String taskDir, Boolean hpsm){
        this.dir = dir;
        this.taskDir = taskDir;
        this.hpsm = hpsm;
    }


    @Override
    public StringBuilder call() throws Exception {
       StringBuilder wfp = new StringBuilder();
        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                File file = dir.toFile();
                String name = file.getName();
                if (BlacklistRules.filteredDirs(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
               /* if (BlacklistRules.filteredDirExt(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }*/

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
             /*   if (!Files.isDirectory(file) && !BlacklistRules.hasBlacklistedExt(file.toString()) && !BlacklistRules.filteredFiles(file.getFileName().toString())) {
                    try {
                        File projectDir = new File(taskDir);
                        String projectName = projectDir.getName();
                        String projectPath = projectDir.getAbsolutePath();
                        String wfpString = Winnowing.wfpForFile(projectName + file.toString().replace(projectPath, ""), file.toString(), hpsm);
                        //String wfpString = Winnowing.wfpForFile(file.toString(), file.toString());
                        if (wfpString != null && !wfpString.isEmpty())
                            wfp.append(wfpString);
                    } catch (Exception e) {
                        log.warn("Exception while creating wfp for file: {}", file, e);
                    }
                }*/
                return FileVisitResult.CONTINUE;
            }
        });
        return wfp;

    }
}
