package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.model.FileSlice;
import com.sectrend.buildscan.model.FileSliceResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class FileUploader {

    private final List<FileSlice> fileSliceArr = new ArrayList<>();
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failCount = new AtomicInteger(0);
    private int chunkNum;
    private long chunkSize = 10 * 1024 * 1024;
    private String[] uploadUrls;
    private boolean printProgress;
    private static final Integer progressNum = 100;
    private static final String PROGRESS_SUFFIX = "UPLOADING ";
    private static final int MAX_RETRY_TIMES = 3;

    public FileUploader(File file, int chunkNum, String[] uploadUrls, boolean printProgress, long chunkSize) {
        this.chunkNum = chunkNum;
        this.uploadUrls = uploadUrls;
        this.printProgress = printProgress;
        this.chunkSize = chunkSize;
        splitFile(file);
        uploadChunks();
    }

    public static FileSliceResult calculateChunkNum(File file, long chunkSize) {
        if (file == null || !file.exists()) {
            return null;
        }

        return calculateChunkNum(file.length(), chunkSize);
    }

    private static FileSliceResult calculateChunkNum(long fileSize, long chunkSize) {
        FileSliceResult fileSliceResult = new FileSliceResult(chunkSize, 0);
        if (fileSize % chunkSize == 0) {
            fileSliceResult.setChunkNum((int) (fileSize / chunkSize));
        } else {
            fileSliceResult.setChunkNum((int) (fileSize / chunkSize + 1));
        }

        if (fileSliceResult.getChunkNum() > FileSliceResult.MAX_CHUNK_NUM) {
            long newChunkSize = fileSize / (FileSliceResult.MAX_CHUNK_NUM - 1);
            fileSliceResult.setChunkSize(newChunkSize);
            if (fileSize % newChunkSize == 0) {
                fileSliceResult.setChunkNum((int) (fileSize / newChunkSize));
            } else {
                fileSliceResult.setChunkNum((int) (fileSize / newChunkSize + 1));
            }
        }
        return fileSliceResult;
    }

    private void splitFile(File file) {
        long fileSize = file.length();
        long start = 0;
        long end;

        for (int i = 0; i < chunkNum; i++) {
            end = Math.min(start + chunkSize, fileSize);
            long chunkSize = end - start;

            RequestBody requestBody = createRequestBody(file, start, chunkSize);
            FileSlice fileSlice = new FileSlice(requestBody, i);
            fileSliceArr.add(fileSlice);
            start = end;
        }
        log.info("Splitting file, fileSize:{}, chunkSize:{}, chunkNum:{}, file:{}", fileSize, chunkSize, chunkNum, file.getAbsolutePath());
    }

    private RequestBody createRequestBody(File file, long start, long chunkSize) {
        return new ChunkedRequestBody(file, start, chunkSize);
    }

    private void uploadChunks() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        for (int i = 0; i < chunkNum; i++) {
            uploadChunk(client, fileSliceArr.get(i));
        }
    }

    private void uploadChunk(OkHttpClient client, FileSlice fileSlice) {
        Request request = new Request.Builder()
                .url(uploadUrls[fileSlice.getIndex()])
                .put(fileSlice.getChunk())
                .header("Content-Type", "application/octet-stream; charset=utf-8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    int succCount = successCount.incrementAndGet();
                    if (printProgress) {
                        // 输出进度条
                        System.out.print("\r" + PROGRESS_SUFFIX + String.format("%.2f%%", ((double) succCount / chunkNum) * 100));
                    }
                } else {
                    if (fileSlice.getRetriedTimes() <= MAX_RETRY_TIMES) {
                        fileSlice.incrRetriedTimes();
                        uploadChunk(client, fileSlice);
                    } else {
                        failCount.incrementAndGet();
                        log.error("Upload failed, chunkIndex={}, chunkSize={}, respCode={}", fileSlice.getIndex(), ((ChunkedRequestBody) fileSlice.getChunk()).contentLength(), response.code());
                        throw new RuntimeException("Upload failed");
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (fileSlice.getRetriedTimes() <= MAX_RETRY_TIMES) {
                    fileSlice.incrRetriedTimes();
                    uploadChunk(client, fileSlice);
                } else {
                    failCount.incrementAndGet();
                    log.error("Upload failed, chunkIndex={}, chunkSize={}", fileSlice.getIndex(), ((ChunkedRequestBody) fileSlice.getChunk()).contentLength(), e);
                    throw new RuntimeException("Upload failed");
                }
            }
        });
    }

    public boolean isSuccess() {
        return chunkNum == successCount.get();
    }

    public boolean isFinished() {
        return successCount.get() + failCount.get() == chunkNum;
    }

    public int getSucceededCount() {
        return successCount.get();
    }

    public int getFailedCount() {
        return failCount.get();
    }
}
