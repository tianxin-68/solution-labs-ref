package com.bi.queryer.ssm.query.template.asset.change.apply;

import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyCreateRsp;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyReq;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyRsp;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.enums.Enabled;
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
@RequestMapping("ssm/template/asset/change/apply")
public class TemplateAssetChangeApplyController {

    @Autowired
    private TemplateAssetChangeApplyService templateAssetChangeApplyService;

    /**
     * 创建资产变更申请
     * @param req
     * @return
     */
    @RequestMapping(value = "create", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<TemplateAssetChangeApplyCreateRsp> create(@RequestBody TemplateAssetChangeApplyReq req){

        TemplateAssetChangeApplyCreateRsp templateAssetChangeApplyCreateRsp = templateAssetChangeApplyService.create(req);
        String message = Enabled.value(templateAssetChangeApplyCreateRsp.getIsAutoApproval()) ? "资产变更申请已自动通过" : "创建资产变更申请成功，请等待管理员审批";
        return SSMResponseMessage.success(message,templateAssetChangeApplyCreateRsp);
    }

    /**
     * 审批资产变更申请
     * @param req
     * @return
     */
    @RequestMapping(value = "approval", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> approval(@RequestBody TemplateAssetChangeApplyReq req){
        return SSMResponseMessage.success("审批资产变更申请成功", templateAssetChangeApplyService.approval(req,false));
    }

    /**
     * 获取资产变更申请详情
     * @param applyId
     * @return
     */
    @RequestMapping(value = "get", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<TemplateAssetChangeApplyRsp> get(Long applyId) {
        return SSMResponseMessage.success("获取资产变更申请成功", templateAssetChangeApplyService.get(applyId));
    }

    /**
     * 获取模板下资产变更申请列表
     * @param tplId
     * @return
     */
    @RequestMapping(value = "getApplyListByTplId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateAssetChangeApplyRsp>> getApplyListByTplId(String tplId){
        return SSMResponseMessage.success("获取模板下资产变更申请列表成功", templateAssetChangeApplyService.getApplyListByTplId(tplId));
    }

    /**
     * 撤销资产变更申请
     * @param applyId
     * @return
     */
    @RequestMapping(value = "revoke", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage revoke(Long applyId){
        templateAssetChangeApplyService.revoke(applyId);
        return SSMResponseMessage.success("撤销资产变更申请成功");
    }

}
