package com.bi.queryer.util.component.datagrid.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.UpperCaseMap;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.ComponentType;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.DataGridDataSetRow;
import com.bi.queryer.util.component.datagrid.DataGridToolButton;
import com.bi.queryer.util.component.datagrid.MergeCellInfo;
import com.bi.queryer.util.component.datasource.IDataSetProvider;
import com.bi.queryer.util.component.portlet.render.PortletRender;

public class DataGridRender extends PortletRender<DataGrid>{
	
	protected DataGrid grid = null;
	
	protected Map<String, List<MergeCellInfo>> mergeInfos = new LinkedHashMap<String, List<MergeCellInfo>>(); 
	
	protected String toolbarId = "";
	
	/**
	 * 起始行索引
	 */
	protected int beginRowIndex = 0;
	public DataGridRender(DataGrid grid){
		super(grid);
		this.grid = grid;
		initToolbar();
	}
	
	/**
	 * 初始化工具栏
	 */
	protected void initToolbar(){
		toolbarId = grid.getId() + "_Bar";
		DataGridToolButton[] btns = grid.getToolButtons();
		int index = -1;
		for(int i = 0; i < btns.length; i++){
			if(btns[i].isShow()){
				index++;
			}
			DataGridToolButton btn = btns[i];
			btn.setId(toolbarId + "_b" + i);
			btn.setActive(index == 0);
		}
	}
	
	@Override
	public String buildBeginHtml() {
		StringBuilder html = new StringBuilder();
		JSONObject dataOption = new JSONObject();
		dataOption.putAll(component.getAttributes());
		JSONObject containerOption = new JSONObject();
		containerOption.put(ComponentConstants.Parameter_Component_Type, ComponentType.DataGridContainer);
		containerOption.put("margin", component.getMargin().toString());
		String sizeStyle = "";
		sizeStyle = " style='width:" + component.getWidth() + "px;margin:auto;height:" + component.getHeight() + "px;'";
		html.append("<div ")
			  .append(" id='").append(component.getId() + "_C").append("'")
			  .append(super.buildDataOptions(containerOption, false))
			  .append(sizeStyle)
			  .append(" class='h5-report-grid-container "
					  + ComponentConstants.CSS_Container_Component 
					  + " " + ComponentConstants.CSS_Component 
					  + " " + getLayoutCSS()
					  + "'>");
			// 工具栏
			html.append(buildToolBar());
//			if(!component.isAutoSize()){
			sizeStyle = " style='width:" + component.getWidth() + "px;" + component.getHeight() + "px;'";
//			}
			html.append("<div ");
			html.append(" id='").append(component.getId()).append("'");
			html.append(buildDataOptions(dataOption));
			html.append(buildMessageOptions());
			html.append(" class='h5-report-grid " + ComponentConstants.CSS_Data_Component + "'");
			html.append(sizeStyle);
			html.append(">");
			html.append("</div>");
		html.append("</div>");
		
		
		return html.toString();
	}
	
	/**
	 * 构建工具栏
	 * @return
	 */
	protected StringBuilder buildToolBar(){
		StringBuilder html = new StringBuilder();
		if(!grid.isShowToolbar()){
			return html;
		}
		
		DataGridToolButton[] btns = grid.getToolButtons();
		html.append("<div ")
			.append(" id='").append(toolbarId).append("'")
			.append(" class='h5-datagrid-toolbar'")
			.append(" >");
		for(int i = 0; i < btns.length; i++){
			if(!btns[i].isShow()){
				continue;
			}
			html.append(buildToolButton(btns[i]));
		}
		html.append("</div>");
		return html;
	}
	
	protected StringBuilder buildToolButton(DataGridToolButton btn){
		StringBuilder html = new StringBuilder();
		JSONObject dataOption = new JSONObject();
		dataOption.put("code", btn.getCode());
		dataOption.put("value", btn.getValue());
		html.append("<div ")
			.append(" id='").append(btn.getId()).append("'");
		if(btn.isActive()){
			html.append(" class='toolbar-btn toolbar-btn-active " + "toolbar-btn-" + btn.getAlign() + "'");
		}else{
			html.append(" class='toolbar-btn " + "toolbar-btn-" + btn.getAlign() + "'");
		}
		html.append(super.buildDataOptions(dataOption, false))
			.append(" >");
		html.append(btn.getTitle());
		html.append("</div>");
		return html;
	}
	
