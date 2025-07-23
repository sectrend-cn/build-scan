package com.buildscan;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author wuguangya
 * @since 2024/2/28
 */
@Slf4j
public class FileUploaderTest {

    public static void main(String[] args) throws InterruptedException {

        System.out.println(10 * 1024 * 1024);
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try (InputStream inputStream = new FileInputStream("D:\\source_code_samples\\ai_toolchain.tar.gz")) {
                    long skip = inputStream.skip(finalI * 10 * 1024 * 1024);
                    log.info("expeted:{} bytes, bytes actually skipped:{}", finalI * 10 * 1024 * 1024, skip);

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        Thread.sleep(15000L);

//        File file = new File("D:\\source_code_samples\\ai_toolchain.tar.gz");
//        long fileSize = 30977884160L;
//        FileSliceResult fileSliceResult = FileUploader.calculateChunkNum(fileSize, 10 * 1024 * 1024);
//        long fileSize = file.length();
//        FileSliceResult fileSliceResult = FileUploader.calculateChunkNum(file, 1 * 1024 * 1024);
//        System.out.println(fileSliceResult.getChunkSize() + ", " + fileSliceResult.getChunkNum());
//
//        long start = 0;
//        long end;
//        for (int i = 0; i < fileSliceResult.getChunkNum(); i++) {
//            end = Math.min(start + fileSliceResult.getChunkSize(), fileSize);
//            long chunkSize = end - start;
//            System.out.println("index=" + i + ", start=" + start + ", chunkSize=" + chunkSize);
//            start = end;
//        }
    }
}
