/*
 * Copyright (C) 2018-2020 SECTREND.COM.CN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.sectrend.buildscan.buildTools.scanner;

import com.sectrend.buildscan.buildTools.scanner.model.*;
import com.sectrend.buildscan.compress.CompressExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Checksum;

import static com.sectrend.buildscan.buildTools.scanner.model.ExtFingerprintScanConstants.*;

/**
 * <p>
 * Winnowing Algorithm implementation for SECTREND.COM.CN.
 * </p>
 * <p>
 * This module implements an adaptation of the original winnowing algorithm by S. Schleimer, D. S. Wilkerson and A. Aiken as described in their
 * seminal article which can be found here: https://theory.stanford.edu/~aiken/publications/papers/sigmod03.pdf
 * </p>
 * <p>
 * The winnowing algorithm is configured using two parameters, the gram size and the window size.
 * </p>
 * <p>
 * For  and OSSKB the values need to be: - GRAM: 30 - WINDOW: 64
 * </p>
 * <p>
 * The result of performing the Winnowing algorithm is a string called WFP (Winnowing FingerPrint). A WFP contains optionally the name of the source
 * component and the results of the Winnowing algorithm for each file.
 * </p>
 * <p>
 * EXAMPLE output:
 * </p>
 * test-component.wfp
 *
 * <pre>
 * component=f9fc398cec3f9dd52aa76ce5b13e5f75,test-component.zip file=cae3ae667a54d731ca934e2867b32aaa,948,test/test-file1.c
 * 4=579be9fb
 * 5=9d9eefda,58533be6,6bb11697
 * 6=80188a22,f9bb9220
 * 10=750988e0,b6785a0d
 * 12=600c7ec9
 * 13=595544cc
 * 18=e3cb3b0f
 * 19=e8f7133d
 * file=cae3ae667a54d731ca934e2867b32aaa,1843,test/test-file2.c
 * 2=58fb3eed
 * 3=f5f7f458
 * 4=aba6add1
 * 8=53762a72,0d274008,6be2454a
 * 10=239c7dfa
 * 12=0b2188c9
 * 15=bd9c4b10,d5c8f9fb
 * 16=eb7309dd,63aebec5
 * 19=316e10eb [...]
 * </pre>
 * <p>
 * Where component is the MD5 hash and path of the component (It could be a path to a compressed file or a URL). file is the MD5 hash, file length and
 * file path being fingerprinted, followed by a list of WFP fingerprints with their corresponding line numbers.
 */
@Slf4j
public class Winnowing {

    private static String UNIX = "unix";
    private static String DOS = "dos";
    private static String MAC = "mac";
    private static String UNKNOWN = "unknown";
    private static int CR = 13;
    private static int LF = 10;
    private static int HT = 9;
    private static int SPACE = 32;
    static private class FileAttr {
        private boolean isBinary ;
        private String   format; //binary,unix,dos, mac; exception: other value

        FileAttr () {
            isBinary  = false;
            format = UNKNOWN;
        }

        public void setIsBinary(boolean binary) {
            isBinary = binary;
        }
        public boolean getIsBinary() { return isBinary; }

        public void setFormat(String format) { this.format = format; }
        public String getFormat() {return format; }
    }
    private static final int GRAM = 30;
    private static final int WINDOW = 64;
    //private static final int ASCII_LF = 10;
    private static final int ASCII_BACKSLASH = 48;

    private static int CRC8_MAXIM_DOW_TABLE_SIZE = 0x100;
    private static int CRC8_MAXIM_DOW_POLYNOMIAL = 0x8C;
    private static int CRC8_MAXIM_DOW_INITIAL = 0x00;
    private static int CRC8_MAXIM_DOW_FINAL = 0x00;

    private static Lock lock = new ReentrantLock(false);

    public static final long MAX_CRC32 = 4294967296L;

    public static List<String> SOURCE_CODE_SUFFIX_LIST =  com.google.common.collect.Lists.newArrayList(".jar", ".war");

    public static List<String> BINARY_SUFFIX_LIST = com.google.common.collect.Lists.newArrayList(".exe",
            ".dll",
            ".so",
            ".a",
            ".o",
            ".ko",
            ".apk",
            ".deb",
            ".rpm",
            ".sh",
            ".cab",
            ".iso",
            ".squashfs",
            ".ext2",
            "ext3",
            ".ext4",
            ".jffs2",
            ".ubifs",
            ".romfs",
            ".ifs",
            ".vdi",
            ".vmdk",
            ".ova",
            ".qcow2",
            ".bin",
            ".trx",
            ".dav",
            ".img",
            ".s19",
            ".s28",
            ".s37",
            ".s",
            ".s1",
            ".s2",
            ".s3",
            ".sx",
            ".srec",
            ".mot",
            ".hex",
            ".wim");

    public static List<List<Byte>> BINARY_MAGIC_NUMBER_LIST = buildBinaryMagicNumberBytes();

