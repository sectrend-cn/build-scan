package com.sectrend.buildscan.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSliceResult {

    public static final int MAX_CHUNK_NUM = 1000;

    private long chunkSize;

    private int chunkNum;
}
