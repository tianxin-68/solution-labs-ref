package com.bi.queryer.util.component.datagrid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.alibaba.fastjson.JSONObject;

import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.component.ComponentFactory;
import com.bi.queryer.util.component.ComponentRender;
import com.bi.queryer.util.component.ComponentType;
import com.bi.queryer.util.component.datagrid.export.IExportAppender;
import com.bi.queryer.util.component.datagrid.export.IExportStyler;
import com.bi.queryer.util.component.exception.PortletException;
import com.bi.queryer.util.component.portlet.Portlet;
import com.bi.queryer.util.component.text.Text;
import com.bi.queryer.util.download.FileDownloadMode;
import com.bi.queryer.util.download.FileDownloadType;

/**
 * 此类是针对easyui的DataGrid进行封装
 * @author contributor
 *
 */
public class DataGrid extends Portlet{
	private static final long serialVersionUID = 1L;

	private Object append_info;//附加属性，用于其他调用datagrid的地方，追加一些额外的信息，chenglj/20141209
	
	/**
	 * 异步加载datagrid数据key
	 */
	public static final String LOAD_DATA_FLAG = "_load_data";// 此参数由前台隐式传递
	
	protected boolean autoSize = true; 
	
	//默认选中行
	protected int selectedRowDefault = -1;
	/**
	 * 列宽自适应
	 */
	protected boolean isFitColumns = false;
	
	protected boolean autoRowHeight = true;
	
	protected boolean isNowrap = false;
	
	protected String idField = "";
	
	protected boolean isPagination = false;
	
	/**
	 * 是否是限制性分页：用于Presto数据源分页标识
	 */
	protected boolean isRestrictedPagination = false;
	
	protected boolean showRowNo = false; // 显示行号
	
	protected boolean singleSelect = true;
	
	protected int pageNumber = 1;
	
	protected int pageSize = 30;
	
	protected List<Integer> pageList = new ArrayList<Integer>();
	
	protected boolean showHeader = true;
	
	protected boolean showFooter = false;
	
	protected boolean showExport = false;
	
	protected FileDownloadType exportType = FileDownloadType.Excel07;
	
	/**
	 * 导出方式，可支持大数据量快速导出csv
	 */
	protected FileDownloadMode exportMode = FileDownloadMode.DEFAULT;
	
	protected DataGridForamtter rowFormatter = null;
	
	
	protected List<DataGridColumn> columns = new ArrayList<DataGridColumn>();
	protected Map<String, DataGridColumn> columnsMap = new LinkedHashMap<String, DataGridColumn>();
	
	protected List<DataGridColumnGroup> groups = new ArrayList<DataGridColumnGroup>();
	
	/**
	 * 显示工具栏
	 */
	protected boolean showToolbar = false;
	
	/**
	 * 工具栏
	 */
	protected List<DataGridToolButton> toolBtns = new ArrayList<DataGridToolButton>();
	
	/**
	 * 标题
	 */
	protected Text title = null;
	
	protected boolean showTitle = false;
	
	/**
	 * 前端展现toolbar的div的id
	 */
	protected String toolbarId = "";
	
	/**
	 * 总记录数，-1：动态计算
	 */
	protected int totalSize = -1;
	
	protected String rowStyler = "";
	
	protected boolean showBorder = true;
	
	/**
	 * 显示交替色
	 */
	protected boolean striped = true;
	
	protected List<? extends JSONSerializable> dataset = null;
	
	/**
	 * 服务上下文路径
	 */
	protected String contextPath = "";
	
	/**
	 * 冻结行数
	 */
	private int frozenRowNum = 0; 
	
	/**
	 * 导出记录条数限制 单位万，默认上限从option配置表里读取
	 */
	private int exportLimit = Integer.parseInt(SC.v("bi.export.limit","10"));
	/**
	 * 钻取模式
	 */
	private DataGridDrillMode drillMode = DataGridDrillMode.SingleColumn;
	
	/**
	 * min分页栏
	 */
	private boolean minPageBar = false;
	
	/**
	 * check和select事件级联
	 */
	private boolean checkSelectCascade = true;
	
