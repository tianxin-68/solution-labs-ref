package com.bi.queryer.util.component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datasource.DataSetProviderType;
import com.bi.queryer.util.component.datasource.DynamicSQLParser;
import com.bi.queryer.util.component.datasource.tag.DynamicSQLTagException;
import com.bi.queryer.util.component.exception.ComponentException;
import com.bi.queryer.util.component.message.ComponentMessage;
import com.bi.queryer.util.component.message.ComponentMessageBody;

/**
 * xml存储通用方法
 * 
 * @author contributor
 *
 */
public abstract class XmlHelper<T extends Component> {

	public static String BASE_VO_PATH = "WEB-INF/classes/com/zhaogang/bi/util/component/ComponentBaseVO.xml";// 基本实体类保存信息
	public static String VO_PATH = "WEB-INF/classes/com/zhaogang/bi/util/component/ComponentVO.xml";// 实体类保存信息
	public static final String DEFAULT_SEQ = "PTL_BP_XMLHELPER_SEQ";// 默认序列
	protected T component = null;
	protected Object componentVO = null;
	protected Map dataInfo = null;// 数据库组成信息

	Connection conn = null;
	CallableStatement pstmt = null;
	ResultSet rs = null;

	public XmlHelper(T component) {
		this.component = component;
		init();
	}

	public XmlHelper(T component, Object componentVO) {
		this.component = component;
		this.componentVO = componentVO;
		init();
	}
	
    public static String getBaseDocPath(){
    	String rootPath = "";
//    	try{
//    	    ActionContext ac = ActionContext.getContext(); 
//		    ServletContext sc = (ServletContext) ac.get(ServletActionContext.SERVLET_CONTEXT); 
//		    rootPath = ServletActionContext.getRequest().getRealPath("/");
//    	}catch(Exception e){
//    		return "d:/javaworkspace/biPortal/WebRoot/WEB-INF/classes/com/zhaogang/bi/util/component/ComponentBaseVO.xml";
//    	}
		return  rootPath+"WEB-INF/classes/com/zhaogang/bi/util/component/ComponentBaseVO.xml";
    }
    
