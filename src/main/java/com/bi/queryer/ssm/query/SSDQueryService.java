package com.bi.queryer.ssm.query;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.DataAuthMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.mail.MailServer;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.meta.dateRangeProgress.DateRangeProgressReq;
import com.bi.queryer.ssm.meta.dateRangeProgress.DateRangeProgressRsp;
import com.bi.queryer.ssm.mgr.fieldCtg.FieldCtgService;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.query.template.model.TemplateRsp;
import com.bi.queryer.ssm.system.access.SystemAccessBlacklistManager;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.authority.AuthorityService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.role.RoleService;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * User: contributor
 * Date: 2020/2/4getAuthCtgList
 * Time: 12:28
 * Description:
 */
@Service
@Scope("prototype")
@Qualifier("SSDQueryService")
public class SSDQueryService {

    private final static String notification_url = "http://localhost:9010/WeiXinWork/Push/Text";
    private final static String notification_requestID = "change-me";
    private final static String notification_etlJobSchedule = "多维分析";
    private final static String APPID = "tx-ssm-server";

    private static final Integer MAX_THREAD_NUM = 4;

    private static ExecutorService singlePool = Executors.newFixedThreadPool(MAX_THREAD_NUM);


    @Autowired
    private BaseDao dao = null;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TemplateConfigService templateService ;

    @Autowired
    private QueryFieldService queryFieldService;

    @Autowired
    private FieldCtgService fieldCtgService;