	/**
	 * 远程服务端排序
	 */
	private boolean remoteSort = false;
	
	/**
	 * 默认最新列宽:不设置宽度时，默认最新宽度
	 */
	protected Integer defaultMinColumnWidth = -1;
	
	private DataSourceType dsType = null;
	
	/**
	 * 列字段名大小写敏感
	 */
	private boolean columnFieldCaseSensitive = false;
	
	/**
	 * 导出样式器
	 */
	private IExportStyler exportStyler = null;
	
	/**
	 * 导出附加器
	 */
	private IExportAppender exportAppender = null; 
	
	/**
	 * 是否显示汇总行
	 */
	private Boolean showSummaryRow = false;
	
	private Boolean showHideColumnButton = false;
	
	private Boolean formatLink = true; // 格式化超链接
	
	public DataGrid(){
		super();
		this.isDataComponent = true;
		setPageList(pageSize);
		toolbarId = "dgToolbar";
		title = new Text();
		title.setFontSize(14);
		title.setFontStyle("bold");
	}
	
	public DataGrid(String code){
		this();
		this.code = code;
	}
	
	public DataGrid(int width, int height, String code){
		this(code);
		this.width = width;
		this.height = height;
	}
	
	public DataGrid(int width, int height){
		this(width, height, "");
	}
	
	protected void setPageList(int pageSize){
		pageList.clear();
		for(int i = 0; i < 4; i++) {
			pageList.add((i + 1) * pageSize);
		}
	}
	
	public String save(){
		 Document document = DocumentHelper.createDocument();
		 Element e = document.addElement("Config");
		 save(e);
		 String xml = document.asXML();
		 return xml;
	}
	
	@Override
	public void save(Element root) {
		super.save(root);
		// 数据源
		Element dsEle = root.addElement("Datasource");
		this.datasource.save(dsEle);
		
		// 属性
		Element dgEle = root.addElement("DataGrid");
		this.saveSettings(dgEle);
	}
	
	public void load(Element root){
		super.load(root);
		// 数据源
		Element dsEle = root.element("Datasource");
		this.datasource.load(dsEle);
		
		// 属性
		Element dgEle = root.element("DataGrid");
		loadSettings(dgEle);
	}
	
