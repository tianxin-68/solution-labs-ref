package com.bi.queryer.util.component.datasource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.bi.queryer.util.component.datasource.tag.DynamicSQLTag;
import com.bi.queryer.util.component.datasource.tag.DynamicSQLTagException;
import com.bi.queryer.util.component.datasource.tag.DynamicSQLTagManager;
import com.bi.queryer.util.component.datasource.tag.DynamicSQLTagType;

/**
 * 动态SQL解析器，用于解析ibatis标签，参数占位符[#parameterName#]或者#parameterName#等
 * <ul>
 *  目前支持标签(标签可嵌套)：
 * 	<li>isEmpty</li>
 * 	<li>isNotEmpty</li>
 * 	<li>isEqual</li>
 * 	<li>isNotEqual</li>
 * </ul>
 * 解析算法说明:<br/>
 * 1、为sql语句添加select标签，将此标签作为xml的root节点<br/>
 * 2、获取select标签直接下级标签elements<br/>
 * 3、遍历elements，通过parameters判断具体elemet是否满足条件<br/>
 * 4、如果不满足条件通过上级删除此element，如果满足则递归遍历其子节点<br/>
 * 5、处理上4后，通过root节点获取其StringValue<br/>
 * 6、对StringValue进行变量替换<br/>
 * 7、返回可执行sql语句
 * @author contributor
 * 
 */
public class DynamicSQLParser {
	
	/**
	 * 特殊字符
	 */
	protected static final String[] special_chars = new String[]{"<", ">"};
	
	/**
	 * 特殊字符转义
	 */
	protected static final String[] special_escape = new String[]{"&lt;", "&gt;"};
	
	/**
	 * 原始SQL
	 */
	private String sql = "";
	
	/**
	 * 可执行的sql
	 */
	private String executableSql = "";
	
	/**
	 * SQL参数
	 */
	private Map<String, ?> parameters = new HashMap<String, Object>();
	
	protected Element root = null; // sql的根节点
	
	protected boolean needEscape = false;// 字符需要转义
	
	public DynamicSQLParser(String sql, Map<String, String> parameters) {
		this.sql = sql;
		this.parameters = parameters;
	}
	
	/**
	 * 解析带动态标签sql
	 * @return 可执行sql
	 * @throws DynamicSQLTagException 
	 */
	public String parse() throws DynamicSQLTagException{
		// 准备
		prepare();
		
		// 递归解析
		parse(root);
		
		// 参数替换
		replaceParameters();
		
		// 结束
		post();
		
		return executableSql;
	}
	
	/**
	 * 递归解析元素
	 * @param parentElement
	 * @throws DynamicSQLTagException 
	 */
	protected void parse(Element parentElement) throws DynamicSQLTagException{
		if(parentElement == null) return ;
		List<?> children = parentElement.elements();
		
		if(children == null || children.isEmpty()) return;
		
		for(int i = 0; i < children.size(); i++){
			Element child = (Element) children.get(i);
			DynamicSQLTag tag = DynamicSQLTagManager.getTag(child);
			if(tag == null) {
				continue;
			}
			tag.setParameters(parameters);
			if(tag.conform()){
				parse(child); // 递归解析
			}else{
				parentElement.remove(child);
			}
		}
	}
	
	/**
	 * 解析前置操作
	 */
	protected void prepare(){
		// 特殊字符转义
		needEscape();
		escape();
		
		wrapSelect();
		parseRootElement();
	}
	
	/**
	 * 解析后置操作
	 */
	protected void post(){
		root = null;
	}
	
	/**
	 * 替换SQL中所有参数[#parameterName#]
	 */
	protected void replaceParameters(){
		root.normalize();
		executableSql = root.getStringValue();
		for(String key : parameters.keySet()){
			String value = parameters.get(key) == null ? "" : parameters.get(key) + "";
			executableSql = executableSql.replaceAll("\\[?\\#\\s*" + key + "\\s*\\#\\]?", value);
		}
	}
	
	/**
	 * 解析根元素
	 */
	protected void parseRootElement(){
		SAXReader saxReader = new SAXReader();
		Document document;
		try {
			document = saxReader.read(new StringReader(sql));
			root = document.getRootElement();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 转义特殊字符
	 */
	protected void escape(){
		if(!needEscape) return;
		sql = StringUtils.replaceEach(sql, special_chars, special_escape);
	}
	/**
	 * 对动态sql包装select标签
	 */
	protected void wrapSelect(){
		if(sql.indexOf("<select") == -1){
			sql = "<select>" + sql + "</select>";
		}
		sql = "<?xml version='1.0' encoding='UTF-8'?>" + sql;
	}
	
	protected String getSql() {
		return sql;
	}

	protected void setSql(String sql) {
		this.sql = sql;
	}

	protected Map<String, ?> getParameters() {
		return parameters;
	}


	public Element getRoot() {
		return root;
	}

	public void setRoot(Element root) {
		this.root = root;
	}

	public String getExecutableSql() {
		return executableSql;
	}

	public void setExecutableSql(String executableSql) {
		this.executableSql = executableSql;
	}
	
	/**
	 * 是否需要转义
	 * @return
	 */
	public boolean needEscape() {
		boolean flag = true;
		List<String> tagPrefixs = new ArrayList<String>();
		for(DynamicSQLTagType tagType : DynamicSQLTagType.values()){
			tagPrefixs.add("<" + tagType.toString());
		}
		tagPrefixs.add("<![CDATA[");
		for(String tagName : tagPrefixs){
			flag = flag && (sql.indexOf(tagName) == -1);
			if(!flag){
				break;
			}
		}
		needEscape = flag;
		return flag;
	}

	public void setParameters(Map<String, ?> parameters) {
		this.parameters = parameters;
	}
}