    /**
     * 推送安全部门kafka日志
     * @param logId
     * @param type  1下载，0查询
     */
    public void sendLogToKafka(String logId, Integer type) {

        try {
            singlePool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);    //延时5秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    List<APIKafkaLog> logList = new ArrayList<>();
                    if (Enabled.value(type)) {
                        //下载
                        logList = (List<APIKafkaLog>) dao.queryObjectList("ssm.query.getKafkaExportLog", logId);
                    } else {
                        //查询
                        logList = (List<APIKafkaLog>) dao.queryObjectList("ssm.query.getKafkaQueryLog", logId);
                    }

                    if (CollUtil.isEmpty(logList) || logList.size() < 1) {
                        return;
                    }
                    APIKafkaLog apiKafkaLog = logList.get(0);

                    String url = SC.v("security.log.send.kafka.url");

                    JSONObject sendJson = new JSONObject();

                    JSONObject json = new JSONObject();

                    json.put("startTime", apiKafkaLog.getStartTime());
                    json.put("endTime", apiKafkaLog.getEndTime());

                    json.put("source", apiKafkaLog.getSource());
                    json.put("eventType", apiKafkaLog.getEventType());
                    json.put("userName", apiKafkaLog.getUserName());

                    json.put("firstlySector", apiKafkaLog.getFirstlySector());
                    json.put("secondarySector", apiKafkaLog.getSecondarySector());
                    json.put("thirdSector", apiKafkaLog.getThirdSector());

                    json.put("filterInfo", getFilterInfo(apiKafkaLog.getFilterInfo()));
                    json.put("rowCount", apiKafkaLog.getRowCount());

                    json.put("sensitiveField", apiKafkaLog.getSensitiveField());
                    json.put("dataType", apiKafkaLog.getDataType());
                    json.put("sensitiveLevel", apiKafkaLog.getSensitiveLevel());

                    json.put("status", apiKafkaLog.getStatus());
                    json.put("tags", apiKafkaLog.getTags());

                    sendJson.put("kafkaLog", json.toJSONString());

                    System.out.println("***********插入安全日志Kafka的url: " + url);
                    System.out.println("***********插入安全日志Kafka数据: " + sendJson.toJSONString());
                    String result = HttpRequest.post(url)
                            .body(String.valueOf(sendJson))
                            .execute().body();
                }
            });

        } catch (Exception e) {
            System.out.println("插入安全日志Kafka异常:" + e.getMessage());
        }
    }

    private String getFilterInfo(String filterInfo) {
        try {
            JSONObject configJSON = JSONObject.parseObject(filterInfo);
            String result = JSONArray.toJSONString(configJSON.getJSONArray("filter"));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 查询ssd_query_field_stats获取常用字段
     * @return 以目录id为key的map
     */
    private HashMap<String, List<SSDQueryFieldStatsEntity>> getSSDQueryFieldStatsList() {
        HashMap<String, List<SSDQueryFieldStatsEntity>> results = new HashMap<>();
        String sqlId = "ssm.query.querySSDQueryFieldStatsList";
        List<SSDQueryFieldStatsEntity> SSDQueryFieldStatsList = (List<SSDQueryFieldStatsEntity>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        if (!SSDQueryFieldStatsList.isEmpty()) {
            SSDQueryFieldStatsList.forEach(entity -> {
                if (results.containsKey(entity.getCtgId())) {
                    results.get(entity.getCtgId()).add(entity);
                } else {
                    List<SSDQueryFieldStatsEntity> newList = new ArrayList<>();
                    newList.add(entity);
                    results.put(entity.getCtgId(), newList);
                }
            });
        }
        return results;
    }

    /**
     * 获取目录修改日志
     * @param ctgId
     * @param ctgChangeLog
     * @return
     */
    public String getCtgChangeLog(String ctgId, List<SSDFieldCtgChangLog> ctgChangeLog) {
        if (BIUtil.isEmpty(ctgChangeLog)) {
            return "";
        }

        String result = "";
        Optional<SSDFieldCtgChangLog> optional = ctgChangeLog.stream().filter(a -> ctgId.equalsIgnoreCase(a.getCtgId())).findAny();
        if (optional.isPresent()) {
            SSDFieldCtgChangLog ssdFieldCtgChangLog = optional.get();
            result = "数据已更新到" + ssdFieldCtgChangLog.getDataDate();
        }

        return result;
    }

    /**
     * 创建数据权限grid
     * @param paramMap
     * @return
     */
    public DataGrid buildDataAuthGrid(Map<String, String> paramMap) {
        DataGrid grid = new DataGrid(-1, -1);
        grid.setAutoSize(true);
        grid.setShowExport(false);
        grid.setPagination(true);// 使用分页
        grid.setShowHideColumnButton(false);

        DataGridColumn column = new DataGridColumn("moduleName", "模块", -1, "left"); // 添加列
        column.setHidden(true); // id不需要显示
        grid.addColumn(column);
        column = new DataGridColumn("dimName", "维度", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("itemValue", "值", -1, "left"); // 添加列
        grid.addColumn(column);

        grid.setDataSetProvider(new IDataGridDataSetProvider() {
            @Override
            public List<? extends JSONSerializable> getDataSet(Map queryParamMap) {
                List<DataAuth> dataAclList = (List<DataAuth>) dao.queryObjectList("ssm.query.queryDataAuthByUser", queryParamMap);
                return dataAclList;
            }

            @Override
            public Integer getDataSetTotalSize(Map queryParamMap) {
                return dao.queryCount("ssm.query.queryDataAuthByUserCount", queryParamMap);
            }
        }, paramMap);
        return grid;
    }

    /**
     * 获取用户数据权限列表
     * 其中dimCode=fieldCode
     * @param userName
     * @return
     */
    public List<MetaFieldDataAuth> getDataAuthByUser(String userName) {
        return getDataAuthByUser(userName, false);
    }

    /**
     * 获取用户数据权限列表
     * 其中dimCode=fieldCode
     * @param userName
     * @param containsNoAuth 是否包含没有权限的信息
     * @return
     */
    public List<MetaFieldDataAuth> getDataAuthByUser(String userName, boolean containsNoAuth) {
        Map<String, String> queryParams = new HashMap<>();
        User user = UserManager.get();
        queryParams.put("userName", userName);
        queryParams.put("deptId", user == null ? "" : user.getDeptId());

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        List<MetaFieldDataAuth> dataAclList = (List<MetaFieldDataAuth>) dao.queryObjectList("ssm.query.queryDataAuthByUser", queryParams,dataEnvDataSourceType);


        List<MetaFieldDataAuth> newDataAclList = new ArrayList<>();
        for (MetaFieldDataAuth dataAuth : dataAclList) {
            if (BIUtil.isNotEmpty(dataAuth.getItemCode()) || BIUtil.isNotEmpty(dataAuth.getItemValue())) {
                newDataAclList.add(dataAuth);
                continue;
            }
            // 以下为空值处理
            // 未配置有所有，则动态添加用户数据权限
            if (DataAuthMode.get(dataAuth.getAuthMode()) == DataAuthMode.no_cfg_all) {
                dataAuth.setItemValue(BIConsts.Ssm_Row_Acl_All_Dim_Value_Title);
                dataAuth.setItemCode(BIConsts.Ssm_Row_Acl_All_Dim_Value);
                dataAuth.setRemark("权限管控模式为：no_cfg_all，系统自动赋值");
                newDataAclList.add(dataAuth);
            }
            if (containsNoAuth && DataAuthMode.get(dataAuth.getAuthMode()) == DataAuthMode.no_cfg_none) {
                dataAuth.setItemValue("无权限");
                dataAuth.setItemCode("无权限");
                dataAuth.setRemark("无权限");
                newDataAclList.add(dataAuth);
            }
        }
        // 管控的字段code和白皮书编码都起效：按管控的字段code对应的白皮书编码copy一份权限清单
        List<MetaFieldDataAuth> whitePaperAclList = new ArrayList<>();
        for(MetaFieldDataAuth dataAuth : newDataAclList){
            if(BIUtil.isEmpty(dataAuth.getWhitePaperCode())) {
                continue;
            }
            MetaFieldDataAuth copy = JSONObject.parseObject(dataAuth.toJSON().toJSONString(), MetaFieldDataAuth.class);
            copy.setFieldCode(dataAuth.getWhitePaperCode());
            copy.setDimRealCode(dataAuth.getWhitePaperCode());
            copy.setDimCode(dataAuth.getWhitePaperCode());
            whitePaperAclList.add(copy);
        }
        newDataAclList.addAll(whitePaperAclList);
        // 去重
        newDataAclList = newDataAclList.stream().distinct().collect(Collectors.toList());
        return newDataAclList;
    }

    /**
     * 获取有权限的字段编码列表
     * @return
     */
    public List<String> getAuthFieldCodes(String userName, String authModule, String authDim) {
        // 权限表存储为字段id权限，转为字段编码
        AuthorityService authorityService = (AuthorityService) SpringContextUtil.getBean("authorityService");
        List<KeyValuePair> fieldAcls = authorityService.getDataItems(userName, authModule, authDim);
        long endTime1 = System.currentTimeMillis();
        List<String> aclCodes = new ArrayList<String>();
        for (KeyValuePair kv : fieldAcls) {
            MetaField f = SSDMetaCacheManager.getField(kv.getKey());
            if (f != null && !aclCodes.contains(f.getCode())) {
                aclCodes.add(f.getCode());
                // 同时添加其扩展字段
                List<MetaField> extendFields = f.getExtendFields();
                if(BIUtil.isNotEmpty(extendFields)){
                    for(MetaField e : extendFields){
                        aclCodes.add(e.getCode());
                    }
                }
            }
        }

        /*
        List<String> extCodes = new ArrayList<String>();
        for (String code : aclCodes) {
            List<MetaField> aclFields = SSDMetaCacheManager.getFieldByCode(code);
            if (aclFields == null) {
                continue;
            }
            for (MetaField f : aclFields) {
                List<MetaField> extendFields = f.getExtendFields();
                if (extendFields == null) {
                    continue;
                }
                for (MetaField ext : extendFields) {
                    if (!extCodes.contains(ext.getCode())) {
                        extCodes.add(ext.getCode());
                    }
                }
            }
        }
        aclCodes.addAll(extCodes);
        */
        // 去重
        aclCodes = aclCodes.stream().distinct().collect(Collectors.toList());
        return aclCodes;
    }

    /**
     * 获取有权限的目录下的所有字段编码列表
     * @return
     */
    public List<String> getAuthFieldCodesByCtg(String userName, String authModule, String authDim, String datasetId) {

        Map<String, Integer> inheritMap = fieldCtgService.queryAllInherit();

        List<MetaFieldCategory> rootFrontCategories = SSDMetaCacheManager.getFrontCategories().values().stream().filter(c ->
                BIConsts.Category_Root_Id.equalsIgnoreCase(c.getParentId()) && c.getDatasetId().contains(datasetId)
        ).collect(Collectors.toList());

        // 目录权限列表：根据开关和用户部门/角色条件决定走哪套逻辑（委托 QueryFieldService 统一处理）
        List<String> ctgAcls = queryFieldService.resolveCtgAcls(userName, authModule, authDim, datasetId, rootFrontCategories, inheritMap);

        // 去重
        ctgAcls = ctgAcls.stream().distinct().collect(Collectors.toList());

        Map<String,String> ctgAclMap = new HashMap<>();
        for(String key : ctgAcls){
            ctgAclMap.put(key,key);
        }

        List<String> aclCodes = new ArrayList<String>();
        Map<String,String> aclCodeMap = new HashMap<>(5000);
        for (String v : ctgAcls) {
            List<MetaField> fieldAcls = SSDMetaCacheManager.getFieldByCategoryWithAuth(v,inheritMap,ctgAclMap);
            for (MetaField f : fieldAcls) {
                if (f != null && !aclCodeMap.containsKey(f.getCode())) {
                    aclCodes.add(f.getCode());
                    aclCodeMap.put(f.getCode(),f.getCode());
                    // 同时添加其扩展字段
                    List<MetaField> extendFields = f.getExtendFields();
                    if (BIUtil.isNotEmpty(extendFields)) {
                        for (MetaField e : extendFields) {
                            aclCodes.add(e.getCode());
                        }
                    }
                    // 添加计算字段的原子字段
                    if(CollUtil.isNotEmpty(f.getCalcAtomFields())){
                        for (MetaField e : f.getCalcAtomFields()) {
                            aclCodes.add(e.getCode());
                        }
                    }
                }
            }
        }

        // 去重
        aclCodes = aclCodes.stream().distinct().collect(Collectors.toList());

        return aclCodes;
    }

    /**
     * 获取有权限的目录列表
     * @return
     */
    public List<String> getAuthCtgList(String userName, String authModule, String authDim) {
        // 权限表存储为字段id权限，转为字段编码
        AuthorityService authorityService = (AuthorityService) SpringContextUtil.getBean("authorityService");
        List<KeyValuePair> ctgAcls = authorityService.getDataItems(userName, authModule, authDim);
        List<String> aclCtgList = new ArrayList<String>();
        for (KeyValuePair kv : ctgAcls) {
            if (StringUtils.isNotBlank(kv.getKey())) {
                aclCtgList.add(kv.getKey());
            }
        }

        //后台配置的权限


        aclCtgList = aclCtgList.stream().distinct().collect(Collectors.toList());
        return aclCtgList;
    }

    /**
     * 获取配置
     * @return
     */
    public ResponseMessage getSystemConfig() {
        long startTime = System.currentTimeMillis();
        ResponseMessage result = new ResponseMessage();

        Map<String, Object> map = new HashMap<>();

        //1 获取用户信息
        User user = UserManager.get();
        //判断多维分析管理员
        String roleId = SC.v("ssm.admin.roleId", "");
        Boolean flag = roleService.queryRoleUserNameExist(roleId, user.getName());

        String objectToJson = JSON.toJSONString(user);
        User userFront = JSON.parseObject(objectToJson, User.class);
        if (Enabled.value(user.getIsAdmin()) || flag) {
            userFront.setIsAdmin(1);
            userFront.setHasSsmAuth(1);
        }

        map.put("user", userFront.toJSON());

        //2 获取最大的导出行数
        BigDecimal maxExportRows = new BigDecimal(SC.v("ssm.export.rows", "200"));
        BigDecimal bigDecimal = maxExportRows.multiply(new BigDecimal(10000));
        map.put("maxExportRows", bigDecimal);

        /**
        //3 获取邮件默认审批人邮箱
        String mailApprove = SC.v("ssm.mail.approve", "contributor@example.com");
        map.put("mailApprove", mailApprove);

        //4 获取邮件默认审批人
        String approveUserName = SC.v("ssm.mail.approveUsername", "contributor@example.com");
        map.put("approveUserName", approveUserName);
        */


        //5 多维分析默认查询行数
        Integer searchLimit = QuerySessionSettingManager.getQueryRowLimit(); //Integer.parseInt(SC.v("ssm.search.limit", "20000"));
        String searchLimitDesc = (searchLimit/10000) + "W";  // SC.v("ssm.search.limit.desc", "2W");
        map.put("searchLimit", searchLimit);
        map.put("searchLimitDesc", searchLimitDesc);

        Integer replicateLimit = Integer.valueOf(SC.v("ssm.replicate.limit", "1000"));
        map.put("replicateLimit", replicateLimit);
        map.put("replicateLimitDesc", replicateLimit);

        map.put("replicateWhiteList", SC.v("ssm.replicate.whitelist", "chenmin"));

        //  是否自动查询 1: 允许自动查询，0，禁止自动查询
        map.put("autoQueryEnabled", Integer.parseInt(SC.v("ssm.auto.query.enabled", "1")));

        // 计算字段支持最大字符数
        map.put("calcFieldMaxCharSize", Integer.parseInt(SC.v("ssm.calc.field.max.char.size", "1024")));

        // 计算维度筛选支持最大字符数
        map.put("calcDimFieldFilterMaxCharSize", SC.v("ssm.calc.dim.field.filter.max.char.size", "10240"));

        //6 自助取数管理员
        /**
        String adminName = SC.v("ssd.admin", "contributor(contributor)");
        map.put("adminName", adminName);
         */

        //7 用户拥有的敏感数据行级权限
        /**
        List<String> aclSensitiveCodes = getAuthFieldCodes(user.getName(), SSDUtil.Field_Auth_Module, SSDUtil.Field_Sensitive_Auth_Dim);
        map.put("aclSensitiveCodes", aclSensitiveCodes);
         */

        //用户是否有权限访问系统
        //不在系统访问黑名单中，则有权限
        map.put("hasSystemAccess", SystemAccessBlacklistManager.isBlacklisted(user.getName())? Enabled.NO.getId(): Enabled.YES.getId());

        result.setData(map);
        long endTime = System.currentTimeMillis();
        System.out.println("getSystemConfig程序运行时间：" + (endTime - startTime) + "ms");
        return result;
    }

    /**
     * 敏感数据邮件申请
     * @param map
     * @return
     */
    public ResponseMessage sendApplySensitiveDataMail(Map<String, Object> map) {
        ResponseMessage result = new ResponseMessage();
        try {

            map.put("subject", "多维分析-敏感数据申请");
            User user = UserManager.get();
            map.put("sendName", user.getName());
            map.put("realName", user.getRealName());

            String templateId = BIUtil.nvl(map.get("templateId"), "");
            String viewId = BIUtil.nvl(map.get("viewId"), "");
            SSDQueryTemplate sSDQueryTemplate = templateService.getTemplateById(templateId,viewId);

            String templateName = "";
            if (sSDQueryTemplate != null) {
                templateName = sSDQueryTemplate.getName();
            }

            map.put("templateName", templateName);

            MailServer.sendSensitiveData(map);

        } catch (Exception e) {
            return new ResponseMessage(false, e.getMessage());
        }

        return result;
    }



    /**
     * 获取用户拥有的所有的行级权限
     * @return
     */
    public ResponseMessage getUserAllSSDRowRole() {

        ResponseMessage result = new ResponseMessage();

        try {

            List<MetaFieldDataAuth> userDataAclList = this.getDataAuthByUser(UserManager.get().getName(), true);

            // 分组合并
            Map<String, List<MetaFieldDataAuth>> dimValueMap = new LinkedHashMap<>();
            for (MetaFieldDataAuth dataAuth : userDataAclList) {
                String fieldCode = dataAuth.getFieldCode();
                List<MetaFieldDataAuth> list = null;
                if (dimValueMap.containsKey(fieldCode)) {
                    list = dimValueMap.get(fieldCode);
                    list.add(dataAuth);
                } else {
                    list = new ArrayList<>();
                    list.add(dataAuth);
                    dimValueMap.put(fieldCode, list);
                }
            }

            List<JSONObject> list = new ArrayList<>();
            for (String fieldCode : dimValueMap.keySet()) {
                List<MetaFieldDataAuth> acl = dimValueMap.get(fieldCode);
                if (BIUtil.isEmpty(acl)) {
                    continue;
                }
                // 去掉code=白皮书编码的权限显示：避免重复
//                if(fieldCode.equalsIgnoreCase(acl.get(0).getWhitePaperCode())){
//                    continue;
//                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("fieldCode", fieldCode);
                jsonObject.put("dimName", acl.get(0).getDimName());
                List<String> dimValues = acl.stream().map(MetaFieldDataAuth::getItemValue).collect(Collectors.toList());
                jsonObject.put("dimValues", BIUtil.listToStr(dimValues));
                list.add(jsonObject);
            }
            result.setData(list);
        } catch (Exception e) {
            return new ResponseMessage(e);
        }

        return result;
    }

    /**
     * 邮件订阅-保存
     * @return
     */
    public ResponseMessage saveMailSubscription(Map<String, Object> map, String sql, String token, JSONArray metainfo) {

        ResponseMessage responseMessage = new ResponseMessage();

        try {

            //收件人
            map.put("emailRecipients", UserManager.get().getName());
            //数据源
            map.put("datasource", "presto");
            //sql
            map.put("querySql", sql);
            //来源
            map.put("source", "ssm");
            //字段集合
            map.put("metaInfo", metainfo);

            Map<String, String> headers = new HashMap<>();
            headers.put(BIConsts.User_Token, token);

            String mailServiceUrl = SC.v("ssm.mailService.address", "");
            if (BIUtil.isNotEmpty(mailServiceUrl)) {
                String result = HttpUtil.doPost(mailServiceUrl + "/config/ssm/save", map, null, headers, 20000);
                System.out.println(result);
                responseMessage = JSON.parseObject(result, ResponseMessage.class);
            } else {
                throw new BIException("没有配置邮件服务url地址");
            }

        } catch (Exception e) {
            return new ResponseMessage(e);
        }

        return responseMessage;
    }

    /**
     * 获取前台目录所有可查询的字段
     * @param ctgId
     * @return
     */
    public List<MetaField> queryFieldsByCtgId(String ctgId) {

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        List<MetaField> metaFieldList = (List<MetaField>) dao.queryObjectList("fieldCtg.queryFieldsByCtgId", ctgId,dataEnvDataSourceType);
        return metaFieldList;
    }

    /**
     * 字段合法性检测
     * @param sql
     * @return
     */
    public ResponseMessage checkField(String sql) {
        ResponseMessage responseMessage = new ResponseMessage();

        Statement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {

            if (sql.toUpperCase().contains("GROUP BY")) {
                sql = sql.replaceAll("GROUP BY", "where 1=2 GROUP BY");
            } else {
                sql += " where 1=2 ";
            }


            conn = DBUtil.getConn(DBUtil.getDataSourceType());
            stmt = conn.createStatement();
            res = stmt.executeQuery(sql);
            while (res.next()) {

            }

        } catch (Exception e) {
            responseMessage = new ResponseMessage(e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return responseMessage;
    }

    /**
     * 发BI Queryer通消息
     */
    public void sendMsg(String msg, List<String> users) {

        System.out.println("BI Queryer通：" + msg);

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("Content", msg);
        jsonObject.put("Sign", notification_etlJobSchedule);
        jsonObject.put("Users", users);

        String result = HttpRequest.post(notification_url)
                .header("RequestID", notification_requestID)
                .header("wx_client_app_id", APPID)
                .body(jsonObject.toJSONString())
                .execute().body();

        System.out.println("BI Queryer通：" + msg + "结果：" + result);
    }

    /**
     * 获取多维分析管理员
     */
    public List<String> mergeAclCodes(List<String> list1, List<String> list2) {
        List<String> allList = new ArrayList<>();
        try {
            allList.addAll(list1);
            allList.removeAll(list2);
            allList.addAll(list2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allList;
    }

    /**
     * 分析config中包含的所有字段
     * @param queryConfigure
     * @return
     * @throws Exception
     */
    @Deprecated
    public List<QueryField> loadAllFieldList(QueryConfigure queryConfigure, List<StarModel> models) {
        return null;
        /*
        List<QueryField> fieldList = queryConfigure.getAllQueryOriginFields();

        if (BIUtil.isEmpty(fieldList)) {
            return Collections.emptyList();
        }

        if(BIUtil.isNotEmpty(models)){
            for(StarModel model : models){
                fieldList.addAll(model.getFields());
            }
        }

        // 拆分所有结果字段和过滤字段
        Map<String, QueryField> resultFields = new HashMap<>(16);
        Map<String, QueryField> filterFields = new HashMap<>(16);
        for(QueryField f : fieldList){
            if(f.getIsResult()){
                resultFields.put(f.getCode(), f);
            }
            if(f.getIsFilter()){
                filterFields.put(f.getCode(), f);
            }
        }

        // 再同步字段的结果和过滤属性
        Map<String, QueryField> allFields = new HashMap<>(16);
        for(QueryField f : fieldList){
            QueryField cp = f.clone();
            if(resultFields.containsKey(cp.getCode())){
                cp.setIsResult(true);
            }
            if(filterFields.containsKey(cp.getCode())){
                cp.setIsFilter(true);
            }
            allFields.put(cp.getId(), cp);
        }
        return new ArrayList<>(allFields.values());
         */
        /*
        Map<String, QueryField> res = new HashMap<>(16);
        for (QueryField f : fieldList) {
            QueryField field = res.get(f.getCode());
            if (field == null) {
                res.put(f.getCode(), f);
                continue;
            }
            if (Boolean.TRUE.equals(f.getIsResult())) {
                field.setIsResult(true);
            }

            if (Boolean.TRUE.equals(f.getIsFilter())) {
                field.setValues(f.getValues());
                field.setIsFilter(true);
            }
        }
        return new HashSet<>(res.values());
         */
    }

    /**
     * 检验规则
     * @return
     */
    public Map<String, Object> checkQueryRules(QueryConfigure config) {

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("flag", true);
        resultMap.put("message", "");
        // 1、校验字段是否全部存在
        Map<String, Object> map1 = SSDUtil.checkFieldExist(config);
        if (CollUtil.isNotEmpty(map1)) {
            return map1;
        }

        // 2、校验字段互斥性
        /*Map<String, Object> map2 = SSDUtil.checkFieldExclude(config);
        if (CollUtil.isNotEmpty(map2)) {
            return map2;
        }*/
        // 5、校验模型数量不能超过3个（20221031多维分析不用）
       /* Map<String, Object> map3 = SSDUtil.checkModelsSize(config);
        if(CollUtil.isNotEmpty(map3)){
            return map3;
        }*/

        // 6、校验是否有日期项
        /*Map<String, Object> map4 = SSDUtil.checkFilterDateField(config, engine);
        if (CollUtil.isNotEmpty(map4)) {
            return map4;
        }*/
        //7 判断查询区域是否有内容
        if (CollUtil.isEmpty(config.getResult().getFields())) {
            resultMap.put("flag", false);
            resultMap.put("message", "请先拖拽需要查询的字段到查询区域再进行查询操作！");
            return resultMap;
        }
        return resultMap;
    }

    /**
     * 校验数据就绪时间
     * @param engine
     * @return
     */
    public DataAvailableTimeResp checkDataAvailableTime(QueryEngine engine) {

        DataAvailableTimeResp dataAvailableTimeResp = new DataAvailableTimeResp();

        try {
            dataAvailableTimeResp = SSDUtil.checkDataAvailableTime(engine);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dataAvailableTimeResp;
    }

    // 校验用户是否有查询模块权限
    public Map<String, Object> checkQueryAuth(QueryEngine engine) {
        Map<String, Object> checkResult = MapUtil.newHashMap();
        // 获取所有前台目录
        Map<String, MetaFieldCategory> allCategoryMap = SSDMetaCacheManager.getFrontCategories();
        // 校验是否缺少目录权限
        Map<String, Set<String>> ctgCheckData = checkCtgAuth(engine, allCategoryMap);
        Set<String> ctgCheckResult = ctgCheckData.get("ctgCheckResult");
        // 校验是否存在有某模块权限，但没有该模块所需的行级权限。
        Set<String> rowCheckResult = checkRowAuth(ctgCheckData.get("usedCtgList"), allCategoryMap);
        if (CollectionUtil.isNotEmpty(ctgCheckResult) || CollectionUtil.isNotEmpty(rowCheckResult)) {
            checkResult.put("ctgCheckResult", ctgCheckResult);
            checkResult.put("rowCheckResult", rowCheckResult);
        }
        return checkResult;
    }


    /**
     * 返回无权限的目录及使用到的有权限的目录（用于后续校验）
     * @param engine
     * @param allCategoryMap
     * @return
     */
    private Map<String, Set<String>> checkCtgAuth(QueryEngine engine, Map<String, MetaFieldCategory> allCategoryMap) {
        Map<String, Set<String>> checkData = MapUtil.newHashMap();
        Set<String> ctgCheckResult = new HashSet<>();
        // 查询所用到的模块（有权限的），用于校验行级权限时使用。
        Set<String> usedCtgList = new HashSet<>();
        String userName = UserManager.get() == null ? "" : UserManager.get().getName();
        // 获取用户目录权限列表
        List<String> aclCtgList = getAuthCtgList(userName, SSDUtil.Ctg_Auth_Module, SSDUtil.Ctg_Auth_Dim);

        String datasetId = engine.getConfig().getSettings().getDatasetId();
        Map<String, Integer> inheritMap = fieldCtgService.queryAllInherit();

        List<MetaFieldCategory> rootFrontCategories = SSDMetaCacheManager.getFrontCategories().values().stream().filter(c ->
                BIConsts.Category_Root_Id.equalsIgnoreCase(c.getParentId()) && c.getDatasetId().contains(datasetId)
        ).collect(Collectors.toList());

        //后台配置的数据集目录权限
        List<String> ctgAuthList = queryFieldService.getConfigAuthCtgList(datasetId, rootFrontCategories, inheritMap);

        //递归查找所有的模块目录
        for(String ctgId : ctgAuthList){
            aclCtgList.addAll(SSDMetaCacheManager.getModuleCtgByCategory(ctgId));
        }

        if (CollUtil.isNotEmpty(ctgAuthList)) {
            aclCtgList.addAll(ctgAuthList);
        }

        // 被用于申请权限的目录数据
        Map<String, List<String>> moduleCategoryMap = MapUtil.newHashMap();
        // 初始化申请权限目录数据（下级的isModule=0且当前目录的isModule=1）
        for (String key : allCategoryMap.keySet()) {
            MetaFieldCategory metaFieldCategory = allCategoryMap.get(key);
            if (metaFieldCategory.isLeafModule()) {
                moduleCategoryMap.put(key, getFieldIdList(metaFieldCategory));
            }
        }
        // 构建星模型
        List<StarModel> models = engine.createModels();
        //遍历星模型，校验所有字段所属的模块，用户是否都有权限
        for (StarModel model : models) {
            for (QueryField field : model.getFields()) {
                if (field.isVirtual()) {
                    continue;
                }
                for (String key : moduleCategoryMap.keySet()) {
                    if (moduleCategoryMap.get(key).contains(field.getId())) {
                        if (aclCtgList.contains(key)) {
                            usedCtgList.add(key);
                        } else {
                            ctgCheckResult.add(allCategoryMap.get(key).getCategoryPath());
                        }
                    }
                }
            }
        }
        checkData.put("ctgCheckResult", ctgCheckResult);
        checkData.put("usedCtgList", usedCtgList);
        return checkData;
    }

    /**
     * 获取目录下的所有字段列表
     * @param metaFieldCategory
     * @return
     */
    private List<String> getFieldIdList(MetaFieldCategory metaFieldCategory) {
        List<String> fieldIdList = ListUtil.list(false);
        if (CollectionUtil.isNotEmpty(metaFieldCategory.getFields())) {
            metaFieldCategory.getFields().forEach(field -> {
                fieldIdList.add(field.getId());
            });
        }
        if (CollectionUtil.isNotEmpty(metaFieldCategory.getChildren())) {
            metaFieldCategory.getChildren().forEach(child -> {
                fieldIdList.addAll(getFieldIdList(child));
            });
        }
        return fieldIdList;
    }

    /**
     * 校验是否存在有某模块权限，但没有该模块所需的行级权限。
     * @param usedCtgList
     * @param allCategoryMap
     * @return
     */
    private Set<String> checkRowAuth(Set<String> usedCtgList, Map<String, MetaFieldCategory> allCategoryMap) {
        Set<String> rowCheckResult = new HashSet<>();
        // 获取用户的行级权限数据
        List<MetaFieldDataAuth> userDataAclList = this.getDataAuthByUser(UserManager.get().getName(), true);
        // 校验用户是否存在，拥有了某个模块权限，但并没有该模块所需的行级权限
        usedCtgList.forEach(item -> {
            MetaFieldCategory metaFieldCategory = allCategoryMap.get(item);
            Map<String, List<String>> authMap = metaFieldCategory.getAuthMap();
            for (String dimCode : authMap.keySet()) {
                List<String> authList = authMap.get(dimCode);
                Boolean canPass = false;
                if (CollectionUtil.isEmpty(authList)) {
                    continue;
                }
                if (authList.contains(BIConsts.Ssm_Row_Acl_All_Dim_Value)) {
                    for (MetaFieldDataAuth metaFieldDataAuth : userDataAclList) {
                        if (dimCode.equals(metaFieldDataAuth.getDimRealCode()) && !"无权限".equals(metaFieldDataAuth.getItemValue())) {
                            canPass = true;
                            break;
                        }
                    }
                } else {
                    for (String auth : authList) {
                        for (MetaFieldDataAuth metaFieldDataAuth : userDataAclList) {
                            if (dimCode.equals(metaFieldDataAuth.getDimRealCode()) && (BIConsts.Ssm_Row_Acl_All_Dim_Value.equals(metaFieldDataAuth.getItemCode()) || auth.equals(metaFieldDataAuth.getItemCode()))) {
                                canPass = true;
                                break;
                            }
                        }
                    }
                }
                if (!canPass) {
                    rowCheckResult.add(metaFieldCategory.getCategoryPath());
                }
            }
        });
        return rowCheckResult;
    }


    /**
     * 通过模板id集合查询用户没有权限的目录
     * @param tplIdList
     * @return
     */
    public List<String> getUserUnAuthCtgIdListByTplIds(List<String> tplIdList) {
        List<String> unAuthCtgIdList = new ArrayList<>();

        List<TemplateRsp> templateRspList = templateService.batchQueryTemplateCfg(tplIdList);
        String ctgAuthDim = SSDUtil.Ctg_Auth_Dim;

        Set<String> datasetIdSet = new HashSet<>();

        //数据集id集合
        for (TemplateRsp templateRsp : templateRspList) {
            datasetIdSet.add(templateRsp.getDatasetId());
        }

        //查询
        List<String> aclCodes = new ArrayList<>();
        for (String datasetId : datasetIdSet) {
            aclCodes.addAll(getAuthFieldCodesByCtg(UserManager.get().getName(), SSDUtil.Ctg_Auth_Module, ctgAuthDim, datasetId));
        }

        //查询用户所有的字段权限
        Map<String, String> aclCodeMap = new HashMap<>();
        aclCodes.stream().forEach(v -> aclCodeMap.put(v, v));

        for (TemplateRsp templateRsp : templateRspList) {
            SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
            queryTemplate.setConfig(templateRsp.getTplConfig());

            QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
            queryConfigure.load();

            for (QueryField qf : queryConfigure.getAllFields()) {
                if (qf.isCommonDate()) {
                    continue;
                }

                if (!aclCodeMap.containsKey(qf.getCode())) {

                    if (qf.getMeta() == null) {
                        continue;
                    }

                    if(StrUtil.isNotEmpty(qf.getModuleCtgId())){

                        unAuthCtgIdList.add(qf.getModuleCtgId());

                    }else {

                        if (CollUtil.isNotEmpty(qf.getMeta().getCategoryIdList())) {
                            for (String ctgId : qf.getMeta().getCategoryIdList()) {
                                String moduleCtgId = SSDMetaCacheManager.getModuleCtgIdByCategoryId(ctgId);
                                if (StrUtil.isNotEmpty(moduleCtgId)) {
                                    unAuthCtgIdList.add(moduleCtgId);
                                }
                            }
                        }

                    }

                }

            }
        }

        unAuthCtgIdList = unAuthCtgIdList.stream().distinct().collect(Collectors.toList());

        return unAuthCtgIdList;
    }

    /**
     * 通过指标编码获取指标名称
     * @param metricCode
     * @return
     */
    public String getMetricNameByMetricCode(String metricCode) {

        String metricName = "";
        List<MetaField> fields = SSDMetaCacheManager.getFieldByCode(metricCode);

        if (CollUtil.isEmpty(fields)) {
            return metricName;
        }

        metricName = fields.get(0).getKpiName();
        return metricName;
    }

    /**
     * 获取时间段进度
     * @param req
     * @return
     */
    public DateRangeProgressRsp getDateRangeProgress(DateRangeProgressReq req){

        DateRangeProgressRsp rsp = new DateRangeProgressRsp();

        LocalDate start = null;
        LocalDate end = null;
        LocalDate baseDate = LocalDate.now();
        DateGranularity dateGranularity = DateGranularity.get(req.getDateGranularity());
        switch (dateGranularity){
            case DAY:
            case WEEK:
                return rsp;
            case MONTH:
                start = baseDate.withDayOfMonth(1);
                end   = baseDate.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case QUARTER:
                int q = (baseDate.getMonthValue() - 1) / 3;
                start = LocalDate.of(baseDate.getYear(), q * 3 + 1, 1);
                end   = start.plusMonths(3).minusDays(1);
                break;
            case YEAR:
                start = LocalDate.of(baseDate.getYear(), 1, 1);
                end   = LocalDate.of(baseDate.getYear(), 12, 31);
                break;
        }

        int total = (int) ChronoUnit.DAYS.between(start, end) + 1;
        int remaining = (int) ChronoUnit.DAYS.between(baseDate, end);
        BigDecimal percent = BigDecimal.valueOf(total - remaining)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        rsp.setTotalDays( total);
        rsp.setLeftDays(remaining);
        rsp.setProgress(percent);
        return rsp;
    }

}
