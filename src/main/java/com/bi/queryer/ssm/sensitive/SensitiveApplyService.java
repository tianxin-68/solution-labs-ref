package com.bi.queryer.ssm.sensitive;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.bi.queryer.ssm.export.SSDExportService;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDExportBaseRV;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.sensitive.model.*;
import com.bi.queryer.ssm.util.KeywordRuleMatcher;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author contributor
 */
@Service
public class SensitiveApplyService{

    public static final FastDateFormat ISO_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    private static String ExportNeedWorkOrderCheck_Path = "export/apply/check";

    @Autowired
    private BaseDao dao;

    @Autowired
    private SSDExportService ssdExportService;

    @Autowired
    private UserService userService;

    /**
     * 查询下载多维分析的敏感级别类型
     * @return
     * @throws Exception
     */
    public SensitiveDownloadInfoRV checkDownloadSensitiveType(QueryEngine engine, SensitiveDownloadInfoRV sensitiveDownloadInfoReq) throws Exception {

        User user = UserManager.get();

        /**
         * 废弃
        //1、查询白名单
        if(getDownloadWhitelistExist(user.getName())){
            //直接返回不走工单数据
            SensitiveDownloadInfoRV sensitiveDownloadInfoRV = new SensitiveDownloadInfoRV(false, false, 1,
                    user.getName(), 1, 0, false, new ArrayList<>(), new ArrayList<>());
            BeanUtils.copyProperties(downloadRequest, sensitiveDownloadInfoRV);
            return sensitiveDownloadInfoRV;
        }


        //2、查询敏感信息
        SensitiveDownloadInfoRV sensitiveDownloadInfoRV = checkSensitiveType(engine,downloadRequest);

        //3、查询数量
        Integer totalSize = queryDownTotalSize(engine);
        Integer maxExportRows = new Integer(SC.v("ssm.export.rows","200"));
        if(totalSize > (maxExportRows * 10000)){
            throw new Exception("您的导出数据量已超过200万条，无法一次性导出，可适当修改数据范围后再次尝试，查询后点击页面右下角\"显示总记录数\"即可查看总记录数，若有疑问可企业微信联系@数据产品技术支持。");
        }
        sensitiveDownloadInfoRV.setRowNum(totalSize);
         */

        /** 废弃
        //4、判断一天累计下载量
        Integer downloadNum = getUserDownLoadNumDay();
        sensitiveDownloadInfoRV.setDownloadedNum(downloadNum);
        sensitiveDownloadInfoRV.setDownloadedThreshold(false);
        //判断超过30w阈值走审批流程
        if(checkUserDownLoadNumDay(downloadNum + totalSize)){
            sensitiveDownloadInfoRV.setDownloadedThreshold(true);
        }
        return sensitiveDownloadInfoRV;
         */
        return null;
    }

    /**
     * 查询用户下载白名单是否存在
     * @return
     */
    public Boolean isInWhitelist(DownloadWorkOrderInfoRV downloadWorkOrderInfoRV){
        Map<String, Object> params = new HashMap<>();
        params.put("userName", UserManager.get().getName());
        List<DownloadWhitelist> whitelists = (List<DownloadWhitelist>) dao.queryObjectList("ssm.query.getDownloadUserWhiteList", params);
        if(BIUtil.isEmpty(whitelists)){
            return false;
        }
        List<String> allFieldCtgIdList = downloadWorkOrderInfoRV.getAllFieldCategoryId();
        allFieldCtgIdList = allFieldCtgIdList.stream().distinct().collect(Collectors.toList());

        // 按目录判断：若下载的字段目录都在白名单目录中，则此下载命中白名单，否则不命中
        Set<String> whitelistCtgIdSet = new HashSet<>();
        for(DownloadWhitelist wl : whitelists){
            if("all".equalsIgnoreCase(wl.getCategoryId())){
                // 若是all(全部模块)，则所有内容都在白名单中
                return true;
            }
            whitelistCtgIdSet.add(wl.getCategoryId());
            // 模块，则添加模块下所有目录
            MetaFieldCategory moduleCtg = SSDMetaCacheManager.getCategoryById(wl.getCategoryId());
            if(moduleCtg != null && BIUtil.isNotEmpty(moduleCtg.getChildren())){
                moduleCtg.getChildren().stream().forEach(c -> whitelistCtgIdSet.add(c.getId()));
            }
        }
        boolean isContains =  whitelistCtgIdSet.containsAll(allFieldCtgIdList);
        return isContains;
    }

