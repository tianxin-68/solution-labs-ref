package com.bi.queryer.sys.dim.dim;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.dim.vo.Dim;
import com.bi.queryer.sys.model.BIModelAndView;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.render.VueDataGridRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 维度配置
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("dim")
public class DimController extends BaseController {

    @Autowired
    private DimService dimService = null;

    @Autowired
    private DataAuthService dataAuthService = null;

    @RequestMapping("")
    public BIModelAndView execute() {

        BIModelAndView biModelAndView = new BIModelAndView("/common/error_404");
        return biModelAndView;
    }

    @RequestMapping("dataAuth")
    public BIModelAndView executeDataAuth() {

        BIModelAndView biModelAndView = new BIModelAndView("/common/error_404");
        return biModelAndView;
    }

    @RequestMapping("datagrid/vue")
    public void buildVueDataGrid() {
        DataGrid grid = dimService.buildDataGrid(toStringMap());
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping("dataAuth/datagrid/vue")
    public void buildDataAuthVueDataGrid() {
        DataGrid grid = dataAuthService.buildDataGrid(toStringMap());
        grid.setRenderClassName(VueDataGridRender.class.getName());
        String result = grid.toJSON().toString();
        writeJSON(result);
    }

    @RequestMapping("saveDim")
    public ResponseMessage saveDim(@RequestBody Dim dim) {
        return dimService.saveDim(dim);
    }

    @RequestMapping("deleteDimByCfgid")
    public ResponseMessage deleteDimByCfgid() {
        String cfgId = stringValue("cfgId");
        return dimService.deleteDimByCfgid(cfgId);
    }

    /**
     * 构建模块维度树
     * @return
     */
    @RequestMapping("buildDataAuthTree")
    public ResponseMessage buildDataAuthTree(){
        return dimService.buildDataAuthTree();
    }

    /**
     * 获取维度表对应的详情
     * @return
     */
    @RequestMapping("getDimDetailList")
    public ResponseMessage getDimDetailList() {
        Map<String, String> map = toStringMap();
        map.put("token", BIUtil.getUToken(request));
        return dimService.getDimDetailList(map);
    }

    /**
     * 删除数据权限配置
     * @return
     */
    @RequestMapping("deleteDataAuthByAuthId")
    public ResponseMessage deleteDataAuthByAuthId(){
        String authId = stringValue("authId");
        return dataAuthService.deleteDataAuthByAuthId(authId);
    }
}
