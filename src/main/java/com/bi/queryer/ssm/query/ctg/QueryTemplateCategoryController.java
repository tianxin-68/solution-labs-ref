package com.bi.queryer.ssm.query.ctg;

import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.ctg.model.TemplateSpaceAddReq;
import com.bi.queryer.ssm.query.ctg.model.TemplateSpaceDetailRsp;
import com.bi.queryer.ssm.query.template.model.TemplateShareReq;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 模版目录相关接口
 */
@Controller
@Scope("prototype")
@RequestMapping("ssd/template")
public class QueryTemplateCategoryController {

    @Autowired
    private QueryTemplateCategoryService service;

    @Autowired
    private TemplateSpaceService spaceService;

    /**
     * 目录树
     * @return
     */
    @RequestMapping(path = {"ctg/tree", "queryCtgDataList"})
    @ResponseBody
    public SSMResponseMessage<List<QueryTemplateCategory>> tree() {
        return SSMResponseMessage.success("查询成功！", service.getCategoryTree(BIConsts.TEMPLATE_CTG_ROOT_ID));
    }

    /**
     * 共享空间目录树
     * @return
     */
    @RequestMapping(path = {"ctg/space"})
    @ResponseBody
    public SSMResponseMessage<List<QueryTemplateCategory>> getSpaceTree() {
        return SSMResponseMessage.success("查询成功！", service.getAllSpaceCategories());
    }

    /**
     * 共享空间目录树和门户有权限的共享空间
     * @return
     */
    @RequestMapping(path = {"ctg/space/listViewableSpace"})
    @ResponseBody
    public SSMResponseMessage<List<QueryTemplateCategory>> listViewableSpace() {
        return SSMResponseMessage.success("查询成功！", service.getAllViewableSpaceCategories());
    }

    /**
     * 有权限编辑的共享空间目录树
     * @return
     */
    @RequestMapping(path = {"ctg/space/listUserEditableSpace"})
    @ResponseBody
    public SSMResponseMessage<List<QueryTemplateCategory>> listUserEditableSpace() {
        return SSMResponseMessage.success("查询成功！", service.listUserEditableSpace());
    }

    /**
     * 新增目录
     * @return
     */
    @RequestMapping(path = {"ctg/add"})
    @ResponseBody
    public SSMResponseMessage<String> addCategory(@RequestBody QueryTemplateCategory ctg) {
        if (BIUtil.isEmpty(ctg.getParentId())) {
            return SSMResponseMessage.operationFailed("上级分类不允许为空");
        }
        return SSMResponseMessage.success("新增目录成功！", service.addCategory(ctg));
    }


    /**
     * 修改目录
     * @return
     */
    @RequestMapping(path = {"ctg/update"})
    @ResponseBody
    public SSMResponseMessage<String> updateCategory(@RequestBody QueryTemplateCategory ctg) {
        if (BIUtil.isEmpty(ctg.getId())) {
            return SSMResponseMessage.operationFailed("请选择要修改的分类（分类id为空）");
        }
        return SSMResponseMessage.success("新增更改成功！", service.updateCategory(ctg));
    }

    /**
     * 删除目录
     * @return
     */
    @RequestMapping(path = {"ctg/delete"})
    @ResponseBody
    public SSMResponseMessage<String> deleteCategory(@RequestBody QueryTemplateCategory ctg) {
        return SSMResponseMessage.success("目录删除成功！", service.deleteCategory(ctg.getId()));
    }

    /**
     * 获取目录详情
     * @return
     */
    @RequestMapping(path = {"ctg/get"}, method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<QueryTemplateCategory> getCategory(String ctgId) {
        return SSMResponseMessage.success("获取目录详情成功！", service.getCategory(ctgId));
    }

    /**
     * 分享目录给用户
     * @return
     */
    @RequestMapping(path = {"ctg/shareCtgToUsers"})
    @ResponseBody
    public SSMResponseMessage<Object> shareToUsers(@RequestBody TemplateShareReq req) {
        service.shareCtgToUsers(req);
        return SSMResponseMessage.success("分享成功！");
    }

    /**
     * 分享目录给部门
     * @return
     */
    @RequestMapping(path = {"ctg/shareCtgToDept"})
    @ResponseBody
    public SSMResponseMessage<Object> shareCtgToDept(@RequestBody TemplateShareReq req) {
        service.shareCtgToDept(req);
        return SSMResponseMessage.success("分享成功！");
    }

    /**
     * 新增共享模版空间
     */
    @RequestMapping(path = {"ctg/saveSpace"})
    @ResponseBody
    public SSMResponseMessage<String> saveSpace(@RequestBody TemplateSpaceAddReq req) {
        return SSMResponseMessage.success("保存成功！", spaceService.saveSpace(req));
    }

    /**
     * 共享空间详情查询（传入空间ID）
     */
    @RequestMapping(path = {"ctg/spaceDetail"})
    @ResponseBody
    public SSMResponseMessage<TemplateSpaceDetailRsp> spaceDetail(@RequestBody TemplateSpaceAddReq req) {
        TemplateSpaceDetailRsp detailRsp = spaceService.spaceDetail(req.getId());
        return SSMResponseMessage.success("详情查询成功！", detailRsp);
    }

    /**
     * 退出共享空间--传目录ID
     */
    @RequestMapping(path = {"ctg/exitSpace"})
    @ResponseBody
    public SSMResponseMessage<TemplateSpaceDetailRsp> exitSpace(@RequestBody TemplateSpaceAddReq req) {
        spaceService.exitSpace(req.getId());
        return SSMResponseMessage.success("退出空间成功！");
    }

}