    /**
     * 查询下载总数据量
     */
    public Integer queryDownTotalSize(QueryEngine engine){
        String sql = engine.buildSql();
        Integer totalSize = engine.getDataSetTotalSize(sql);
        if(totalSize==null){
            totalSize = 0;
        }
        return totalSize;
    }

    /**
     * 查询敏感数据信息
     * @return
     */
    public SensitiveDownloadInfoRV buildSensitiveInfo(QueryEngine engine, SensitiveDownloadInfoRV downloadRequest) throws Exception {
        User user = UserManager.get();
        QueryConfigure queryConfigure = new QueryConfigure();
        queryConfigure.load(downloadRequest.getConfig());

        downloadRequest.setHighSensitive(false);
        downloadRequest.setBusinessSensitive(false);
        downloadRequest.setBusinessSensitiveType(new ArrayList<>());
        downloadRequest.setHighSensitiveCategoryId(new ArrayList<>());
        downloadRequest.setUserName(user.getName());

        List<QueryField> allFields = new ArrayList<>();

        Set<String> resultFieldIds = new HashSet<>();
        allFields.addAll(queryConfigure.getResult().getFields());
        // 结果字段id，用于判断是否是结果字段。因为过滤字段前端传递过来的时，是否是结果字段存在误判
        allFields.stream().filter(f-> Enabled.isTrue(f.getIsShow()) && !f.isAppend()).forEach( f -> resultFieldIds.add(f.getId()));
        allFields.addAll(queryConfigure.getFilter().getFields());

        Set<String> highSensitiveCategoryId = new HashSet<>();
        Set<String> allFieldCategoryId = new HashSet<>();
        Set<String> allDimFieldName = new HashSet<>();
        Set<String> allMeasureFieldName = new HashSet<>();
        Set<String> allFilterFieldName = new HashSet<>();
        Set<String> allHighSensitiveFieldName = new HashSet<>();

        for (QueryField queryField : allFields) {
            MetaField metaField = queryField.getMeta();
            // 是否有高敏感数据(c3/c4)
            DataSensitiveLevel dataSensitiveLevel = DataSensitiveLevel.C1;
            if (metaField != null && resultFieldIds.contains(queryField.getId())) { // 只处理结果字段
                dataSensitiveLevel = DataSensitiveLevel.get(metaField.getSensitiveLevel());
                if(dataSensitiveLevel.isHigh()) {
                    if (BIUtil.isNotEmpty(metaField.getCategoryIdList())) {
                        highSensitiveCategoryId.addAll(metaField.getCategoryIdList());
                    }
                    String title = this.getQueryFieldFullName(queryField, true);
                    allHighSensitiveFieldName.add(title);
                }
            }

            if (metaField != null) {
                if (queryField.getIsFilter()) {
                    allFilterFieldName.add(metaField.getTitle());
                } else {
                    if (Enabled.value(metaField.getIsMeasure())) {
                        allMeasureFieldName.add(this.getQueryFieldFullName(queryField, false));
                    } else {
                        allDimFieldName.add(metaField.getTitle());
                    }
                }
                if (BIUtil.isNotEmpty(metaField.getCategoryIdList())
                        && !queryField.isCommonDate()
                        && Enabled.isTrue(queryField.getIsShow())) { // 排除公共日期和隐藏字段
                    // 通过字段所在模块获取字段的目录id
                    String ctgId = SSDMetaCacheManager.getCategoryIdByFieldIdAndModuleCtgId(queryField.getId(), queryField.getModuleCtgId());
                    if(BIUtil.isNotEmpty(ctgId)){
                        allFieldCategoryId.add(ctgId);
                    }
                }
            }

            // 设置敏感等级:取最大的
            DataSensitiveLevel reqLevel = DataSensitiveLevel.get(downloadRequest.getSensitiveLevel());
            if(reqLevel.getLevel() < dataSensitiveLevel.getLevel()){
                downloadRequest.setSensitiveLevel(dataSensitiveLevel.getCode());
            }
        }

        // 判断组合敏感:若下载字段的敏感等级小于C4，则将本次下载的敏感等级调整为C4并添加相关匹配规则信息
        List<SensitiveRule> matchedRules = this.matchSensitiveRule(allDimFieldName, allMeasureFieldName);
        List<String> highSensitiveRuleInfos = new ArrayList<>();
        if(BIUtil.isNotEmpty(matchedRules)){
            matchedRules.stream().forEach(r ->{ highSensitiveRuleInfos.add(r.getRuleExpression());});
        }

        downloadRequest.setHighSensitive(DataSensitiveLevel.get(downloadRequest.getSensitiveLevel()).isHigh());
        downloadRequest.setHighSensitiveCategoryId(new ArrayList<>(highSensitiveCategoryId));

        //拼装downloadData下载数据(目录路径名称：（交易/订单明细）)
        if(BIUtil.isNotEmpty(allFieldCategoryId)) {
            List<MetaFieldCategory> ctgNameList = this.getMetaFieldCategoryInfoList(allFieldCategoryId);
            Set<String> allFieldCategoryName = ctgNameList.stream().filter(c -> c.getParent() != null).map(c -> c.getParent().getName() + "/" + c.getName()).collect(Collectors.toSet());
            downloadRequest.getDownloadData().add(new DownloadChildrenContentRV("多维分析", allFieldCategoryName));
        }

        //拼装downloadContent下载内容
        if (DataSensitiveLevel.get(downloadRequest.getSensitiveLevel()).isHigh()) {
            String label = String.format("高敏字段(%s)", downloadRequest.getSensitiveLevel());
            downloadRequest.getDownloadContent().add(new DownloadChildrenContentRV(label, allHighSensitiveFieldName));
        }
        if (BIUtil.isNotEmpty(matchedRules)) {
            String label = String.format("高敏组合(%s)", DataSensitiveLevel.C4.getCode());
            downloadRequest.getDownloadContent().add(new DownloadChildrenContentRV(label, highSensitiveRuleInfos));
        }
        if (CollUtil.isNotEmpty(allDimFieldName)) {
            downloadRequest.getDownloadContent().add(new DownloadChildrenContentRV("维度", allDimFieldName));
        }
        if (CollUtil.isNotEmpty(allMeasureFieldName)) {
            downloadRequest.getDownloadContent().add(new DownloadChildrenContentRV("指标", allMeasureFieldName));
        }
        if (CollUtil.isNotEmpty(allFilterFieldName)) {
            downloadRequest.getDownloadContent().add(new DownloadChildrenContentRV("筛选条件", allFilterFieldName));
        }

        // 若匹配组合敏感，则设置当前请求单数据为C4
        if (BIUtil.isNotEmpty(matchedRules)) {
            downloadRequest.setSensitiveLevel(DataSensitiveLevel.C4.getCode());
        }

        // 设置所有字段目录id，用于后续判断白名单规则
        if (BIUtil.isNotEmpty(allFieldCategoryId)) {
            downloadRequest.setAllFieldCategoryId(new ArrayList<>(allFieldCategoryId));
        }

        //3、查询数量
        Integer totalSize = queryDownTotalSize(engine);
        Integer maxExportRows = new Integer(SC.v("ssm.export.rows","200"));
        if(totalSize > (maxExportRows * 10000)){
            throw new Exception("您的导出数据量已超过200万条，无法一次性导出，可适当修改数据范围后再次尝试，查询后点击页面右下角\"显示总记录数\"即可查看总记录数，若有疑问可企业微信联系@数据产品技术支持。");
        }
        downloadRequest.setRowNum(totalSize);

        return downloadRequest;
    }

