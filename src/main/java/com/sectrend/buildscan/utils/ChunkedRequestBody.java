package com.sectrend.buildscan.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class ChunkedRequestBody extends RequestBody {
    private File file;
    private long start;
    private long chunkSize;

    public ChunkedRequestBody(File file, long start, long chunkSize) {
        this.file = file;
        this.start = start;
        this.chunkSize = chunkSize;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("application/octet-stream; charset=utf-8");
    }

    @Override
    public long contentLength() {
        return chunkSize;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            long skip = inputStream.skip(start);
            if (start != skip) {
                log.error("bytes skipped, expected:{}, actual:{}", start, skip);
            }

            byte[] buffer = new byte[(int) chunkSize];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) {
                return;
            }
            sink.write(buffer, 0, bytesRead);
            if (bytesRead != chunkSize) {
                log.error("bytes read={}, chunkSize={}", bytesRead, chunkSize);
            }
        }
    }
}
