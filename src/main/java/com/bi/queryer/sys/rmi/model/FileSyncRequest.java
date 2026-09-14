package com.bi.queryer.sys.rmi.model;

import com.bi.queryer.util.BIUtil;

import java.io.Serializable;

/**
 * @Author contributor
 * @Date 19:45 2025/1/13
 * @Description TODO
 **/
public class FileSyncRequest implements Serializable {
    public static final String OPERATE_DELETE = "delete";
    public static final String OPERATE_WRITE = "write";
    public static final String OPERATE_READ = "read";

    private String operate;

    private String fileFullName;

    private byte[] fileContent;

    public FileSyncRequest(String operation, String fileFullName) {
        this.operate = operation;
        this.fileFullName = fileFullName;
    }

    public FileSyncRequest(String operation, String fileFullName, byte[] fileContent) {
        this.operate = operation;
        this.fileFullName = fileFullName;
        this.fileContent = fileContent;
    }

    public String getOperate() {
        if(BIUtil.isEmpty(operate)){
            operate = OPERATE_WRITE;
        }
        return operate;
    }

    public void setOperate(String operate) {
        this.operate = operate;
    }

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
}