	/**
	 * 保存熟悉设置
	 * @param dgEle
	 */
	public void saveSettings(Element dgEle){
		if(dgEle == null) return;
		super.save(dgEle);
		
		dgEle.addAttribute("guid", guid);
		dgEle.addAttribute("width", width+"");
		dgEle.addAttribute("height", height+"");
		dgEle.addAttribute("pageSize", pageSize + "");
		dgEle.addAttribute("showTitle", showTitle + "");
		dgEle.addAttribute("isFitColumns", isFitColumns + "");
		dgEle.addAttribute("autoRowHeight", autoRowHeight + "");
		dgEle.addAttribute("isNowrap", isNowrap + "");
		dgEle.addAttribute("isPagination", isPagination + "");
		dgEle.addAttribute("showRowNo", showRowNo + "");
		dgEle.addAttribute("rownumbers", showRowNo + "");
		dgEle.addAttribute("singleSelect", singleSelect + "");
		dgEle.addAttribute("showHeader", showHeader + "");
		dgEle.addAttribute("showFooter", showFooter + "");
		dgEle.addAttribute("showExport", showExport + "");
		dgEle.addAttribute("showBorder", showBorder + "");
		dgEle.addAttribute("autoSize", autoSize + "");
		dgEle.addAttribute("showToolbar", showToolbar + "");
		dgEle.addAttribute("striped", striped + "");
		dgEle.addAttribute("exportType", exportType.getExtName() + "");
		dgEle.addAttribute("exportMode", exportMode.getModeName() + "");
		dgEle.addAttribute("exportLimit", exportLimit+"");
		
		// 标题
		if(title != null){
			Element titleEle = dgEle.addElement("Title");
			title.save(titleEle);
		}
		
		// 分组
		Element grpsEle = dgEle.addElement("ColumnGroups");
		if(groups != null) {
			for(DataGridColumnGroup grp : groups){
				Element grpEle = grpsEle.addElement("Group");
				grp.save(grpEle);
			}
		}
		
		// 列
		Element columnsEle = dgEle.addElement("Columns");
		if(columns != null) {
			for(DataGridColumn col : columns){
				Element colEle = columnsEle.addElement("Column");
				col.save(colEle);
			}
		}
		
	}
	
	
	/**
	 * 加载属性设置
	 * @param dgEle
	 */
	public void loadSettings(Element dgEle){
		if(dgEle == null) return;
		String str = dgEle.attributeValue("id");
		if(str != null && !"".equals(str)) {
			id = str;
		}
		str = dgEle.attributeValue("width");
		if(str != null && !"".equals(str)) {
			this.width = Integer.valueOf(str);
		}
		str = dgEle.attributeValue("height");
		if(str != null && !"".equals(str)) {
			this.height = Integer.valueOf(str);
		}
		str = dgEle.attributeValue("guid");
		if(str != null && !"".equals(str)) {
			guid = str;
		}
		str = dgEle.attributeValue("pageSize");
		if(str != null && !"".equals(str)) {
			pageSize = Integer.valueOf(str);
			setPageList(pageSize);
		}
		str = dgEle.attributeValue("exportType");
		if(str != null && !"".equals(str)) {
			exportType = FileDownloadType.getType(dgEle.attributeValue("exportType"));
		}
		str = dgEle.attributeValue("exportMode");
		if(str != null && !"".equals(str)) {
			exportMode = FileDownloadMode.getType(dgEle.attributeValue("exportMode"));
		}
		str = dgEle.attributeValue("exportLimit");
		if(str != null && !"".equals(str)) {
			this.exportLimit = Integer.valueOf(str);
		}
		
		showTitle = "true".equalsIgnoreCase(dgEle.attributeValue("showTitle"));
		isFitColumns = "true".equalsIgnoreCase(dgEle.attributeValue("isFitColumns"));
		autoRowHeight = "true".equalsIgnoreCase(dgEle.attributeValue("autoRowHeight"));
		isNowrap = !"false".equalsIgnoreCase(dgEle.attributeValue("isNowrap"));
		isPagination = "true".equalsIgnoreCase(dgEle.attributeValue("isPagination"));
		showRowNo = "true".equalsIgnoreCase(dgEle.attributeValue("rownumbers"));
		singleSelect = !"false".equalsIgnoreCase(dgEle.attributeValue("singleSelect"));
		showHeader = !"false".equalsIgnoreCase(dgEle.attributeValue("showHeader"));
		showFooter = "true".equalsIgnoreCase(dgEle.attributeValue("showFooter"));
		showExport = "true".equalsIgnoreCase(dgEle.attributeValue("showExport"));
		showBorder = !"false".equalsIgnoreCase(dgEle.attributeValue("showBorder"));
		autoSize = !"false".equalsIgnoreCase(dgEle.attributeValue("autoSize"));
		showToolbar = !"false".equalsIgnoreCase(dgEle.attributeValue("showToolbar"));
		striped = !"false".equalsIgnoreCase(dgEle.attributeValue("striped"));
		
		
		
		// 行样式
		Element rowStylerEle = dgEle.element("RowStyler");
		if(rowStylerEle != null &&
				!"false".equalsIgnoreCase(rowStylerEle.attributeValue("enable"))){
			rowStyler = rowStylerEle.getTextTrim();
		}
		
		// 标题
		Element titleEle = dgEle.element("Title");
		if(titleEle != null){
			title.load(titleEle);
		}
		
		// 列
		Element colsEle = dgEle.element("Columns");
		if(colsEle != null) {
			List colEleList = colsEle.elements("Column");
			for(int i = 0; i < colEleList.size(); i++) {
				Element colEle = (Element) colEleList.get(i);
				DataGridColumn column = new DataGridColumn();
				column.load(colEle);
				columns.add(column);
				columnsMap.put(column.getField(), column);
			}
		}
		
		// 类分组
		Element colGrpsEle = dgEle.element("ColumnGroups");
		if(colGrpsEle != null) {
			List grpEleList = colGrpsEle.elements("Group");
			for(int i = 0; i < grpEleList.size(); i++){
				Element grpEle = (Element) grpEleList.get(i);
				DataGridColumnGroup grp = new DataGridColumnGroup();
				grp.load(grpEle);
				
				// 添加列
				for(DataGridColumn col : columns){
					if(grp.getId().equalsIgnoreCase(col.getGroup())){
						grp.addColumn(col);
					}
				}
				groups.add(grp);
			}
		}
		
	}
	
