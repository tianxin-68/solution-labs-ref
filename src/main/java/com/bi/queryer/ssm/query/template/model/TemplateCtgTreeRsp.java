package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.ssm.portal.template.entity.TmpAnalysisTplCtgRelEntity;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.template.enums.FavTemplateType;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2025/9/15 17:15
 * @Description:
 */
@Data
public class TemplateCtgTreeRsp extends QueryTemplateCategory {
    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 是否收藏
     */
    private Integer isFav;

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
     * 分类路径
     */
    private String ctgNamePath;


    private String tplType;
    private String tplOwner;

    /**
     * 默认选中的视图ID
     */
    private String defaultSelectViewId;

    /**
     * 视图集合
     */
    private List<TemplateViewEntity> viewList = new ArrayList<>();

    private Integer isInPublicDomain;

    private String ctgId;

    //根目录名称
    private String rootCtgName;

    //根目录id
    private String rootCtgId;

    private String favCtgId;

    public static TemplateCtgTreeRsp of(QueryTemplateCategory ctg) {
        TemplateCtgTreeRsp rsp = new TemplateCtgTreeRsp();
        rsp.setId(ctg.getId());
        rsp.setName(ctg.getName());
        rsp.setType(FavTemplateType.CTG.getCode());
        rsp.setParentId(ctg.getParentId());
        rsp.setDescription(ctg.getDescription());
        rsp.setCreatedBy(ctg.getCreatedBy());
        rsp.setUpdatedBy(ctg.getUpdatedBy());
        rsp.setCreatedTime(ctg.getCreatedTime());
        rsp.setUpdatedTime(ctg.getUpdatedTime());
        rsp.setCtgId(ctg.getId());
        return rsp;
    }

    public static TemplateCtgTreeRsp of(TemplateViewPageRsp tpl) {
        TemplateCtgTreeRsp rsp = new TemplateCtgTreeRsp();
        rsp.setId(tpl.getTplId());
        rsp.setName(tpl.getTplName());
        rsp.setTplType(tpl.getTplType());
        rsp.setParentId(tpl.getCtgId());
        rsp.setDescription(tpl.getTplDesc());
        rsp.setCreatedBy(tpl.getCreatedBy());
        rsp.setUpdatedBy(tpl.getUpdatedBy());
        rsp.setCreatedTime(tpl.getCreatedTime());
        rsp.setUpdatedTime(tpl.getUpdatedTime());
        rsp.setCtgNamePath(tpl.getCtgNamePath());
        rsp.setDefaultSelectViewId(tpl.getDefaultSelectViewId());
        rsp.setViewList(tpl.getViewList());
        rsp.setTplOwner(tpl.getTplOwner());
        rsp.setIsFav(tpl.getIsFav());
        rsp.setIsTop(tpl.getIsTop());
        rsp.setDatasetId(tpl.getDatasetId());
        rsp.setDatasetName(tpl.getDatasetName());
        rsp.setCtgId(tpl.getCtgId());
        rsp.setType(FavTemplateType.QUERY_TEMPLATE.getCode());
        rsp.setDatasetType(tpl.getDatasetType());
        return rsp;
    }

    public static TemplateCtgTreeRsp of(TemplateViewPageRsp tplEntity, TemplateFavEntity fav) {
        TemplateCtgTreeRsp rsp;
        if (tplEntity == null) {
            rsp = new TemplateCtgTreeRsp();
            rsp.setCtgId(fav.getCtgId());
        } else {
            rsp = of(tplEntity);
        }
        rsp.setId(fav.getTplId());
        rsp.setType(fav.getFavTplType());
        rsp.setParentId(fav.getCtgId());
        rsp.setIsTop(fav.getIsTop());
        rsp.setIsFav(fav.getIsFav());
        rsp.setSortId(fav.getTopSortId());
        rsp.setCreatedTime(fav.getCreatedTime());
        rsp.setUpdatedTime(fav.getUpdatedTime());
        rsp.setCreatedBy(fav.getCreatedBy());
        rsp.setUpdatedBy(fav.getUpdatedBy());
        rsp.setFavCtgId(fav.getCtgId());
        return rsp;
    }

    /** genAI_feature/v3.15.0_start */
    /**
     * 个人临时看板挂载到查询模板目录树下
     */
    public static TemplateCtgTreeRsp of(TmpAnalysisTplCtgRelEntity row) {
        TemplateCtgTreeRsp rsp = new TemplateCtgTreeRsp();
        rsp.setId(row.getAnalysisTplId());
        rsp.setName(row.getAnalysisTplName());
        rsp.setParentId(row.getCtgId());
        rsp.setDescription(row.getAnalysisTplDesc());
        rsp.setCreatedBy(row.getCreatedBy());
        rsp.setUpdatedBy(row.getUpdatedBy());
        rsp.setCreatedTime(row.getCreatedTime());
        rsp.setUpdatedTime(row.getUpdatedTime());
        rsp.setCtgId(row.getCtgId());
        rsp.setParentId(row.getCtgId());
        rsp.setTplOwner(row.getAnalysisTplOwner());
        rsp.setType(FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode());
        return rsp;
    }
    /** genAI_feature/v3.15.0_end */
}