	public JSONObject buildOptions(){
	
		JSONObject option = buildColumnOptions();
		/*
		// 列
		DataGridColumnRender columnBuilder = new DataGridColumnRender(grid);
		String columns = "{" + columnBuilder.buildSubQuerySql().toString() + "}";
		option = JSONObject.fromObject(columns);
		*/
		
		// 其他option
		option.put("width", grid.getWidth());
		option.put("height", grid.getHeight());
		option.put("fitColumns", grid.isFitColumns());
		option.put("autoRowHeight", grid.isAutoRowHeight());
		option.put("nowrap", grid.isNowrap());
		option.put("idField", grid.getIdField());
		option.put("loadMsg", "数据加载中...");
		option.put("pagination", grid.isPagination());
		option.put("isPagination", grid.isPagination());
		option.put("isRestrictedPagination", grid.isRestrictedPagination());
		option.put("selectedRowDefault", grid.getSelectedRowDefault());
		option.put("pageNumber", grid.getPageNumber());
		option.put("pageSize", grid.getPageSize());
		option.put("pageList", grid.getPageList());
		option.put("minPageBar", grid.isMinPageBar());
		option.put("rownumbers", grid.isShowRowNo());
		option.put("singleSelect", grid.isSingleSelect());
		option.put("remoteSort",  grid.isRemoteSort());
		option.put("striped",  grid.isStriped());
		option.put("border",  grid.isShowBorder());
		option.put("showHeader",  grid.isShowHeader());
		option.put("showFooter",  grid.isShowFooter());
		option.put("contextPath", grid.getContextPath());
		option.put("showTitle", grid.isShowTitle());
		option.put("frozenRowNum", grid.getFrozenRowNum());
		if(!StringUtil.isEmpty(grid.getToolbarId())){
			option.put("toolbar", "#" + grid.getToolbarId());
		}
		option.put("showToolbar", grid.isShowToolbar());
		option.put("showExport", grid.isShowExport());
		option.put("exportType", grid.getExportType().getExtName());
		option.put("exportMode", grid.getExportMode().getModeName());
		option.put("entityName", grid.getEntityName());
		option.put("entityId", grid.getEntityId());
		option.put("checkSelectCascade", grid.isCheckSelectCascade());
		option.put("exportLimit", grid.getExportLimit());
		option.put("showSummaryRow", grid.getShowSummaryRow());
		option.put("showHideColumnButton", grid.getShowHideColumnButton());
		
		if(!StringUtil.isEmpty(grid.getRowStyler())) {
			option.put("rowStyler", grid.getRowStyler());
		}
		
		// 废弃easyui的工具栏，使用自定义的。解决在手机端上最后一行被遮挡
		if(grid.isShowToolbar()){
			//option.put("toolbar", "#" + toolbarId);
		}
		
		// 工具栏选项
		JSONArray btnOpts = new JSONArray();
		for(DataGridToolButton btn : grid.getToolButtons()){
			JSONObject btnOpt = new JSONObject();
			btnOpt.put("id", btn.getId());
			btnOpt.put("show", btn.isShow());
			btnOpts.add(btnOpt);
		}
		option.put("toolbars", btnOpts);
		
		// 钻取信息
		option.put("drill", buildDrillOption());
		
		// 消息
		buildMessage();
		option.put("messages", this.dataMsgs);
		
		// 标题
		if(grid.getTitle() != null){
			option.put("title", BIUtil.getStrWithReg(grid.getTitle().getText(),component.getDataSetQueryParam()));
		}
		
		// 外部属性
		JSONObject extAttrs = new JSONObject();
		for(String key : grid.getAttributes().keySet()){
			extAttrs.put(key, grid.getAttributes().get(key));
		}
		//option.put("extAttrs", extAttrs);
		
		return option;
	}
	
