package com.sectrend.buildscan.model;

import okhttp3.RequestBody;

public class FileSlice {

    private final RequestBody chunk;
    private final int index;
    private volatile int retriedTimes = 0;

    public FileSlice(RequestBody chunk, int index) {
        this.chunk = chunk;
        this.index = index;
    }

    public RequestBody getChunk() {
        return chunk;
    }

    public int getIndex() {
        return index;
    }

    public void incrRetriedTimes() {
        ++retriedTimes;
    }

    public int getRetriedTimes() {
        return retriedTimes;
    }
}
