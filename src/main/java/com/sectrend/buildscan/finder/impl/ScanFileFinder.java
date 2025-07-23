
package com.sectrend.buildscan.finder.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ScanFileFinder extends SimpleFileFinder {
   private final Logger logger = LoggerFactory.getLogger(ScanFileFinder.class);

   private final List<String> excludedFileNames;

   private List<String> fileNames;


   public ScanFileFinder(final List<String> excludedFileNames) {
      this.excludedFileNames = excludedFileNames;
   }

   public List findFiles(final File directoryToSearch, final List<String> filenamePatterns, final int depth, final boolean findInsideMatchingDirectories) {
      return super.findFiles(directoryToSearch, filenamePatterns, depth, findInsideMatchingDirectories).stream().filter((file) -> !this.excludedFileNames.contains(file.getName())).collect(Collectors.toList());
   }

   @Override
   public List<String> getFilePaths() {
      return fileNames;
   }

   @Override
   public void setFilePaths(List<String> filePaths) {
      this.fileNames = filePaths;
   }
}