    private static List<List<Byte>> buildBinaryMagicNumberBytes() {
        List<String> binaryMagicNumberList = com.google.common.collect.Lists.newArrayList("MZ\\x50\\x00",
                "\\x7FELF",
                "UBI\\x23",
                "-rom1fs-",
                "imagefs",
                "\\x27\\x05\\x19\\x56",
                "\\x80\\x80\\x00\\x02",
                "\\x83\\x80\\x00\\x00",
                "\\x93\\x00\\x00\\x00",
                "sqsh",
                "hsqs",
                "shsq",
                "qshs",
                "tqsh",
                "hsqt",
                "sqlz",
                "\\x53\\xef",
                "\\x85\\x19",
                "\\x19\\x85",
                "\\xeb~\\xff\\x00",
                "\\x00\\xff~\\xeb",
                "\\x3a\\xff\\x26\\xed",
                "S0",
                "S1",
                "S2",
                "S3",
                "S4",
                "S5",
                "S6",
                "S7",
                "S8",
                "S9");

        List<List<Byte>> result = new ArrayList<>();

        for (String hexString:binaryMagicNumberList){
            byte temp = 0;
            int escapeState = 0;  // A state machine
            int unescapeCount = 0;
            List<Byte> content = new ArrayList<>();
            for (char ch: hexString.toCharArray()) {
                if (escapeState == 2 && unescapeCount >= 2) {
                    // Read normal content done
                    content.add(temp);
                    escapeState = 0;
                    unescapeCount = 0;
                    temp = 0;
                }
                if (ch == '\\') {
                    // Read "\"
                    escapeState = 1;
                } else if (escapeState == 1 && ch == 'x') {
                    // Read "\x"
                    escapeState = 2;
                } else if (escapeState == 2) {
                    // Convert content after "\x"
                    temp = (byte)(temp << 4 | Integer.valueOf(String.valueOf(ch), 16));
                    unescapeCount += 1;
                } else {
                    // Just a normal ascii char
                    content.add((byte)ch);
                }
            }
            if (escapeState == 2 && unescapeCount >= 2) {
                // Pop remained result
                content.add(temp);
            }
            result.add(content);
        }
        return result;
    }

    private static byte[] toLittleEndian(long number) {
        byte[] b = new byte[4];
        b[0] = (byte) (number & 0xFF);
        b[1] = (byte) ((number >> 8) & 0xFF);
        b[2] = (byte) ((number >> 16) & 0xFF);
        b[3] = (byte) ((number >> 24) & 0xFF);
        return b;
    }

    private static char normalize(char c) {
        if (c < '0' || c > 'z') {
            return 0;
        } else if (c <= '9' || c >= 'a') {
            return c;
        } else if (c >= 'A' && c <= 'Z') {
            return (char) (c + 32);
        } else {
            return 0;
        }
    }

    private static boolean isBinaryFile(File f) throws IOException {
        String type = Files.probeContentType(f.toPath());
        if (type == null) {
            // type couldn't be determined, assume binary
            return true;
        } else {
            return !type.startsWith("text");
        }
    }

    private static String zeroPaddedString(String hexString) {
        while (hexString.length() != 8) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    private static long crc32c(String s) {
        Checksum checksum = new MyCrc32c();
        checksum.update(s.getBytes(), 0, s.getBytes().length);
        //checksum.update(byteArrayToInt(s.getBytes()));
        return checksum.getValue();
    }

    private static String crc32cHex(long l) {
        Checksum checksum = new MyCrc32c();
        checksum.update(toLittleEndian(l), 0, toLittleEndian(l).length);
        //checksum.update(byteArrayToInt(toLittleEndian(l)));
        return zeroPaddedString(Long.toHexString(checksum.getValue()));
    }

    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value += bytes[i] & 0xff << (3 - i) * 8;
        }
        return value;
    }


    private static long min(List<Long> l) {
        List<Long> sortedList = new ArrayList<>(l);
        Collections.sort(sortedList);
        return sortedList.get(0);
    }

    private static byte convertFormat(ByteBuffer buffer, byte lastByte) {
        int len = buffer.limit();
        byte[] bytes = buffer.array();
        int maxIndex = len-1;
        int modifyIndex = 0;
        for (int i=0; i<len; i++) {
            if (bytes[i] == CR) {
                bytes[modifyIndex] = (byte)LF;
                modifyIndex++;
            } else if (bytes[i] == LF) {
                if (lastByte != CR) {
                    bytes[modifyIndex] = bytes[i];
                    modifyIndex++;
                }
            }else {
                bytes[modifyIndex] = bytes[i];
                modifyIndex++;
            }
            lastByte = bytes[i];
        }
        buffer.position(0);
        buffer.limit(modifyIndex);
        return lastByte;
    }

    public static WfpDTO getMD5OfFile(File file, Boolean hpsm, String filename, Integer binaryFlag, List<String> binaryScanPathList, String filepath, boolean formatEnable) throws IOException, NoSuchAlgorithmException {
        //FileAttr attr = getFileAttr(file);
        Boolean isBianry = isBinary(file);
        boolean skipHpsm = checkFileSizeIsLimit(file.length(), 4, "M") || isBianry;
        MessageDigest md = MessageDigest.getInstance("MD5");
        WfpDTO wfpDTO = new WfpDTO();
        wfpDTO.setSkipHpsm(skipHpsm);
        // 区分是否符合二进制扫描条件
        checkBinaryFile(wfpDTO, binaryFlag, binaryScanPathList, BINARY_SUFFIX_LIST, BINARY_MAGIC_NUMBER_LIST, filename.replace("\\", "/"), filepath);

        WIncalDTO wIncalDTO = new WIncalDTO();
        //Long fileSize = 0L;
        WinDto winDto = new WinDto();
        crc8MAXIMDOWGenerateTable(winDto.getCrc8List());

        try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 32);
            int readLen = 0;
            long offset  = 0;
            byte lastByte = 0;
            while ((readLen = channel.read(buffer, offset)) != -1) {
                buffer.flip();
                offset += readLen;
                //指定文件读取偏移量（指定位置的读取方式,position不主动偏移）
                channel.position(offset);
                log.debug("file:{} formatEnable:{} is_binary:{}",file.getAbsolutePath(), formatEnable, isBianry);
                //强制转换并且文件是非二进制文件才可以进行unix格式转换
                if (formatEnable && !isBianry) {
                    byte[] bytes = buffer.array();
                    int modifyIndex = 0;
                    byte tmp = 0;
                    for (int i=0; i<readLen; i++) {
                        tmp = bytes[i];  //先保存当前字符
                        if (bytes[i] == CR) {
                            bytes[modifyIndex] = (byte)LF;
                            //log.info("0 index:[{}] v=[{}] v1=[{}]",modifyIndex, bytes[modifyIndex],(char)bytes[modifyIndex]);
                            modifyIndex++;
                        } else if (bytes[i] == LF) {
                            if (lastByte != CR) {
                                bytes[modifyIndex] = bytes[i];
                                //log.info("1 index:[{}] v=[{}] v1=[{}]",modifyIndex, bytes[modifyIndex],(char)bytes[modifyIndex]);
                                modifyIndex++;
                            }
                        } else {
                            bytes[modifyIndex] = bytes[i];
                            //log.info("2 index:[{}] v=[{}] v1=[{}]",modifyIndex, bytes[modifyIndex],(char)bytes[modifyIndex]);
                            modifyIndex++;
                        }
                        lastByte = tmp;  //操作结束保存当前字符为上一次字符

                    }
                    buffer.position(0);
                    buffer.limit(modifyIndex);

                }

                md.update(buffer);
                buffer.clear();

            }
            byte[] digest = md.digest();
            if (hpsm) {
                finalCal(winDto);
            }
            wfpDTO.setMd5(bytesToHex(digest));
            wfpDTO.setFileSize(offset);
            wfpDTO.setWinDto(winDto);
            wfpDTO.setWIncalDTO(wIncalDTO);
            return wfpDTO;
        }
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


