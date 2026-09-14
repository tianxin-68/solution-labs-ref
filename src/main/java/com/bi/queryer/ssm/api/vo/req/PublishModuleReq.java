package com.bi.queryer.ssm.api.vo.req;

import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetReq;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgNeedAuthEntity;
import com.bi.queryer.ssm.mgr.fieldCtg.model.FieldCtgRelEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布模块请求参数
 */
@Data
public class PublishModuleReq {

    /**
     * 数据集
     */
    public DatasetReq datasetReq = new DatasetReq();

    /**
     * 模块目录
     */
    public MetaFieldCategory categoryModule = new MetaFieldCategory();

    /**
     * 目录权限
     */
    public List<CtgNeedAuthEntity> ctgDataAuthList = new ArrayList<>();

    /**
     * 字段集合
     */
    public List<MetaField> fieldList = new ArrayList<>();

    /**
     * 字段目录关联
     */
    public List<FieldCtgRelEntity> fieldCtgRelList = new ArrayList<>();

    /**
     * 表集合
     */
    public List<MetaTable> tableList = new ArrayList<>();

    /**
     * 表关联
     */
    public List<MetaTableRelation> tableRelList = new ArrayList<>();

    /**
     * 主子表关系配置
     */
    List<MetaTablePriSubCfg> tablePriSubCfgList = new ArrayList<>();

    private List<String> deletedFieldIds = new ArrayList<>();

}