	/**
	 * 构建列option
	 * @return
	 */
	protected JSONObject buildColumnOptions() {
		// 列
		DataGridColumnRender columnBuilder = new DataGridColumnRender(grid);
		String columns = "{" + columnBuilder.build().toString() + "}";
		JSONObject option = JSONObject.parseObject(columns);
		return option;
	}
	
	/**
	 * 构建钻取信息,暂时只支持单列可钻取
	 * @return
	 */
	protected JSONObject buildDrillOption(){
		JSONObject option = new JSONObject();
		DataGridColumn drillColumn = null;
		JSONArray parameters = new JSONArray();
		for(DataGridColumn col : grid.getColumns()){
			if(!col.isHidden() && col.isCanDrill()){
				drillColumn = col;
			}
			if(col.isCanDrill() || col.isDrillParameter()){
				parameters.add(col.getOriginField());
			}
		}
		if(drillColumn == null){
			return option;
		}
		option.put("mode", drillColumn.getDrillMode());
		option.put("field", drillColumn.getField());
		option.put("idField", drillColumn.getDrillIDName());
		option.put("levelField", drillColumn.getDrillLevelName());
		option.put("parameters", parameters);
		return option;
	}
	
	/**
	 * 添加默认排序参数
	 * @param queryParam
	 */
	public void addColumnSortParameter(Map queryParam){
		queryParam.put(ComponentConstants.DataGrid_Parameter_Sort, buildDefaultSortInfo(queryParam));
	}
	