    protected String getQueryFieldFullName(QueryField queryField, boolean appendSensitiveLevel){
        MetaField metaField = queryField.getMeta();
        if(metaField == null){
            return queryField.getTitle();
        }

        String title = metaField.getTitle();
        if(Enabled.isTrue(queryField.getIsAnalysis())) {
            // 若是分析字段，则带上原始字段名称
            MetaField rawField = SSDMetaCacheManager.getField(queryField.getAnalysisConfig().getMeasureId());
            if(rawField != null){
                title = rawField.getTitle() + "_" + title;
            }
        }
        if(appendSensitiveLevel){
            title = String.format("%s(%s)", title,  DataSensitiveLevel.get(metaField.getSensitiveLevel()).getCode());
        }
        return title;
    }

    /**
     * 组装下载内容字段
     * 前台页面展示
     * @return
     */
    public Map<String, List<DownloadChildrenContentRV>> assembleDownloadDataInfo(Set<String> allFieldCategoryId, Set<String> allDimFieldName,
                                                                                 Set<String> allMeasureFieldName, Set<String> allFilterFieldName,
                                                                                 Set<String> allHighSensitiveFieldName, String sensitiveLevel) {

        List<DownloadChildrenContentRV> downloadDataList = new ArrayList<>();
        List<DownloadChildrenContentRV> downloadContentList = new ArrayList<>();
        Map<String, List<DownloadChildrenContentRV>> result = new HashMap<String, List<DownloadChildrenContentRV>>(){{
            put("downloadData", downloadDataList);
            put("downloadContent", downloadContentList);
        }};

        //拼装downloadData下载数据(目录路径名称：（交易/订单明细）)
        if (CollUtil.isNotEmpty(allFieldCategoryId) && allFieldCategoryId.size() > 0) {
            Set<String> allFieldCategoryName = new HashSet<>();
            List<MetaFieldCategory> ctgNameList = getMetaFieldCategoryInfoList(allFieldCategoryId);
            ctgNameList.forEach(ctg->{
                if(null != ctg.getParent()){
                    allFieldCategoryName.add( ctg.getParent().getName() + "/" + ctg.getName());
                }
                /*
                if(StringUtils.isNotBlank(e.getParentName())){
                    allFieldCategoryName.add(e.getParentName());
                }
                 */
            });
            DownloadChildrenContentRV downloadChildrenContentRV = new DownloadChildrenContentRV("多维分析", allFieldCategoryName);
            downloadDataList.add(downloadChildrenContentRV);
        }
        result.put("downloadData", downloadDataList);

        //拼装downloadContent下载内容
        if (CollUtil.isNotEmpty(allHighSensitiveFieldName)) {
            String label = String.format("高敏数据(%s)", sensitiveLevel);
            DownloadChildrenContentRV downloadChildrenContentRV = new DownloadChildrenContentRV(label, allHighSensitiveFieldName);
            downloadContentList.add(downloadChildrenContentRV);
        }
        if (CollUtil.isNotEmpty(allDimFieldName)) {
            DownloadChildrenContentRV downloadChildrenContentRV = new DownloadChildrenContentRV("维度", allDimFieldName);
            downloadContentList.add(downloadChildrenContentRV);
        }
        if (CollUtil.isNotEmpty(allMeasureFieldName)) {
            DownloadChildrenContentRV downloadChildrenContentRV = new DownloadChildrenContentRV("指标", allMeasureFieldName);
            downloadContentList.add(downloadChildrenContentRV);
        }
        if (CollUtil.isNotEmpty(allFilterFieldName)) {
            DownloadChildrenContentRV downloadChildrenContentRV = new DownloadChildrenContentRV("筛选条件", allFilterFieldName);
            downloadContentList.add(downloadChildrenContentRV);
        }
        result.put("downloadContent", downloadContentList);

        return result;
    }

