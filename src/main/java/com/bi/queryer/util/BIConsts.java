package com.bi.queryer.util;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public abstract class BIConsts {

    /**
     * 请求来源key
     */
    public static final String Request_Source_Key = "_req_src_";

    /**
     * 应用上下文
     */
    public static final String App_Cxt = "cxt";

    /**
     * web socket应用上下文
     */
    public static final String App_WS_Cxt = "wsCxt";

    /**
     * 应用环境
     */
    public static final String App_Page_Object = "BIPage";

    /**
     * 应用版本号
     */
    public static final String App_Version = "v";

    public static final String App_Common_JS = "commonJs";

    public static final String App_Common_Css = "commonCss";

    public static final String App_Name = "appName";

    /**
     * 模型视图的js变量前缀
     */
    public static final String Model_View_JS_Var_Prefix = "_bi_js_var_";

    /**
     * 用户
     */
    public static final String SESSION_KEY_USER = "_s_usr_";

    /**
     * 登录时session错误信息
     */
    public static final String SESSION_KEY_LOGIN_ERROR = "_s_login_err";

    /**
     * 登录错误次数
     */
    public static final String SESSION_KEY_LOGIN_ERROR_COUNT = "_s_login_err_cnt";

    /**
     * 验证码
     */
    public static final String SESSION_KEY_LOGIN_CODE = "_s_login_code";

    /**
     * 请求来源，用于记录日志来源
     */
    public static final String REQUEST_ORIGIN = "_req_origin";

    public static final String DEFAULT_USER = "Defaut-User";

    /**
     * 菜单根节点id
     */
    public static final String MENU_ROOT_ID = "-1";

    public static final String MENU_ROOT_NAME = "全部菜单";

    public static final String Menu_Home_Code = "home";

    public static final String Menu_Home_Name = "首页";

    //整型转小数精度
    public static final String INT_TO_DOUBLE_PRECISION = "1.000000";

    /**
     * 多维分析敏感字段权限
     */
    public static final String SSD_ROLE_SENSITIVE = "SSD_Sensitive";

    public static final String QUERY_ERROR_TIP = "查询出错了。已邮件通知管理员，请联系@数据产品技术支持，协助解决，不要重复点击哦~";

    @Deprecated
    public static final String QUERY_ERROR_TIMEOUT = "查询超时(超过%s分钟)，系统自动中断查询，请检查是否限制了查询时间范围，或将现有的时间范围适当缩短哦~ ";

    public static final String SSM_ERROR_TIMEOUT_Prefix = "查询超时";

    public static final String SSM_ERROR_TIMEOUT = SSM_ERROR_TIMEOUT_Prefix + "(超过%s分钟)，系统自动中断查询，请将缩短时间范围或减少同环比、占比/汇总后再试试~ ";

    public static final String SSM_ERROR_TIMEOUT_COLUMN_DIMENSION = SSM_ERROR_TIMEOUT_Prefix + "(超过%s分钟)。将【%s】放到行区域可提升查询性能。";

    public static final String SSM_ERROR_RATE_LIMIT = "当前系统负载过高";

    public static final String OLAP_API_ERROR_RATE_LIMIT_PREFIX  = "多维OLAP_API不支持高并发查询，查询被限流";

    // OLAP API 限流统一提示，数量超限与抢锁失败共用
    public static final String OLAP_API_ERROR_RATE_LIMIT = OLAP_API_ERROR_RATE_LIMIT_PREFIX + "，请稍后重试！";

    public static final String SSM_ERROR_TOO_MANY_PARTITION = "当前查询数据量过大(分区过多)，导致集群负载过高";

    public static final String SSM_ERROR_VIEW_LIMIT = "当前服务集群压力过大，只可访问特定视图，有疑问请联系【数据产品技术支持】";

    public static final Integer Export_Page_Size = 5000; // 每页最大导出记录数

    /**
     * 单点登录token
     */
    public static final String User_Token = "u_token";

    /**
     * 设备指纹的标识
     */
    public static final String USER_BLACK_BOX = "black_box";

    public static final String Cookies_Prefix = "_bi.portal_";

    public static final Long Token_Expire_Minute = 5L;

    public static final String Token_Error = "te";

    public static final String Login_Redirect = "redirect";

    public static final String Category_Root_Id = "-1";

    public static final String Category_Root_Name = "所有分类";

    /**
     * 目录内侧人员赋权生产的权限数据的id前缀
     */
    public static final String Category_Manual_Auth_Prefix = "ctg_manual_auth_";

    /**
     * 多维分析 目录 测试标识
     */
    public static final String Ssd_Category_Test_Identification = "开发测试中";

    /**
     * 多维分析维度的“所有”行级权限值
     */
    public static final String Ssm_Row_Acl_All_Dim_Value = "-9999";

    /**
     * 多维分析维度的“所有”行级权限值
     */
    public static final String Ssm_Row_Acl_All_Dim_Value_Title = "所有";

    /**
     * 所有
     */
    public static final String SSM_ALL = "-9999";


    /**
     * 多维看板目录节点
     */
    public static final String TEMPLATE_CTG_ROOT_ID = "-1";

    public static final String TEMPLATE_CTG_MY_ID = "my";

    public static final String TEMPLATE_CTG_SHARE_ID = "share";

    public static final String TEMPLATE_CTG_ROOT_NAME = "全部";

    public static final String TEMPLATE_CTG_MY_NAME = "我的模板";

    public static final String TEMPLATE_CTG_SHARE_NAME = "他人分享";

    public static final String TEMPLATE_CTG_PUBLIC_NAME = "公共模板";

    public static final String TEMPLATE_CTG_PUBLIC_ID = "public";

    /**
     * 共享空间
     */
    public static final String TEMPLATE_CTG_SPACE_ID = "space";

    /**
     * 模板节点的类型
     */
    public static final String TEMPLATE_NODE_TYPE = "template";

    public static final Integer COLUMN_DIM_ITEM_MAX_COUNT = 100;

    public static final String COLUMN_DIM_FIELD_SUFFIX = "_c__d_";

    /**
     * 查询行数限制
     */
    public static final String Query_Row_Limit = "queryRowLimit";


    public static final String SEPARATOR = "|!|";

    public static final String SEPARATOR_FOR_SPLIT = "\\|!\\|";

    public static final String Filter_Value_Is_Null = "IS_NULL";

    public static final String Filter_Value_Is_Not_Null = "IS_NOT_NULL";

    public final static String Custom_Field_Name_Suffix = "_ctm_";

    public final static String LOD_FIELD_CTM_SUFFIX = "_ctm_m_lod_";

    public final static String GROUPING_VALUE = "_grp_v";

    public final static String GROUPING_KEY = "_grp_k";

    public final static String DATE_RAW_KEY = "_dt_k";

    public final static String DATE_CODE = "dt";

    public final static String ROW_NUMBER_KEY = "_r_n";

    public final static String MAIN_TABLE_ALIAS = "main";

    public static final String ORDER_BY = "ORDER BY /*sort*/";

    public static final String ORDER_BY_ALIAS_SUFFIX = "__order_by";

    public static final String ROW_TOTAL_COLUMN_CODE = "_rt_total_";

    public static final String ROW_TOTAL_COLUMN_TITLE = "行总计";

    public static final String PIVOT_TABLE_ALIAS = "_pivot";

    public static final String PIVOT_TABLE_SHORT_ALIAS = "_pvt";

    public static final String NULL_VALUE = "(null)";

    /**
     * 全部指标标识，在同环比、占比小计等TAB页放置的"全部指标"的CODE
     * 注意和常量ALL_MEASURE_CODE的使用上的区别
     */
    public static final String ALL_MEASURE = "all";

    /**
     * 所有维度
     */
    public static final String ALL_DIM = "all";

    public static final String ANALYSIS_CURRENT_TITLE = "本期值";

    public static final String ANALYSIS_PATH_SEPARATOR = "/";

    /**
     * 最小日期粒度编码
     */
    public static final String MIN_DATE_GRANULARITY_CODE = "_com_d";

    public static final String COUNT_DISTINCT_FLAG = "count(distinct";

    /**
     * 当前分析数据集
     */
    public static final String ANALYSIS_CURRENT_DATASET = "ds_cur";

    /**
     * 查询超时时间（秒）
     */
    public static final Integer QUERY_TIME_OUT_SEC = 300;

    /**
     * 查询最大超时时间（秒）
     */
    public static final Integer QUERY_MAX_TIME_OUT_SEC = 1800;

    /**
     * 无权限时查询内容
     */
    public static final String NO_AUTH_CONTENT = "***";

    /**
     * 无权限表头提示
     */
    public static final String NO_AUTH_COLUMN_TIPS = "(无权限)";

    /**
     * 在多维行列维度上放置的"全部指标"标签的code
     * 注意和常量ALL_MEASURE的使用上的区别
     */
    public static final String ALL_MEASURE_CODE = "__ALL_MEASURE__";

    public static final String ALL_MEASURE_NAME = "全部指标";

    /**
     * 业务门户根节点
     */
    public static final String PORTAL_MENU_ROOT_ID = "-1";
    public static final String PORTAL_MENU_ROOT_NAME = "业务门户";

    //特殊维度过滤值映射
    public static final Map<String, String> SPECIAL_DIM_FILTER_VALUE_MAP =
            ImmutableMap.of("_empty_", "");

    //特殊维度值的排序，添加时请按顺序添加
    public static final Map<String, String> SPECIAL_DIM_VALUE_NUMBER_MAP =
            ImmutableMap.of("(other_concat_raw)", "90", "未知", "97", "", "98", "(null)", "99");

    /**
     * 对比日期后缀
     */
    public static final String COMPARE_DATE_SUFFIX = "_vs";

    /**
     * 日均值格式化后缀
     */
    public static final String AVG_FORMAT_SUFFIX = " ";

    //热表库名前缀
    public static final String HOT_TABLE_SCHEMA = "bi_hot";

    //最大结束日期
    public static final String MAX_END_DATE = "2999-12-31";

    public static final String CROSS_HEADER_KEY_SUFFIX = "_cross_header";

    /**
     * 获取日期进度函数标识
     */
    public static final String GET_DATE_PROGRESS_FLAG = "bi_get_date_progress(null,null)";

    /**
     * 获取剩余天数函数标识
     */
    public static final String GET_LEFT_DAYS_FLAG = "bi_get_left_days(null,null)";

    /**
     * 计算开店月份数标识
     */
    public static final String BI_SHOP_OPEN_MONTH_FLAG = "bi_shop_open_month(";

    /**
     * 计算开店月份范围标识
     */
    public static final String BI_SHOP_OPEN_MONTH_RANGE_FLAG = "bi_shop_open_month_range(";

    /**
     * 对比日期占位符
     */
    public static final String COMPARE_DATE_PLACEHOLDER = "compare_date_placeholder";

    /**
     * 排序项前缀
     */
    public static final String ORDER_BY_ITEM_PREFIX = "_sort_rn";

    /**
     * 函数标识后缀
     */
    public static final String KEYWORD_FUNC_FLAG = "@Func";

    /**
     * 查询原始值的列编码的后缀
     */
    public static final String RAW_VALUE_COLUMN_CODE_SUFFIX = "@Raw";

    /**
     * 列维度查询的sessionid前缀
     */
    public static final String HEADER_PREPARE_SESSIONID_PREFIX = "Header_Prepare_";


    /**
     * 模板资源更新时间Redis Key前缀
     */
    public static final String TEMPLATE_ASSET_UPDATETIME_REDIS_KEY = "templateAssetUpdateTime@";

    /**
     * 用户信息Redis Key前缀
     */
    public static final String USER_INFO_REDIS_KEY = "userInfo@";


    public static final String TARGET_VALUE_TABLE_ALIAS = "tv";

    /**
     * agent自定义时间段标识
     */
    public static final String DATE_RANGE_TYPE_IDENTIFIER = "date_custom_";

    /**
     * 外部调用多维的鉴权码OLAP_API_KEY
     */
    public static final String OLAP_API_KEY = "olap_api_key";

    /**
     * 外部调用多维的鉴权码OLAP_API_USER_NAME
     */
    public static final String OLAP_API_USER_NAME = "x-username";

    /**
     * olap-api查询的sessionid前缀
     */
    public static final String OLAP_API_SESSION_ID_PREFIX = "OLAP_";

    /**
     * 城市行权限码
     */
    public static final String CITY_ROW_AUTH_CODE = "AAA";

    /**
     * 虚拟过滤值：实际使用时过过滤掉
     */
    public static final String VIRTUAL_FILTER_VALUE = "v_f_v";

    // Redis 分布式锁命名空间
    public static class Locks {
        // OLAP API 按 api_key + 用户维度的并发限流锁
        public static final String OLAP_API_RATE_LIMIT_LOCK = "OLAP_API_RATE_LIMIT_LOCK";
    }
}
