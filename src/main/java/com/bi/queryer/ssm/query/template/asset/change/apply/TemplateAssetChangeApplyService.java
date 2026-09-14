package com.bi.queryer.ssm.query.template.asset.change.apply;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyCreateRsp;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyEntity;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyReq;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyRsp;
import com.bi.queryer.ssm.query.template.change.log.TemplateChangeLogService;
import com.bi.queryer.ssm.query.template.change.log.model.TemplateChangeLogEntity;
import com.bi.queryer.ssm.query.template.enums.ApprovalStatusType;
import com.bi.queryer.ssm.query.template.enums.TemplateChangeContentType;
import com.bi.queryer.ssm.query.template.enums.TemplateViewType;
import com.bi.queryer.ssm.query.template.enums.ViewStatusType;
import com.bi.queryer.ssm.query.template.model.TemplateCfgDtlEntity;
import com.bi.queryer.ssm.query.template.view.TemplateViewService;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.sys.user.vo.UserRsp;
import com.bi.queryer.util.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class TemplateAssetChangeApplyService {


    @Autowired
    private BaseDao dao;

    @Autowired
    private TemplateConfigService configService = null;

    @Autowired
    private TemplateChangeLogService templateChangeLogService;


    /**
     * 创建资产变更申请
     *
     * @param req
     * @return
     */
    public TemplateAssetChangeApplyCreateRsp create(TemplateAssetChangeApplyReq req) {

        TemplateAssetChangeApplyCreateRsp templateAssetChangeApplyCreateRsp = new TemplateAssetChangeApplyCreateRsp();

        String viewId = req.getViewId();
        User user = UserManager.get();

        //1. 判断视图是否有未处理的申请
        Integer unfinishedApplyCount = (Integer) dao.queryObject("ssm.template.asset.change.apply.getUnfinishedApplyCountByViewId", viewId);
        if (unfinishedApplyCount > 0) {
            throw new RuntimeException("该视图有未完结的资产变更申请,无法重复申请");
        }

        //4. 给审批人发送BI Queryer通
        List<String> receivers = configService.getTemplateOwnerNameByTplId(req.getTplId());

        //判断申请人是不是模板owner
        //如果是管理员，点击申请后直接自动通过
        boolean isOwner = receivers.contains(user.getName());

        TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity = new TemplateAssetChangeApplyEntity();
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                //2. 创建资产变更申请
                templateAssetChangeApplyEntity.setTplId(req.getTplId());
                templateAssetChangeApplyEntity.setViewId(req.getViewId());
                templateAssetChangeApplyEntity.setApplyRemark(req.getApplyRemark());
                templateAssetChangeApplyEntity.setApprovalStatus(ApprovalStatusType.APPLY.getCode());
                templateAssetChangeApplyEntity.setCreatedBy(user.getName());

                dao.insert("ssm.template.asset.change.apply.add", templateAssetChangeApplyEntity);
                Long applyId = templateAssetChangeApplyEntity.getApplyId();

                //3. 更新视图表的last_asset_change_apply_id
                Map<String, Object> param = new HashMap<>();
                param.put("applyId", applyId);
                param.put("viewId", viewId);
                param.put("updatedBy", user.getName());
                dao.update("ssm.template.view.updateLastAssetChangeApplyId", param);

                //申请人是模板owner，自动审批通过
                if (isOwner) {

                    TemplateAssetChangeApplyReq applyReq = new TemplateAssetChangeApplyReq();
                    applyReq.setApplyId(applyId);
                    applyReq.setTplId(templateAssetChangeApplyEntity.getTplId());
                    applyReq.setViewId(templateAssetChangeApplyEntity.getViewId());
                    applyReq.setApprovalStatus(ApprovalStatusType.APPROVED.getCode());
                    applyReq.setApprovalRemark("系统自动通过");
                    approval(applyReq,true);
                    templateAssetChangeApplyCreateRsp.setIsAutoApproval(Enabled.YES.getId());

                } else {
                    //4. 给审批人发送BI Queryer通
                    /**
                     * 【多维分析-模版资产更新申请】:for管理员(模版owner+其他管理员):
                     * xxx申请发布xxx视图，更新xxx模版的资产。
                     * 申请理由：xxx
                     * 点击链接查看并审批：xxx--这个审批视图的链接
                     */

                    String viewName = "";
                    TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", templateAssetChangeApplyEntity.getViewId());
                    if (templateViewEntity != null) {
                        viewName = templateViewEntity.getViewName();
                    }

                    String tplName = "";
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put("templateId", templateAssetChangeApplyEntity.getTplId());
                    SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);
                    if (tpl != null) {
                        tplName = tpl.getName();
                    }

                    StringBuilder messageText = new StringBuilder();
                    messageText.append(String.format("%s申请发布%s视图，更新%s模版的资产\n",
                            templateAssetChangeApplyEntity.getCreatedBy(),
                            viewName,
                            tplName
                    ));

                    messageText.append(String.format("申请理由：%s\n", templateAssetChangeApplyEntity.getApplyRemark()));

                    String uiBaseUrl = SC.v("ssm.ui.base.url", "https://datastudio-test.example.com");
                    String templateViewUrl = String.format("%s/ssm/#/template/%s?viewId=%s&applyId=%s",
                            uiBaseUrl,
                            templateAssetChangeApplyEntity.getTplId(),
                            templateAssetChangeApplyEntity.getViewId(),
                            templateAssetChangeApplyEntity.getApplyId());
                    messageText.append(String.format("点击链接查看并审批：%s", templateViewUrl));

                    sendApplyNotification("多维分析-模版资产更新申请", messageText.toString(), receivers);
                }
            }
        });

        templateAssetChangeApplyCreateRsp.setApplyId(templateAssetChangeApplyEntity.getApplyId());
        return templateAssetChangeApplyCreateRsp;
    }


    /**
     * 发送申请BI Queryer通
     * 排除高管，避免过扰
     * 审批人排除cm、波哥（若审批人有其他人）
     * @param sign
     * @param messageText
     * @param receivers
     */
    public void sendApplyNotification(String sign,String messageText,List<String> receivers) {

        List<String> finalReceivers = new ArrayList<>();

        String bi_queryerMsgExcludeUsers = SC.v("ssm.template.asset.change.apply.bi_queryer.msg.exclude.user", "chenmin,sunjie3");
        if (StrUtil.isNotEmpty(bi_queryerMsgExcludeUsers)) {
            List<String> excludeUserList = Arrays.asList(bi_queryerMsgExcludeUsers.split(","));
            finalReceivers = receivers.stream().filter(receiver -> !excludeUserList.contains(receiver)).collect(Collectors.toList());
        }

        //如果排除后，一个接收人都没有，则使用原始接收人
        if(CollUtil.isEmpty(finalReceivers)){
            finalReceivers.addAll(receivers);
        }

        SSDUtil.sendNotification(sign, messageText, finalReceivers);
    }

    /**
     * 审批资产变更申请
     * @param req
     * @param isAutoApproval 是否自动审批通过
     */
    public String approval(TemplateAssetChangeApplyReq req,boolean isAutoApproval) {

        //判断资产变更申请是否已经被处理
        TemplateAssetChangeApplyEntity templateAssetChangeApplyEntityDb = (TemplateAssetChangeApplyEntity) dao.queryObject("ssm.template.asset.change.apply.get", req.getApplyId());
        if (templateAssetChangeApplyEntityDb == null) {
            throw new RuntimeException(String.format("资产变更申请ID: %s不存在", req.getApplyId()));
        }

        ApprovalStatusType approvalStatusType = ApprovalStatusType.getByCode(templateAssetChangeApplyEntityDb.getApprovalStatus());
        if (ApprovalStatusType.APPLY != approvalStatusType) {
            throw new RuntimeException(String.format("资产变更申请ID: %s已被处理,无需重复处理！请刷新页面重新加载待审批列表！", req.getApplyId()));
        }

        String userName = UserManager.get().getName();
        String[] newViewId = {""};

        String viewId = templateAssetChangeApplyEntityDb.getViewId();
        String tplId = templateAssetChangeApplyEntityDb.getTplId();

        //1. 更新资产变更申请
        TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity = new TemplateAssetChangeApplyEntity();
        templateAssetChangeApplyEntity.setApplyId(req.getApplyId());
        templateAssetChangeApplyEntity.setApprovalStatus(req.getApprovalStatus());
        templateAssetChangeApplyEntity.setApprovalRemark(req.getApprovalRemark());
        templateAssetChangeApplyEntity.setApprovedBy(userName);
        templateAssetChangeApplyEntity.setUpdatedBy(userName);
        templateAssetChangeApplyEntity.setCreatedBy(templateAssetChangeApplyEntityDb.getCreatedBy());
        templateAssetChangeApplyEntity.setTplId(tplId);
        templateAssetChangeApplyEntity.setViewId(viewId);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                dao.update("ssm.template.asset.change.apply.approval", templateAssetChangeApplyEntity);
                TemplateViewService viewService = (TemplateViewService)SpringContextUtil.getBean("templateViewService");
                TemplateViewEntity viewEntity = viewService.getByViewId(viewId);

                //2.审批通过后更新视图资产
                if(ApprovalStatusType.APPROVED == ApprovalStatusType.getByCode(req.getApprovalStatus())){

                    //复制一份视图到公共视图，并且设置为有效
                    viewEntity.setIsActive(Enabled.YES.getId());
                    viewEntity.setViewStatus(ViewStatusType.ACTIVE.getCode());
                    viewEntity.setViewType(TemplateViewType.PUBLIC.getCode());

                    //移除资产失效时间
                    viewEntity.setAssetExpiresTime(null);

                    //位置放在公共视图最后
                    Double maxSortId = (Double) dao.queryObject("ssm.template.view.queryMaxSortIdByTplIdAndViewType", viewEntity);
                    viewEntity.setSortId(maxSortId);
                    newViewId[0] = viewService.copyViewByViewEntity(viewEntity);

                    //同步设置其他视图的状态
                    TemplateCfgDtlEntity cfgDtlEntity = (TemplateCfgDtlEntity)dao.queryObject("ssm.query.template.cfg.dtl.queryByViewId", viewId);
                    if (cfgDtlEntity != null && StrUtil.isEmpty(cfgDtlEntity.getDatasetId())) {
                        cfgDtlEntity.setDatasetId(viewEntity.getDatasetId());
                    }
                    viewService.setViewDisabled(tplId, newViewId[0],cfgDtlEntity);

                    //更新模板最后更新时间
                    viewService.updateTemplateUpdatedTime(req.getTplId());

                    //插入资产变更日志
                    TemplateChangeLogEntity templateChangeLogEntity = new TemplateChangeLogEntity();
                    templateChangeLogEntity.setTplId(tplId);
                    templateChangeLogEntity.setCreatedBy(UserManager.get().getName());
                    templateChangeLogEntity.setChangeType(TemplateChangeContentType.ASSET.getCode());
                    templateChangeLogEntity.setChangeName(TemplateChangeContentType.ASSET.getName());

                    String changeContent = String.format("更新了%s(通过了%s的模版上线申请)"
                            ,TemplateChangeContentType.ASSET.getName()
                            ,templateAssetChangeApplyEntityDb.getCreatedBy()
                            );

                    templateChangeLogEntity.setChangeContent(changeContent);
                    templateChangeLogService.add(templateChangeLogEntity);

                }

                //3. 发送BI Queryer通
                //发给申请用户
                sendApprovalMsgToApplyUser(templateAssetChangeApplyEntity,viewEntity,isAutoApproval);
                if(!isAutoApproval){
                    sendApprovalMsgToOtherAdminUser(templateAssetChangeApplyEntity,viewEntity, newViewId[0]);
                }


            }});

        return newViewId[0];
    }


    /**
     * 因为资产一致，则自动审批通过
     * @param templateAssetChangeApplyEntity
     */
    public void sysAutoApproval(TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity) {


        TemplateAssetChangeApplyEntity templateAssetChangeApplyEntityDb = (TemplateAssetChangeApplyEntity) dao.queryObject("ssm.template.asset.change.apply.get", templateAssetChangeApplyEntity.getApplyId());
        if (templateAssetChangeApplyEntityDb == null) {
           return;
        }

        ApprovalStatusType approvalStatusType = ApprovalStatusType.getByCode(templateAssetChangeApplyEntityDb.getApprovalStatus());
        if (ApprovalStatusType.APPLY != approvalStatusType) {
           return;
        }

        //1. 设置申请完结
        templateAssetChangeApplyEntity.setApprovalStatus(ApprovalStatusType.APPROVED.getCode());
        templateAssetChangeApplyEntity.setApprovalRemark("该视图资产与线上模版资产一致，系统自动通过");
        dao.update("ssm.template.asset.change.apply.approval", templateAssetChangeApplyEntity);

        //2. 发送BI Queryer通
        TemplateViewService viewService = (TemplateViewService)SpringContextUtil.getBean("templateViewService");
        TemplateViewEntity viewEntity = viewService.getByViewId(templateAssetChangeApplyEntity.getViewId());
        String templateViewUrl = getTemplateViewUrl(templateAssetChangeApplyEntity.getTplId(), templateAssetChangeApplyEntity.getViewId());

        /**
         * 发给申请人
         */
        List<String> receivers = new ArrayList<>();
        receivers.add(templateAssetChangeApplyEntity.getCreatedBy());
        StringBuilder messageText = new StringBuilder();
        messageText.append(String.format("系统自动通过了你的模版资产更新申请，你的%s视图已转为生效视图",viewEntity.getViewName()));
        messageText.append(String.format("点击链接查看：%s",templateViewUrl));
        sendApplyNotification("多维分析-模版资产申请已通过",messageText.toString(),receivers);

        /**
         * 发给管理员
         */

        StringBuilder sendToAdminMessageText = new StringBuilder();
        sendToAdminMessageText.append(String.format("系统自动通过了%s的模版资产更新申请，%s的%s视图已转为生效视图（因该视图资产与线上模版资产一致）",
                templateAssetChangeApplyEntity.getCreatedBy(),
                templateAssetChangeApplyEntity.getCreatedBy(),
                viewEntity.getViewName()));
        List<String> adminReceivers = configService.getTemplateOwnerNameByTplId(viewEntity.getTplId());
        sendApplyNotification("多维分析-模版资产申请已被系统通过",sendToAdminMessageText.toString(),adminReceivers);

    }

    /**
     * 获取模版视图的URL
     * @param tplId
     * @param viewId
     * @return
     */
    public String getTemplateViewUrl(String tplId, String viewId) {

        String uiBaseUrl = SC.v("ssm.ui.base.url", "https://datastudio-test.example.com");
        String templateViewUrl = String.format("%s/ssm/#/template/%s?viewId=%s",
                uiBaseUrl,
                tplId,
                viewId
        );

        return templateViewUrl;
    }

    /**
     * 发送资产变更申请通过/驳回的BI Queryer通消息给申请用户
     *
     */
    public void sendApprovalMsgToApplyUser(TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity, TemplateViewEntity viewEntity,boolean isAutoApproval) {


        /**
         * 【多维分析-模版资产申请已通过】/【多维分析-模版资产申请被驳回】:for用户:
         * xxx（系统）通过/拒绝了你的模版资产更新申请，你的xxx视图已转为生效视图/临时视图
         * (拒绝理由xxx)
         * 点击链接查看：xxx--用户个人视图的链接
         *
         */

        User user = UserManager.get();
        String userName = user.getName();

        String sign = "";
        StringBuilder messageText = new StringBuilder();
        if(ApprovalStatusType.REJECTED == ApprovalStatusType.getByCode(templateAssetChangeApplyEntity.getApprovalStatus())){
            sign = "多维分析-模版资产申请被驳回";
            messageText.append(String.format("%s拒绝了你的模版资产更新申请，你的%s视图已转为临时视图\n"
                    ,userName, viewEntity.getViewName()
            ));
            messageText.append(String.format("拒绝理由:%s\n", templateAssetChangeApplyEntity.getApprovalRemark()));
        }else{
            sign = "多维分析-模版资产申请已通过";
            String approvalUserName = userName;
            if(isAutoApproval){
                approvalUserName = "系统自动";
            }
            messageText.append(String.format("%s通过了你的模版资产更新申请，你的%s视图已转为生效视图\n"
                    ,approvalUserName, viewEntity.getViewName()
            ));

        }

        String templateViewUrl = getTemplateViewUrl(templateAssetChangeApplyEntity.getTplId(), templateAssetChangeApplyEntity.getViewId());
        messageText.append(String.format("点击链接查看：%s",templateViewUrl));

        List<String> receivers = new ArrayList<>();
        receivers.add(templateAssetChangeApplyEntity.getCreatedBy());
        sendApplyNotification(sign,messageText.toString(),receivers);

    }

    /**
     * 发送资产变更申请通过/驳回的BI Queryer通消息给其它管理员
     * @param templateAssetChangeApplyEntity
     * @param viewEntity
     */
    public void sendApprovalMsgToOtherAdminUser(TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity, TemplateViewEntity viewEntity,String newViewId){

        /**
         * 【多维分析-模版资产申请已被其他管理员通过】/【多维分析-模版资产申请已被其他管理员被驳回】/【多维分析-模版资产申请已被系统通过】:for其他管理员:
         * 管理员xxx通过/拒绝了xxx的模版资产更新申请，xxxx的xxx视图已转为生效视图/临时视图
         * 系统自动通过了xxx的模版资产更新申请，xxxx的xxx视图已转为生效视图（因该视图资产与线上模版资产一致）
         * (拒绝理由xxx)
         * 点击链接查看：xxx--若该审批被管理员通过，查看新复制的公共视图；若该审批为驳回/被系统通过，不带链接
         */

        User user = UserManager.get();
        String userName = user.getName();

        String sign = "";
        StringBuilder messageText = new StringBuilder();
        if(ApprovalStatusType.REJECTED == ApprovalStatusType.getByCode(templateAssetChangeApplyEntity.getApprovalStatus())){
            sign = "多维分析-模版资产申请已被其他管理员被驳回";
            messageText.append(String.format("管理员%s拒绝了%s的模版资产更新申请，%s的%s视图已转为临时视图\n",
                    userName,
                    templateAssetChangeApplyEntity.getCreatedBy(),
                    templateAssetChangeApplyEntity.getCreatedBy(),
                    viewEntity.getViewName()
            ));
            messageText.append(String.format("拒绝理由:%s", templateAssetChangeApplyEntity.getApprovalRemark()));
        }else{
            sign = "多维分析-模版资产申请已被其他管理员通过";
            messageText.append(String.format("管理员%s通过了%s的模版资产更新申请，%s的%s视图已转为生效视图\n" ,
                    userName,
                    templateAssetChangeApplyEntity.getCreatedBy(),
                    templateAssetChangeApplyEntity.getCreatedBy(),
                    viewEntity.getViewName()
            ));

            String templateViewUrl = getTemplateViewUrl(templateAssetChangeApplyEntity.getTplId(), newViewId);
            messageText.append(String.format("点击链接查看：%s",templateViewUrl));
        }

        List<String> receivers = configService.getTemplateOwnerNameByTplId(viewEntity.getTplId());

        //排除掉审批人自己
        receivers.remove(userName);
        sendApplyNotification(sign,messageText.toString(),receivers);

    }



    /**
     * 撤销资产变更申请
     * @param applyId
     */
    public void revoke(Long applyId) {

        Map<String, Object> map = new HashMap<>();
        map.put("applyId", applyId);
        map.put("updatedBy", UserManager.get().getName());
        dao.update("ssm.template.asset.change.apply.revoke", map);

    }

    /**
     * 获取资产变更申请详情
     * @param applyId
     * @return
     */
    public TemplateAssetChangeApplyRsp get(Long applyId) {

        TemplateAssetChangeApplyRsp result = new TemplateAssetChangeApplyRsp();

        TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity = (TemplateAssetChangeApplyEntity) dao.queryObject("ssm.template.asset.change.apply.get", applyId);
        if (templateAssetChangeApplyEntity == null) {
            throw new RuntimeException(String.format("资产变更申请ID: %s不存在", applyId));
        }

        result.setApplyId(applyId);
        result.setTplId(templateAssetChangeApplyEntity.getTplId());
        result.setViewId(templateAssetChangeApplyEntity.getViewId());
        result.setApplyUser(templateAssetChangeApplyEntity.getCreatedBy());
        result.setApplyRemark(templateAssetChangeApplyEntity.getApplyRemark());
        result.setApplyTime(templateAssetChangeApplyEntity.getCreatedTime());
        result.setApplyPendingTime(templateAssetChangeApplyEntity.getApplyPendingTime());

        result.setApprovalStatus(templateAssetChangeApplyEntity.getApprovalStatus());
        result.setApprovalRemark(templateAssetChangeApplyEntity.getApprovalRemark());
        result.setApprovedBy(templateAssetChangeApplyEntity.getApprovedBy());
        result.setApprovedTime(templateAssetChangeApplyEntity.getApprovedTime());

        //查询视图信息
        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", templateAssetChangeApplyEntity.getViewId());
        if (templateViewEntity != null) {
            result.setViewName(templateViewEntity.getViewName());
        }

        //查询申请人和审批人的用户信息
        List<String> userNameList = new ArrayList<>();
        userNameList.add(templateAssetChangeApplyEntity.getCreatedBy());
        userNameList.add(templateAssetChangeApplyEntity.getApprovedBy());
        List<UserRsp> userList = (List<UserRsp>) dao.queryObjectList("user.batchQueryUser", userNameList);
        Map<String, UserRsp> userMap = userList.stream().collect(Collectors.toMap(UserRsp::getUserName, v -> v));

        UserRsp applyUser = userMap.get(templateAssetChangeApplyEntity.getCreatedBy());
        if(applyUser!=null){
            result.setApplyUserName(applyUser.getUserName());
        }

        UserRsp approvedUser = userMap.get(templateAssetChangeApplyEntity.getApprovedBy());
        if(approvedUser!=null){
            result.setApprovedUserName(approvedUser.getUserName());
        }

        return result;
    }

    /**
     * 获取模用户待审批的资产变更申请列表
     * @param tplId
     * @return
     */
    public List<TemplateAssetChangeApplyRsp> getApplyListByTplId(String tplId) {

        User user = UserManager.get();

        //判断是不是模板owner
        //不是模板owner不可审批，直接返回空列表
        List<String> templateOwnerNameList = configService.getTemplateOwnerNameByTplId(tplId);
        if (!templateOwnerNameList.contains(user.getName())) {
            return new ArrayList<>();
        }

        return listByTplId(tplId);
    }

    /**
     * 获取模板下所有的资产变更申请列表
     * @param tplId
     * @return
     */
    public List<TemplateAssetChangeApplyRsp> listByTplId(String tplId) {

        List<TemplateAssetChangeApplyRsp> result = new ArrayList<>();
        result = (List<TemplateAssetChangeApplyRsp>) dao.queryObjectList("ssm.template.asset.change.apply.getUnfinishedApplyListByTplId", tplId);

        if (CollUtil.isNotEmpty(result)) {

            List<String> viewIdList = result.stream().map(TemplateAssetChangeApplyRsp::getViewId).collect(Collectors.toList());
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("viewIdList", viewIdList);
            List<TemplateViewEntity> viewEntityList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.batchGetByViewId", queryParams);

            Map<String, TemplateViewEntity> viewMap = new HashMap<>();
            for (TemplateViewEntity templateViewEntity : viewEntityList) {
                viewMap.put(templateViewEntity.getViewId(), templateViewEntity);
            }

            //查询申请人的用户信息
            List<String> userNameList = result.stream().map(TemplateAssetChangeApplyRsp::getApplyUser).collect(Collectors.toList());
            List<UserRsp> userList = (List<UserRsp>) dao.queryObjectList("user.batchQueryUser", userNameList);
            Map<String, UserRsp> userMap = userList.stream().collect(Collectors.toMap(UserRsp::getUserName, v -> v));

            //设置视图名称
            for (TemplateAssetChangeApplyRsp templateAssetChangeApplyRsp : result) {
                TemplateViewEntity templateViewEntity = viewMap.get(templateAssetChangeApplyRsp.getViewId());
                if (templateViewEntity != null) {
                    templateAssetChangeApplyRsp.setViewName(templateViewEntity.getViewName());
                }

                UserRsp userRsp = userMap.get(templateAssetChangeApplyRsp.getApplyUser());
                if (userRsp != null) {
                    templateAssetChangeApplyRsp.setApplyUserName(userRsp.getUserName());
                }

            }

        }

        return result;
    }


}
