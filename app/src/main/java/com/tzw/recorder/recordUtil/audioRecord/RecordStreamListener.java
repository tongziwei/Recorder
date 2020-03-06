package com.tzw.recorder.recordUtil.audioRecord;

/**
 * Created by clara.tong on 2020/3/4
 */
public interface RecordStreamListener {
    void recordOfByte(byte[] audioData, int offsetInBytes, int sizeInBytes);
}