    /**
     * 查询FieldCategory表的信息
     * 包括目录的路径名称（交易/订单明细），目录owner
     * @param ctgIdList
     * @return
     */
    public List<MetaFieldCategory> getMetaFieldCategoryInfoList(Collection<String> ctgIdList) {
        if(CollUtil.isEmpty(ctgIdList) || ctgIdList.size() < 1){
            return new ArrayList<>();
        }
        ctgIdList = ctgIdList.stream().distinct().collect(Collectors.toList());
        return SSDMetaCacheManager.getFieldCategoryList(ctgIdList);
        /**
        Map<String, Object> mapDevOwner = new HashMap<>();
        mapDevOwner.put("ctgIdList", ctgIdList);
        List<MetaFieldCategory> result = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryCtgRptDevOwnerList", mapDevOwner);
        return result;
         */
    }


    /**
     * 判断用户一天累计是否超过阈值
     */
    public Boolean checkUserDownLoadNumDay(Integer downloadTotalNum) {
        String thresholdNumFlag = SC.v("ssm.thresholdNum.flag", "0");
        Integer thresholdNum = Integer.valueOf(SC.v("ssm.download.thresholdNum", "300000"));
        //超过30w阈值走审批流程
        if (Enabled.value(thresholdNumFlag) && thresholdNum < downloadTotalNum) {
            return true;
        }
        return false;
    }

    /**
     * 查询用户当天已下载数量
     * @return 下载数量
     */
    public Integer getUserDownLoadNumDay(){
        User user = UserManager.get();
        String userName = user.getName();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userName", userName);
        Integer downloadNum =  dao.queryCount("ssm.query.queryUserExportNumDaily", paramMap);
        return downloadNum;
    }

