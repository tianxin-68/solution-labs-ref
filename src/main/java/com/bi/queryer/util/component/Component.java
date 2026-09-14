package com.bi.queryer.util.component;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.JSONSerializable;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.api.ComponentRequest;
import com.bi.queryer.util.component.common.Alignment;
import com.bi.queryer.util.component.common.Theme;
import com.bi.queryer.util.component.datasource.Datasource;
import com.bi.queryer.util.component.datasource.IDataSetProvider;
import com.bi.queryer.util.component.exception.ComponentException;
import com.bi.queryer.util.component.message.ComponentMessage;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件基类
 * @author contributor
 *
 */
public abstract class Component extends XMLSerializable implements JSONSerializable,  Serializable, Cloneable{
	
	private static final long serialVersionUID = 1L;

	public static final String Auto_Id_Suffix = "__Auto";
	
	/**
	 * 唯一标识，组件内部使用
	 */
	protected String guid = "";
	
	/**
	 * 组件：组件唯一标示
	 * 前台:html元素id
	 */
	protected String id = "";
	
	/**
	 * 组件名称
	 */
	protected String name = "";
	
	/**
	 * 组件命名空间
	 * 如果组件是数据组件，则为sqlid的前缀
	 */
	protected  String namespace = "";
	
	/**
	 * 组件编码
	 * 如果组件是数据组件，则为sqlid的后缀
	 */
	protected String code = "";
	
	/**
	 * 组件宽度
	 */
	protected int width = 100;
	
	/**
	 * 组件高度
	 */
	protected int height = 22;
	
	/**
	 * 组件大小自动适应
	 */
	protected boolean autoSize = false; // 大小自动适应
	
	/**
	 * 组件静态数据，如果staticValues不为空，则优先渲染staticValues值，而不是去请求数据
	 */
	protected JSONObject staticValue = new JSONObject();
	
	/**
	 * 父组件
	 */
	protected Component parent = null;
	
//	protected List<Component> children = new Vector<Component>();
	
	/**
	 * 子组件列表
	 */
	protected List<Component> children = new ArrayList<Component>();
	
	/**
	 * 此索引由系统内部分配<br/>
	 * 组件在其父组件中的子组件列表中的索引
	 */
	protected int index = 0; // 组件在其兄弟节点下的索引
	
	/**
	 * 数据组件适用，组件发布时组件数据集优先使用此属性
	 * 数据集provider
	 */
	protected IDataSetProvider dataSetProvider = null; 
	
	/**
	 * 数据组件适用，组件发布时，若为设置dataSetProvider属性，则使用此属性作为组件数据集provider
	 * 数据集provider beanName，用于从spring中获取provider
	 */
	protected String dataSetProviderBeanName = "defaultDataSetProvider";
	
	/**
	 * 数据组件适用
	 * 数据集查询参数，通常用于ibatis查询参数使用
	 */
	protected Map dataSetQueryParam = null;
	
	/**
	 * 组件渲染器class，用于自定义渲染器
	 */
	protected String renderClassName = null;
	
	/**
	 * 组件请求
	 * 如果request=null，系统运行时会自动继承其父亲的request
	 */
	protected ComponentRequest request = null;
	
	/**
	 * 是否是数据组件
	 */
	protected boolean isDataComponent = false;
	
	/**
	 * 是否是容器组件
	 */
	protected boolean isContainer = false;
	
	/**
	 * 组件发送的消息列表<br/>
	 * portal配置时，此属性在组件级别只负责存储MessageBody，receiver和scope由portal来分配
	 */
	protected ComponentMessage message = null;
	
	/**
	 * 保存在实体类中的id值
	 */
	protected String entityId;
	
	/**
	 * 实体名
	 */
	protected String entityName;
	
	/**
	 * 自定义属性
	 */
	protected Map<String, Object> attributes = new HashMap<String, Object>();
	
