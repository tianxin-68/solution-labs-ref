package com.bi.queryer.util.component;

/**
 * 组件类型枚举
 * @author contributor
 *
 */
public enum ComponentType {
	DataGrid("com.bi.queryer.util.component.datagrid.DataGrid", "com.bi.queryer.util.component.datagrid.render.DataGridRender", "DataGrid表格"),
	TreeGrid("com.bi.queryer.util.component.treegrid.TreeGrid", "com.bi.queryer.util.component.treegrid.render.TreeGridRender", "TreeGrid表格"),
	ComboDataGrid("com.bi.queryer.util.component.combogrid.ComboDataGrid", "com.bi.queryer.util.component.combogrid.render.ComboDataGridRender", "下拉表格"),
	Text("com.bi.queryer.util.component.text.Text", "com.bi.queryer.util.component.text.render.TextRender", "普通文本"),
	ReportPage("com.bi.queryer.util.component.report.ReportPage", "com.bi.queryer.util.component.report.render.ReportPageRender", "报表页"),
	NavigationBar("com.bi.queryer.util.component.navbar.NavigationBar", "com.bi.queryer.util.component.navbar.render.NavigationBarRender", "导航栏"),
	MenuItem("com.bi.queryer.util.component.menu.MenuItemComponent", "com.bi.queryer.util.component.menu.render.MenuItemRender", "默认菜单项"),
	EntryPoint("com.bi.queryer.util.component.menu.EntryPoint", "com.bi.queryer.util.component.menu.render.EntryPointRender", "入口"),
	EntryPointContainer("com.bi.queryer.util.component.menu.EntryPointContainer", "com.bi.queryer.util.component.menu.render.EntryPointContainerRender", "入口容器"),
	Swiper("com.bi.queryer.util.component.swiper.Swiper", "com.bi.queryer.util.component.swiper.render.SwiperRender", "左右切换组件容器"),
	SwipeSlider("com.bi.queryer.util.component.swiper.SwipeSlider", "com.bi.queryer.util.component.swiper.render.SwipeSliderRender", "左右切换项"),
	KPINavBoard("com.bi.queryer.util.component.board.KPINavBoard", "com.bi.queryer.util.component.board.render.KPINavBoardRender", "KPI-Nav看板"),
	KPINavGrid("com.bi.queryer.util.component.board.KPINavGrid", "com.bi.queryer.util.component.board.render.KPINavGridRender", "指标Nav-Grid"),
	KPINavListGrid("com.bi.queryer.util.component.board.KPINavListGrid", "com.bi.queryer.util.component.board.render.KPINavListGridRender", "指标Nav-List-Grid"),
	KPIFitGrid("com.bi.queryer.util.component.board.KPIFitGrid", "com.bi.queryer.util.component.board.render.KPIFitGridRenderTwo", "指标Fit-Grid"),
	KPIFitBoard("", "", "指标Fit-Grid容器(仅作为类型标识)"),
	ProgressBar("com.bi.queryer.util.component.progress.ProgressBar", "com.bi.queryer.util.component.progress.render.ProgressBarRender", "进度条"),
	ChartContainer("", "", "图表容器(仅作为类型标识)"),
	DataGridContainer("", "", "数据表格容器(仅作为类型标识)"),
	Chart("com.bi.queryer.util.component.chart.Chart", "com.bi.queryer.util.component.chart.render.ChartRender", "图表"),
	OlapChart("com.bi.queryer.util.component.chart.OlapChart", "com.bi.queryer.util.component.chart.render.OlapChartRender", "图表"),
	TabContainer("com.bi.queryer.util.component.tab.TabContainer", "com.bi.queryer.util.component.tab.render.TabContainerRender", "页签容器"),
	TabPanel("com.bi.queryer.util.component.tab.TabPanel", "com.bi.queryer.util.component.tab.render.TabPanelRender", "页签panel"),
	IFrame("com.bi.queryer.util.component.iframe.IFrame", "com.bi.queryer.util.component.iframe.render.IFrameRender", "iframe"),
	InputContainer("com.bi.queryer.util.component.input.InputContainer", "com.bi.queryer.util.component.input.render.InputContainerRender", "查询条件"),
	Input("com.bi.queryer.util.component.input.InputBase", "com.bi.queryer.util.component.input.render.InputBaseRender", "输入框明细内容"),
	Portal("com.bi.queryer.util.rpt.designer.model.Portal", "com.bi.queryer.util.rpt.designer.render.PortalRender", "报表"),
	VLayout("com.bi.queryer.util.component.layout.VLayout", "com.bi.queryer.util.component.layout.render.VLayoutRender", "垂直布局"),
	HLayout("com.bi.queryer.util.component.layout.HLayout", "com.bi.queryer.util.component.layout.render.HLayoutRender", "水平布局"),
	ResponseLayout("com.bi.queryer.util.component.layout.ResponseLayout", "com.bi.queryer.util.component.layout.render.ResponseLayoutRender", "响应式布局"),
	RichText("com.bi.queryer.util.component.richtext.RichText","com.bi.queryer.util.component.richtext.render.RichTextRender", "文本"),
	Filter("com.bi.queryer.util.component.filter.Filter","com.bi.queryer.util.component.filter.render.FilterRender", "过滤器"),
	DateFilter("com.bi.queryer.util.component.filter.DateFilter","com.bi.queryer.util.component.filter.render.DateFilterRender", "日期过滤器"),
	DateFilterContainer("", "", "过滤器容器(仅作为类型标识)"),
	ComboboxFilter("com.bi.queryer.util.component.filter.ComboboxFilter","com.bi.queryer.util.component.filter.render.ComboboxFilterRender", "下拉选择过滤器"),
	ComboboxFilterContainer("", "", "过滤器容器(仅作为类型标识)"),
	InputFilter("com.bi.queryer.util.component.filter.InputFilter","com.bi.queryer.util.component.filter.render.InputFilterRender", "文本输入过滤器"),
	InputFilterContainer("", "", "过滤器容器(仅作为类型标识)"),
	Button("com.bi.queryer.util.component.button.Button","com.bi.queryer.util.component.button.render.ButtonRender", "按钮(查询、导出等)"),
	None("NULL", "NULL", ""),
	Auto("", "", "自动");
	private String modelClass ;
	private String renderClass;
	private String desc;
	
	private ComponentType(String modelClass, String renderClass, String desc){
		this.modelClass = modelClass;
		this.renderClass = renderClass;
		this.desc = desc;
	}
	
	public static ComponentType getType(String typeStr){
		for(ComponentType type : values()){
			if(type.toString().equalsIgnoreCase(typeStr)) {
				return type;
			}
		}
		return Auto;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelClass(String modelClass) {
		this.modelClass = modelClass;
	}

	public String getRenderClass() {
		return renderClass;
	}

	public void setRenderClass(String renderClass) {
		this.renderClass = renderClass;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