	/**
	 * 添加类分组
	 * @param grp
	 * @return
	 */
	public DataGrid addColumnGroup(DataGridColumnGroup grp){
		if(!this.groups.contains(grp)){
			this.groups.add(grp);
		}
		// 添加分组里的列
		for(DataGridColumn col : grp.getColumns()){
			addColumn(col);
		}
		return this;
		//调用递归添加列内容的方法
		//return addColumnGroup(grp,true);
	}
	
	/**
	 * 可递归多层添加列信息
	 * @param grp
	 * @return
	 */
	public DataGrid addColumnGroup(DataGridColumnGroup grp,boolean flag){
		if(flag&&!this.groups.contains(grp)){
			this.groups.add(grp);
		}
		// 添加分组里的列
		for(DataGridColumn col : grp.getColumns()){
			addColumn(col);
		}
		if(grp.getChildren() != null){
		    for(DataGridColumnGroup group :grp.getChildren()){
			    addColumnGroup(group,false);
		    }
		}
		return this;
	}
	/**
	 * 添加列分组
	 * @param grpId
	 * @param title
	 * @return
	 */
	public DataGrid addColumnGroup(String grpId, String title){
		DataGridColumnGroup grp = new DataGridColumnGroup(grpId, title);
		return addColumnGroup(grp);
	}
	
	/**
	 * 添加列
	 * @param column
	 * @return
	 */
	public DataGrid addColumn(DataGridColumn column){
		if(column == null) return this;
		if(columnsMap.containsKey(column.getField())){
			return this;
		}
		columns.add(column);
		columnsMap.put(column.getField(), column);
		
		/*
		// 避免重复添加
		if(column.isDisplayColumn()) {
			return this;
		}
		
		// 若有显示规则，则以重新创建规则字段，原始字段隐藏
		Map<String, Object> displayRules = column.getValueDisplayRules();
		if(displayRules != null && !displayRules.isEmpty()) {
			column.setHidden(true);
			
			DataGridColumn displayColumn =  new DataGridColumn(column.getFieldByCode() + DataGridColumn.Display_Field_Suffix,
					column.getTitle(), column.getWidth(), column.getAlign(), column.getValueFormat(), column.getValueSuffix());
			displayColumn.setHidden(false);
			displayColumn.getValueDisplayRules().putAll(displayRules);
			// 添加显示字段后，需清理原始字段显示规则
			displayRules.clear();
			this.addColumn(displayColumn);
		}
		*/
		return this;
	}
	
	/**
	 * 添加列
	 * @param field
	 * @param title
	 * @param width
	 * @return
	 */
	public DataGrid addColumn(String field, String title, int width){
		DataGridColumn column = new DataGridColumn(field, title, width);
		return addColumn(column);
	}
	
	/**
	 * 添加列
	 * @param field
	 * @param title
	 * @return
	 */
	public DataGrid addColumn(String field, String title){
		DataGridColumn column = new DataGridColumn(field, title, -1);
		return addColumn(column);
	}
	
	/**
	 * 删除列
	 * @param column
	 * @return
	 */
	public List<DataGridColumn> removeColumn(DataGridColumn column){
		if(column == null) return columns;
		columns.remove(column);
		columnsMap.remove(column.getField());
		
		// 同时删除display字段
		DataGridColumn displayColumn = columnsMap.get(column.getField() + DataGridColumn.Display_Field_Suffix);
		if(displayColumn != null) {
			columns.remove(displayColumn);
			columnsMap.remove(displayColumn.getField());
		}
		
		// 同时删除分组中的列
		for(DataGridColumnGroup grp : groups){
			grp.getColumns().remove(column);
		}
		
		return columns;
	}
	
