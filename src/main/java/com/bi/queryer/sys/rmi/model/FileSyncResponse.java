package com.bi.queryer.sys.rmi.model;

import java.io.Serializable;

/**
 * @Author contributor
 * @Date 19:45 2025/1/13
 * @Description TODO
 **/
public class FileSyncResponse implements Serializable {

    private String fileFullName;

    private byte[] fileContent;

    private long lastModifiedTime;

    public String getFileFullName() {
        return fileFullName;
    }

    public void setFileFullName(String fileFullName) {
        this.fileFullName = fileFullName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
