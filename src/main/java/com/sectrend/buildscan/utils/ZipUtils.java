package com.sectrend.buildscan.utils;


import com.sectrend.buildscan.compress.CompressExtractor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipUtils {

    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);


    public static File getZipFile(File sourceFile, String zipOutputDirectory){
        File file = null;

        try {
            logger.info("--------Compressing project files--------");
            file= fileToZip(sourceFile, zipOutputDirectory);
            logger.info("--------Project file compression completed--------");
        } catch (Exception e) {
            logger.error("Failed to compress file", e);
        }
        return file;
    }

    public static File getZipFileByList(List<String> filePathList, String zipOutputDirectory){
        File file = null;
        File zipFile = null;
        try {
            logger.info("--------Zip binary files start--------");
            file = new File(zipOutputDirectory+"");
            file.mkdirs();
            zipFile = new File(zipOutputDirectory +File.separator + "binaryList.zip");

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            for (String filePath:filePathList){
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    //创建ZIP实体，并添加进压缩包
                    String relativePath = getRelativePath(file, filePath);
                    ZipEntry zipEntry = new ZipEntry(relativePath);
//                    ZipEntry zipEntry = new ZipEntry(filePath);
                    zos.putNextEntry(zipEntry);
                    byte[] bufs = new byte[1024 * 10];
                    //读取待压缩的文件并写进压缩包里
                    fis = new FileInputStream(filePath);
                    bis = new BufferedInputStream(fis, 1024 * 10);
                    int read = 0;
                    while ((read = bis.read(bufs, 0, 1024 * 10)) != -1) {
                        zos.write(bufs, 0, read);
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }finally {
                    if(bis!=null ){
                        bis.close();
                        fis.close();
                    }
                }
            }

            zos.close();
            fos.close();
            logger.info("--------Zip binary files completed--------");
        } catch (Exception e) {
            logger.error("Failed to compress binary file", e);
        }
        return zipFile;
    }

    private static String getRelativePath(File baseDirectory, String filePath) {
        File absoluteFile = new File(filePath);
        String basePath = baseDirectory.getAbsolutePath() + File.separatorChar;
        String absolutePath = absoluteFile.getAbsolutePath();

        if (!absolutePath.startsWith(basePath)) {
            throw new IllegalArgumentException("File is not located under the base directory: " + filePath);
        }

        return absolutePath.substring(basePath.length()).replace("\\", "/");
    }

    /**
     * sourceFile一定要是文件夹
     * 默认会在同目录下生成zip文件
     *
     * @param sourceFile 需要压缩文件的地址
     * @param zipOutputDirectory 文件压缩位置
     * @throws Exception
     */
    public static File  fileToZip(File sourceFile, String zipOutputDirectory) throws Exception {

        if (!sourceFile.exists() || !sourceFile.isDirectory()) {
            throw new RuntimeException("The file/folder does not exist");
        }

        Set<String> addedEntries = new HashSet<>();

        logger.info("File compression location" + zipOutputDirectory + File.separator + sourceFile.getName());
        File file = new File(zipOutputDirectory+"");
        file.mkdirs();
        File zipFile = new File(zipOutputDirectory +File.separator + sourceFile.getName() + ".zip");

        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

        fileToZip(zos, sourceFile, "", addedEntries);
        zos.close();
        fos.close();
        return zipFile;
    }


    private static void fileToZip(ZipOutputStream zos, File sourceFile, String path, Set<String> addedEntries) throws Exception {

        if (null == sourceFile || Files.isSymbolicLink(sourceFile.toPath())) {
            return;
        }
        //如果是文件夹只创建zip实体即可，如果是文件，创建zip实体后还要读取文件内容并写入
        if (sourceFile.isDirectory()) {
            path = path + sourceFile.getName() + "/";
            if (addedEntries.contains(path)) {
                return;
            }
            addedEntries.add(path);
            ZipEntry zipEntry = new ZipEntry(path);
            zos.putNextEntry(zipEntry);

            File[] files = sourceFile.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file : files) {
                fileToZip(zos, file, path, addedEntries);
                String compressPath = CompressExtractor.rootCompressPathMap.get(file.getAbsolutePath());
                if (StringUtils.isNotBlank(compressPath)) {
                    fileToZip(zos, new File(compressPath), path, addedEntries);
                }
            }
        } else {
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                String filePath = path + sourceFile.getName();
                if (addedEntries.contains(filePath)) {
                    return;
                }
                addedEntries.add(filePath);
                //创建ZIP实体，并添加进压缩包
                ZipEntry zipEntry = new ZipEntry(filePath);
                zos.putNextEntry(zipEntry);
                byte[] bufs = new byte[1024 * 10];
                //读取待压缩的文件并写进压缩包里
                fis = new FileInputStream(sourceFile);
                bis = new BufferedInputStream(fis, 1024 * 10);
                int read = 0;
                while ((read = bis.read(bufs, 0, 1024 * 10)) != -1) {
                    zos.write(bufs, 0, read);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
            } finally {
                if(bis!=null ){
                    bis.close();
                    fis.close();
                }
            }
        }
    }
}

