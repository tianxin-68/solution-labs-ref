package com.bi.queryer.ssm.mgr.fieldCtg.model;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-25  14:52
 * @Description:
 */
@Data
public class DatasetCtgDataAuthRsq {

    /**
     * 全部 all 数据集 dataset 目录 ctg
     */
    private String itemType;

    /**
     * 全部=-9999 数据集=数据集id 目录=目录id
     */
    private String itemValue;

    /**
     * 是否继承
     */
    private Integer isInherited = Enabled.YES.getId();

    /**
     * 权限集合
     */
    private List<DatasetCtgDataAuthDetailRsq> dataAuthList = new ArrayList<>();

}