	/**
	 * json格式：用于组件单独使用
	 * <br/>
	 * {<br/>
	 * 	  option:{width,height,,,columns:[[]],,,,},<br/>
	 *    data:{rows:[],total:100}<br/>
	 *    error:"错误信息"<br/>
	 * }
	 */
	public JSONObject toJSON(){
		JSONObject gridJSON = new JSONObject();
		String error = "";
		if(columns.isEmpty()){
			error = "未配置列信息";
		}
		JSONObject option = new JSONObject();
		JSONObject dataset = new JSONObject();
		long t1 = System.currentTimeMillis();
		try{
			ComponentRender<?> render = ComponentFactory.getRender(this);
			option = render.buildOptions();
			dataset = render.buildDataSet();
		}catch(Exception e){
			e.printStackTrace();
			error = e.getMessage();
		}
		long t2 = System.currentTimeMillis();
		option.put("consumeTime", (t2-t1));
		gridJSON.put("option", option);
		gridJSON.put("data", dataset);
		gridJSON.put("error", error);
		return gridJSON;
	}
	
	public DataGridColumn getColumn(String colField){
		return columnsMap.get(colField);
	}

	public boolean isFitColumns() {
		return isFitColumns;
	}

	public void setFitColumns(boolean isFitColumns) {
		this.isFitColumns = isFitColumns;
	}

	public boolean isNowrap() {
		return isNowrap;
	}

	public void setNowrap(boolean isNowrap) {
		this.isNowrap = isNowrap;
	}

	public String getIdField() {
		return idField;
	}

	public void setIdField(String idField) {
		this.idField = idField;
	}

	public boolean isPagination() {
		return isPagination;
	}

	public void setPagination(boolean isPagination) {
		this.isPagination = isPagination;
	}

	public boolean isShowRowNo() {
		return showRowNo;
	}

	public void setShowRowNo(boolean showRowNo) {
		this.showRowNo = showRowNo;
	}

	public boolean isSingleSelect() {
		return singleSelect;
	}

	public void setSingleSelect(boolean singleSelect) {
		this.singleSelect = singleSelect;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public List<Integer> getPageList() {
		return pageList;
	}

	public void setPageList(List<Integer> pageList) {
		this.pageList = pageList;
	}

	public boolean isShowHeader() {
		return showHeader;
	}

	public void setShowHeader(boolean showHeader) {
		this.showHeader = showHeader;
	}

	public boolean isShowFooter() {
		return showFooter;
	}

	public void setShowFooter(boolean showFooter) {
		this.showFooter = showFooter;
	}

	public DataGridForamtter getRowFormatter() {
		return rowFormatter;
	}

	public void setRowFormatter(DataGridForamtter rowFormatter) {
		this.rowFormatter = rowFormatter;
	}

	public DataGridColumn[] getColumns() {
		return columns.toArray(new DataGridColumn[columns.size()]);
	}

	public boolean isAutoSize() {
		return autoSize;
	}

	/**
	 * 
	 * @param autoSize
	 */
	public void setAutoSize(boolean autoSize) {
		this.autoSize = autoSize;
	}

	public boolean isAutoRowHeight() {
		return autoRowHeight;
	}

	public void setAutoRowHeight(boolean autoRowHeight) {
		this.autoRowHeight = autoRowHeight;
	}

	public List<DataGridColumnGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<DataGridColumnGroup> groups) {
		this.groups = groups;
	}
	
	/**
	 * 获取根分组
	 * @param grpId
	 * @return
	 */
	public DataGridColumnGroup getRootGroup(String grpId) {
		DataGridColumnGroup grp = null;
		if(grpId == null || "".equals(grp)) return grp;
		for(DataGridColumnGroup group : groups) {
			if(grpId.equalsIgnoreCase(group.getId())) {
				grp = group;
				break;
			}
		}
		return grp;
	}
	
