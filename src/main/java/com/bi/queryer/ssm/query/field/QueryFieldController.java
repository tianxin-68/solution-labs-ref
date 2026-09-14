package com.bi.queryer.ssm.query.field;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.enums.CategoryType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaSensitiveField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.filter.FieldFilterCacheConfig;
import com.bi.queryer.ssm.mgr.fieldCtg.model.AuthApplyCtgTreeRsp;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgListReq;
import com.bi.queryer.ssm.query.field.model.FieldDataUpdateTimeReq;
import com.bi.queryer.ssm.query.field.model.FieldDataUpdateTimeRsp;
import com.bi.queryer.ssm.query.field.model.FieldMetaDataReq;
import com.bi.queryer.ssm.query.filter.MultiSelectFilterCacheManager;
import com.bi.queryer.ssm.query.filter.MultiSelectFilterDatasetProvider;
import com.bi.queryer.ssm.query.filter.MultiSelectFilterResult;
import com.bi.queryer.ssm.query.log.SSMFilterQueryLogEntity;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * @Author contributor
 * @Date 14:19 2023-11-09
 * @Description 查询字段相关请求处理
 **/
@Controller
@Scope("prototype")
@RequestMapping("ssd/field")
public class QueryFieldController extends BaseController {

    @Autowired
    protected QueryFieldService service = null;

    /**
     * 字段tree（含分类）
     *
     * @return
     */
    @RequestMapping("tree")
    @ResponseBody
    public ResponseMessage buildFieldTree() {
        long startTime = System.currentTimeMillis();
        ResponseMessage result = new ResponseMessage();
        CategoryType categoryType = CategoryType.get(stringValue("categoryType"));

        String datasetId = stringValue("datasetId");
        List<JSONObject> tree = service.buildFieldTree(categoryType,datasetId);
        result.setData(tree);
        long endTime = System.currentTimeMillis();
        System.out.println("buildFieldTree程序运行时间：" + (endTime - startTime) + "ms");
        return result;
    }

    @RequestMapping("ctg/info")
    @ResponseBody
    public ResponseMessage getCtgInfo() {
        long startTime = System.currentTimeMillis();
        ResponseMessage result = new ResponseMessage();
        String ctgId = stringValue("ctgId");
        result.setData(service.getCtgInfo(ctgId));
        long endTime = System.currentTimeMillis();
        System.out.println("getCtgInfo程序运行时间：" + (endTime - startTime) + "ms");
        return result;
    }


    /**
     * 通过字段id获取互斥字段
     *
     * @return 互斥字段编码列表
     */
    @RequestMapping("getExcludeFieldsById")
    @ResponseBody
    @FreeCheckAuthority
    public ResponseMessage getExcludeFieldsById() {
        ResponseMessage result = new ResponseMessage();
        String fieldIdsStr = this.stringValue("fieldIds");
        String datasetId = stringValue("datasetId");
        String moduleCtgIds = this.stringValue("moduleCtgIds");
        String compareFieldIds = this.stringValue("compareFieldIds");

        //需要比较互斥的字段类型,lod场景只需要比较维度
        String compareFieldType = this.stringValue("compareFieldType");

        Set<String> excludeCodes = new HashSet<>();
        if (BIUtil.isEmpty(fieldIdsStr)) {
            result.setData(excludeCodes);
            return result;
        }
        List<String> fieldIdList = Arrays.asList(fieldIdsStr.split(","));
        excludeCodes = FieldUtil.getExcludeFieldById(datasetId, fieldIdList,moduleCtgIds,compareFieldIds,compareFieldType);
        result.setData(excludeCodes);
        return result;
    }