    public static String getDocPath(){
    	String rootPath = "";
//    	try{
//    	    ActionContext ac = ActionContext.getContext(); 
//		    ServletContext sc = (ServletContext) ac.get(ServletActionContext.SERVLET_CONTEXT); 
//		    rootPath = ServletActionContext.getRequest().getRealPath("/");
//    	}catch(Exception e){
//    		return "d:/javaworkspace/biPortal/WebRoot/WEB-INF/classes/com/zhaogang/bi/util/component/ComponentVO.xml";
//    	}
		return rootPath+"WEB-INF/classes/com/zhaogang/bi/util/component/ComponentVO.xml";
    }
	public void init() {
		try{
			BASE_VO_PATH = getBaseDocPath();
			VO_PATH = getDocPath();
			if(component.getType()==null||component.getType()==ComponentType.Input){
				return;
			}
		    String className = XmlHelperType
				.getType(component.getType().toString()).getXmlVOClass();
		if (this.componentVO == null) {
			this.componentVO = Class.forName(className).newInstance();
//				((ComponentXmlVO)this.componentVO).setInfoName(this.component.getName());
		} else {
			checkVOType();
		}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 保存XML格式的通用方法
	 * 
	 * @param component
	 *            待保存的实体类
	 * @param configPath
	 *            保存文件路径
	 * @return
	 */
	public abstract void save(Element root);

	/**
	 * 读取XML格式的通用方法
	 * 
	 * @param configPath
	 *            读取文件路径
	 * @return
	 */
	public abstract void load(Element root);

	public boolean loadData(long key){
		return loadData(key,true);
	}
	/**
	 * 通过ID值加载数据库数据
	 * 
	 * @return
	 */
	public boolean loadData(long key,boolean isLoadConfig){
		if (this.componentVO == null) {
			return false;
		}
		((ComponentXmlVO) this.componentVO).setInfoId(key);
		return loadData(isLoadConfig);
	}

	/**
	 * 加载数据库数据
	 * 
	 * @return
	 * @throws Exception 
	 */
	public boolean loadData(boolean isLoadConfig){
		boolean flag = false;
		if (this.componentVO == null) {
			return flag;
		}
		long key = 0;
		try {
			key = ((ComponentXmlVO) this.componentVO).getInfoId();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ComponentException("compnentVO类型有错，请检查！");
		}
		if(key == 0){
			return flag;
		}
		if (dataInfo == null) {
			dataInfo = new HashMap();
			loadVOFromXml(dataInfo, VO_PATH, component.getType().toString());
		}
		flag = loadComponentInfo(key,isLoadConfig);// 加载组件信息
		//如果没有明细信息被配置
//		if(((ComponentXmlVO)this.componentVO).getSqlConfig()==null){
//			flag = false;
//		}
		return flag;
	}

	/**
	 * 保存到数据库中
	 */
	public long saveData(){
		long key = 0;
		if (!prepareForSave()) {
			System.out.println("数据库映射实体VO不能为空！");
			return key;
		}
		if (dataInfo == null) {
			dataInfo = new HashMap();
			loadVOFromXml(dataInfo, VO_PATH, component.getType().toString());
		}
		try {
			conn = DBUtil.getConn();
			conn.setAutoCommit(false);
			key = insertInfoToDataBase(this.componentVO, dataInfo, true);
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
				throw new ComponentException(e1.getMessage());
			}
			throw new  ComponentException(e.getMessage());
		} finally {
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
					throw new  ComponentException(e.getMessage());
				}
			}
			if(pstmt != null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
					throw new  ComponentException(e.getMessage());
				}
			}
			if(conn != null){
			    try {
				    conn.close();
			    } catch (SQLException e) {
				    e.printStackTrace();
				    throw new  ComponentException(e.getMessage());
			    }
			}
			return key;
		}
	}

	/**
	 * 检查设置实体类的类型是否匹配
	 */
	private void checkVOType(){
		String className = XmlHelperType
				.getType(component.getType().toString()).getXmlVOClass();
		if (this.componentVO == null) {
			throw new  ComponentException("实体VO类不能设置为空值！");
		} else if (!className.equalsIgnoreCase(this.componentVO.getClass()
				.getName())) {
			this.componentVO = null;
			throw new  ComponentException("实体VO类不匹配！");
		}else{
			long key = ((ComponentXmlVO)this.componentVO).getInfoId();
//			String infoName = ((ComponentXmlVO)this.componentVO).getInfoName();
//			if(StringUtil.isEmpty(infoName)){
//				((ComponentXmlVO)this.componentVO).setInfoName(this.component.getName());
//			}else{
//				this.component.setName(infoName);
//			}
		}
	}

	/**
	 * 加载数据库实体信息
	 * 
	 * @param infoMap
	 * @param path
	 * @param id
	 */
	public static void loadVOFromXml(Map infoMap, String path, String id){
		Element root = getRootElement(path);
		List<Element> tables = root.elements("table");
		Element componentTable = null;
		for (Element table : tables) {
			if (id.equalsIgnoreCase(StringUtil.ifNull(table
					.attributeValue("id")))) {
				componentTable = table;
				break;
			}
		}
		Element include = componentTable.element("include");
		if (include != null) {
			String parentId = StringUtil.ifNull(include
					.attributeValue("relate"));
			boolean relateBase = "true".equalsIgnoreCase(StringUtil
					.ifNull(include.attributeValue("relateBase")));
			String parentPath = getDocPath();
			if (relateBase) {
				parentPath = getBaseDocPath();
			}
			loadVOFromXml(infoMap, parentPath, parentId);
		}
		infoMap.put("id",
				StringUtil.ifNull(componentTable.attributeValue("id")));
		infoMap.put("class",
				StringUtil.ifNull(componentTable.attributeValue("class")));
		infoMap.put("tablespace",
				StringUtil.ifNull(componentTable.attributeValue("tablespace")));
		infoMap.put("table",
				StringUtil.ifNull(componentTable.attributeValue("table")));

		String tablespace = StringUtil.ifNull(componentTable
				.attributeValue("tablespace"));
		String table = StringUtil
				.ifNull(componentTable.attributeValue("table"));
		String tableName = table;
		if (!StringUtil.isEmpty(tablespace)) {
			tableName = tablespace + "." + table;
		}
		infoMap.put("tableName", tableName);
		List<Element> tds = componentTable.elements("td");
		List colInfoList = (infoMap.get("colList") == null ? new ArrayList()
				: (List) infoMap.get("colList"));
		infoMap.put("colList", colInfoList);
		Element formatRoot = getRootElement(getDocPath());
		List<Element> formatters = formatRoot.element("formatters")==null?null:formatRoot.element("formatters").elements("formatter");
		for (Element td : tds) {
			Map mp = new HashMap();
			if (!checkRepeat(colInfoList, td)) {
				continue;
			}
			mp.put("name", StringUtil.ifNull(td.attributeValue("name")));
			mp.put("column", StringUtil.ifNull(td.attributeValue("column")));
			mp.put("type", StringUtil.ifNull(td.attributeValue("type")));
			mp.put("priKey", StringUtil.ifNull(td.attributeValue("priKey")));
			mp.put("sequence", StringUtil.ifNull(td.attributeValue("sequence")));
			mp.put("relate", StringUtil.ifNull(td.attributeValue("relate")));
			mp.put("relateBase",
					StringUtil.ifNull(td.attributeValue("relateBase")));	
			//加载显示列表内容相关的属性
			InputColumnInfo(mp,td,formatters);
			colInfoList.add(mp);
			if ("true".equalsIgnoreCase(StringUtil.ifNull(td
					.attributeValue("priKey")))) {
				infoMap.put("childKeyName", StringUtil.ifNull(td.attributeValue("name")));
			}
		}
	}
   /**
    * 写入配置界面展示的样式
    * @param mp
    * @param td
    */
	private static void InputColumnInfo(Map mp, Element td,List<Element> formatters) {

		mp.put("showable", StringUtil.ifNull(td.attributeValue("showable")));
		mp.put("editable", StringUtil.ifNull(td.attributeValue("editable")));
		Element column = td.element("column");
		if(column != null){
			mp.put("editor",StringUtil.ifNull(column.attributeValue("editor")));
			mp.put("colName",StringUtil.ifNull(column.attributeValue("colName")));
			mp.put("width",StringUtil.ifNull(column.attributeValue("width")));
			mp.put("align",StringUtil.ifNull(column.attributeValue("align")));
			mp.put("required",StringUtil.ifNull(column.attributeValue("required")));
			mp.put("hint",StringUtil.ifNull(column.attributeValue("hint")));
			mp.put("default",StringUtil.ifNull(column.attributeValue("default")));
			mp.put("precision",StringUtil.ifNull(column.attributeValue("precision")));
			mp.put("relateTable",StringUtil.ifNull(column.attributeValue("relateTable")));
			mp.put("relateColumn",StringUtil.ifNull(column.attributeValue("relateColumn")));
			mp.put("showColumn",StringUtil.ifNull(column.attributeValue("showColumn")));
			mp.put("relateCond",StringUtil.ifNull(column.attributeValue("relateCond")));
			mp.put("onlyAdd",StringUtil.ifNull(column.attributeValue("onlyAdd")));
			String formatStr = StringUtil.ifNull(column.attributeValue("format"));
			mp.put("format",formatStr);
			if(formatters==null){
				return;
			}
			if(!StringUtil.isEmpty(formatStr)){
				for (Element formatter : formatters) {
					String id = formatter.attributeValue("id");
					if(id.equalsIgnoreCase(formatStr)){
						List<Element> formats = formatter.elements("format");
						List formatList = new ArrayList();
						for(Element format : formats){
							String[] info = new String[2];
							info[0] = format.attributeValue("value");
							info[1] = format.attributeValue("text");
							String color = StringUtil.ifNull(format.attributeValue("color"));
							if(!StringUtil.isEmpty(color)){
								info[1] = "<font color="+color+">"+info[1]+"</font>";
							}
							formatList.add(info);
						}
						mp.put("formatList", formatList);
						break;
					}
				}
			}	
		}	
	}

	private static boolean checkRepeat(List list, Element el) {
		for (Object obj : list) {
			Map mp = (Map) obj;
			if (StringUtil.ifNull(mp.get("name")).equalsIgnoreCase(
					StringUtil.ifNull(el.attributeValue("name")))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 加载组件信息
	 */
	private boolean loadComponentInfo(long key,boolean isLoadConfig){
		// 通过主键设置值
		boolean flag = false;
		try {
			conn = DBUtil.getConn();

			flag = loadAllValue(dataInfo, key,isLoadConfig);// 从数据库加载信息到MAP
			if(!flag){
				return flag;
			}
			setVOProperty(this.componentVO, dataInfo,isLoadConfig);// 从MAP加载信息到数据库实体类
			loadComponentInfo();// 从数据库实体类加载信息到组件
		} catch (Exception e) {
			e.printStackTrace();
			flag = false;
			throw new ComponentException(e.getMessage());
		} finally {
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
					flag = false;
					throw new ComponentException(e.getMessage());
				}
			}
			if(pstmt != null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
					flag = false;
					throw new ComponentException(e.getMessage());
				}
			}
			if(conn != null){
			    try {
				    conn.close();
			    } catch (SQLException e) {
				    e.printStackTrace();
				    flag = false;
				    throw new ComponentException(e.getMessage());
			    }
			}
			return flag;
		}

	}

	/**
	 * 从数据库实体类加载信息到组件
	 * 
	 * @throws DocumentException
	 */
	private void loadComponentInfo() throws DocumentException {
		String infoConfig = ((ComponentXmlVO) componentVO).getInfoConfig();
		SAXReader saxReader = null;
		Document document = null;
		if (!StringUtil.isEmpty(infoConfig)) {
			saxReader = new SAXReader();
			//System.out.println("最终的数据为：...." + infoConfig);
			document = saxReader.read(new StringReader(infoConfig));
			Element root = document.getRootElement();
			load(root);
			loadCommon(root);//加载的共有方法
		}
		if (component.isDataComponent
				&& ((ComponentXmlVO) componentVO).getSqlConfig() != null) {
			SqlXmlVO<T> sqlVO = ((ComponentXmlVO) componentVO).getSqlConfig();
			//向component中注入sql相关的属性
			sqlVO.load(component);
			// 参数中加入SQL原始语句用于后期查询
			Map parameters = component.getDataSetQueryParam() == null ? new HashMap()
					: component.getDataSetQueryParam();
			//将组件得到的外部参数替换到组件默认参数中
			Map requestParams = component.getRequest()==null?new HashMap():component.getRequest().getParams();
			component.setDataSetQueryParam(parameters);
		}
	}