	/**
	 * 通过组id获取列分组
	 * @param grpId
	 * @return 列分组
	 */
	public DataGridColumnGroup getGroup(String grpId) {
		return getGroup(groups, grpId);
	}
	
	protected DataGridColumnGroup getGroup(List<DataGridColumnGroup> groups, String grpId) {
		DataGridColumnGroup group = null;
		if(groups != null) {
			for(DataGridColumnGroup grp : groups){
				if(grpId.equalsIgnoreCase(grp.getId())) {
					group = grp;
					break;
				}else {
					group = getGroup(grp.getChildren(), grpId);
					if(group != null) {
						break;
					}
				}
			}
		}
		return group;
	}
	
	public boolean contains(DataGridColumn col){
		return this.columns.contains(col);
	}

	public boolean isShowToolbar() {
		return showToolbar;
	}

	public void setShowToolbar(boolean showToolbar) {
		this.showToolbar = showToolbar;
	}

	@Override
	public ComponentType getType() {
		return ComponentType.DataGrid;
	}

	public String getToolbarId() {
		return toolbarId;
	}

	public void setToolbarId(String toolbarId) {
		this.toolbarId = toolbarId;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}

	public Text getTitle() {
		return title;
	}

	public void setTitle(Text title) {
		this.title = title;
	}

	public boolean isShowTitle() {
		return showTitle;
	}

	public void setShowTitle(boolean showTitle) {
		this.showTitle = showTitle;
	}

	public String getRowStyler() {
		return rowStyler;
	}

	public void setRowStyler(String rowStyler) {
		this.rowStyler = rowStyler;
	}

	public boolean isStriped() {
		return striped;
	}

	public void setStriped(boolean striped) {
		this.striped = striped;
	}

	public boolean isShowBorder() {
		return showBorder;
	}

	public void setShowBorder(boolean showBorder) {
		this.showBorder = showBorder;
	}

	public List<? extends JSONSerializable> getDataset() {
		return dataset;
	}

	public void setDataset(List<? extends JSONSerializable> dataset) {
		this.dataset = dataset;
	}

	/*public IDataGridDataSetProvider getDataSetProvider() {
		return (IDataGridDataSetProvider) dataSetProvider;
	}*/

	public void setDataSetProvider(IDataGridDataSetProvider dataSetProvider, Map dataSetQueryParam) {
		this.dataSetProvider = dataSetProvider;
		this.dataSetQueryParam = dataSetQueryParam;
	}

	public Map getDataSetQueryParam() {
		return dataSetQueryParam;
	}

