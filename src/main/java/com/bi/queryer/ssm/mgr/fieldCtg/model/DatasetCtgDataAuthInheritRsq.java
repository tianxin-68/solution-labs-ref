package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-26  16:55
 * @Description: 权限继承返回实体
 */
@Data
public class DatasetCtgDataAuthInheritRsq {

     /**
     * 继承来源名称
     */
    private String inheritedName;

    /**
     * 数据权限集合
     */
    private List<DatasetCtgDataAuthDetailRsq> dataAuthList = new ArrayList<>();

}