/**
 * 加载的共有方法
 * @param root
 */
	private void loadCommon(Element root) {
		//设置组件数据源
		//Element datasource = root.element("Datasource");
		component.setDataSetProviderBeanName(DataSetProviderType.xmlDataSetProvider.toString());
		//设置组件消息
		Element msgBody = root.element("TransMessage");
		if(msgBody != null){
			ComponentMessage msg = new ComponentMessage();
			msg.setId(StringUtil.ifNull(msgBody.attributeValue("id")));
			String rcsStr = StringUtil.ifNull(msgBody.attributeValue("reciever"));
			if(!StringUtil.isEmpty(rcsStr)){
				msg.setReceivers(Arrays.asList(rcsStr.split(",")));
			}
			List<Element> els = msgBody.elements("body");
			if(els!= null&& els.size()>0){
				List<ComponentMessageBody> msgList = new ArrayList<ComponentMessageBody>();
				for(Element el :els){
					ComponentMessageBody body = new ComponentMessageBody(StringUtil.ifNull(el.attributeValue("name")),StringUtil.ifNull(el.attributeValue("defaultValue")));
				    body.setCurrentValue(StringUtil.ifNull(el.attributeValue("currentValue")));
				    body.setValueType(StringUtil.ifNull(el.attributeValue("valueType")));
				    body.setType(StringUtil.ifNull(el.attributeValue("type")));
				    body.setTitle(StringUtil.ifNull(el.attributeValue("title")));
				    msgList.add(body);
				}
				msg.setBodys(msgList);
			}
			component.setMessage(msg);
		}	
	}

	/**
	 * 加载数据到对象中
	 * 
	 * @param vo
	 * @param infoMap
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 */
	private void setVOProperty(Object vo, Map infoMap,boolean isLoadConfig)
			throws IllegalArgumentException, IllegalAccessException,
			SQLException, InstantiationException, ClassNotFoundException {
		List<Map> colList = (List) infoMap.get("colList");
		for (Map mp : colList) {
			//不加载xml数据
			if(!isLoadConfig&&"info_config".equalsIgnoreCase(StringUtil.ifNull(mp.get("column")))){
				continue;
			}
			String name = StringUtil.ifNull(mp.get("name"));
			String type = StringUtil.ifNull(mp.get("type"));
			String relate = StringUtil.ifNull(mp.get("relate"));
			Object obj = mp.get("value");
			Field field = findFieldByName(vo, name);
			field.setAccessible(true);
			if (obj == null) {
				if("Long".equalsIgnoreCase(type)||"Integer".equalsIgnoreCase(type)||"Double".equalsIgnoreCase(type)){
					field.set(vo, 0);
				}else{
					field.set(vo, null);
				}
			} else if ("Clob".equalsIgnoreCase(type)) {
//				CLOB clob = (CLOB) obj;
//				String data = clob.getSubString((long) 1, (int) clob.length());
//				field.set(vo, data);
			} else if (!StringUtil.isEmpty(relate)) {
				boolean relateBase = "true".equalsIgnoreCase(StringUtil
						.ifNull(mp.get("relateBase")));
				String path = VO_PATH;
				if (relateBase) {
					path = BASE_VO_PATH;
				}
				Map childMap = new HashMap();
				loadVOFromXml(childMap, path, relate);
				long key = Long.valueOf(StringUtil.ifNullToZero(obj));
				if (key == 0) {
					field.set(vo, null);
				} else {
					loadAllValue(childMap, key,isLoadConfig);
					String className = StringUtil.ifNull(childMap.get("class"));
					Object childObj = Class.forName(className).newInstance();
					setVOProperty(childObj, childMap,isLoadConfig);
					field.set(vo, childObj);
				}

			} else if ("Long".equalsIgnoreCase(type)) {
				field.set(vo, Long.valueOf(obj.toString()));
			} else if ("Integer".equalsIgnoreCase(type)) {
				field.set(vo, Integer.valueOf(obj.toString()));
			} else if ("Double".equalsIgnoreCase(type)) {
				field.set(vo, Double.valueOf(obj.toString()));
			} else if ("Timestamp".equalsIgnoreCase(type)) {
				field.set(vo,((Timestamp) obj));
			} else {
				field.set(vo, obj);
			}
		}
	}

	/**
	 * 根据名称找到属性对象
	 * 
	 * @param vo
	 * @param name
	 * @return
	 */
	public static Field findFieldByName(Object vo, String name) {
		Field[] feilds = vo.getClass().getDeclaredFields();
		for (Field f : feilds) {
			try {
				if (f.getName().equalsIgnoreCase(name)) {
					return f;
				}

			} catch (Exception e) {
				continue;
			}
		}
		if (vo.getClass().getSuperclass() != null) {
			feilds = vo.getClass().getSuperclass().getDeclaredFields();
			for (Field f : feilds) {
				try {
					if (f.getName().equalsIgnoreCase(name)) {
						return f;
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		return null;
	}

	/**
	 * 从数据库中取出所有数据
	 * 
	 * @param infoMap
	 * @param key
	 * @throws SQLException
	 */
	private boolean loadAllValue(Map infoMap, long key,boolean isLoadConfig) throws SQLException {
		boolean flag = false;
		StringBuffer sb = new StringBuffer();
		StringBuffer columnSB = new StringBuffer();
		String tableName = StringUtil.ifNull(infoMap.get("tableName"));
		List<Map> colList = (List) infoMap.get("colList");
		String colKey = "";
		for (Map mp : colList) {
			if(!isLoadConfig&&"info_config".equalsIgnoreCase(StringUtil.ifNull(mp.get("column")))){
				continue;
			}
			if ("true".equalsIgnoreCase(StringUtil.ifNull(mp.get("priKey")))) {
				colKey = StringUtil.ifNull(mp.get("column"));
			}
			columnSB.append(StringUtil.ifNull(mp.get("column"))).append(",");
		}
		sb.append("SELECT ")
				.append(columnSB.toString().substring(0,
						columnSB.toString().length() - 1)).append(" FROM ")
				.append(tableName).append(" WHERE ").append(colKey)
				.append("= ?").append(" AND is_active = 1");
		pstmt = conn.prepareCall(sb.toString());
		pstmt.setObject(1, key);
		rs = pstmt.executeQuery();
		while (rs.next()) {
			for (Map mp : colList) {
				if(!isLoadConfig&&"info_config".equalsIgnoreCase(StringUtil.ifNull(mp.get("column")))){
					continue;
				}
				Object obj = rs.getObject(StringUtil.ifNull(mp.get("column")));
				mp.put("value", obj);
				flag = true;
			}
		}
		return flag;
	}

	/**
	 * 保存前准备工作
	 * 
	 * @return
	 */
	private boolean prepareForSave() {
		if (this.componentVO == null) {
			return false;
		}
		Document doc = createDocument();
		Element rootSave = doc.addElement(this.component.getType().toString());
		save(rootSave);
		//公有的保存逻辑
		saveCommon(rootSave);
		((ComponentXmlVO) this.componentVO).setInfoConfig(doc.asXML());
		// 如果为数据实体
		return true;
	}
    /**
     * 共有属性的保存逻辑
     * @param rootSave
     */
	private void saveCommon(Element root) {
		//保存组件数据源
		Element datasource = root.addElement("Datasource");
		datasource.addAttribute("type", DataSetProviderType.xmlDataSetProvider.toString());
		
		//保存组件消息
		ComponentMessage msgVO = component.getMessage();
		if(msgVO!= null){
		    Element msg = root.addElement("TransMessage");
			msg.addAttribute("id", StringUtil.ifNull(msgVO.getId()));
			msg.addAttribute("sender", msgVO.getSender()==null?"":StringUtil.ifNull(msgVO.getSender().getId()));
			StringBuffer rcs = new StringBuffer();
			String[] recievers = msgVO.getReceivers();
			if(recievers != null){
				int i = 0;
				for(String str : recievers){
					rcs.append("str");
					if(i != recievers.length-1){
						rcs.append(",");
					}
					i++;
				}
			}
			msg.addAttribute("receiver", rcs.toString());
			msg.addAttribute("scope", StringUtil.ifNull(StringUtil.ifNull(msgVO.getScope())));
	        ComponentMessageBody[] msgList = msgVO.getContents();
	        if(msgList != null&&msgList.length>0){
	        	for(ComponentMessageBody msgBody : msgList){
	        		 Element body = msg.addElement("body");
	        		 body.addAttribute("name", StringUtil.ifNull(msgBody.getName()));
	        		 body.addAttribute("currentValue", StringUtil.ifNull(msgBody.getCurrentValue()));
	        		 body.addAttribute("defaultValue", StringUtil.ifNull(msgBody.getDefaultValue()));
	        		 body.addAttribute("valueType", StringUtil.ifNull(msgBody.getValueType()));
	        		 body.addAttribute("type", StringUtil.ifNull(msgBody.getType()));
	        		 body.addAttribute("title", StringUtil.ifNull(msgBody.getTitle()));
	        	}   	
	        }
	    }
	}
	private Document saveSql() {
		Document docSql = createDocument();
		Element rootSql = docSql.addElement("Sql");
		Element sqlStr = rootSql.addElement("SqlStr");
		return docSql;
	}

	private long insertInfoToDataBase(Object vo, Map metaInfo, boolean isDelete)
			throws Exception {
		StringBuffer sb = new StringBuffer();
		if (vo == null || metaInfo == null) {
			return 0;
		}

		List infoList = (List) metaInfo.get("colList");
		Map fieldInfo = getFieldInfoFromVO(vo);// 取出属性信息
		// 子属性修改
		List childKeyList = new ArrayList();
		for (Object obj : infoList) {
			Map mp = (Map) obj;
			String relate = StringUtil.ifNull(mp.get("relate"));
			boolean relateBase = "true".equalsIgnoreCase(StringUtil.ifNull(mp
					.get("relateBase")));
			if (!StringUtil.isEmpty(relate)) {
				Map childMap = new HashMap();
				String path = VO_PATH;
				if (relateBase) {
					path = BASE_VO_PATH;
				}

				loadVOFromXml(childMap, path, relate);
				if (childMap.get("childKeyName") != null) {
					childKeyList.add(childMap.get("childKeyName"));
				}
				Object property = fieldInfo.get(StringUtil.ifNull(mp
						.get("name")));
				insertInfoToDataBase(property, childMap, true);
			}

		}
		Map keyInfo = findKeyInfo(fieldInfo, infoList);
		if (isDelete) {
			deleteVO(keyInfo, metaInfo);
		}
		// List clobList = null;
		long key = setKeyValue(vo, keyInfo);// 设置对象的主键,返回主键值
		fieldInfo.put(keyInfo.get("name"), key);
		// 新增的方法
		StringBuffer columnSB = new StringBuffer();
		StringBuffer valueSB = new StringBuffer();

		for (Object obj : infoList) {
			Map mp = (Map) obj;
			columnSB.append(mp.get("column")).append(",");
			valueSB.append("?,");
		}
		sb.append("INSERT INTO ")
				.append(metaInfo.get("tableName"))
				.append("(")
				.append(columnSB.toString().substring(0,
						columnSB.toString().length() - 1))
				.append(") VALUES(")
				.append(valueSB.toString().substring(0,
						valueSB.toString().length() - 1)).append(")");
		pstmt = conn.prepareCall(sb.toString());
		// pstmt属性
		setPstmtState(pstmt, fieldInfo, infoList, childKeyList);
		pstmt.executeUpdate();
		return key;
	}

	private void setPstmtState(CallableStatement pstmt, Map fieldInfo,
			List infoList, List childKeyList) throws SQLException {
		int count = 1;
		int i = 0;
		for (Object ob : infoList) {
			Map mp = (Map) ob;
			String type = StringUtil.ifNull(mp.get("type"));
			String relate = StringUtil.ifNull(mp.get("relate"));
			Object obj = fieldInfo.get(StringUtil.ifNull(mp.get("name")));
			if (!StringUtil.isEmpty(relate)) {// 如果关联了对象，则只用存主键
				if (obj == null) {
					pstmt.setLong(count++, 0);
				} else {
					String childName = (String) childKeyList.get(i++);
					pstmt.setObject(
							count++,
							getObjectValueByName(obj,childName));
				}
			} else {
				pstmt.setObject(count++, obj);
			}
		}
	}

	/**
	 * 通过名称取得属性
	 * 
	 * @param vo
	 * @param name
	 * @return
	 */
	private Object getObjectValueByName(Object vo, String name) {
		Object obj = null;
		Field[] feilds = vo.getClass().getDeclaredFields();
		boolean isSet = false;
		for (Field f : feilds) {
			try {
				f.setAccessible(true);
				if (f.getName().equalsIgnoreCase(name)) {
					obj = f.get(vo);
					isSet = true;
					break;
				}
			} catch (Exception e) {
				continue;
			}
		}
		if (!isSet && vo.getClass().getSuperclass() != null) {
			feilds = vo.getClass().getSuperclass().getDeclaredFields();
			for (Field f : feilds) {
				try {
					if (f.getName().equalsIgnoreCase(name)) {
						obj = f.get(vo);
						isSet = true;
						break;
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		return obj;
	}

	// 设置对象主键
	private long setKeyValue(Object vo, Map keyInfo) throws SQLException {
		long key = Long
				.valueOf(StringUtil.ifNullToZero(keyInfo.get("value")));
		if (key == 0) {
			String sequence = StringUtil.isEmpty(StringUtil.ifNull(keyInfo
					.get("sequence"))) ? DEFAULT_SEQ : StringUtil
					.ifNull(keyInfo.get("sequence"));
			String sql = "SELECT " + sequence + ".nextval SEQ FROM DUAL";
			pstmt = conn.prepareCall(sql);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				key = rs.getLong("SEQ");
				System.out.println("新生成的key值。。。" + key);
			}
		}
		keyInfo.put("value", key);
		setObjectValueByName(vo, StringUtil.ifNull(keyInfo.get("name")), key);
		return key;
	}

	private void setObjectValueByName(Object vo, String name, Object value) {
		Field[] feilds = vo.getClass().getDeclaredFields();
		boolean isSet = false;
		for (Field f : feilds) {
			try {
				f.setAccessible(true);
				if (f.getName().equalsIgnoreCase(name)) {
					f.set(vo, value);
					isSet = true;
					break;
				}

			} catch (Exception e) {
				continue;
			}
		}
		if (!isSet && vo.getClass().getSuperclass() != null) {
			feilds = vo.getClass().getSuperclass().getDeclaredFields();
			for (Field f : feilds) {
				try {
					if (f.getName().equalsIgnoreCase(name)) {
						f.set(vo, value);
						isSet = true;
						break;
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	// 先进行删除逻辑
	private void deleteVO(Map keyInfo, Map metaInfo) throws SQLException {
		String sql = "DELETE FROM " + metaInfo.get("tableName") + " WHERE "
				+ keyInfo.get("column") + " = ?";
		pstmt = conn.prepareCall(sql);
		pstmt.setLong(1,
				Long.valueOf(StringUtil.ifNullToZero(keyInfo.get("value"))));
		pstmt.executeUpdate();

	}

	private Map findKeyInfo(Map fieldInfo, List infoList) {
		Map info = null;
		for (Object obj : infoList) {
			Map mp = (Map) obj;
			if ("true".equalsIgnoreCase(StringUtil.ifNull(mp.get("priKey")))) {
				info = new HashMap();
				info.put("name", StringUtil.ifNull(mp.get("name")));
				info.put("value", StringUtil.ifNullToZero(fieldInfo
						.get(StringUtil.ifNull(mp.get("name")))));
				info.put("column", StringUtil.ifNull(mp.get("column")));
				info.put("type", StringUtil.ifNull(mp.get("type")));
				info.put("sequence", StringUtil.ifNull(mp.get("sequence")));
				break;
			}
		}
		return info;
	}

	// 取得基本属性信息
	private Map getFieldInfoFromVO(Object vo) {
		Map map = new HashMap();
		Field[] feilds = vo.getClass().getDeclaredFields();
		for (Field f : feilds) {
			try {
				f.setAccessible(true);
				map.put(f.getName(), f.get(vo));
			} catch (Exception e) {
				continue;
			}
		}
		if (vo.getClass().getSuperclass() != null) {
			feilds = vo.getClass().getSuperclass().getDeclaredFields();
			for (Field f : feilds) {
				try {
					f.setAccessible(true);
					if (!map.containsKey(f.getName())) {
						map.put(f.getName(), f.get(vo));
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		return map;
	}

	/**
	 * 保存文本到指定路径下
	 * 
	 * @param configPath
	 *            xml文件路径
	 * @return
	 */
	public static boolean saveText(String sql, String configPath) {
		SAXReader saxReader = new SAXReader();
		Document document;
		try {
			document = saxReader.read(new StringReader(sql));
			return saveDocument(document, configPath);
		} catch (DocumentException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Element getRootElement(String configPath) {
		Element root = null;
		SAXReader saxReader = new SAXReader();
		try {
			Document document = saxReader.read(configPath);
			root = document.getRootElement();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new ComponentException(e.getMessage());
		}
		return root;
	}

	/**
	 * 创造空的xml文件的方法
	 * 
	 * @return
	 */
	public static Document createDocument() {
		return DocumentHelper.createDocument();
	}

	/**
	 * 将XML文档保存到指定路径下的方法
	 * 
	 * @param doc
	 *            被保存文档
	 * @param configPath
	 *            保存路径
	 */
	public static boolean saveDocument(Document doc, String configPath) {
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("UTF-8");
		Writer out = null;
		try {
			// 创建一个输出流对象
			out = new FileWriter(configPath);
			// 创建一个dom4j创建xml的对象
			XMLWriter writer = new XMLWriter(out, format);
			// 调用write方法将doc文档写到指定路径
			writer.write(doc);
			writer.close();
			System.out.print("生成XML文件成功");
			return true;
		} catch (IOException e) {
			System.out.print("生成XML文件失败");
			e.printStackTrace();
			return false;
		}
	}
   
	/**
	 * 为保存到XML编码SQL
	 * @param sql
	 * @return
	 */
	public static String codeSql(String sql){
		if(sql==null){
			return "";
		}
		return sql.replaceAll( "<!\\[CDATA\\[", "__aaa____").replaceAll("\\]\\]>","__bbb____");
	}
	/**
	 * 为取出SQL解码SQL
	 * @return
	 */
	public static String decodeSql(String sql){
		if(sql==null){
			return "";
		}
		return sql.replaceAll( "__aaa____", "<![CDATA[").replaceAll("__bbb____","]]>");
	}
	
/**
 * 根据参数列表解析sql
 * @param sqlStr 原始sql
 * @param paramsList  参数列表
 * @return
 * @throws DynamicSQLTagException
 */
	public static String parseSql(String sqlStr,JSONArray paramsList) throws DynamicSQLTagException{
		String sql = "";
		if(StringUtil.isEmpty(sqlStr)){
			return sql;
		}
		Map paramMap = new HashMap();
		if(paramsList == null || paramsList.size()==0){
			DynamicSQLParser parser = new DynamicSQLParser(sqlStr, paramMap);
			sql = parser.parse().replaceAll(
					"\\[?\\#[0-9a-zA-Z\u4e00-\u9fa5]*\\#\\]?", "1");
		}else{
		    for(int i = 0;i<paramsList.size();i++){
		    	JSONObject obj = (JSONObject)paramsList.getJSONObject(i);
		    	paramMap.put(obj.get("name"), EnvVariableManager.value(StringUtil.ifNull(obj.get("value"))));
		    }
		    DynamicSQLParser parser = new DynamicSQLParser(sqlStr, paramMap);
			sql = parser.parse();
		}
		return sql;
	}
	
	public T getComponent() {
		return component;
	}
	
	public T getComponent(long key){
		loadData(key);
		return component;
	}

	public void setComponent(T component) {
		this.component = component;
	}

	public Object getComponentVO() {
		return componentVO;
	}
	public Object getComponentVO(long key){
		if(!loadData(key)){
			((ComponentXmlVO)componentVO).setInfoId(0);
		}
		return componentVO;
	}
	
	public Object getComponentVO(long key,boolean isLoadConfig){
		if(!loadData(key,isLoadConfig)){
			((ComponentXmlVO)componentVO).setInfoId(0);
		}
		return componentVO;
	}

	public void setComponentVO(Object componentVO){
		this.componentVO = componentVO;
		checkVOType();
	}

}