	public void setDataSetQueryParam(Map dataSetQueryParam) {
		this.dataSetQueryParam = dataSetQueryParam;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Object getAppend_info() {
		return append_info;
	}

	public void setAppend_info(Object append_info) {
		this.append_info = append_info;
	}

	public int getFrozenRowNum() {
		return frozenRowNum;
	}

	public void setFrozenRowNum(int frozenRowNum) {
		this.frozenRowNum = frozenRowNum;
	}

	public DataGridDrillMode getDrillMode() {
		return drillMode;
	}

	public void setDrillMode(DataGridDrillMode drillMode) {
		this.drillMode = drillMode;
	}

	public boolean isMinPageBar() {
		return minPageBar;
	}

	public void setMinPageBar(boolean minPageBar) {
		this.minPageBar = minPageBar;
	}
	
	public boolean isShowExport() {
		return showExport;
	}

	public void setShowExport(boolean showExport) {
		this.showExport = showExport;
	}

	/**
	 * 添加工具按钮
	 * @param title
	 * @param code
	 * @param value
	 */
	public DataGrid addToolButton(String title, String code, String value){
		return addToolButton(title,code, value, "left");
	}
	/**
	 * 添加工具按钮
	 * @param title
	 * @param code
	 * @param value
	 */
	public DataGrid addToolButton(String title, String code, String value, String align){
		return addToolButton(title, code, value, align, true);
	}
	/**
	 * 添加工具按钮
	 * @param title
	 * @param code
	 * @param value
	 */
	public DataGrid addToolButton(String title, String code, String value, String align, boolean show){
		DataGridToolButton btn = new DataGridToolButton();
		btn.setTitle(title);
		btn.setCode(code);
		btn.setValue(value);
		btn.setAlign(align);
		btn.setShow(show);
		btn.setIndex(toolBtns.size());
		toolBtns.add(btn);
		return this;
	}
	
	public DataGridToolButton[] getToolButtons(){
		DataGridToolButton bts[] = new DataGridToolButton[toolBtns.size()];
		return toolBtns.toArray(bts);
	}

	@Override
	public DataGrid clone(boolean cascade) throws PortletException {
		DataGrid grid = new DataGrid();
		clone(grid, cascade);
		return grid;
	}

	@Override
	public int getDesignerBarHeight() {
		return 0;
	}

	public boolean isCheckSelectCascade() {
		return checkSelectCascade;
	}

	public void setCheckSelectCascade(boolean checkSelectCascade) {
		this.checkSelectCascade = checkSelectCascade;
	}

	public boolean isRemoteSort() {
		return remoteSort;
	}

	public void setRemoteSort(boolean remoteSort) {
		this.remoteSort = remoteSort;
	}

	public Integer getDefaultMinColumnWidth() {
		return defaultMinColumnWidth;
	}

	public void setDefaultMinColumnWidth(Integer defaultMinColumnWidth) {
		this.defaultMinColumnWidth = defaultMinColumnWidth;
	}

	public DataSourceType getDsType() {
		return dsType;
	}

	public void setDsType(DataSourceType dsType) {
		this.dsType = dsType;
	}

	public boolean isColumnFieldCaseSensitive() {
		return columnFieldCaseSensitive;
	}

	public void setColumnFieldCaseSensitive(boolean columnFieldCaseSensitive) {
		this.columnFieldCaseSensitive = columnFieldCaseSensitive;
	}

	public IExportStyler getExportStyler() {
		return exportStyler;
	}

	public void setExportStyler(IExportStyler exportStyler) {
		this.exportStyler = exportStyler;
	}

	public IExportAppender getExportAppender() {
		return exportAppender;
	}

	public void setExportAppender(IExportAppender exportAppender) {
		this.exportAppender = exportAppender;
	}

	public int getSelectedRowDefault() {
		return selectedRowDefault;
	}

	public void setSelectedRowDefault(int selectedRowDefault) {
		this.selectedRowDefault = selectedRowDefault;
	}

	public FileDownloadType getExportType() {
		return exportType;
	}

	public void setExportType(FileDownloadType exportType) {
		this.exportType = exportType;
	}
	
	public FileDownloadMode getExportMode() {
		return exportMode;
	}

	public void setExportMode(FileDownloadMode exportMode) {
		this.exportMode = exportMode;
	}

	public int getExportLimit() {
		return exportLimit;
	}

	public void setExportLimit(int exportLimit) {
		this.exportLimit = exportLimit;
	}

	public boolean isRestrictedPagination() {
		if(this.datasource != null) {
			DataSourceType datasourceType = DataSourceType.getType(datasource.getType());
			DBType dbType = DBType.getType(datasourceType.getDialect());
			if(dbType == DBType.Presto || dbType == DBType.Trino) {
				isRestrictedPagination = true;
			}else {
				isRestrictedPagination = false;
			}
		}
		return isRestrictedPagination;
	}

	public void setRestrictedPagination(boolean isRestrictedPagination) {
		this.isRestrictedPagination = isRestrictedPagination;
	}

	public Boolean getShowSummaryRow() {
		return showSummaryRow;
	}

	public void setShowSummaryRow(Boolean showSummaryRow) {
		this.showSummaryRow = showSummaryRow;
	}

	public Boolean getShowHideColumnButton() {
		return showHideColumnButton;
	}

	public void setShowHideColumnButton(Boolean showHideColumnButton) {
		this.showHideColumnButton = showHideColumnButton;
	}

	public Boolean getFormatLink() {
		return formatLink;
	}

	public void setFormatLink(Boolean formatLink) {
		this.formatLink = formatLink;
	}
	
}