	public void addPageParameter(Map queryParam){
		if(!grid.isPagination()){
			return;
		}
		// 分页参数
		Integer currPageNo = null;
		if(queryParam == null){
			queryParam = new HashMap();
		}
		if(queryParam.get("currPageNo") != null){
			currPageNo = Integer.valueOf(queryParam.get("currPageNo") + "");
		}
		Integer prePageSize = null;
		if(queryParam.get("prePageSize") != null){
			prePageSize = Integer.valueOf(queryParam.get("prePageSize") + "");
		}
		if(currPageNo != null && prePageSize != null){
			//if(currPageNo == 0) currPageNo = 1; // 索引从1开始
			int pageRowLower = (currPageNo - 1) * prePageSize + 1;
			queryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower, pageRowLower == 1 ? 0 : (pageRowLower -1));
			queryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower2, pageRowLower == 1 ? 0 : (pageRowLower-1));
			int pageRowUpper = currPageNo * prePageSize;
			queryParam.put(ComponentConstants.DataGrid_Parameter_PageRowUpper, pageRowUpper);
		}else {
			queryParam.put(ComponentConstants.DataGrid_Parameter_PageRowLower, 0);
			queryParam.put(ComponentConstants.DataGrid_Parameter_PageRowUpper, Integer.MAX_VALUE);
		}
		if(currPageNo != null) {
			queryParam.put("currPageNo", (currPageNo - 1));
		}
	}
	
	/**
	 * 构建默认排序信息
	 * @return
	 */
	public String buildDefaultSortInfo(Map queryParam){
		String currentSortColumn = StringUtil.ifNull(queryParam.get("__sortColumn"));
		String currentSortType = StringUtil.ifNull(queryParam.get("__sortType"));
		StringBuilder sb = new StringBuilder();
		String nullsLastClause = " ";
		DataSourceType dsType = grid.getDsType();
		if(dsType != null && "oracle".equalsIgnoreCase(dsType.getDialect())){
			nullsLastClause = " NULLS LAST ";
		}
		int index = -1;
		boolean hasCurrentSortColumn = false;
		for(DataGridColumn col : grid.getColumns()){
			hasCurrentSortColumn = hasCurrentSortColumn || col.getField().equalsIgnoreCase(currentSortColumn);
			if(col.isDefaultSortColumn()){
				index++;
				if(index != 0){
					sb.append(",");
				}
				sb.append(col.getField() + " " + col.getDefaultSortType()).append(" ").append(nullsLastClause);
			}
		}
		if(index == -1){
			sb.append(" 1 ASC ").append(nullsLastClause);
		}else if(hasCurrentSortColumn){
			sb = new StringBuilder();
			sb.append(currentSortColumn).append(" ").append(currentSortType).append(nullsLastClause);
		}
		return sb.toString();
	}
	
	/**
	 * 获取页列表
	 * @return
	 */
	private String getPageList(){
		JSONArray arr = new JSONArray();
		for(Integer value : grid.getPageList()) {
			arr.add(value);
		}
		return arr.toString();
	}
	
	public static void main(String[] args) {
		JSONArray ja = new JSONArray();
		List<Integer> list = new ArrayList<Integer>();
		for(int i = 0 ;i < 4; i++){
			//list.add(i);
			ja.add(i);
		}
		//ja.put(list);
		System.out.println(ja);
		
		String str = "{aa:'bb',a2:'ccdd',a3:fun,";
		int index = str.lastIndexOf(",");
		System.out.println(index + ":" + (str.length() -1));
		StringBuilder sb = new StringBuilder(str);
		str = sb.replace(index, index + 1, "").toString();
		System.out.println(sb);
	}
	
	
	/**
	 * 构建合并单元格信息：根据列内容相同的单元格进行合并
	 * @param dataset
	 * @return
	 */
	protected JSONArray buildMergeCellInfo(List<? extends JSONSerializable> dataset){
		JSONArray jsonArr = new JSONArray();
		try{
			if(dataset == null || dataset.isEmpty()) {
				return jsonArr;
			}
			
			// 可进行合并的列
			List<DataGridColumn> mergeColumns = getMergeColumns();
			if(mergeColumns.size() == 0) return jsonArr;
			// 单元格合并信息<field, info>
			// 初始化
			for(DataGridColumn dgColumn : mergeColumns){
				mergeInfos.put(dgColumn.getField(), new ArrayList<MergeCellInfo>());
			}
			
			// 上一行数据<field, 合并列组合信息>
			Map<String, String> preRowValues = new HashMap<String, String>();
			int size = dataset.size();
			String fieldName = "";
			for(int i = 0; i < size; i++){
				JSONObject row = dataset.get(i).toJSON();
				String value = "";
				for(DataGridColumn dgColumn : mergeColumns){
					fieldName = dgColumn.getField();
					value = value + row.get(fieldName);
					if(value.equals(preRowValues.get(fieldName))){
						List<MergeCellInfo> infoList = mergeInfos.get(fieldName);
						MergeCellInfo merge = infoList.get(infoList.size() - 1);// 获取最后一个合并信息
						merge.rowspan++;
					}else{
						preRowValues.put(fieldName, value);
						MergeCellInfo merge = new MergeCellInfo();
						merge.field = dgColumn.getField();
						merge.index = i;
						merge.rowspan = 1;
						mergeInfos.get(fieldName).add(merge);
					}
				}
			}
			
			// 计算开始合并行号
			jsonArr = mergeCellInfo2Json();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return jsonArr;
	}
	
	/**
	 * 将合并单元格信息转为json
	 * @return
	 */
	protected JSONArray mergeCellInfo2Json(){
		JSONArray jsonArr = new JSONArray();
		try{
			for(List<MergeCellInfo> list : mergeInfos.values()){
				for(MergeCellInfo info : list){
					JSONObject obj = new JSONObject();
					obj.put("index", info.index + beginRowIndex);
					obj.put("rowspan", info.rowspan);
					obj.put("field", info.field);
					jsonArr.add(obj);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return jsonArr;
	}
	
	/**
	 * 获取可合并列
	 */
	private List<DataGridColumn> getMergeColumns(){
		List<DataGridColumn> columns = new ArrayList<DataGridColumn>();
		for(DataGridColumn column : grid.getColumns()) {
			if(column.isMergeCell()) {
				columns.add(column);
			}
		}
		return columns;
	}
	
	/**
	 * 将数据集转换为json格式
	 * @param dataset
	 * @param grid
	 * @return
	 */
	protected String dataset2JSON(List<UpperCaseMap> dataset){
		// TODO
		return "";
	}
	
	/**
	 * 获取数据集总行数，用于分页显示
	 * @param dataset
	 * @param grid
	 * @return
	 */
	public int getTotalSize(List<UpperCaseMap> dataset){
		// TODO
		return 0;
	}

	public int getBeginRowIndex() {
		return beginRowIndex;
	}

	public void setBeginRowIndex(int beginRowIndex) {
		this.beginRowIndex = beginRowIndex;
	}

	public Map<String, List<MergeCellInfo>> getMergeInfos() {
		return mergeInfos;
	}

	public void setMergeInfos(Map<String, List<MergeCellInfo>> mergeInfos) {
		this.mergeInfos = mergeInfos;
	}
	
	@Override
	public JSONObject buildDataSet() {
		JSONObject datasetJSON = new JSONObject();
		JSONArray rows = new JSONArray();
		datasetJSON.put("rows", rows);
		
		// 添加总记录数
		datasetJSON.put("total", 0);
		
		Map queryParam = grid.getDataSetQueryParam();
		if(queryParam == null || queryParam.isEmpty()) {
			return datasetJSON;
		}
		
		// 添加分页参数
		addPageParameter(queryParam);
		
		// 排序参数
		addColumnSortParameter(queryParam);
		
		if("true".equalsIgnoreCase(queryParam.get(DataGrid.LOAD_DATA_FLAG) + "")){// 加载
 			IDataSetProvider provider = grid.getDataSetProvider();
			if(provider == null){
				log.error("未设置IDataGridDataSetProvider");
				return datasetJSON;
			}else{
				grid.setDataset(provider.getDataSet(queryParam));
				if(grid.isPagination()){
					grid.setTotalSize(provider.getDataSetTotalSize(queryParam));
				}
			}
		}else{
			datasetJSON.put("width", grid.getWidth());
			datasetJSON.put("height", grid.getHeight());
			return datasetJSON;
		}
		
		List<? extends JSONSerializable> dataset = grid.getDataset();
		if(dataset == null || dataset.size() == 0){
			return datasetJSON;
		}
		
		// 构建数据
		rows = buildData(dataset);
		datasetJSON.put("rows", rows);
		
		if(grid.isPagination() && grid.getTotalSize() < 0){
			datasetJSON.put("total", rows.size());
		}else{
			datasetJSON.put("total", grid.getTotalSize());
		}
		
		// 合并单元格
		JSONArray mergeCellInfo = buildMergeCellInfo(dataset);
		datasetJSON.put("mergeCellInfo", mergeCellInfo);
		
		return datasetJSON;
	}
	
	/**
	 * 构建数据行数据，不包括total size 
	 */
	public JSONArray buildData(List<? extends JSONSerializable> dataset) {
		JSONArray rows = new JSONArray();
		if(dataset == null || dataset.size() == 0){
			return rows;
		}
		
		DataGridColumn[] columns = grid.getColumns();
		Map<String, DataGridColumn> colMap = new HashMap<String, DataGridColumn>();
		for(DataGridColumn dgc : columns){
			colMap.put(dgc.getField(), dgc);
		}
		
		// 数据
		DataGridDataSetRow dsRow = new DataGridDataSetRow(grid, null);
		for(JSONSerializable row : dataset){
			rows.add(dsRow.format(colMap, row));
		}
		return rows;
	}
	
	/**
	 * 构建数据集行数据,带格式化的数据行
	 * @return
	 */
	public List<DataGridDataSetRow> buildDataSetRow(List<? extends JSONSerializable> dataset){
		return buildDataSetRow(dataset, false);
	}
	
	/**
	 * 构建数据集行数据,带格式化的数据行
	 * @return
	 */
	public List<DataGridDataSetRow> buildDataSetRow(List<? extends JSONSerializable> dataset, boolean isExport){
		List<DataGridDataSetRow> rows = new ArrayList<DataGridDataSetRow>();
		if(dataset == null || dataset.size() == 0){
			return rows;
		}
		for(JSONSerializable data : dataset){
			DataGridDataSetRow row = new  DataGridDataSetRow(grid, data);
			row.setExport(isExport);
			rows.add(row);
		}
		return rows;
	}

	public String buildData() {
		JSONObject dataset = buildDataSet();
		return dataset.toString();
	}
	
	@Override
	public String designerBeginHtml() {
		return super.designerBeginHtml();
	}
	
	@Override
	public String designerEndHtml() {
		return super.designerEndHtml();
	}
}