	/**
	 * 是否可配，即是否可以在portal设计界面中配置
	 */
	protected boolean configurable = false;
	
	/**
	 * 组件渲染主题
	 */
	protected String theme = "";
	
	protected Datasource datasource = new Datasource();
	
	/**
	 * 容器组件属性<br/>
	 * 子节点在当前portlet中的布局方向：垂直 / 水平
	 */
	protected Alignment childLayoutAlign = Alignment.Vertical;
	
	public Component(){
		this.theme = Theme.Auto.toString().toLowerCase();
	}
	
	public Component(String id, String code){
		this();
		this.id = id;
		this.code = code;
	}
	
	/**
	 * 获取组件类型
	 * @return {@link ComponentType}
	 */
	public abstract ComponentType getType();
	
	/**
	 * 添加子组件
	 * @param child
	 */
	public void addChild(Component child){
		if(!children.contains(child)){
			children.add(child);
			child.parent = this;
			child.index = children.size() - 1;
			if(StringUtil.isEmpty(child.getId())){
				child.id = child.getType().toString() + child.index + Auto_Id_Suffix;
			}
			if(StringUtil.isEmpty(child.getCode())){
				child.code = child.id;
			}
		}
	}
	
	public void setDataSetProvider(IDataSetProvider dataSetProvider, Map dataSetQueryParam) {
		this.dataSetProvider = dataSetProvider;
		this.dataSetQueryParam = dataSetQueryParam;
	}
	
	@Override
	public JSONObject toJSON() throws ComponentException{
		JSONObject json = new JSONObject();
		return json;
	}
	
