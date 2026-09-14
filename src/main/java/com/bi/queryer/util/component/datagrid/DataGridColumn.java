package com.bi.queryer.util.component.datagrid;

import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.component.XMLSerializable;
import com.bi.queryer.util.component.common.FunctionItem;
import com.bi.queryer.util.component.datagrid.alarm.Alarm;
import com.bi.queryer.util.component.datagrid.alarm.AlarmType;
import org.dom4j.Element;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;

public class DataGridColumn extends XMLSerializable implements Serializable, Cloneable{
	private static final long serialVersionUID = 1L;

	public static final String STYLER_FUNCTION = "setColumnStyle";
	
	public static final String Display_Field_Suffix = "_display_";
	
	protected String id = "";
	
	protected String group = "";
	
	protected String title = "";// 标题
	
	protected String field = "";// 字段名称，唯一标识
	
	protected String originField = "";
	
	protected String keyField = "";// 该列的键字段
	
	protected int width = -1;// 列宽，默认自动计算
	
	protected int rowspan = 0;
	
	protected int colspan = 0;
	
	protected boolean isFrozen = false;
	
	/**
	 * 数据对齐方式
	 */
	private String align = "center";
	
	/**
	 * 表头对齐方式
	 */
	private String halign = "center";
	
	private boolean remoteSortable = false;// 远程排序
	
	private boolean localSortable = false; // 本地排序

	private boolean hidden = false; // 是否隐藏，若true则不渲染
	
	private String defaultSortType = "asc";
	private boolean defaultSortColumn = false;
	
	
	/**
	 * 是否合并单元格：根据内容进行合并
	 */
	private boolean mergeCell = false;
	
	private int level = 1;// 层次级别号
	
	protected boolean check = false; // 是否是checkbox
	
	protected String valueSuffix = ""; // 值前缀
	
	protected String valuePrefix = "";// 值后缀
	
	protected String valueFormat = "";// 数字格式化
	
	protected DecimalFormat valueFormatter = null;
	
	protected Map<String, Alarm> alarms = new HashMap<String, Alarm>();// 预警
	
	protected String alarmExpression = "";// 预警表达式，内部使用
	
	protected boolean exportable = true;// 字段可以导出
	
	protected boolean exportFormat = false;// 导出格式化

	protected String exportAppendString = ""; // 导出时添加附加信息

	protected String sensitiveDataType = ""; // 敏感类型
	
	protected String nullValue = "";
	
	protected DataType dataType = DataType.String;
	
	protected boolean canDrill = false;// 是否可以钻取
	
	protected boolean drillParameter = false; // 列是否作为钻取参数
	
	protected DataGridDrillMode drillMode = DataGridDrillMode.SingleColumn;
	
	protected String drillIDName = "drillDimId";// 钻取的Id列名称,用于钻取参数名传递给后台ibatis
	protected String drillLevelName = "drillLevel";// 钻取层级参数名,用于钻取参数名传递给后台ibatis
	
	protected boolean enableAlarm = true;// 启动预警
	
	protected boolean canExternalLink = false;//是否可以外部链接
	protected String externalUrl = "";//外部链接
	
	protected String nameExpression = "";//列名称表达式
	
	protected String jsForamtter = ""; // 显示格式化 若jsForamtter=="default",js默认调用区分颜色方法
	
	protected List<AlarmType> exportAlarmTypes = new ArrayList<AlarmType>();// 导出告警类型列表
	
	protected boolean isMessager = false; // 是否可作为消息，用于把当前列field和列value作为消息传递出去
	
	protected boolean isClob = false; // 是否是大文本列，用于前台格式化显示
	
	protected boolean canFilter = false; // 是否可以筛选过滤，用于表头筛选过滤
	
	protected boolean canHide = true; //  是否可隐藏，用于表格渲染后字段可动态隐藏
	
	protected boolean operator = false; // 是否是可操作字段，若为true，则此字段不展示具体数据，只展示具体操作，如：修改、删除、移动等
	
	protected List<FunctionItem> operations = new ArrayList<FunctionItem>(); // 存储具体操作项，只有operator=true时起效
	
	protected Map<String, Object> valueDisplayRules = new LinkedHashMap<String, Object>(); // 值显示规则，用于简易枚举时显示中文
	
	public DataGridColumn(){
		
	}
	
	public DataGridColumn(String field){
		this(field, field);
	}
	