    /**
     * 查询下载工单流程申请单参数
     * @param sensitiveDownloadInfoRV
     * @return
     */
    public DownloadWorkOrderInfoRV buildWorkOrderInfo(SensitiveDownloadInfoRV sensitiveDownloadInfoRV) throws Exception {
        //1、将敏感的基本信息同步sensitiveDownloadInfoRV转downloadWorkOrderInfoRV
        //DownloadWorkOrderInfoRV workOrder = transferSensitiveToWorkOrder(sensitiveDownloadInfoRV);
        DownloadWorkOrderInfoRV workOrder = new DownloadWorkOrderInfoRV();
        BeanUtils.copyProperties(sensitiveDownloadInfoRV, workOrder);
        //判断下载数据的敏感和数据量是否需要走工单，提示信息
        boolean needApply = false;
        // 需走审批的导出记录数
        Integer needApplyDataCount = Integer.valueOf(SC.v("ssm.export.need.apply.data.count", "1000"));
        String workOrderRemark = String.format("下载内容的记录数超过%s条，需走工单3.0流程审批。", needApplyDataCount);
        // 大于1000
        if(sensitiveDownloadInfoRV.getRowNum() > needApplyDataCount){
            needApply = true;
        }
        // 白名单，不需要申请
        if(this.isInWhitelist(workOrder)){
            needApply = false;
        }
        workOrder.setNeedApply(needApply);
        workOrder.setWorkOrderRemark(workOrderRemark);

        //4、高敏数据
        if (workOrder.getHighSensitive()) {
            workOrder.setBusinessSensitiveType(new ArrayList<>(Arrays.asList(String.format("高敏数据(%s)", workOrder.getSensitiveLevel()))));
        }

        // 设置申请人信息
        this.setWorkOrderApprovers(workOrder);

        return workOrder;
    }

    /**
     * 设置工单申请人和审批人信息
     * @param workOrder
     */
    protected void setWorkOrderApprovers(DownloadWorkOrderInfoRV workOrder){
        User user = UserManager.get();
        String applicant = "";
        String applicantEMail = "";
        String applicantRealName = "";
        if (user != null && StringUtils.isNotBlank(user.getName())) {
            applicant = user.getName();
            applicantEMail = user.getName() + "@example.com";
            applicantRealName = user.getRealName();
        }
        workOrder.setApplicant(applicant);
        workOrder.setApplicantEMail(applicantEMail);
        workOrder.setApplicantRealName(applicantRealName);

        // 上级审批人
        String applicantDept = "";
        String deptLeaderEMail = "";

        List<DownloadWorkOrderInfoRV> deptList = userService.getUserDeptInfo(applicant);
        if (CollUtil.isNotEmpty(deptList) && deptList.size() > 0) {
            applicantDept = deptList.get(0).getApplicantDept();
            deptLeaderEMail = deptList.get(0).getDeptLeaderEMail() + "@example.com";

        }
        workOrder.setApplicantDept(applicantDept);
        workOrder.setDeptLeaderEMail(deptLeaderEMail);

        // 模块审批人
        String moduleOwnerEMail = "";
        List<String> moduleOwnerEMailList = new ArrayList<>(this.getModuleOwnerApprovers(workOrder));
        if (BIUtil.isNotEmpty(moduleOwnerEMailList)) {
            moduleOwnerEMail = BIUtil.listToStr(moduleOwnerEMailList);
        }else{
            moduleOwnerEMail = workOrder.getDeptLeaderEMail();
        }
        workOrder.setModuleOwnerEmail(moduleOwnerEMail);

        // 信安审批人：用于前端显示，实际审批人由文件服务器指定
        workOrder.setSecurityEMail(SC.v("ssm.download.securityEMail", "contributor@example.com"));
    }

