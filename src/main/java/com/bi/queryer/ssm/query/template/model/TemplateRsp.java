package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

/**
 * 返回给前台的模版相关信息、包含置顶顺序、是否可编辑等信息
 */
@Data
public class TemplateRsp extends TemplateEntity{

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 置顶顺序
     */
    private Double topSortId;

    /**
     * 是否收藏
     */
    private Integer isFav;

    /**
     * 配置config
     */
    private String tplConfig;

    /**
     * 数据集id
     */
    private String datasetId;

    /**
     * 数据集名称
     */
    private String datasetName;

    /**
     * 数据集数据类型
     */
    private String datasetType;

    /**
     * 分类类型
     */
    private String ctgType;

    /**
     * 分类路径
     */
    private String ctgNamePath;
}