	/**
	 *  不设置宽度，列宽默认计算填充整个grid
	 * @param field
	 * @param title
	 */
	public DataGridColumn(String field, String title){
		this(field, title, -1);
	}
	
	public DataGridColumn(String field, String title, int width){
		this.originField = field;
		this.field = field.toUpperCase();
		this.field = field;
		this.title = title;
		this.width = width;
	}
	
	public DataGridColumn(String field, String title, int width, String align){
		this(field, title, width, align, "");
	}
	
	public DataGridColumn(String field, String title, int width, String align, String valueFormat){
		this(field, title, width, align, valueFormat, "");
	}
	
	public DataGridColumn(String field, String title, int width, String align, String valueFormat, String valueSuffix){
		this.originField = field;
		this.field = field.toUpperCase();
		this.field = field;
		this.title = title;
		this.width = width;
		if(BIUtil.isEmpty(align)){
			align = "center";
		}
		this.align = align;
		this.valueFormat = valueFormat;
		this.valueSuffix = valueSuffix;
	}
	
	/**
	 * 添加预警
	 * @param alarm
	 * @return
	 */
	public DataGridColumn addAlarm(Alarm alarm){
		String key = alarm.getType().toString();
		if(!alarms.containsKey(key)){
			alarms.put(key, alarm);
		}
		alarm.setColumn(this);
		return this;
	}
	
	/**
	 * 获取所有预警
	 * @return
	 */
	public Alarm[] getAlarms(){
		Alarm[] array = new Alarm[alarms.size()];
		int index = 0;
		for(Alarm a : alarms.values()){
			array[index] = a;
			index ++;
		}
		return array;
	}
	
	/**
	 * 设置导出告警
	 * @param lessThanZero 小于零的告警类型
	 * @param equalZero  等于零的告警类型
	 * @param greaterThanZero  大于零的告警类型
	 */
	public void setExportAlarmType(AlarmType lessThanZero, AlarmType equalZero, AlarmType greaterThanZero){
		this.exportAlarmTypes.clear();
		this.exportAlarmTypes.add(lessThanZero);
		this.exportAlarmTypes.add(equalZero);
		this.exportAlarmTypes.add(greaterThanZero);
	}
	
	/**
	 * 设置默认导出告警类型
	 * @param enable
	 */
	public void setExportAlarm(boolean enable){
		if(!enable) {
			this.exportAlarmTypes.clear();
			return;
		}
		this.setExportAlarmType(AlarmType.Red_Font, null, AlarmType.Green_Font);
	}
	
	public DataGridColumn removeAlarm(AlarmType type){
		alarms.remove(type.toString());
		return this;
	}
	
	public DataGridColumn addOperation(String name, String jsFunc) {
		FunctionItem func = new FunctionItem("_func_" + (this.operations.size() + 1), name, jsFunc);
		this.addOperation(func);
		return this;
	}
	
	public DataGridColumn addOperation(String name, String jsFunc, String enableExpression) {
		FunctionItem func = new FunctionItem("_func_" + (this.operations.size() + 1), name, jsFunc);
		func.setEnableExpression(enableExpression);
		this.addOperation(func);
		return this;
	}
	
	public DataGridColumn addOperation(FunctionItem func) {
		if(func == null) {
			return this;
		}
		if(!this.operations.contains(func)) {
			this.operations.add(func);
		}
		return this;
	}
	
	public DataGridColumn addValueDisplayRule(String realValue, Object displayValue) {
		this.valueDisplayRules.put(realValue, displayValue);
		return this;
	}
	