    /**
     * 通过字段id获取互斥字段
     *
     * @return 互斥字段编码列表
     */
    @RequestMapping("getExcludeFieldsByIdOld")
    @ResponseBody
    @FreeCheckAuthority
    public ResponseMessage getExcludeFieldsByIdOld() {
        ResponseMessage result = new ResponseMessage();
        String fieldIdsStr = this.stringValue("fieldIds");
        String datasetId = stringValue("datasetId");
        String moduleCtgIds = this.stringValue("moduleCtgIds");
        String compareFieldIds = this.stringValue("compareFieldIds");

        //需要比较互斥的字段类型,lod场景只需要比较维度
        String compareFieldType = this.stringValue("compareFieldType");

        Set<String> excludeCodes = new HashSet<>();
        if (BIUtil.isEmpty(fieldIdsStr)) {
            result.setData(excludeCodes);
            return result;
        }
        List<String> fieldIdList = Arrays.asList(fieldIdsStr.split(","));
        excludeCodes = FieldUtil.getExcludeFieldByIdOld(datasetId, fieldIdList,moduleCtgIds,compareFieldIds,compareFieldType);
        result.setData(excludeCodes);
        return result;
    }

    /**
     * 查询敏感字段方法
     *
     * @return
     */
    @RequestMapping("querySensitive")
    @ResponseBody
    public ResponseMessage querySensitiveField() {
        ResponseMessage result = new ResponseMessage();
        List<MetaSensitiveField> sensitiveFieldList = service.querySensitiveField();
        result.setData(sensitiveFieldList);
        return result;
    }

    /**
     * 获取字段值排序集合
     * @return
     */
    @RequestMapping("getFieldValueSortNumList")
    @ResponseBody
    public ResponseMessage getFieldValueSortNumList(){
        String fieldCodes = this.stringValue("fieldCodes");
        return service.getFieldValueSortNumList(fieldCodes);
    }

    /**
     * 获取字段元信息集合
     * @return
     */
    @RequestMapping("getFieldMetaDataList")
    @ResponseBody
    public ResponseMessage getFieldMetaDataList(){
        String fieldIds = this.stringValue("fieldIds");
        String moduleCtgId = this.stringValue("moduleCtgId");
        return service.getFieldMetaDataList(fieldIds,moduleCtgId);
    }

    /**
     * 获取字段元信息集合
     * @return
     */
    @RequestMapping("getFieldMetaDataListV2")
    @ResponseBody
    public ResponseMessage getFieldMetaDataListV2(@RequestBody FieldMetaDataReq fieldMetaDataReq){
        return service.getFieldMetaDataListV2(fieldMetaDataReq);
    }

