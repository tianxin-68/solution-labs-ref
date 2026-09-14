package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.enums.DataEnv;
import lombok.Data;

@Data
public class MetaFieldCtgRel {

    /**
     * 字段id
     */
    private String fieldId;

    /**
     * 分类类型:front=前台，back=后台
     */
    private String ctgType;

    /**
     * 字段分类id
     */
    private String ctgId;

    /**
     * 分类名称
     */
    private String ctgName;

    /**
     * 排序id
     */
    private Double sortId;

    private String dataEnv = DataEnv.OLD_SSM.getCode();

}