    /**
     * 将敏感的基本信息同步sensitiveDownloadInfoRV转downloadWorkOrderInfoRV
     * @return
     * @throws Exception
     */
    public DownloadWorkOrderInfoRV transferSensitiveToWorkOrder(SensitiveDownloadInfoRV sensitiveDownloadInfoRV) throws Exception {
        DownloadWorkOrderInfoRV downloadWorkOrderInfoRV = new DownloadWorkOrderInfoRV();
        BeanUtils.copyProperties(sensitiveDownloadInfoRV, downloadWorkOrderInfoRV);
        //判断下载数据的敏感和数据量是否需要走工单，提示信息
        boolean needApply = false;
        // 需走审批的导出记录数
        Integer needApplyDataCount = Integer.valueOf(SC.v("ssm.export.need.apply.data.count", "1000"));
        String workOrderRemark = String.format("下载内容的记录数超过%s条，需走工单3.0流程审批。", needApplyDataCount);
        // 大于1000
        if(sensitiveDownloadInfoRV.getRowNum() > needApplyDataCount){
            needApply = true;
        }
        // 白名单，不需要申请
        if(this.isInWhitelist(downloadWorkOrderInfoRV)){
            needApply = false;
        }
        downloadWorkOrderInfoRV.setNeedApply(needApply);
        downloadWorkOrderInfoRV.setWorkOrderRemark(workOrderRemark);

        //checkNeedApply(workOrder);

        /*
        Boolean needApply = false;
        // 需走审批的导出记录数
        Integer needApplyDataCount = Integer.valueOf(SC.v("ssm.export.need.apply.data.count", "1000"));
        if(sensitiveDownloadInfoRV.getRowNum() > needApplyDataCount){ // 大于1000
            needApply = true;
        }

        // 白名单
        User user = UserManager.get();
        if(getDownloadWhitelistExist(user.getName())){
            needApply = false;
        }
        workOrder.setNeedApply(needApply);

        String workOrderRemark = String.format("下载内容的记录数超过%s条，需走工单3.0流程审批。", needApplyDataCount);
        workOrder.setWorkOrderRemark(workOrderRemark);
        */
        return downloadWorkOrderInfoRV;
    }

    /**
     * 获取模块owner审批人
     * @param downloadWorkOrderInfoRV
     * @return
     */
    public List<String> getModuleOwnerApprovers(DownloadWorkOrderInfoRV downloadWorkOrderInfoRV) {
        List<String> approverEmailList = new ArrayList<>();
        List<String> ctgIdList = downloadWorkOrderInfoRV.getAllFieldCategoryId();
        if(BIUtil.isEmpty(ctgIdList)){
            return approverEmailList;
        }
        for(String ctgId : ctgIdList){
            MetaFieldCategory fieldCtg = SSDMetaCacheManager.getCategoryById(ctgId);
            if(fieldCtg == null) {
                continue;
            }

            // 优先取当前目录的owner，再取其模块owner
            if(BIUtil.isNotEmpty(fieldCtg.getRptDevOwner())){
                approverEmailList.add(fieldCtg.getRptDevOwner().split(",")[0]);
            }else {
                String moduleCtgId = SSDMetaCacheManager.getModuleCtgIdByCategoryId(ctgId);
                if(BIUtil.isNotEmpty(moduleCtgId)){
                    MetaFieldCategory moduleCtg = SSDMetaCacheManager.getCategoryById(moduleCtgId);
                    if(moduleCtg != null && BIUtil.isNotEmpty(moduleCtg.getRptDevOwner())) {
                        approverEmailList.add(moduleCtg.getRptDevOwner().split(",")[0]);
                    }
                }
            }
        }
        // 去重后取前3个
        approverEmailList = approverEmailList.stream().distinct().map(item -> item + "@example.com").limit(3).collect(Collectors.toList());
        return approverEmailList;
    }

    /**
     * 废弃 by contributor
     * 判断二级审批的人员（BI审批）
     * @param workOrder
     * @return
     */
    /**
    @Deprecated
    public Set<String> getSecondLevelApproval2(DownloadWorkOrderInfoRV workOrder) {

        Set<String> approverEMailList = new HashSet<>();

        //是否
        Boolean needPersonSensitiveApproval = false;
        Boolean needBusinessSensitiveApproval = false;

        //个人敏感 + 解密导出
        //if (workOrder.getPersonSensitive() && Enabled.value(workOrder.getDecryptSensitiveField())) {
        if (workOrder.getHighSensitive()) {
            needPersonSensitiveApproval = true;
        }

        //商业敏感 + 大于10w
        if (workOrder.getBusinessSensitive() && workOrder.getRowNum() > 100000) {
            needBusinessSensitiveApproval = true;
        }

        //查询对应下载目录的owner
        List<MetaFieldCategory> ctgRptDevOwnerList = getMetaFieldCategoryInfoList(workOrder.getAllFieldCategoryId());
        if (CollUtil.isNotEmpty(ctgRptDevOwnerList) && ctgRptDevOwnerList.size() > 0) {
            //如果是个人或商业敏感，审批人只需要存在敏感数据的目录
            if (needPersonSensitiveApproval) {
                ctgRptDevOwnerList.forEach(e -> {
                    if (StringUtils.isNotBlank(e.getRptDevOwner())) {
                        if (CollUtil.isNotEmpty(workOrder.getHighSensitiveCategoryId()) && workOrder.getHighSensitiveCategoryId().contains(e.getId())) {
                            approverEMailList.add(e.getRptDevOwner() + "@example.com");
                        }
                    }
                });
            } else if(!needPersonSensitiveApproval && needBusinessSensitiveApproval){
                ctgRptDevOwnerList.forEach(e -> {
                    if (StringUtils.isNotBlank(e.getRptDevOwner())) {
                        if (CollUtil.isNotEmpty(workOrder.getBusinessSensitiveCategoryId()) && workOrder.getBusinessSensitiveCategoryId().contains(e.getId())) {
                            approverEMailList.add(e.getRptDevOwner() + "@example.com");
                        }
                    }
                });
            } else {
                //如果都不存在，则审批人随便给一个（实际上用不到）
                approverEMailList.add(ctgRptDevOwnerList.get(0).getRptDevOwner() + "@example.com");
            }
        }

        return approverEMailList;
    }
    */

