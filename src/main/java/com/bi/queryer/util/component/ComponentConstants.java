package com.bi.queryer.util.component;

/**
 * 组件相关常量类
 * @author contributor
 *
 */
public abstract class ComponentConstants {
	
	/**
	 * datagrid 排序参数
	 */
	public static final String DataGrid_Parameter_Sort = "gridSortInfo";
	
	/**
	 * 分页下限
	 */
	public static final String DataGrid_Parameter_PageRowLower = "pageRowLower";
	
	/**
	 * MySQL分页下限
	 */
	public static final String DataGrid_Parameter_PageRowLower2 = "pageRowLower2";
	
	/**
	 * 分页上限
	 */
	public static final String DataGrid_Parameter_PageRowUpper = "pageRowUpper";
	
	/**
	 * 分页每页条数
	 */
	public static final String DataGrid_Parameter_PageSize = "pageSize";
	
	/**
	 * H5报表根模块id
	 */
//	public static final String H5_Report_RootId = "h5_report_root";
	public static final String H5_Report_RootId = "BI_App_Ref";
	
	/**
	 * session回退信息
	 */
	public static final String SESSION_BACK_INFO = "__back_info";
	
	/**
	 * 组件code
	 */
	public static final String Parameter_Component_Code = "__code";
	
	/**
	 * 组件ID
	 */
	public static final String Parameter_Component_ID = "__cid";
	
	/**
	 * 上级组件ID
	 */
	public static final String Parameter_Component_PID = "__cpid";
	
	/**
	 * 组件namespace
	 */
	public static final String Parameter_Component_Namespace = "__ns";
	
	/**
	 * 组件类型
	 */
	public static final String Parameter_Component_Type = "__ct";
	
	/**
	 * 系统上下文
	 */
	public static final String Parameter_Context = "__cxt";
	
	/**
	 * 报表页面
	 */
	public static final String Parameter_Page = "rptPage";
	
	/**
	 * 标示是否是iframe，用于native拦截iframe请求时不处理
	 */
	public static final String Parameter_Iframe = "__if";
	
	/**
	 * 组件索引
	 */
	public static final String Parameter_Component_Index = "__idx";
	
	/**
	 * 模块ID
	 */
	public static final String Parameter_Module_Id = "_m_id";
	
	/**
	 * 模块编码
	 */
	public static final String Parameter_Module_Code = "_m_code";
	
	/**
	 * 模块父ID
	 */
	public static final String Parameter_Module_Pid = "_m_pid";
	
	/**
	 * 模块根ID
	 */
	public static final String Parameter_Module_Root = "_m_r";
	
	/**
	 * kpiboard编码
	 */
	public static final String Parameter_KPIBoard_Code = "_kb_c";
	
	/**
	 * kpiboard名称
	 */
	public static final String Parameter_KPIBoard_Name = "_kb_n";
	
	/**
	 * 消息key
	 */
	public static final String Parameter_Msg_Key = "_msg_k";
	
	/**
	 * 消息scope
	 */
	public static final String Parameter_Msg_Scope = "_scope";
	
	/**
	 * 消息接受者
	 */
	public static final String Parameter_Msg_Receiver = "_receiver";
	
	/**
	 * 钻取层级
	 */
	public static final String Parameter_Drill_Level = "drillLevel";
	
	/**
	 * 钻取维度名称
	 */
	public static final String Parameter_Drill_DimName = "drillDimName";
	
	/**
	 * 平台
	 */
	public static final String Parameter_Platform = "_plfm";
	
	/**
	 * 数据组件css类
	 */
	public static final String CSS_Data_Component = "h5-data-component";
	
	/**
	 * 容器组件css类
	 */
	public static final String CSS_Container_Component = "h5-container-component";
	
	public static final String CSS_Component = "h5-component";
	
	/**
	 * 查询sql语句的key值
	 */
	public static final String Component_SQL = "query-sql";
	/**
	 * 查询sql语句的数据源key值
	 */
	public static final String Component_SQLSource = "query-sqlSource";

	/**
	 * 查询sql语句count的key值
	 */
	public static final String Component_SQL_Count = "query-sql-count";
	/**
	 * uniqueId分隔符
	 */
	public static final String Split_UniqueId = "____";
	
	/**
	 * 移动首页bean
	 */
	public static final String Mobile_Main_Page = "mobileMainPage";
	
	/**
	 * session portal key
	 */
	public static final String SESSION_PORTAL = "_ptl_instnt";
	
	/**
	 * 根portlet的id
	 */
	public static final String PORTLET_ROOT_ID = "_p_rt_id";
	
	/**
	 * 根portlet的name
	 */
	public static final String PORTLET_ROOT_NAME = "root";
	
	/**
	 * portal宽度
	 */
	public static final String PORTAL_WIDTH = "_ptl_w";
	
	/**
	 * portal高度
	 */
	public static final String PORTAL_HEIGHT = "_ptl_h";
	
	public static final String PORTLET_RENDER_MODE = "_rder_mode";
	
	/**
	 * html标签中设计器相关选项
	 */
	public static final String PORTLET_DESIGNER_OPTION = "data-designer";
}