//    public static  String wfpForFile(String filename, String filepath, Boolean hpsm) throws Exception {
//
//        File file = new File(filepath);
//        //String fileContents = FileUtils.readFileToString(file, Charset.defaultCharset());
//        //List<String> list = FileUtils.readLines(file, StandardCharsets.UTF_8);
//        byte[] bytes = readFromByteFile(filepath);
//        String fileContents = new String(bytes,"utf-8");
//       /* String fileContents = stringBuilder.toString();
//        byte[] bytes = fileContents.getBytes();*/
//
//       /* String name = file.getName();
//        if (!BlacklistRules.filteredBinaryFile(name)) {
//            return "";
//        }*/
//
//
//        String fileMD5 = DigestUtils.md5Hex(bytes);
//
//        StringBuffer wfpBuilder = new StringBuffer();
//        String s = "";
//        WinDto winDto = null;
//        if (hpsm) {
//            Winnowing winnowing = new Winnowing();
//            winDto = winnowing.calcHpsm(fileContents);
//            s = winDto.getWfp();
//        }
//
//        wfpBuilder.append(String.format("file=%s,%d,%s", fileMD5, bytes.length, filename));
//
//        if (isBinary(file) || checkFileSizeIsLimit(file.length(),4,"M")){
//            wfpBuilder.append(String.format(",%d\n",0));
//            return wfpBuilder.toString();
//        }
//
//        if (hpsm) {
//            wfpBuilder.append(String.format(",%d\n",winDto.getSize()));
//            wfpBuilder.append(String.format("hpsm=%s\n", s));
//        }
//
//        // Skip snippet analysis for binaries or non source code files, or empty files.
//        if (/*fileContents.length() == 0 || */BlacklistRules.GenerateOnlyWfp(filename) || BlacklistRules.isMarkupOrJSON(fileContents) /*|| isBinary(file)*/) {
//            return wfpBuilder.toString();
//        }
//        String gram = "";
//        List<Long> window = new ArrayList<>();
//        char normalized = 0;
//        long minHash = MAX_CRC32;
//        long lastHash = MAX_CRC32;
//        int lastLine = 0;
//        int line = 1;
//        String output = "";
//
//        for (char c : fileContents.toCharArray()) {
//            if (c == '\n') {
//                line++;
//                normalized = 0;
//            } else {
//                normalized = normalize(c);
//            }
//
//            if (normalized > 0) {
//                gram += normalized;
//
//                if (gram.length() >= GRAM) {
//                    Long gramCRC32 = crc32c(gram);
//                    window.add(gramCRC32);
//
//                    if (window.size() >= WINDOW) {
//                        minHash = min(window);
//                        if (minHash != lastHash) {
//                            String minHashHex = crc32cHex(minHash);
//                            if (lastLine != line) {
//                                if (output.length() > 0) {
//                                    wfpBuilder.append(output + "\n");
//                                }
//                                output = String.format("%d=%s", line, minHashHex);
//
//                            } else {
//                                output += "," + minHashHex;
//                            }
//
//                            lastLine = line;
//                            lastHash = minHash;
//                        }
//                        // Shift window
//                        window.remove(0);
//                    }
//                    // Shift gram
//                    gram = gram.substring(1);
//                }
//
//            }
//
//        }
//        if (output.length() > 0) {
//            wfpBuilder.append(output + "\n");
//        }
//
//        return wfpBuilder.toString();
//
//    }

    /**
     * Calculates the WFP
     *
     * @param filename
     * @param filepath
     * @return
     * @throws IOException
     */
    public static String wfpForFile(BinaryFilterParam binaryFilterParam, String filename, String filepath, Boolean hpsm, boolean formatEnable) throws Exception {
        File file = new File(filepath);
        WfpDTO md5OfFile = getMD5OfFile(file, hpsm, filename, binaryFilterParam.getMixedBinaryScanFlag(), binaryFilterParam.getMixedBinaryScanFilePathList(), filepath, formatEnable);
        StringBuffer wfpBuilder = new StringBuffer();
        String wfpStr = "";
        WinDto winDto = md5OfFile.getWinDto();
        if (hpsm) {
            wfpStr = md5OfFile.getWinDto().getWfp();
        }
        String line = String.format("file=%s,%d,%s", md5OfFile.getMd5(), md5OfFile.getFileSize(), filename).replace("\\", "/");
        wfpBuilder.append(line);

        filename = filename.replace("\\", "/");
        if (binaryFilterParam.getMixedBinaryScanFlag() == 1 && md5OfFile.getIsBinary()) {
            binaryFilterParam.getBinaryScanList().add(filename);
            binaryFilterParam.getBinaryRealScanList().add(filepath);
        }
        /*int isSoFile = getIsSoFile(binaryFilterParam, filename, filepath, file);
        int isJarFile = getIsJarFile(file);
        String[] dirNameList = filename.split("/");
        String dirPath = null;
        for (String dirName : dirNameList) {

            if (StringUtils.isBlank(dirPath)) {
                dirPath = dirName + "/";
            } else {
                dirPath += dirName;
                if (filename.equals(dirPath)) {
                    continue;
                }
                dirPath += "/";
            }

            DirectoryFileStateInfo directoryFileState = binaryFilterParam.getDirectoryPathFileMap().get(dirPath);
            if (directoryFileState == null) {
                directoryFileState = new DirectoryFileStateInfo();
                binaryFilterParam.getDirectoryPathFileMap().put(dirPath, directoryFileState);
            }
            if (directoryFileState.getIsAllSoFile() == null || directoryFileState.getIsAllSoFile() == 1) {
                directoryFileState.setIsAllSoFile(isSoFile);
            }

            if (directoryFileState.getIsAllJarFile() == null || directoryFileState.getIsAllJarFile() == 1) {
                directoryFileState.setIsAllJarFile(isJarFile);
            }
        }*/

        if (md5OfFile.getSkipHpsm()) {
            wfpBuilder.append(String.format(",%d\n", 0));
            return wfpBuilder.toString();
        }

        if (hpsm) {
            wfpBuilder.append(String.format(",%d\n", winDto.getSize()));
        }
        if (md5OfFile.isSkipCrc()) {
            return wfpBuilder.toString();
        }

        String output = md5OfFile.getWIncalDTO().getOutput();
        wfpBuilder.append(md5OfFile.getWIncalDTO().getSb());

        if (output.length() > 0) {
            wfpBuilder.append(output + "\n");
        }

        return wfpBuilder.toString();
    }

    private static int getIsJarFile(File file) {
        int isJarFile = 0;
        if (file.getName().endsWith(".jar")) {
            isJarFile = 1;
        }
        return isJarFile;
    }

    private static int getIsSoFile(BinaryFilterParam binaryFilterParam, String filename, String filepath, File file) {
        int isSoFile = 0;
        if (BlacklistRules.isSoFile(file.getName())) {
            binaryFilterParam.getSoBinaryScanList().add(filename);
            binaryFilterParam.getSoBinaryRealScanList().add(filepath);
            isSoFile = 1;
        }
        return isSoFile;
    }

    public static String wfpForFile(BinaryFilterParam binaryFilterParam, String filename, String filepath, Boolean hpsm, Boolean unzipArchives, Map<String, String> softWarePackagePathMap, ExtWfpHashInfo wfpHashInfo, boolean formatEnable) throws Exception {
        File file = new File(filepath);
        WfpDTO md5OfFile = getMD5OfFile(file, hpsm, filename, binaryFilterParam.getMixedBinaryScanFlag() , binaryFilterParam.getMixedBinaryScanFilePathList(), filepath, formatEnable);
        StringBuffer wfpBuilder = new StringBuffer();
        String wfpStr = "";
        WinDto winDto = md5OfFile.getWinDto();
        if (hpsm) {
            wfpStr = md5OfFile.getWinDto().getWfp();
        }
        String line = String.format("file=%s,%d,%s", md5OfFile.getMd5(), md5OfFile.getFileSize(), filename).replace("\\", "/");
        wfpBuilder.append(line);
        StringBuilder newLine = new StringBuilder(line);
        if (hpsm) {
            newLine.append(String.format(",%d\n", winDto.getSize()));
        }

        getWfpFileInfo(wfpHashInfo, newLine.toString(), unzipArchives, softWarePackagePathMap, md5OfFile.getIsBinary(), md5OfFile.getIsSourceCodeFilter());
        if (md5OfFile.getSkipHpsm()) {
            wfpBuilder.append(String.format(",%d\n", 0));
            return wfpBuilder.toString();
        }

        if (hpsm) {
            wfpBuilder.append(String.format(",%d\n", winDto.getSize()));
        }
        if (md5OfFile.isSkipCrc()) {
            return wfpBuilder.toString();
        }

        String output = md5OfFile.getWIncalDTO().getOutput();
        wfpBuilder.append(md5OfFile.getWIncalDTO().getSb());

        if (output.length() > 0) {
            wfpBuilder.append(output + "\n");
        }

        return wfpBuilder.toString();
    }

    private static void checkBinaryFile(WfpDTO wfpDTO, Integer binaryFlag, List<String> binaryScanPathList, List<String> binarySuffixList, List<List<Byte>> binaryMagicNumberList, String fileName, String filepath) {
        boolean isBinary = false;
        if (binaryFlag==1 && SOURCE_CODE_SUFFIX_LIST.stream().noneMatch(fileName::endsWith)){
            // 文件后缀符合要求的，直接记为二进制
            if (CollectionUtils.isNotEmpty(binaryScanPathList)){
                if (binaryScanPathList.stream().anyMatch(x -> {
                    return x.endsWith("/") ? fileName.startsWith(x) : fileName.equals(x);
                })){
                    wfpDTO.setIsSourceCodeFilter(true);
                }
//                return binaryScanPathList.stream().anyMatch(x -> {
//                    return x.endsWith("/") ? filePath.startsWith(x) : filePath.equals(x);
//                });
            }
            if (binarySuffixList.stream().anyMatch(fileName::endsWith)){
                isBinary = true;
            } else {
                try (FileInputStream fis = new FileInputStream(filepath)) {
                    // 读取文件的前几个字节
                    byte[] fileBytes = new byte[8];
                    if (fis.read(fileBytes) != 8) {
                        log.warn("The file is too short for comparison:{}", filepath);
                    }
                    boolean match = false;
                    for (List<Byte> b : binaryMagicNumberList) {
                        boolean flag = true;
                        // 将文件前四个字节与列表中的四个元素逐一比较
                        for (int i = 0; i < b.size(); i++) {
                            if (fileBytes[i] != b.get(i)) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag){
                            match = true;
                            break;
                        }
                    }
                    if (match){
                        isBinary = true;
                        log.warn("This file belongs to compliant binary type:{}", filepath);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        wfpDTO.setIsBinary(isBinary);
    }

    public static void getWfpFileInfo(ExtWfpHashInfo wfpHashInfo, String line, Boolean unzipArchives, Map<String, String> softWarePackagePathMap, boolean isBinary, Boolean isSourceCodeFilter) {
        ExtWfpFileInfo wfpInfo = getWfpInfo(line);
        if (StringUtils.isNotBlank(wfpInfo.getFilePath())) {
            wfpInfo.setLineCount(!wfpInfo.isSoftwarePackage() ? wfpInfo.getLineCount() : 0);
            // 记录当前文件的根目录名称
            CopyOnWriteArraySet<String> rootNameList = wfpHashInfo.getRootNameList();
            rootNameList.add(wfpInfo.getRootName());
            // 记录自定义组件包路径Hash对应关系
            if (wfpInfo.getFilePath().endsWith(".jar") || wfpInfo.getFilePath().endsWith(".aar") || wfpInfo.getFilePath().endsWith(".tgz") || wfpInfo.getFilePath().endsWith(".war")) {
                wfpHashInfo.getLineInfo().getCustomComponentPackageHashMap().put(wfpInfo.getFilePath(), wfpInfo.getFileHash());
            }

            // 记录文件路径的HASH对应关系
            if (wfpInfo.isDirHashFlag()) {
                wfpHashInfo.getFileHashMap().put(wfpInfo.getFilePath(), wfpInfo.getFileHash());
            }

            List<String> hashFileList = wfpHashInfo.getHashFileMap().getOrDefault(wfpInfo.getFileHash(), Lists.newArrayList());
            hashFileList.add(wfpInfo.getFilePath());
            wfpHashInfo.getHashFileMap().put(wfpInfo.getFileHash(), hashFileList);

            // 记录文件路径的文件行对应关系
            wfpHashInfo.getLineInfo().getFileLineCountMap().put(wfpInfo.getFilePath(), new AtomicInteger(wfpInfo.getLineCount()));
            List<String> parentPathNameList = wfpInfo.getParentPathNameList();
            if (parentPathNameList == null) {
                parentPathNameList = new ArrayList<>();
            }

            // 记录目录的文件总行对应关系
            parentPathNameList.forEach(parentPathName -> {
                wfpHashInfo.getLineInfo().getDirLineCountMap().computeIfAbsent(
                        parentPathName,
                        key -> new AtomicInteger(0)).getAndAdd(wfpInfo.getLineCount());
                wfpHashInfo.getLineInfo().incrDirFileCountBy(parentPathName, 1);
            });

            // 判断当前文件是否为软件包解压后的内容，是的话行数需要计入软件包行数
            if (unzipArchives) {
                String softwarePackagePath = getSoftwareParentPath(wfpInfo.getFilePath(), softWarePackagePathMap);
                if (StringUtils.isNotBlank(softwarePackagePath)) {
                    wfpHashInfo.getLineInfo().incrSoftWarePackageLineCountBy(softwarePackagePath, wfpInfo.getLineCount());
                }
            }

            if (isBinary){
                wfpHashInfo.getBinaryScanList().add(wfpInfo.getFilePath());
            }

            if (isSourceCodeFilter){
                wfpHashInfo.getFilterSourceCodeScanList().add(wfpInfo.getFilePath());
            }

            // 累加文件代码行
            wfpHashInfo.getLineInfo().incrTotalLineCountBy(wfpInfo.getLineCount());
            wfpHashInfo.getLineInfo().incrTotalFileCountBy(1);
        }
    }

    public static String getSoftwareParentPath(String filePath, Map<String, String> softWarePackagePathMap) {
        Optional<String> parentPathOp = softWarePackagePathMap.keySet().stream().filter(x -> filePath.startsWith(x) && !filePath.equals(x)).findFirst();
        if (!parentPathOp.isPresent()) {
            return null;
        }
        String parentPath = parentPathOp.get();
        // 判断当前匹配的路径对应的是否是软件包的内容
        boolean endsWithSupportedFileType = false;
        for (String fileType : CompressExtractor.ARCHIVER_NAME_LIST) {
            if (parentPath.endsWith(fileType)) {
                endsWithSupportedFileType = true;
                break;
            }
        }

        if (!endsWithSupportedFileType) {
            return softWarePackagePathMap.get(parentPath);
        }
        return null;
    }


    public static ExtWfpFileInfo getWfpInfo(String wfpLine) {
        ExtWfpFileInfo wfpFileInfo = new ExtWfpFileInfo();
        //replaceall性能太慢了
        wfpLine = wfpLine.replace("\n", "").replace("\\", "/");
        String[] fingerprintFile = wfpLine.split(",");
        if (fingerprintFile.length >= 3) {
            wfpFileInfo.setFileHash(fingerprintFile[0].substring(5));
            wfpFileInfo.setFileSize(Long.parseLong(fingerprintFile[1]));
            String path = "";
            int lineCount;
            try {
                lineCount = Integer.parseInt(fingerprintFile[fingerprintFile.length - 1]);
                path = String.join(",", Arrays.copyOfRange(fingerprintFile, 2, fingerprintFile.length - 1));
            } catch (NumberFormatException e) {
                lineCount = 0;
                path = String.join(",", Arrays.copyOfRange(fingerprintFile, 2, fingerprintFile.length));
            }

            wfpFileInfo.setFilePath(path);
            wfpFileInfo.setLineCount(lineCount);

            String[] pathSegments = path.split("/");
            String rootName = pathSegments[0];
            wfpFileInfo.setRootName(rootName);
            // 获取文件上级目录
            String parentDirectory = "";
            List<String> parentPathNameList = new ArrayList<>();
            if (pathSegments.length >= 2) {
                int lastIndex = path.lastIndexOf("/");
                if (lastIndex != -1) {
                    parentDirectory = path.substring(0, lastIndex);
                } else {
                    parentDirectory = ""; // 根目录或无上级目录的情况
                }
//                String[] parentDirectories = new String[pathSegments.length - 1]; // 创建一个数组来存储所有的上级目录
                parentPathNameList = new ArrayList<>(pathSegments.length - 1);
                StringBuilder parentPath = new StringBuilder();
                for (int i = 0; i < pathSegments.length - 1; i++) {
                    parentPath.append(pathSegments[i]).append("/");
                    parentPathNameList.add(parentPath.toString()); // 将当前上级目录添加到数组中
                }
            }

            wfpFileInfo.setParentPathNameList(parentPathNameList);
            wfpFileInfo.setParentPath(parentDirectory);

            // 获取文件名
            String fileName = pathSegments[pathSegments.length - 1];
            wfpFileInfo.setFileName(fileName);

            // 获取文件后缀
            String suffixName = "";
            int index = path.lastIndexOf(".");
            if (index > 0 && index < path.length() - 1) {
                suffixName = path.substring(index + 1);
            }
            wfpFileInfo.setSuffixName(suffixName);

            // 判断当前匹配的路径对应的是否是软件包的内容
            boolean endsWithSupportedFileType = false;
            for (String fileType : CompressExtractor.ARCHIVER_NAME_LIST) {
                if (path.endsWith(fileType)) {
                    endsWithSupportedFileType = true;
                    break;
                }
            }
            wfpFileInfo.setSoftwarePackage(endsWithSupportedFileType);
        }

        // 如果文件无后缀类型 或者文件后缀类型不在白名单内 或者文件名为许可证、指定依赖文件时，行数默认为0
        if (StringUtils.isBlank(wfpFileInfo.getSuffixName()) ||
                !CODE_SUFFIX_WHITE_LIST.contains(wfpFileInfo.getSuffixName()) ||
                EXCLUDE_LINE_COUNT_SUFFIX_LIST.contains(wfpFileInfo.getSuffixName()) ||
                EXCLUDE_FILE_NAME_LIST.contains(wfpFileInfo.getFileName()) ||
                EXCLUDE_LICENSE_NAME_LIST.contains(wfpFileInfo.getFileName().toLowerCase())) {
            wfpFileInfo.setLineCount(0);
        }

        // 判断当前文件是否参与精确目录计算
        wfpFileInfo.setDirHashFlag(participateDirHash(wfpFileInfo));

        return wfpFileInfo;
    }


    public static boolean participateDirHash(ExtWfpFileInfo wfpFileInfo) {

        // 判断文件大小是否超额
        if (wfpFileInfo.getFileSize() > WFP_DIRECTORY_MAX_FILE_SIZE) {
            log.info("File size exceeded, directory hash not calculated： {}", wfpFileInfo.getFilePath());
            return false;
        }

        // 判断文件哈希值是否应被过滤
        if (WFP_FILTER_FILE_HASH.contains(wfpFileInfo.getFileHash())) {
            log.info("File Hash is filtered and directory Hash is not calculated： {}", wfpFileInfo.getFilePath());
            return false;
        }

        // 判断文件路径是否应被过滤
        if (wfpFileInfo.getFilePath().contains(",")) {
            log.info("The file path contains comma which is filtered and does not calculate directory hash： {}", wfpFileInfo.getFilePath());
            return false;
        }

        // 判断文件名是否被过滤
        if (wfpFileInfo.getFilePath().startsWith(".")) {
            log.info("The file path starts with '.' The beginning is filtered and directory hash is not calculated： {}", wfpFileInfo.getFilePath());
            return false;
        }

        // 判断文件后缀(忽略大小写)
        if (ExtFingerprintScanConstants.WFP_DIRECTORY_FILTER_POSTFIX.stream()
                .anyMatch(postfix -> wfpFileInfo.getFilePath().toLowerCase().endsWith(postfix))) {
            log.debug("File suffix filtered, directory hash not calculated： {}", wfpFileInfo.getFilePath());
            return false;
        }

        // 判断文件路径节点是否有被过滤
        if (adjustFilePathNodeFilter(wfpFileInfo.getFilePath().split("/"))) {
            log.info("File path node filtered, directory hash not calculated： {}", wfpFileInfo.getFilePath());
            return false;
        }
        return true;
    }

    public static boolean adjustFilePathNodeFilter(String[] filePathNodes) {
        for (String filePathNode : filePathNodes) {
            if (ExtFingerprintScanConstants.WFP_DIRECTORY_FILTER_FILE_PATH_NODE.contains(filePathNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件大小处于限制内
     *
     * @param fileLen  文件长度
     * @param fileSize 限制大小
     * @param fileUnit 限制的单位（B,K,M,G）
     * @return
     */
    public static boolean checkFileSizeIsLimit(Long fileLen, int fileSize, String fileUnit) {
//        long len = file.length();
        double fileSizeCom = 0;
        if ("B".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen;
        } else if ("K".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen / 1024;
        } else if ("M".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen / (1024 * 1024);
        } else if ("G".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen / (1024 * 1024 * 1024);
        }
        if (fileSizeCom > fileSize) {
            return true;
        }
        return false;
    }

    private static void calcHpsm(byte[] bytes, WinDto winDto, int limit) {
        List<Character> normalized = winDto.getNormalized();
        List<Integer> crcLines = winDto.getCrcLines();
        List<Integer> crc8List = winDto.getCrc8List();
        Integer lastLine = winDto.getLastLine();
        Integer allCount = winDto.getGlobalIndex();
        for (int i = 0; i < limit; i++) {

            char charValue = (char) bytes[i];
            if (charValue == LF) {
                if (normalized.size() > 0) {
                    crcLines.add(crc8MAXIMDOWBuffer(crc8List, normalized));
                    normalized = new ArrayList<>();
                } else if ((lastLine + 1) == allCount) {
                    crcLines.add(0xFF);
                } else if ((allCount - lastLine) > 1) {
                    crcLines.add(0x00);
                }
                lastLine = allCount;
            } else {
                char normalize = normalize(charValue);
                if (normalize != 0) {
                    normalized.add(normalize);
                }
            }
            allCount++;
        }
        winDto.setGlobalIndex(winDto.getGlobalIndex() + limit);
        winDto.setNormalized(normalized);
        winDto.setLastLine(lastLine);
    }

    private static void calcHpsm(byte oneByte, WinDto winDto, int limit) {
        List<Character> normalized = winDto.getNormalized();
        List<Integer> crcLines = winDto.getCrcLines();
        List<Integer> crc8List = winDto.getCrc8List();
        Integer lastLine = winDto.getLastLine();
        Integer allCount = winDto.getGlobalIndex();

        char charValue = (char) oneByte;
        if (charValue == LF) {
            if (normalized.size() > 0) {
                crcLines.add(crc8MAXIMDOWBuffer(crc8List, normalized));
                normalized = new ArrayList<>();
                winDto.setNormalized(normalized);
            } else if ((lastLine + 1) == allCount) {
                crcLines.add(0xFF);
            } else if ((allCount - lastLine) > 1) {
                crcLines.add(0x00);
            }
            lastLine = allCount;
        } else {
            char normalize = normalize(charValue);
            if (normalize != 0) {
                normalized.add(normalize);
            }
        }
        allCount++;

        winDto.setGlobalIndex(winDto.getGlobalIndex() + limit);
        //winDto.setNormalized(normalized);
        winDto.setLastLine(lastLine);
    }

    private static void finalCal(WinDto winDto) {
        List<Integer> crcLines = winDto.getCrcLines();
        List<String> crcLinesHex = winDto.getCrcLinesHex();
        for (Integer crcLine : crcLines) {
//			String hex = Integer.toHexString(crcLine);
            String hex = String.format("%02x", crcLine);
            crcLinesHex.add(hex);
        }

        StringBuilder sb = new StringBuilder();
        for (String linesHex : crcLinesHex) {
            sb.append(linesHex);
        }
        winDto.setWfp(sb.toString());
        winDto.setSize(crcLines.size());
    }

    private static WinDto calcHpsm(String fileContents) {

        WinDto winDto = new WinDto();

        List<Integer> crc8List = new ArrayList<>();
        char[] chars = fileContents.toCharArray();
        int lastLine = 0;
        List<Character> normalized = new ArrayList<>();
        List<Integer> crcLines = new ArrayList<>();

        crc8MAXIMDOWGenerateTable(crc8List);
        for (int i = 0; i < chars.length; i++) {
            if (crcLines.size() == 253) {
                log.info("new char is {}, size {}", chars[i], normalized.size());
            }
            if (chars[i] == LF) {
                if (normalized.size() > 0) {
                    crcLines.add(crc8MAXIMDOWBuffer(crc8List, normalized));
                    normalized = new ArrayList<>();
                } else if ((lastLine + 1) == i) {
                    crcLines.add(0xFF);
                } else if ((i - lastLine) > 1) {
                    crcLines.add(0x00);
                }
                lastLine = i;
            } else {
                char normalize = normalize(chars[i]);
                if (normalize != 0) {
                    normalized.add(normalize);
                }
            }
        }
        List<String> crcLinesHex = new ArrayList<>();
        for (Integer crcLine : crcLines) {
//			String hex = Integer.toHexString(crcLine);
            String hex = String.format("%02x", crcLine);
            crcLinesHex.add(hex);
        }

        StringBuilder sb = new StringBuilder();
        for (String linesHex : crcLinesHex) {
            sb.append(linesHex);
        }
        winDto.setWfp(sb.toString());
        winDto.setSize(crcLines.size());
        return winDto;
    }

    private static int crc8MAXIMDOWBuffer(List<Integer> crc8List, List<Character> normalized) {
        int crc = CRC8_MAXIM_DOW_INITIAL;
        for (int i = 0; i < normalized.size(); i++) {
            crc = crc8MAXIMDOWByte(crc8List, crc, normalized.get(i));
            crc ^= CRC8_MAXIM_DOW_FINAL;
        }
        return crc;
    }

    private static int crc8MAXIMDOWByte(List<Integer> crc8List, int crc, int i) {
        int index = i ^ crc;
        return crc8List.get(index) ^ (crc >> 8);
    }


    private static void crc8MAXIMDOWGenerateTable(List<Integer> crc8List) {
        for (int i = 0; i < CRC8_MAXIM_DOW_TABLE_SIZE; i++) {
            crc8List.add(crc8MAXIMDOWByteNoTable(0, i));
        }
    }

    private static Integer crc8MAXIMDOWByteNoTable(int crc, int by) {
        crc ^= by;
        for (int i = 0; i < 8; i++) {
            int isSet = crc & 0x01;
            crc >>= 1;
            if (isSet != 0) {
                crc ^= CRC8_MAXIM_DOW_POLYNOMIAL;
            }
        }
        return crc;
    }


    public static byte[] readFromByteFile(String pathname) throws IOException {
        File filename = new File(pathname);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] temp = new byte[1024];
        int size = 0;
        while ((size = in.read(temp)) != -1) {
            out.write(temp, 0, size);
        }
        in.close();
        byte[] content = out.toByteArray();
        return content;
    }
//    //creat and drop by yiming.xie
//    public static FileAttr getFileAttr(File file) {
//        FileAttr attr = new FileAttr();
//        int lastChar  =  0;
//        //一个文件内会出现多个类型的换行符
//        int mac = 0;   //mac类型 0：未出现 1：出现过
//        int dos = 0;   //dos类型 0：未出现 1：出现过
//        int unix = 0;  //unix类型 0：未出现 1：出现过
//        FileInputStream fin = null;
//        try {
//            fin = new FileInputStream(file);
//            long len = file.length();
//            for (int j = 0; j < (int) len; j++) {
//                int t = fin.read();
//                //log.info("nike xxxx byte=[{}] int[{}]",(char)(t), t);
//                if (t < 32 && t != 9 && t != 10 && t != 13) {
//                    if (j < 10 ) { //头10个字符有不可见字符
//                        attr.setIsBinary(true);
//                        attr.setFormat(UNKNOWN);
//                    }
//                    break;
//                }
//
//                if (t == CR ) {
//                    if (j == len-1) {  //最后一个字符，直接确定是mac格式
//                        attr.setFormat(MAC);
//                        mac = 1;
//                    }
//                } else if (t == LF) {
//                    if (lastChar == CR) {
//                        attr.setFormat(DOS); //上一个字符为CR，当前字符为LF，是dos格式
//                        dos = 1;
//                    } else {
//                        attr.setFormat(UNIX); //上一个字符不是CR，当前字符是LF，是unix格式
//                        unix = 1;
//                    }
//                } else {
//                    if (lastChar == CR) {
//                        attr.setFormat(MAC);  //普通字符的前一个字符是CR，是mac格式
//                    }
//                }
//                lastChar = t;
//                //文件出现两种以上的换行符，当作UNIX类型，后续不做格式转换逻辑
//                if (mac + dos +unix >= 2) {
//                    attr.setFormat(UNIX);
//                    break;
//                }
//            }
//            //非二进制文件，并且没有找到任何CR或LF或者CRLF字符，当作unix格式
//            if (attr.getIsBinary() == false && attr.getFormat() == UNKNOWN) {
//                attr.setFormat(UNIX);
//            }
//        } catch (Exception e) {
//            log.error("判断二进制文件异常,filepath：{}", file.getAbsolutePath(), e.getMessage());
//        } finally {
//            if (fin != null) {
//                try {
//                    fin.close();
//                } catch (IOException e) {
//                    log.error("关闭文件流异常,filepath：{}", file.getAbsolutePath(), e.getMessage());
//                }
//            }
//        }
//        return attr;
//    }

    //判断二进制类型的逻辑和kb入库的逻辑保持一致
    public static boolean isBinary(File file) {
        boolean isBinary = false;
        FileInputStream fin = null;
        try {
            int fixedLen = 4194304; //4*1024*1024; 4M
            byte[] bytes = new byte[fixedLen];
            fin = new FileInputStream(file);
            int len = fin.read(bytes);

            //判断zip文件
            if (len >= 3) {
                if (bytes[0] == 'P' && bytes[1]=='K' && bytes[2] < 9)
                    return true;
            }
            for (int j = 0; j < len; j++) {
                //if (t < SPACE && t != HT && t != LF && t != CR) {
                if (bytes[j] == 0) {
                    return true;
                }
            }
            return isBinary;
        } catch (Exception e) {
            log.error("判断二进制文件异常,filepath：{}", file.getAbsolutePath(), e.getMessage());
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    log.error("关闭文件流异常,filepath：{}", file.getAbsolutePath(), e.getMessage());
                }
            }
        }
        return isBinary;
    }


    private static boolean isBinaryFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            int bytesRead;
            byte[] buffer = new byte[1024];

            // 读取文件内容，并检查是否包含非文本字符
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    if ((buffer[i] & 0xFF) <= 8) {
                        return true; // 包含非文本字符，判断为二进制文件
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false; // 不包含非文本字符，判断为非二进制文件
    }


    public static String getWfpForSnippet(String snippet, Boolean hpsm) {
        byte[] bytes = snippet.getBytes(StandardCharsets.UTF_8);
        String fileMD5 = "d41d8cd98f00b204e9800998ecf8427e";

        StringBuilder wfpBuilder = new StringBuilder();
        String s = "";
        WinDto winDto = null;
        if (hpsm) {
            winDto = calcHpsm(snippet);
            s = winDto.getWfp();
        }

        wfpBuilder.append(String.format("file=%s,%d,%s", fileMD5, bytes.length, UUID.randomUUID().toString()));


        if (hpsm) {
            wfpBuilder.append(String.format(",%d\n", winDto.getSize()));
            wfpBuilder.append(String.format("hpsm=%s\n", s));
        }


        String gram = "";
        List<Long> window = new ArrayList<>();
        char normalized = 0;
        long minHash = MAX_CRC32;
        long lastHash = MAX_CRC32;
        int lastLine = 0;
        int line = 1;
        String output = "";

        for (char c : snippet.toCharArray()) {
            if (c == '\n') {
                line++;
                normalized = 0;
            } else {
                normalized = normalize(c);
            }

            if (normalized > 0) {
                gram += normalized;

                if (gram.length() >= GRAM) {
                    Long gramCRC32 = crc32c(gram);
                    window.add(gramCRC32);

                    if (window.size() >= WINDOW) {
                        minHash = min(window);
                        if (minHash != lastHash) {
                            String minHashHex = crc32cHex(minHash);
                            if (lastLine != line) {
                                if (!output.isEmpty()) {
                                    wfpBuilder.append(output + "\n");
                                }
                                output = String.format("%d=%s", line, minHashHex);

                            } else {
                                output += "," + minHashHex;
                            }

                            lastLine = line;
                            lastHash = minHash;
                        }
                        // Shift window
                        window.remove(0);
                    }
                    // Shift gram
                    gram = gram.substring(1);
                }

            }

        }
        if (!output.isEmpty()) {
            wfpBuilder.append(output + "\n");
        }

        return wfpBuilder.toString();
    }


//    public static String convertEncoding(String originalString, String encoding) {
//        try {
//            if (StringUtils.isBlank(originalString)) {
//                return originalString;
//            }
//            byte[] utf8Bytes = originalString.getBytes(encoding);
//            return new String(utf8Bytes, encoding);
//        } catch (Exception e) {
//        }
//        return originalString;
//    }

}