    @RequestMapping("getFieldEnumDataList")
    @ResponseBody
    public ResponseMessage getFieldEnumDataList(){
        try {
            String pkId = this.stringValue("pkId");
            String threadNum = this.stringValue("threadNum");

            service.getFieldEnumDataList(Long.valueOf(pkId),Integer.valueOf(threadNum));
            return ResponseMessage.success("");
        }  catch (Exception e) {
            e.printStackTrace();
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 刷新过滤器数据缓存:用于定时调度
     * @return
     */
    @RequestMapping("flush/filter/data/cache")
    @ResponseBody
    public ResponseMessage flushFilterDataCache() {
        ResponseMessage result = new ResponseMessage();
        List<FieldFilterCacheConfig> filterCacheConfigs = MultiSelectFilterCacheManager.getFieldFilterCacheConfigs();
        List<String> infos = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        // 定时刷新缓存为24小时过期
        params.put("cacheExpireMinute", this.stringValue("cacheExpireMinute", "1440"));
        params.put("pagination","true");
        params.put("currentPageNum", 1);
        params.put("pageSize", 200);
        params.put("cacheSpaceKey", MultiSelectFilterCacheManager.cache_space_schedule_key);

        Integer sleepSecond = this.intValue("sleepSecond", 1);
        long t1 = System.currentTimeMillis();
        MultiSelectFilterDatasetProvider datasetProvider = (MultiSelectFilterDatasetProvider) SpringContextUtil.getBean("multiSelectFilterDatasetProvider");
        for(FieldFilterCacheConfig cfg : filterCacheConfigs){
            MetaField metaField = SSDMetaCacheManager.getField(cfg.getFieldId());
            if(metaField == null){
                infos.add(String.format("%s 字段不存在：id=%s,code=%s,title=%s", DateUtil.now(),cfg.getFieldId(), cfg.getFieldCode(), cfg.getFieldTitle()));
                continue;
            }
            try {
                long tx1 = System.currentTimeMillis();
                // 先删除
                MultiSelectFilterDatasetProvider.clearCache(cfg.getFieldId());
                // 再构建
                params.put("sessionId", Guid.id());
                MultiSelectFilterResult filterResult = (MultiSelectFilterResult) datasetProvider.buildDataset(metaField, params);
                long tx2 = System.currentTimeMillis();

                String info = String.format("%s id=%s,code=%s,title=%s,rows=%s,缓存%s,耗时%s秒",
                        DateUtil.now(),
                        metaField.getId(), metaField.getCode(), metaField.getTitle(),
                        filterResult.getRows() != null ? filterResult.getRows().size() : 0,
                        filterResult.getRows() != null ? "成功" : "失败",
                        (tx2 - tx1)/1000.0
                );
                System.out.println(info);
                infos.add(info);
                Thread.sleep(sleepSecond * 1000);
            } catch (Exception e) {
                String info = String.format("%s 写缓存失败，原因：%s，字段信息：id=%s,code=%s,title=%s", DateUtil.now(), e.getMessage(),cfg.getFieldId(), cfg.getFieldCode(), cfg.getFieldTitle());
                infos.add(info);
            }
        }
        long t2 = System.currentTimeMillis();
        infos.add(String.format("总耗时：%s秒", (t2-t1)/1000.0));
        result.setData(BIUtil.listToStr(infos, "\n"));
        return result;
    }

    /**
     * 刷新过滤器配置缓存:用于定时调度和后台手动刷新
     * @return
     */
    @RequestMapping("flush/filter/cfg/cache")
    @ResponseBody
    public ResponseMessage flushFilterConfigCache() {
        ResponseMessage result = new ResponseMessage();
        MultiSelectFilterCacheManager.flushConfigAllServer();
        result.setData("刷新过滤器配置缓存成功");
        return result;
    }

    /**
     * 更新过滤日志
     * @return
     */
    @RequestMapping("/filter/log/update")
    @ResponseBody
    public ResponseMessage updateFilterQueryLog(@RequestBody SSMFilterQueryLogEntity entity) {
        ResponseMessage result = new ResponseMessage();
        service.updateFilterQueryLog(entity);
        return result;
    }

    /**
     * 查询字段数据更新时间
     * @param req
     * @return
     */
    @RequestMapping("/queryFieldDataUpdateTime")
    @ResponseBody
    public SSMResponseMessage<String> queryFieldDataUpdateTime(@RequestBody FieldDataUpdateTimeReq req){
        return SSMResponseMessage.success("查询字段数据更新时间成功！",service.queryFieldDataUpdateTime(req));
    }

    /**
     * 查询字段数据更新时间V2
     * @param req
     * @return
     */
    @RequestMapping("/queryFieldDataUpdateTimeV2")
    @ResponseBody
    public SSMResponseMessage<FieldDataUpdateTimeRsp> queryFieldDataUpdateTimeV2(@RequestBody FieldDataUpdateTimeReq req){
        return SSMResponseMessage.success("查询字段数据更新时间成功！",service.queryFieldDataUpdateTimeV2(req));
    }

    /**
     * 构建权限申请目录树
     * <p>
     * 供前台权限申请页展示目录树，包含目录基本信息、敏感等级、继承关系及当前用户权限状态。
     * </p>
     *
     * @param req 请求参数，datasetId 指定数据集范围
     * @return 权限申请目录树节点列表
     */
    @RequestMapping("buildAuthApplyCtgTree")
    @ResponseBody
    public SSMResponseMessage<List<AuthApplyCtgTreeRsp>> buildAuthApplyCtgTree(@RequestBody CtgListReq req) {
        return service.buildAuthApplyCtgTree(req);
    }

    /**
     * 获取当前登录用户在指定数据集下第一个有权限的目录 id
     *
     * @param datasetId 数据集 id，为空时使用默认数据集
     * @return 第一个有权限的目录 id，无可用目录时 data 为空
     */
    @RequestMapping("firstAuthCtgId")
    @ResponseBody
    public SSMResponseMessage<String> getFirstAuthCtgId(String datasetId) {
        return service.getFirstAuthCtgId(datasetId);
    }

}