    /**
     * 查询下载多维分析的敏感级别类型和工单信息
     * @return
     * @throws Exception
     */
    public DownloadWorkOrderInfoRV getDownloadSensitiveAndWorkOrderInfo(QueryEngine engine, SensitiveDownloadInfoRV downloadRequest) throws Exception {

        //1、先查询敏感类型，下载等基本信息
        SensitiveDownloadInfoRV sensitiveDownloadInfoRV = this.buildSensitiveInfo(engine, downloadRequest); //checkDownloadSensitiveType(engine, downloadRequest);

        //2、根据个人敏感是否解密，生成工单信息
        DownloadWorkOrderInfoRV downloadWorkOrderInfoRV = this.buildWorkOrderInfo(sensitiveDownloadInfoRV);

        //3、根据是否走工单或直接下载
        if (!downloadWorkOrderInfoRV.getNeedApply()) {
            ssdExportService.submitExportTask(engine, downloadWorkOrderInfoRV);
        }

        return  downloadWorkOrderInfoRV;
    }

    /**
     * 将工单信息转换成为下载需要的信息
     * workOrder to fileServiceExportBaseRV
     * @param fileServiceExportBaseRV, workOrder
     * @return
     */
    public SSDExportBaseRV transferWorkOrderToFileServiceExportInfo(SSDExportBaseRV fileServiceExportBaseRV, DownloadWorkOrderInfoRV downloadWorkOrderInfoRV) {

        User user = UserManager.get();

        //1、敏感信息和基本信息
        fileServiceExportBaseRV.setNeedApply(downloadWorkOrderInfoRV.getNeedApply());
        fileServiceExportBaseRV.setIsApplyApprove(downloadWorkOrderInfoRV.getNeedApply() ? 1 : 0);
        fileServiceExportBaseRV.setPersonSensitive(downloadWorkOrderInfoRV.getHighSensitive());
        fileServiceExportBaseRV.setBusinessSensitive(downloadWorkOrderInfoRV.getBusinessSensitive());
        fileServiceExportBaseRV.setDownloadRemark(downloadWorkOrderInfoRV.getDownloadRemark());
        fileServiceExportBaseRV.setRptRows(downloadWorkOrderInfoRV.getRowNum());
        fileServiceExportBaseRV.setModuleOwnerEmail(downloadWorkOrderInfoRV.getModuleOwnerEmail());
        fileServiceExportBaseRV.setSensitiveLevel(downloadWorkOrderInfoRV.getSensitiveLevel());

        String sensitiveTypeJson = "";
        if (CollUtil.isNotEmpty(downloadWorkOrderInfoRV.getBusinessSensitiveType())) {
            sensitiveTypeJson = downloadWorkOrderInfoRV.getBusinessSensitiveType().stream().filter(e -> StringUtils.isNotBlank(e)).collect(Collectors.joining(","));
        }
        fileServiceExportBaseRV.setSensitiveType(sensitiveTypeJson);

        //2、下载目录和下载内容
        String downloadDataJson = "";
        if (CollUtil.isNotEmpty(downloadWorkOrderInfoRV.getDownloadData())) {
            for (DownloadChildrenContentRV entity : downloadWorkOrderInfoRV.getDownloadData()) {
                String tempDetail = entity.getChildrenContent().stream().filter(e -> StringUtils.isNotBlank(e)).collect(Collectors.joining(","));
                if (StringUtils.isBlank(tempDetail)) {
                    tempDetail = "无";
                }
                downloadDataJson += entity.getLabel() + ":" + tempDetail + " \n";
            }
        }
        fileServiceExportBaseRV.setDownloadData(downloadDataJson);

        String downloadContent = "";
        if (CollUtil.isNotEmpty(downloadWorkOrderInfoRV.getDownloadContent())) {
            for (DownloadChildrenContentRV entity : downloadWorkOrderInfoRV.getDownloadContent()) {
                String tempDetail = entity.getChildrenContent().stream().filter(e -> StringUtils.isNotBlank(e)).collect(Collectors.joining(","));
                if (StringUtils.isBlank(tempDetail)) {
                    tempDetail = "无";
                }
                downloadContent += entity.getLabel() + ":" + tempDetail + " \n";
            }
        }
        fileServiceExportBaseRV.setDownloadContent(downloadContent);

        //3、转换审批人信息，如果发现为空则重新获取
        if (StringUtils.isBlank(downloadWorkOrderInfoRV.getApplicant())) {
            String applicant = "";
            if (user != null && StringUtils.isNotBlank(user.getName())) {
                applicant = user.getName();
            }
            downloadWorkOrderInfoRV.setApplicant(applicant);
        }
        fileServiceExportBaseRV.setApplicant(downloadWorkOrderInfoRV.getApplicant());


        if (StringUtils.isBlank(downloadWorkOrderInfoRV.getDeptLeaderEMail())) {
            String deptLeaderEMail = "";
            List<DownloadWorkOrderInfoRV> deptList = userService.getUserDeptInfo(user.getName());
            if (CollUtil.isNotEmpty(deptList) && deptList.size() > 0) {
                deptLeaderEMail = deptList.get(0).getDeptLeaderEMail() + "@example.com";

            }
            downloadWorkOrderInfoRV.setDeptLeaderEMail(deptLeaderEMail);
        }
        fileServiceExportBaseRV.setDeptLeaderEMail(downloadWorkOrderInfoRV.getDeptLeaderEMail());

        if (StringUtils.isBlank(downloadWorkOrderInfoRV.getSecurityEMail())) {
            String securityEMail = SC.v("ssm.download.securityEMail");
            if (StringUtils.isBlank(securityEMail)) {
                securityEMail = "contributor@example.com";
            }
            downloadWorkOrderInfoRV.setSecurityEMail(securityEMail);
        }
        fileServiceExportBaseRV.setSecurityEMail(downloadWorkOrderInfoRV.getSecurityEMail());

        if (StringUtils.isBlank(downloadWorkOrderInfoRV.getModuleOwnerEmail())) {
            String moduleOwnerEMail = "";
            List<String> approverEMailList = this.getModuleOwnerApprovers(downloadWorkOrderInfoRV);
            if (BIUtil.isNotEmpty(approverEMailList)) {
                moduleOwnerEMail = BIUtil.listToStr(approverEMailList); //approverEMailList.stream().filter(e -> StringUtils.isNotBlank(e)).collect(Collectors.joining(","));
            } else {
                moduleOwnerEMail = downloadWorkOrderInfoRV.getDeptLeaderEMail();
            }
            downloadWorkOrderInfoRV.setModuleOwnerEmail(moduleOwnerEMail);
        }
        fileServiceExportBaseRV.setModuleOwnerEmail(downloadWorkOrderInfoRV.getModuleOwnerEmail());

        return fileServiceExportBaseRV;

    }

    /**
     * 维度和指标来匹配敏感规则
     * @param dimensionNames
     * @param measureNames
     * @return
     */
    public List<SensitiveRule> matchSensitiveRule(Set<String> dimensionNames, Set<String> measureNames){
        List<SensitiveRule> matchedRules = new ArrayList<>();
        List<SensitiveRule> rules = (List<SensitiveRule>) dao.queryObjectList("ssm.query.getAllSensitiveRules", new HashMap());
        if(BIUtil.isEmpty(rules)){
            return matchedRules;
        }
        Set<String> downloadFieldNames = new HashSet<>();
        downloadFieldNames.addAll(dimensionNames);
        downloadFieldNames.addAll(measureNames);

        for(SensitiveRule rule : rules){
            if(BIUtil.isEmpty(rule.getRuleExpression())){
                continue;
            }
            boolean isMatched = KeywordRuleMatcher.matches(downloadFieldNames, rule.getRuleExpression());
            if(isMatched) {
                matchedRules.add(rule);
            }
        }
        return matchedRules;
    }


}
