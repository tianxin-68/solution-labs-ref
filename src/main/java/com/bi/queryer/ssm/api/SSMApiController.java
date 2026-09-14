package com.bi.queryer.ssm.api;

import com.bi.queryer.ssm.api.vo.req.*;
import com.bi.queryer.ssm.api.vo.rsp.DataCtgAuthDetailRsp;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@Scope("prototype")
@RequestMapping("/ssm/api")
@Slf4j
public class SSMApiController {

    @Autowired
    private SSMApiService ssmApiService;

    /**
     * 发布模块
     *
     * @return
     */
    @RequestMapping(value = "publishModule", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage publishModule(@RequestBody PublishModuleReq publishModuleReq) {
        try {
            return ssmApiService.publishModule(publishModuleReq);
        } catch (Exception e) {
            log.error("发布模块失败", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 下线模块
     *
     * @param offlineModuleReq
     * @return
     */
    @RequestMapping(value = "offlineModule", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage offlineModule(@RequestBody OfflineModuleReq offlineModuleReq) {
        return ssmApiService.offlineModule(offlineModuleReq);
    }

    /**
     * 发布字段
     *
     * @param metaFields
     * @return
     */
    @RequestMapping(value = "publishField", method = RequestMethod.POST)
    @FreeCheckAuthority
    @ResponseBody
    public SSMResponseMessage publishField(@RequestBody List<MetaField> metaFields) {
        try {
            return ssmApiService.publishField(metaFields);
        } catch (Exception e) {
            log.error("发布字段失败", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 更新分类排序
     *
     * @param categoryList
     * @return
     */
    @RequestMapping(value = "changeCtgSortId", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage changeCtgSortId(@RequestBody List<MetaFieldCategory> categoryList) {
        return ssmApiService.changeCtgSortId(categoryList);
    }

    /**
     * 查询模块信息
     * @param queryCategoryReq
     * @return
     */
    @RequestMapping(value = "queryCategoryById", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<MetaFieldCategory> queryCategoryById(@RequestBody QueryCategoryReq queryCategoryReq){
        return ssmApiService.queryCategoryById(queryCategoryReq);
    }

    /**
     * 同步多维模块权限
     * @return
     */
    @RequestMapping(value = "syncModuleCtgAuth", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage syncModuleCtgAuth(@RequestBody List<SyncModuleCtgAuthReq> list) {
        return ssmApiService.syncModuleCtgAuth(list);
    }

    /**
     * 查询数据目录权限列表
     * @param queryDataCtgAuthReq
     * @return
     */
    @RequestMapping(value = "queryDataCtgAuthList", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<DataCtgAuthDetailRsp>> queryDataCtgAuthList(@RequestBody QueryDataCtgAuthReq queryDataCtgAuthReq){
        DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
        return SSMResponseMessage.success("", ssmApiService.queryDataCtgAuthList(queryDataCtgAuthReq,mgpDataSourceType));
    }

    /**
     * 构建用户目录权限明细
     * @return
     */
    @RequestMapping(value = "buildUserCtgAuthDtl", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage buildUserCtgAuthDtl(){
        return ssmApiService.buildUserCtgAuthDtl();
    }

    /**
     * 发送目录权限临期提醒邮件
     * @return
     */
    @RequestMapping(value = "sendCtgAuthExpiringMail", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage sendCtgAuthExpiringMail() {
        try {
            return ssmApiService.sendCtgAuthExpiringMail();
        } catch (Exception e) {
            log.error("发送权限临期提醒邮件失败", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

}