	public void load(Element root){
		if(root == null) {
			return;
		}
		String str = root.attributeValue("id");
		if(!BIUtil.isEmpty(str)){
			id = str;
		}
		
		str = root.attributeValue("name");
		if(!BIUtil.isEmpty(str)){
			name = str;
		}
		
		str = root.attributeValue("guid");
		if(!BIUtil.isEmpty(str)){
			guid = str;
		}
		
		str = root.attributeValue("width");
		if(!BIUtil.isEmpty(str)){
			width = Integer.valueOf(str);
		}
		
		str = root.attributeValue("height");
		if(!BIUtil.isEmpty(str)){
			height = Integer.valueOf(str);
		}
		
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || this.id == null) return false;
		Component c = (Component) obj;
		return this.id.equals(c.getId());
	}

	public String getGuid() {
		if(BIUtil.isEmpty(guid)) {
			guid = id;
		}
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void addAttribute(String name, Object value){
		attributes.put(name, value);
	}
	
	public Object getAttribute(String name){
		return attributes.get(name);
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	@Override
	public String toString() {
		return "type:" + getType() + "\t name:" + name  + "\t namespace:" + namespace + "\t id:" + id + "\t code:" + code ;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Component getParent() {
		return parent;
	}

	public void setParent(Component parent) {
		this.parent = parent;
	}

	public Component[] getChildren() {
		Component array[] = new Component[children.size()];
		children.toArray(array);
		return array;
	}
	
	/**
	 * 获取直接下级
	 * @return
	 */
	public Component getChild(String childId){
		for(Component c : children){
			if(childId.equals(c.getId())){
				return c;
			}
		}
		
		return null;
	}
	
	/**
	 * 递归获取下级
	 * @param childId
	 * @param recursion
	 * @return
	 */
	public Component getChild(String childId, boolean recursion){
		if(recursion){
			return getChild(childId);
		}
		
		return getChild(childId, this);
	}
	
	protected Component getChild(String childId, Component parent){
		Component child = null;
		for(Component c : parent.children){
			if(childId.equals(c.getId())){
				child = c;
				break;
			}else{
				getChild(childId, c);
			}
		}
		return child;
	}
	
	public void clone(Component c, boolean cascade){
		c.id = this.id;
		c.name = this.name;
		c.code = this.code;
		c.namespace = this.namespace;
		c.width = this.width;
		c.height = this.height;
		c.autoSize = this.autoSize;
		c.staticValue = this.staticValue;
		c.entityId = this.entityId;
		c.entityName = this.entityName;
		c.dataSetProvider = this.dataSetProvider;
		c.dataSetProviderBeanName = this.dataSetProviderBeanName;
		c.theme = this.theme;
		if(this.dataSetQueryParam != null){
			c.dataSetQueryParam = new HashMap();
			c.dataSetQueryParam.putAll(this.dataSetQueryParam);
		}
		c.renderClassName = this.renderClassName;
		if(this.request != null){
			c.request = this.request.clone(); 
		}
		c.isDataComponent = this.isDataComponent;
		c.isContainer = this.isContainer;
		if(this.message != null){
			c.message = this.message.clone();
		}
		c.attributes.putAll(this.attributes);
		c.configurable = this.configurable;
		c.theme = this.theme;
		c.childLayoutAlign = this.childLayoutAlign;
		
		// 克隆子节点
		if(cascade){
			if(this.children != null){
				for(Component child : this.children){
					Component childCopy = child.clone(cascade);
					c.addChild(childCopy);
				}
			}
		}
	}
	
	public Component clone(boolean cascade) throws ComponentException{
		Component c = null;
		try {
			c = (Component) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new ComponentException(e);
		}
		return c;
	}

	public boolean isAutoSize() {
		return autoSize;
	}

	public void setAutoSize(boolean autoSize) {
		this.autoSize = autoSize;
	}

	public JSONObject getStaticValue() {
		return staticValue;
	}

	public void setStaticValue(JSONObject staticValue) {
		this.staticValue = staticValue;
	}
	
	public boolean isRoot(){
		return (parent == null);
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public IDataSetProvider getDataSetProvider() {
		return dataSetProvider;
	}

	public void setDataSetProvider(IDataSetProvider dataSetProvider) {
		this.dataSetProvider = dataSetProvider;
	}

	public String getDataSetProviderBeanName() {
		return dataSetProviderBeanName;
	}

	public void setDataSetProviderBeanName(String dataSetProviderBeanName) {
		this.dataSetProviderBeanName = dataSetProviderBeanName;
	}

	public Map getDataSetQueryParam() {
		return dataSetQueryParam;
	}

	public void setDataSetQueryParam(Map dataSetQueryParam) {
		this.dataSetQueryParam = dataSetQueryParam;
	}

	public String getRenderClassName() {
		return renderClassName;
	}

	public void setRenderClassName(String renderClassName) {
		this.renderClassName = renderClassName;
	}

	public ComponentRequest getRequest() {
		return request;
	}

	public void setRequest(ComponentRequest request) {
		this.request = request;
	}

	public boolean isDataComponent() {
		return isDataComponent;
	}

	public void setDataComponent(boolean isDataComponent) {
		this.isDataComponent = isDataComponent;
	}

	public ComponentMessage getMessage() {
		return message;
	}

	public void setMessage(ComponentMessage message) {
		this.message = message;
		this.message.setSender(this);
	}

	public boolean isContainer() {
		return isContainer;
	}

	public void setContainer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	public boolean isConfigurable() {
		return configurable;
	}

	public void setConfigurable(boolean configurable) {
		this.configurable = configurable;
	}

	public void setChildren(List<Component> children) {
		this.children = children;
	}

	public String getTheme() {
		if(StringUtil.isEmpty(theme)){
			theme = Theme.Auto.toString().toLowerCase();
		}
		return theme;
	}

	public void setTheme(String theme) {
		if(theme != null) {
			theme = theme.toLowerCase();
		}
		this.theme = theme;
	}

	public Alignment getChildLayoutAlign() {
		return childLayoutAlign;
	}

	public void setChildLayoutAlign(Alignment childLayoutAlign) {
		this.childLayoutAlign = childLayoutAlign;
	}

	public Datasource getDatasource() {
		return datasource;
	}

	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	
}