	@Override
	public void load(Element columnEle) {
		if(columnEle == null) return;
		id = columnEle.attributeValue("id");
		String str = columnEle.attributeValue("group");
		if(str != null && !"".equals(str)){
			group = str;
		}
		
		str = columnEle.attributeValue("field");
		if(str != null && !"".equals(str)){
			field = str;
		}
		str = columnEle.attributeValue("keyField");
		if(str != null && !"".equals(str)){
			keyField = str;
		}
		
		str = columnEle.attributeValue("title");
		if(str != null && !"".equals(str)){
			title = str;
		}
		str = columnEle.attributeValue("align");
		if(str != null && !"".equals(str)){
			align = str;
		}
		str = columnEle.attributeValue("halign");
		if(str != null && !"".equals(str)){
			halign = str;
		}
		str = columnEle.attributeValue("defaultSortType");
		if(str != null && !"".equals(str)){
			defaultSortType = str;
		}
		
		str = columnEle.attributeValue("width");
		if(str != null && !"".equals(str)){
			width = Integer.valueOf(str);
		}
		str = columnEle.attributeValue("rowspan");
		if(str != null && !"".equals(str)){
			rowspan = Integer.valueOf(str);
		}
		str = columnEle.attributeValue("colspan");
		if(str != null && !"".equals(str)){
			colspan = Integer.valueOf(str);
		}
		isFrozen = "true".equalsIgnoreCase(columnEle.attributeValue("isFrozen"));
		remoteSortable = "true".equalsIgnoreCase(columnEle.attributeValue("sortable"));
		localSortable = "true".equalsIgnoreCase(columnEle.attributeValue("localSortable"));
		exportable = "true".equalsIgnoreCase(columnEle.attributeValue("exportable"));
		hidden = "true".equalsIgnoreCase(columnEle.attributeValue("hidden"));
		mergeCell = "true".equalsIgnoreCase(columnEle.attributeValue("mergeCell"));
		check = "true".equalsIgnoreCase(columnEle.attributeValue("check"));
		enableAlarm = "true".equalsIgnoreCase(columnEle.attributeValue("enableAlarm"));
		dataType = DataType.getType(columnEle.attributeValue("dataType"));
		exportFormat = "true".equalsIgnoreCase(columnEle.attributeValue("exportFormat"));
		isMessager = "true".equalsIgnoreCase(columnEle.attributeValue("isMessager"));
		isClob = "true".equalsIgnoreCase(columnEle.attributeValue("isClob"));
		
		str = columnEle.attributeValue("nullValue");
		nullValue = str;
		
		str = columnEle.attributeValue("valueSuffix");
		if(str != null && !"".equals(str)){
			valueSuffix = str;
		}else {
			valueSuffix = "";
		}
		
		str = columnEle.attributeValue("valuePrefix");
		if(str != null && !"".equals(str)){
			valuePrefix = str;
		}else {
			valuePrefix = "";
		}
		
		str = columnEle.attributeValue("valueFormat");
		if(str != null && !"".equals(str)){
			valueFormat = str;
			valueFormatter = new DecimalFormat(valueFormat);
		}else {
			valueFormat = "";
		}
		//预警类型
		String alarmTypeStr = columnEle.attributeValue("alarmType");
		// 预警
		Element AlarmsEle = columnEle.element("Alarm");
		if(AlarmsEle != null) {
			List expressionEleList = AlarmsEle.elements("Expression");
			if(expressionEleList != null) {
				this.alarms.clear();
				for(int i = 0; i < expressionEleList.size(); i++){
					final Element expression = (Element)expressionEleList.get(i);
					String alarmColor = expression.attributeValue("type");
					final AlarmType alarmType = AlarmType.getType(alarmColor + "_" +alarmTypeStr );
					this.addAlarm(new Alarm() {
						@Override
						public AlarmType getType() {
							// TODO Auto-generated method stub
							return alarmType;
						}
						@Override
						public String expression() {
							// TODO Auto-generated method stub
							return expression.getText();
						}
					});
				}
			}
		}
		
		
	}
	
	@Override
	public void save(Element columnEle) {
		if(columnEle == null) return;
		columnEle.addAttribute("id", id);
		columnEle.addAttribute("group", group);
		columnEle.addAttribute("field", field);
		columnEle.addAttribute("title", title);
		columnEle.addAttribute("align", align);
		columnEle.addAttribute("halign", halign);
		columnEle.addAttribute("sortable", remoteSortable +"");
		columnEle.addAttribute("localSortable", localSortable+"");
		columnEle.addAttribute("defaultSortType", defaultSortType);
		columnEle.addAttribute("dataType", dataType.toString());
		columnEle.addAttribute("externalUrl", externalUrl);
		columnEle.addAttribute("hidden", hidden+"");
		columnEle.addAttribute("width", width+"");
		columnEle.addAttribute("rowspan", rowspan+"");
		columnEle.addAttribute("colspan", colspan+"");
		columnEle.addAttribute("jsForamtter", jsForamtter);
		columnEle.addAttribute("canDrill", canDrill+"");
		columnEle.addAttribute("valueFormat", valueFormat);
		columnEle.addAttribute("valuePrefix", valuePrefix);
		columnEle.addAttribute("valueSuffix", valueSuffix);
		columnEle.addAttribute("isFrozen", isFrozen+"");
		columnEle.addAttribute("mergeCell", mergeCell+"");
		columnEle.addAttribute("exportable", exportable+"");
		columnEle.addAttribute("enableAlarm", enableAlarm+"");
		columnEle.addAttribute("exportFormat", exportFormat+"");
		columnEle.addAttribute("isMessager", isMessager+"");
		columnEle.addAttribute("isClob", isClob+"");
		columnEle.addAttribute("nullValue", nullValue+"");
		String alarmTypeStr = "";
		if(!alarms.isEmpty()) {
			Element AlarmEle = columnEle.addElement("Alarm");
			Alarm[] alarms = getAlarms();
			for(Alarm alarm : alarms) {
				AlarmType alarmType = alarm.getType();
				String[] alarmTypeArray = alarmType.toString().split("_");
				alarmTypeStr = alarmTypeArray[1];
				String alarmTypeColor = alarmTypeArray[0];
				Element expressionEle = AlarmEle.addElement("Expression");
				expressionEle.addAttribute("type", alarmTypeColor);
				expressionEle.addCDATA(alarm.expression());
				
			}
		}
		columnEle.addAttribute("alarmType", alarmTypeStr+"");
		
	}
	
