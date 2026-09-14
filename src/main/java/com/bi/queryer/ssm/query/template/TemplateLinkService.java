package com.bi.queryer.ssm.query.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: contributor
 * @CreateTime: 2024-01-11  14:36
 * @Description: 模板跳转
 */
@Service
@Scope("prototype")
public class TemplateLinkService extends TemplateBaseService{

    @Autowired
    private QueryTemplateCategoryService ctgService;

    /**
     *
     * @param req
     * @return
     */
   public List<TemplateLinkRsp> queryTemplateLinkList(TemplateLinkReq req) {

       List<TemplateLinkRsp> result = new ArrayList<>();

       List<QueryTemplateCategory> categories = ctgService.getAllCategories();

       Map<String, Object> queryParams = new HashMap<>();
       User user = UserManager.get();
       queryParams.put("userName", user.getName());

       List<String> ctgIdList = categories.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
       queryParams.put("ctgIdList", ctgIdList);

       //查询用户能查询的所有模板
       List<TemplateLinkRsp> tplList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryTemplateByCtg", queryParams);

       if (CollUtil.isEmpty(tplList)) {
           return result;
       }

       //获取模版id
       List<String> tplIdList = tplList.stream().map(TemplateLinkRsp::getTplId).collect(Collectors.toList());

       //查询模版配置支持的维度
       //模版有多个视图，此处取有效的任一个
       queryParams.put("tplIds", tplIdList);
       List<TemplateCfgDtlEntity> templateCfgDtlEntities = (List<TemplateCfgDtlEntity>) dao.queryObjectList("ssm.query.template.cfg.dtl.getTplFieldDimCodes", queryParams);
       Map<String, TemplateCfgDtlEntity> tplFieldDimCodesMap = new HashMap<>();
       for (TemplateCfgDtlEntity templateCfgDtlEntity : templateCfgDtlEntities) {
           tplFieldDimCodesMap.put(templateCfgDtlEntity.getTplId(), templateCfgDtlEntity);
       }

       //获取没有解构配置编码的模版，需要从模版视图配置中获取
       List<String> withoutFieldDimCodesTplIds =  new ArrayList<>();
       for (TemplateLinkRsp linkRsp : tplList) {

           //去掉模板本身
           if (linkRsp.getTplId().equalsIgnoreCase(req.getTplId())) {
               continue;
           }

           //校验 维度code 是否在模板维度中
           TemplateCfgDtlEntity templateCfgDtlEntity = tplFieldDimCodesMap.get(linkRsp.getTplId());
           if (templateCfgDtlEntity == null) {
               withoutFieldDimCodesTplIds.add(linkRsp.getTplId());
               continue;
           }

           String tplConfigFieldDimCodes = templateCfgDtlEntity.getTplConfigFieldDimCodes();
           if (StrUtil.isEmpty(tplConfigFieldDimCodes)) {
               continue;
           }

           List<String> tplConfigFieldDimCodesList = Arrays.asList(tplConfigFieldDimCodes.split(","));

           boolean isCanLink = true;
           for (String fieldCode : req.getFieldCodeList()) {

               if (!tplConfigFieldDimCodesList.contains(fieldCode)) {
                   isCanLink = false;
               }

               if (!isCanLink) {
                   break;
               }
           }

           if (!isCanLink) {
               continue;
           }

           result.add(linkRsp);

       }

       //如果没有历史模版，则直接返回结果
       if(CollUtil.isEmpty(withoutFieldDimCodesTplIds)){
           return result;
       }

       //历史模版没有解构配置编码，需要从模版配置中获取
       List<TemplateViewEntity> tplCfgList = (List<TemplateViewEntity>)dao.queryObjectList("ssm.template.view.queryCfgByTplIds",withoutFieldDimCodesTplIds);
       Map<String, TemplateViewEntity> tplCfgMap = new HashMap<>();
       for(TemplateViewEntity templateCfg : tplCfgList){
           tplCfgMap.put(templateCfg.getTplId(),templateCfg);
       }

       for (TemplateLinkRsp linkRsp : tplList) {

           //去掉模板本身
           if (linkRsp.getTplId().equalsIgnoreCase(req.getTplId())) {
               continue;
           }

           //不在历史模版不处理
           if (!withoutFieldDimCodesTplIds.contains(linkRsp.getTplId())) {
               continue;
           }

           boolean isCanLink = true;
           for (String fieldCode : req.getFieldCodeList()) {

               TemplateViewEntity viewEntity = tplCfgMap.get(linkRsp.getTplId());
               if (viewEntity == null) {
                   continue;
               }

               //校验 维度code 是否在模板行维度/列维度/筛选条件中
               isCanLink = checkTemplateCanLink(fieldCode, viewEntity.getTplConfig());
               if (!isCanLink) {
                   break;
               }
           }

           if (!isCanLink) {
               continue;
           }

           result.add(linkRsp);
       }

       return result;
   }

