package com.bi.queryer.ssm.mgr.fieldDef;

import com.bi.queryer.ssm.meta.HierarchyInfo;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.mgr.fieldDef.model.AsyncMgpMetricReq;
import com.bi.queryer.ssm.mgr.fieldDef.model.FieldReq;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.bi.queryer.sys.model.BIModelAndView;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.render.VueDataGridRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("fieldDef")
public class FieldDefController extends BaseController {

	@Autowired
	private FieldDefService fieldDefService = null;

	@RequestMapping("")
	public BIModelAndView execute() {
		BIModelAndView biModelAndView = new BIModelAndView("ssm/fieldDef/fieldDef_manager");
		return biModelAndView;
	}

	@RequestMapping("datagrid/vue")
	public void buildVueDataGrid() {
		DataGrid grid = fieldDefService.buildDataGrid(toStringMap());
		grid.setRenderClassName(VueDataGridRender.class.getName());
		String result = grid.toJSON().toString();
		writeJSON(result);
	}

	@RequestMapping("saveFieldDef")
	public ResponseMessage saveFieldDef(@RequestBody MetaField metaField) {
		return fieldDefService.saveFieldDef(metaField);
	}

	@RequestMapping("deleteFieldDefById")
	public ResponseMessage deleteFieldDefById(String id) {
		return fieldDefService.deleteFieldDefById(id);
	}

	/**
	 * 字段树
	 * @return
	 */
	@RequestMapping("buildFieldTree")
	public ResponseMessage buildFieldTree() {
		return fieldDefService.buildFieldTree();
	}

	/**
	 * 保存层次
	 * @param hierarchyInfo
	 * @return
	 */
	@RequestMapping("saveHierarchyInfo")
	public ResponseMessage saveHierarchyInfo(@RequestBody HierarchyInfo hierarchyInfo) {
		return fieldDefService.saveHierarchyInfo(hierarchyInfo);
	}

	@RequestMapping("getHierarchyInfoByFieldId")
	public ResponseMessage getHierarchyInfoByFieldId() {
		String fieldId = stringValue("fieldId");
		return fieldDefService.getHierarchyInfoByFieldId(fieldId);
	}

	@RequestMapping("exportMethod")
	public void exportMethod(@RequestParam(value = "searchTxt", required = false) String searchTxt,
							 @RequestParam(value = "type", required = false) String type,
							 @RequestParam(value = "tableId", required = false) String tableId,
							 @RequestParam(value = "fact_table", required = false) String fact_table,
							 HttpServletResponse response) {

		fieldDefService.exportMethod(searchTxt, type, tableId, fact_table, response);
	}

	@RequestMapping("importMethod")
	public ResponseMessage importMethod(@RequestParam("files") MultipartFile files) {
		return fieldDefService.importMethod(files);
	}

	@RequestMapping("saveFieldDefAuthAssociate")
	public ResponseMessage saveFieldDefAuthAssociate(@RequestBody Map<String, Object> map) {
		return fieldDefService.saveFieldDefAuthAssociate(map);
	}

	@RequestMapping("getFieldAuthDimByFieldCode")
	public ResponseMessage getFieldAuthDimByFieldCode() {
		String fieldCode = stringValue("fieldCode");
		return fieldDefService.getFieldAuthDimByFieldCode(fieldCode);
	}

	@RequestMapping("getTableByFieldTableId")
	public ResponseMessage getTableByFieldTableId() {
		String tableId = stringValue("tableId");
		return fieldDefService.getTableByFieldTableId(tableId);
	}

	@RequestMapping("getTableFieldList")
	public ResponseMessage getTableFieldList() {
		String tableId = stringValue("tableId");
		return fieldDefService.getTableFieldList(tableId);
	}

	@RequestMapping("importField")
	public ResponseMessage importField(@RequestBody Map<String, Object> map) {
		return fieldDefService.importField(map);
	}

	/**
	 * 查看关联模板
	 * @return
	 */
	@RequestMapping("viewAssociateTemplate")
	public ResponseMessage viewAssociateTemplate() {
		String fieldId = stringValue("fieldId");
		return fieldDefService.viewAssociateTemplate(fieldId);
	}

	/**
	 * 查询所有白皮书指标
	 */
	@RequestMapping("/getManualIndexWhitePaperList")
	public ResponseMessage getManualIndexWhitePaperList() {
		return new ResponseMessage(fieldDefService.getAllWhitePaperList());
	}

	/**
	 * 查询所有关联白皮书指标的字段code
	 */
	@RequestMapping("/queryAllFieldCodeKpiRel")
	public ResponseMessage queryAllFieldCodeKpiRel(@RequestBody FieldReq req) {
		return fieldDefService.queryAllFieldCodeKpiRel(req);
	}


	/**
	 * 同步指标平台替换数据
	 * @param req
	 * @return
	 */
	@FreeCheckAuthority
	@RequestMapping("asyncMgpMetric")
	public ResponseMessage asyncMgpMetric(@RequestBody AsyncMgpMetricReq req) {
		return fieldDefService.asyncMgpMetric(req);
	}
}
