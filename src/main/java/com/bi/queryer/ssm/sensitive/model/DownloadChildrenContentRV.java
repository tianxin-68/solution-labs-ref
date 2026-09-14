package com.bi.queryer.ssm.sensitive.model;


import lombok.Data;

import java.io.Serializable;
import java.util.*;

/**
 * @description 下载之内容信息
 * @author contributor
 * @date 2021-12-14
 */
@Data
public class DownloadChildrenContentRV implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签
     */
    private String label;


    /**
     * 内容
     */
    private List<String> childrenContent;

    public DownloadChildrenContentRV() {
    }

    public DownloadChildrenContentRV(String label, List<String> childrenContent) {
        this.label = label;
        this.childrenContent = childrenContent;
    }

    public DownloadChildrenContentRV(String label, Collection<String> childrenContent) {
        this.label = label;
        this.childrenContent = new ArrayList<>(childrenContent);
    }

}