	public String toJson() {
		return "";
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getField() {
		if(field != null){
			field = field.toUpperCase();
		}
		return field;
	}

	public void setField(String field) {
		this.field = field;
		this.originField = field;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getRowspan() {
		return rowspan;
	}

	public void setRowspan(int rowspan) {
		this.rowspan = rowspan;
	}

	public int getColspan() {
		return colspan;
	}

	public void setColspan(int colspan) {
		this.colspan = colspan;
	}

	public String getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public String getHalign() {
		return halign;
	}

	public void setHalign(String halign) {
		this.halign = halign;
	}

	public boolean isRemoteSortable() {
		return remoteSortable;
	}

	public void setRemoteSortable(boolean remoteSortable) {
		this.remoteSortable = remoteSortable;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof DataGridColumn)) return false;
		DataGridColumn col = (DataGridColumn) obj;
		if(col.getField() == null) return false;
		return col.getField().equalsIgnoreCase(getField());
	}

	public boolean isFrozen() {
		return isFrozen;
	}

	public void setFrozen(boolean isFrozen) {
		this.isFrozen = isFrozen;
	}

	@Override
	public String toString() {
		String str = "field:" + field + "," + "title:" + title + "," + "group:" + group; 
		return str;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public boolean isMergeCell() {
		return mergeCell;
	}

	public void setMergeCell(boolean mergeCell) {
		this.mergeCell = mergeCell;
	}

	public String getId() {
		if(null == id || "".equals(id)){
			id = getField();
		}
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getKeyField() {
		return keyField;
	}

	public void setKeyField(String keyField) {
		this.keyField = keyField;
	}

	public String getValueSuffix() {
		return valueSuffix;
	}

	public void setValueSuffix(String valueSuffix) {
		this.valueSuffix = valueSuffix;
	}

	public String getValuePrefix() {
		return valuePrefix;
	}

	public void setValuePrefix(String valuePrefix) {
		this.valuePrefix = valuePrefix;
	}

	public String getValueFormat() {
		return valueFormat;
	}

	public void setValueFormat(String valueFormat) {
		this.valueFormat = valueFormat;
	}

	public DecimalFormat getValueFormatter() {
		if(BIUtil.isNotEmpty(valueFormat)){
			try{
				valueFormatter = new DecimalFormat(valueFormat);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return valueFormatter;
	}

	public String getAlarmExpression() {
		return alarmExpression;
	}

	public void setAlarmExpression(String alarmExpression) {
		this.alarmExpression = alarmExpression;
	}

	public boolean isCheck() {
		return check;
	}

	public void setCheck(boolean check) {
		this.check = check;
	}

	public boolean isExportable() {
		return exportable;
	}

	public void setExportable(boolean exportable) {
		this.exportable = exportable;
	}

	public boolean isExportFormat() {
		return exportFormat;
	}

	public void setExportFormat(boolean exportFormat) {
		this.exportFormat = exportFormat;
	}

	public String getDefaultSortType() {
		return defaultSortType;
	}

	public void setDefaultSortType(String defaultSortType) {
		this.defaultSortType = defaultSortType;
	}

	public boolean isDefaultSortColumn() {
		return defaultSortColumn;
	}

	public void setDefaultSortColumn(boolean defaultSortColumn) {
		this.defaultSortColumn = defaultSortColumn;
	}

	public String getNullValue() {
		return nullValue;
	}

	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public boolean isCanDrill() {
		return canDrill;
	}

	public void setCanDrill(boolean canDrill) {
		this.canDrill = canDrill;
	}

	public boolean isDrillParameter() {
		return drillParameter;
	}

	public void setDrillParameter(boolean drillParameter) {
		this.drillParameter = drillParameter;
	}

	public void setValueFormatter(DecimalFormat valueFormatter) {
		this.valueFormatter = valueFormatter;
	}

	public String getOriginField() {
		return originField;
	}

	public void setOriginField(String originField) {
		this.originField = originField;
	}

	public DataGridDrillMode getDrillMode() {
		return drillMode;
	}

	public void setDrillMode(DataGridDrillMode drillMode) {
		this.drillMode = drillMode;
	}

	public boolean isEnableAlarm() {
		return enableAlarm;
	}

	public void setEnableAlarm(boolean enableAlarm) {
		this.enableAlarm = enableAlarm;
	}

	public String getDrillIDName() {
		return drillIDName;
	}

	public void setDrillIDName(String drillIDName) {
		this.drillIDName = drillIDName;
	}

	public String getDrillLevelName() {
		return drillLevelName;
	}

	public void setDrillLevelName(String drillLevelName) {
		this.drillLevelName = drillLevelName;
	}

	public String getExternalUrl() {
		return externalUrl;
	}

	public void setExternalUrl(String externalUrl) {
		this.externalUrl = externalUrl;
	}

	public boolean isCanExternalLink() {
		return canExternalLink;
	}

	public void setCanExternalLink(boolean canExternalLink) {
		this.canExternalLink = canExternalLink;
	}

	public String getNameExpression() {
		return nameExpression;
	}

	public void setNameExpression(String nameExpression) {
		this.nameExpression = nameExpression;
	}
	

	
	public boolean isLocalSortable() {
		return localSortable;
	}

	public void setLocalSortable(boolean localSortable) {
		this.localSortable = localSortable;
	}

	public String getJsForamtter() {
		return jsForamtter;
	}

	public void setJsForamtter(String jsForamtter) {
		this.jsForamtter = jsForamtter;
	}

	public List<AlarmType> getExportAlarmTypes() {
		return exportAlarmTypes;
	}

	public void setExportAlarmTypes(List<AlarmType> exportAlarmTypes) {
		this.exportAlarmTypes = exportAlarmTypes;
	}

	public boolean isMessager() {
		return isMessager;
	}

	public void setMessager(boolean isMessager) {
		this.isMessager = isMessager;
	}

	public boolean isClob() {
		return isClob;
	}

	public void setClob(boolean isClob) {
		this.isClob = isClob;
	}

	public List<FunctionItem> getOperations() {
		return operations;
	}

	public void setOperations(List<FunctionItem> operations) {
		this.operations = operations;
	}

	public boolean isCanFilter() {
		return canFilter;
	}

	public void setCanFilter(boolean canFilter) {
		this.canFilter = canFilter;
	}

	public boolean isCanHide() {
		return canHide;
	}

	public void setCanHide(boolean canHide) {
		this.canHide = canHide;
	}

	public boolean isOperator() {
		return operator;
	}

	public void setOperator(boolean operator) {
		this.operator = operator;
		if(this.width <= 0) {
			this.width = 120;
		}
		this.exportable = false;
	}

	public Map<String, Object> getValueDisplayRules() {
		return valueDisplayRules;
	}

	public void setValueDisplayRules(Map<String, Object> valueDisplayRules) {
		this.valueDisplayRules = valueDisplayRules;
	}
	
	public void clearValueDisplayRules() {
		if(this.valueDisplayRules != null) {
			this.valueDisplayRules.clear();
		}
	}
	
	public boolean isDisplayColumn() {
		return this.getField().toLowerCase().contains(Display_Field_Suffix);
	}


	public String getExportAppendString() {
		return exportAppendString;
	}

	public void setExportAppendString(String exportAppendString) {
		this.exportAppendString = exportAppendString;
	}

	public String getSensitiveDataType() {
		return sensitiveDataType;
	}

	public void setSensitiveDataType(String sensitiveDataType) {
		this.sensitiveDataType = sensitiveDataType;
	}
}
