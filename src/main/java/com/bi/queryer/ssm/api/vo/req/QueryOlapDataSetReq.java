package com.bi.queryer.ssm.api.vo.req;

/**
 * @Author contributor
 * @Date 11:16 2026/4/8
 * @Description TODO
 **/
public class QueryOlapDataSetReq {
    /**
     * 是否将结果数据集集转为文件
     */
    private boolean toFile = false;

    private boolean zipFile = false;

    /**
     * csv文件分隔符
     */
    private char csvDelimiter = ',';

    public boolean isToFile() {
        return toFile;
    }

    public void setToFile(boolean toFile) {
        this.toFile = toFile;
    }

    public boolean isZipFile() {
        return zipFile;
    }

    public void setZipFile(boolean zipFile) {
        this.zipFile = zipFile;
    }

    public char getCsvDelimiter() {
        return csvDelimiter;
    }

    public void setCsvDelimiter(char csvDelimiter) {
        this.csvDelimiter = csvDelimiter;
    }
}
