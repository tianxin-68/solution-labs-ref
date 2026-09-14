package com.bi.queryer.ssm.mgr.fieldCtg;

import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.MetaFieldDataAuth;
import com.bi.queryer.ssm.mgr.fieldCtg.model.*;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.model.BIModelAndView;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.render.VueDataGridRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("fieldCtg")
public class FieldCtgController extends BaseController {

    @Autowired
    private FieldCtgService fieldCtgService = null;

    @RequestMapping("")
    public BIModelAndView execute() {
        BIModelAndView biModelAndView = new BIModelAndView("ssm/fieldCtg/fieldCtg_manager");
        return biModelAndView;
    }

    /**
     * 构建字段目录树
     *
     * @param type
     * @return
     */
    @RequestMapping("buildFieldCtgTree")
    public ResponseMessage buildFieldCtgTree(String type, String datasetId) {
        return fieldCtgService.buildFieldCtgTree(type, datasetId);
    }

    @RequestMapping("saveFieldCtg")
    public ResponseMessage saveFieldCtg(@RequestBody MetaFieldCategory metaFieldCategory) {
        return fieldCtgService.saveFieldCtg(metaFieldCategory);
    }

    @RequestMapping("deleteFieldCtgById")
    public ResponseMessage deleteFieldCtgById(String id) {
        return fieldCtgService.deleteFieldCtgById(id);
    }

    @RequestMapping("datagrid/vue")
    public void buildVueDataGrid() {
        DataGrid grid = fieldCtgService.buildDataGrid(toStringMap());
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping("saveField")
    public ResponseMessage saveField(@RequestBody Map<String, Object> map) {
        return fieldCtgService.saveField(map);
    }

    @RequestMapping("getFieldByCtgId")
    public ResponseMessage getFieldByCtgId(@RequestBody Map<String, Object> map) {
        return fieldCtgService.getFieldByCtgId(map);
    }

    /**
     * 删除字段与目录的关联
     *
     * @param map
     * @return
     */
    @RequestMapping("deleteFieldCtgAssociate")
    public ResponseMessage deleteFieldCtgAssociate(@RequestBody Map<String, Object> map) {
        return fieldCtgService.deleteFieldCtgAssociate(map);
    }

    @RequestMapping("exportMethod")
    public void exportMethod(@RequestParam(value = "searchTxt", required = false) String searchTxt,
                             @RequestParam(value = "ctgType", required = false) String ctgType,
                             @RequestParam(value = "ctgId", required = false) String ctgId,
                             @RequestParam(value = "type", required = false) String type,
                             HttpServletResponse response) {

        fieldCtgService.exportMethod(searchTxt,ctgType,ctgId, type, response);
    }

    /**
     * 获取目录的数据权限设置
     */
    @RequestMapping("queryFieldCtgAuthSetting")
    public ResponseMessage queryFieldCtgAuthSetting(@RequestBody CtgAuthDetailReq req) {
        return fieldCtgService.queryFieldCtgAuthSetting(req);
    }

    /**
     * 保存数据权限设置
     */
    @RequestMapping("saveCtgAuthSetting")
    public ResponseMessage saveCtgAuthSetting(@RequestBody CtgAuthSaveReq req) {
        return fieldCtgService.saveCtgAuthSetting(req);
    }

    /**
     * 获取权限申请的目录列表
     */
    @RequestMapping("queryAuthApplyCtgList")
    public ResponseMessage queryAuthApplyCtgList(@RequestBody CtgListReq req) {
        return fieldCtgService.queryAuthApplyCtgList(req);
    }

    /**
     * 获取用户行级权限
     */
    @RequestMapping("getUserDataAuth")
    public ResponseMessage getUserDataAuth() {
        SSDQueryService queryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");
        User user = UserManager.get();
        List<MetaFieldDataAuth> userDataAclList = queryService.getDataAuthByUser(user.getName());
        Map<String, List<MetaFieldDataAuth>> resultMap = userDataAclList.stream().collect(Collectors.groupingBy(MetaFieldDataAuth::getDimRealCode));
        return new ResponseMessage(resultMap);
    }

    /**
     * 获取模块说明列表
     */
    @RequestMapping("getModuleDescList")
    public ResponseMessage getModuleDescList(Integer pageNum, Integer pageSize) {
        return fieldCtgService.getModuleDescList(pageNum, pageSize);
    }

    /**
     * 保存数据集和目录的权限配置
     *
     * @return
     */
    @RequestMapping("saveDatasetCtgDataAuth")
    @ResponseBody
    public SSMResponseMessage saveDatasetCtgDataAuth(@RequestBody DatasetCtgDataAuthSaveReq datasetCtgDataAuthSaveReq) {
        return fieldCtgService.saveDatasetCtgDataAuth(datasetCtgDataAuthSaveReq);
    }

    /**
     * 查询权限配置
     *
     * @param datasetCtgDataAuthQueryReq
     * @return
     */
    @RequestMapping("queryDatasetCtgDataAuth")
    @ResponseBody
    public SSMResponseMessage<DatasetCtgDataAuthRsq> queryDatasetCtgDataAuth(@RequestBody DatasetCtgDataAuthQueryReq datasetCtgDataAuthQueryReq) {
        return fieldCtgService.queryDatasetCtgDataAuth(datasetCtgDataAuthQueryReq);
    }

    /**
     * 查询继承的权限配置
     *
     * @param datasetCtgDataAuthQueryReq
     * @return
     */
    @RequestMapping("queryDatasetCtgInheritedDataAuth")
    @ResponseBody
    public SSMResponseMessage<List<DatasetCtgDataAuthInheritRsq>> queryDatasetCtgInheritedDataAuth(@RequestBody DatasetCtgDataAuthQueryReq datasetCtgDataAuthQueryReq) {
        return fieldCtgService.queryDatasetCtgInheritedDataAuth(datasetCtgDataAuthQueryReq);
    }

    /**
     * 人员组织变更，权限处理
     * @return
     */
    @RequestMapping("employeeChange")
    @ResponseBody
    public SSMResponseMessage employeeTransfer(@RequestBody EmployeeTransferReq employeeTransferReq){
        return fieldCtgService.employeeTransfer(employeeTransferReq);
    }

}