    /**
     * 获取模板跳转详情
     * @param req
     * @return
     */
    public TemplateLinkDetailRsp getTemplateLinkDetailByTemplateId(TemplateLinkReq req) {
        TemplateLinkDetailRsp templateLinkDetailRsp = new TemplateLinkDetailRsp();

        String tplId = req.getTplId();
        String viewId = req.getViewId();

        /**
         * 历史模版的默认视图id = tplId
         */
        if (StrUtil.isEmpty(req.getViewId())) {
            viewId = tplId;
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("tplId", tplId);
        queryParams.put("viewId", viewId);
        queryParams.put("tplIds", Arrays.asList(tplId));

        //1. 跳转到其他模板的明细
        List<TemplateLinkRsp> linkToOtherTemplateList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryLinkToOtherTemplateList", queryParams);
        buildTemplateLinkField(linkToOtherTemplateList);

        //2. 被其他模板跳转至本模板的明细
        List<TemplateLinkRsp> linkFromOtherTemplateList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryLinkFromOtherTemplateList", queryParams);
        buildTemplateLinkField(linkFromOtherTemplateList);
        buildTemplateLinkView(linkFromOtherTemplateList);

        templateLinkDetailRsp.setLinkToOtherTemplateList(linkToOtherTemplateList);
        templateLinkDetailRsp.setLinkFromOtherTemplateList(linkFromOtherTemplateList);
        buildTemplateLinkView(linkFromOtherTemplateList);
        return templateLinkDetailRsp;
    }

    public void buildTemplateLinkField(List<TemplateLinkRsp> templateLinkList){

        if(CollUtil.isEmpty(templateLinkList)){
            return;
        }

        List<String> linkIdList = templateLinkList.stream().map(TemplateLinkRsp::getLinkId).collect(Collectors.toList());
        Map<String,Object> queryParams = new HashMap<>();
        queryParams.put("linkIdList",linkIdList);
        List<TemplateLinkFieldRsp> templateLinkFieldList = (List<TemplateLinkFieldRsp>) dao.queryObjectList("ssm.template.link.field.getByLinkIds", queryParams);
        if(CollUtil.isEmpty(templateLinkFieldList)){
            return;
        }

        Map<String,List<TemplateLinkFieldRsp>> templateLinkFieldMap = new HashMap<>();
        for (TemplateLinkFieldRsp templateLinkFieldRsp : templateLinkFieldList) {
            String linkId = templateLinkFieldRsp.getLinkId();
            List<TemplateLinkFieldRsp> fieldList = templateLinkFieldMap.get(linkId);
            if(fieldList == null){
                fieldList = new ArrayList<>();
            }

            fieldList.add(templateLinkFieldRsp);
            templateLinkFieldMap.put(linkId,fieldList);
        }

        for (TemplateLinkRsp templateLinkRsp : templateLinkList) {
            String linkId = templateLinkRsp.getLinkId();
            List<TemplateLinkFieldRsp> fieldList = templateLinkFieldMap.get(linkId);
            Collections.sort(fieldList);
            templateLinkRsp.setTemplateLinkFieldList(fieldList);
        }


    }

    public List<TemplateLinkAddReq> buildTemplateLinkAddReq(List<TemplateLinkRsp> templateLinkList) {

        //构造跳转信息
        List<TemplateLinkAddReq> linkAddReqList = new ArrayList<>();
        for (TemplateLinkRsp linkItem : templateLinkList) {
            TemplateLinkAddReq linkAddReq = new TemplateLinkAddReq();

            List<TemplateLinkFieldAddReq> linkFieldAddReqList = new ArrayList<>();
            for(TemplateLinkFieldRsp templateLinkFieldRsp :linkItem.getTemplateLinkFieldList()){
                TemplateLinkFieldAddReq linkFieldAddReq = new TemplateLinkFieldAddReq();
                linkFieldAddReq.setFieldCode(templateLinkFieldRsp.getFieldCode());
                linkFieldAddReq.setFieldId(templateLinkFieldRsp.getFieldId());
                linkFieldAddReq.setFieldTitle(templateLinkFieldRsp.getFieldTitle());
                linkFieldAddReqList.add(linkFieldAddReq);
            }

            linkAddReq.setTplId(linkItem.getTplId());
            linkAddReq.setViewId(linkItem.getViewId());
            linkAddReq.setTemplateLinkFieldList(linkFieldAddReqList);
            linkAddReqList.add(linkAddReq);
        }
        return linkAddReqList;
    }

    /**
     * 构建模板跳转视图
     * @param templateLinkList
     */
    public void buildTemplateLinkView(List<TemplateLinkRsp> templateLinkList) {

        if (CollUtil.isEmpty(templateLinkList)) {
            return;
        }

        List<String> viewIdList = templateLinkList.stream().map(TemplateLinkRsp::getViewId).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(viewIdList)) {
            return;
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("viewIdList", viewIdList);
        List<TemplateViewEntity> viewEntityList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.batchGetByViewId", queryParams);
        Map<String, TemplateViewEntity> viewMap = new HashMap<>();

        for (TemplateViewEntity viewEntity : viewEntityList) {
            viewMap.put(viewEntity.getViewId(), viewEntity);
        }

        for (TemplateLinkRsp templateLinkRsp : templateLinkList) {
            TemplateViewEntity viewEntity = viewMap.get(templateLinkRsp.getViewId());

            if (viewEntity == null) {
                continue;
            }

            templateLinkRsp.setViewName(viewEntity.getViewName());
            templateLinkRsp.setViewId(viewEntity.getViewId());
        }

    }

    /**
     * 校验模板是否可以跳转
     * @param fieldCode
     * @param tplConfig
     * @return
     */
   public boolean checkTemplateCanLink(String fieldCode,String tplConfig) {

       if (StrUtil.isEmpty(tplConfig)) {
           return false;
       }

       try {

           SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
           queryTemplate.setConfig(tplConfig);

           QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
           queryConfigure.load();

           //查询行维度里面有没有指定的筛选字段
           Long rowFieldCount = queryConfigure.getResult().getRowDimensions().stream().filter(a -> a.getCode().equalsIgnoreCase(fieldCode)).count();
           if (rowFieldCount > 0) {
               return true;
           }

           //查询列维度里面有没有指定的筛选字段
           Long colFieldCount = queryConfigure.getResult().getColDimensions().stream().filter(a -> a.getCode().equalsIgnoreCase(fieldCode)).count();
           if (colFieldCount > 0) {
               return true;
           }

           //查询过滤条件里面有没有指定的筛选字段
           Long filterFieldCount = queryConfigure.getFilter().getFields().stream().filter(a -> a.getCode().equalsIgnoreCase(fieldCode)).count();
           if (filterFieldCount > 0) {
               return true;
           }

       } catch (Exception e) {
           e.printStackTrace();
       }

       return false;
   }

    /**
     * 保存模板跳转
     */
   public void saveTemplateLink(String tplId,String viewId,List<TemplateLinkAddReq> templateLinkList) {

       Map<String,Object> params = new HashMap<>();
       params.put("tplId", tplId);
       params.put("viewId", viewId);

       //通过模版id查询跳转链接id
       List<String> linkIdList = (List<String>)dao.queryObjectList("ssm.template.link.queryLinkidBySourceTplIdAndViewId",params);
       if (CollUtil.isNotEmpty(linkIdList)) {
           params.put("linkIdList", linkIdList);
           dao.delete("ssm.template.link.field.batchDeleteByLinkId", params);
       }

       //先删除
       dao.delete("ssm.template.link.deleteTemplateLinkBySourceTpIdAndViewId", params);

       //插入
       if (CollUtil.isEmpty(templateLinkList)) {
           return;
       }

       String userName = UserManager.get().getName();
       List<TemplateLinkEntity> templateLinkEntityList = new ArrayList<>();
       List<TemplateLinkFieldEnity> templateLinkFieldEnityList = new ArrayList<>();

       Double sortId = 0.0;
       for (TemplateLinkAddReq templateLinkAddReq : templateLinkList) {

           List<TemplateLinkFieldAddReq> templateLinkFieldList = templateLinkAddReq.getTemplateLinkFieldList();
           if (CollUtil.isEmpty(templateLinkFieldList)) {
               //兼容历史逻辑，不传字段列表的场景
               templateLinkFieldList = new ArrayList<>();
               TemplateLinkFieldAddReq fieldAddReq = new TemplateLinkFieldAddReq();
               fieldAddReq.setFieldId(templateLinkAddReq.getFieldId());
               fieldAddReq.setFieldCode(templateLinkAddReq.getFieldCode());
               fieldAddReq.setFieldTitle(templateLinkAddReq.getFieldTitle());
               templateLinkFieldList.add(fieldAddReq);
           }

           if(CollUtil.isEmpty(templateLinkFieldList)){
               continue;
           }

           TemplateLinkEntity entity = new TemplateLinkEntity();

           String linkId = Guid.id();
           entity.setLinkId(linkId);
           entity.setSourceTplId(tplId);
           entity.setSourceTplViewId(viewId);
           entity.setTargetTplId(templateLinkAddReq.getTplId());
           entity.setTargetTplViewId(templateLinkAddReq.getViewId());

           sortId++;
           entity.setSortId(sortId);
           entity.setCreatedBy(userName);
           templateLinkEntityList.add(entity);


           Double fieldSortId = 0.0;
           for (TemplateLinkFieldAddReq templateLinkFieldAddReq : templateLinkFieldList) {
               TemplateLinkFieldEnity templateLinkFieldEnity = new TemplateLinkFieldEnity();
               templateLinkFieldEnity.setLinkId(linkId);
               templateLinkFieldEnity.setFieldId(templateLinkFieldAddReq.getFieldId());
               templateLinkFieldEnity.setFieldCode(templateLinkFieldAddReq.getFieldCode());
               templateLinkFieldEnity.setFieldTitle(templateLinkFieldAddReq.getFieldTitle());

               fieldSortId++;
               templateLinkFieldEnity.setSortId(fieldSortId);
               templateLinkFieldEnityList.add(templateLinkFieldEnity);
           }


       }

       Map<String, Object> map = new HashMap<>();
       map.put("templateLinkEntityList", templateLinkEntityList);
       dao.insert("ssm.template.link.batchInsertTemplateLink", map);

       if(CollUtil.isNotEmpty(templateLinkFieldEnityList)) {
           map.put("templateLinkFieldList", templateLinkFieldEnityList);
           dao.insert("ssm.template.link.field.batchInsert", map);
       }
   }

    /**
     * 删除失效的
     * @param tplId
     * @param invalidFieldCodeList
     */
   public void deleteInvalidTemplateLink(String tplId ,String viewId,List<String> invalidFieldCodeList) {

       if (CollUtil.isEmpty(invalidFieldCodeList)) {
           return;
       }

       Map<String, Object> map = new HashMap<>();
       map.put("fieldCodeList", invalidFieldCodeList);
       map.put("tplId", tplId);
       map.put("viewId", viewId);

       //查询失效的模版跳转id
       List<String> invalidLinkIdList = (List<String>) dao.queryObjectList("ssm.template.link.queryInvalidLinkId", map);
       if (CollUtil.isEmpty(invalidLinkIdList)) {
           return;
       }

       map.put("linkIdList", invalidLinkIdList);

       dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
           @Override
           public void execute() {
               dao.delete("ssm.template.link.deleteInvalidTemplateLink", map);
               dao.delete("ssm.template.link.field.batchDeleteByLinkId", map);
           }
       });
   }


    public List<TemplateLinkEntity> buildLinkEntityList(List<TemplateLinkRsp> linkList) {
        List<TemplateLinkEntity> tplLinkList = new ArrayList<>();
        Double sortId = 0.0;
        if (CollectionUtil.isNotEmpty(linkList)) {
            for (TemplateLinkRsp linkItem : linkList) {
                TemplateLinkEntity entity = new TemplateLinkEntity();
                BeanUtil.copyProperties(linkItem, entity);

                String linkId = Guid.id();
                entity.setLinkId(linkId);
                entity.setSourceTplId(linkItem.getQueryTplId());
                entity.setSourceTplViewId(linkItem.getQueryViewId());
                entity.setTargetTplId(linkItem.getTplId());
                entity.setTargetTplViewId(linkItem.getViewId());
                sortId++;
                entity.setSortId(sortId);
                entity.setCreatedBy(UserManager.get().getName());

                List<TemplateLinkFieldEnity> templateLinkFieldEnityList = buildLinkEntityFieldList(linkId, linkItem);
                entity.setTemplateLinkFieldList(templateLinkFieldEnityList);

                tplLinkList.add(entity);
            }
        }
        return tplLinkList;
    }

    /**
     * 构建跳转关联字段
     * @return
     */
    public List<TemplateLinkFieldEnity> buildLinkEntityFieldList(String linkId,TemplateLinkRsp linkItem) {
        List<TemplateLinkFieldEnity> linkFieldEnityList = new ArrayList<>();

        List<TemplateLinkFieldRsp> linkFieldList = linkItem.getTemplateLinkFieldList();
        if (CollectionUtil.isNotEmpty(linkFieldList)) {
            for (TemplateLinkFieldRsp linkFieldItem : linkFieldList) {
                TemplateLinkFieldEnity linkFieldEnity = new TemplateLinkFieldEnity();
                linkFieldEnity.setLinkId(linkId);
                linkFieldEnity.setFieldId(linkFieldItem.getFieldId());
                linkFieldEnity.setFieldCode(linkFieldItem.getFieldCode());
                linkFieldEnity.setFieldTitle(linkFieldItem.getFieldTitle());
                linkFieldEnity.setSortId(linkFieldItem.getSortId());
                linkFieldEnityList.add(linkFieldEnity);
            }
        }

        return linkFieldEnityList;
    }

    public void saveTemplateLinkFiled(List<TemplateLinkEntity> templateLinkEntityList){

        List<TemplateLinkFieldEnity> templateLinkFieldEnityList = new ArrayList<>();

        for (TemplateLinkEntity templateLinkEntity : templateLinkEntityList) {
            if(CollUtil.isEmpty(templateLinkEntity.getTemplateLinkFieldList())){
                continue;
            }

            templateLinkFieldEnityList.addAll(templateLinkEntity.getTemplateLinkFieldList());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("templateLinkFieldList", templateLinkFieldEnityList);
        dao.insert("ssm.template.link.field.batchInsert", map);
    }

}
