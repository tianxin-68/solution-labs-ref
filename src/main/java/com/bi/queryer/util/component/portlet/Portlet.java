package com.bi.queryer.util.component.portlet;

import java.io.StringReader;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.Component;
import com.bi.queryer.util.component.ComponentConstants;
import com.bi.queryer.util.component.SizeMode;
import com.bi.queryer.util.component.exception.ComponentException;

/**
 * portlet基类<br/>
 * 继承此类的组件可在设计器中被配置<br/>
 * @author contributor
 *
 */
public abstract class Portlet extends Component{
	
	private static final long serialVersionUID = 1L;

	// 描述
	protected String desc = "";
	
	// 所属分组ID
	protected String categoryId = "";
	
	// 所属分组名称
	protected String categoryName = "";
	
	protected SizeMode widthMode = SizeMode.Auto;
	
	/**
	 * 通过计算后的实际宽度：实际宽度-左右边距
	 */
	protected int realWidth = -1;
	
	/**
	* 通过计算后的实际高度：实际高度-左右边距
	*/
	protected int realHeight = -1;
	
	protected SizeMode heightMode = SizeMode.Auto;
	
	private PortletMargin margin = new PortletMargin();
	
	protected String parentId = "";
	
	/**
	 * 是否是根节点
	 */
	protected boolean isRoot = false;
	
	/**
	 * portlet渲染模式
	 */
	protected PortletRenderMode renderMode = PortletRenderMode.Publisher;
	
	public Portlet(){
		super();
		this.configurable = true;
	}
	
	/*@Override
	public Portlet getParent() {
		return (Portlet) super.getParent();
	}*/
	
	public Portlet[] getChildrenPortlet() {
		Component[] src = super.getChildren();
		Portlet[] result = new Portlet[src.length];
		for(int i = 0; i < src.length; i++){
			result[i] = (Portlet) src[i];
		}
		return result;
	}
	
	@Override
	public void clone(Component c, boolean cascade) {
		try {
			Portlet p = (Portlet) c;
			super.clone(c, cascade);
			p.desc = this.desc;
			p.categoryId = this.categoryId;
			p.categoryName = this.categoryName;
			p.widthMode = this.widthMode;
			p.heightMode = this.heightMode;
			p.margin = this.margin.clone();
			p.parentId = this.parentId;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 子类实现克隆
	public Portlet clone() throws PortletException{
		
	}
	 */
	
	/**
	 * 设计模式下工具栏的高度
	 * @return
	 */
	public int getDesignerBarHeight(){
		return 0;
	}
	
	/**
	 * 获取已被占用的高度
	 * @return
	 */
	public int getOccupiedHeight(PortletRenderMode mode){
		int height = 0;
		if(mode == PortletRenderMode.Designer){
			// bar + 左右边框
			height = getDesignerBarHeight();
			height = height + 1 + 1;
		}
		return height;
	}
	
	public int getOccupiedWidth(PortletRenderMode mode){
		int width = 0;
		if(mode == PortletRenderMode.Designer){
			// 左右边框
			width = 1 + 1;
		}
		return width;
	}
	
	/**
	 * 对象序列化为xml
	 * @param e
	 */
	public String toXML(){
		Element root = DocumentHelper.createElement("Portlet");  
		save(root);
		String xml = root.asXML();
		return xml;
	}
	
	/**
	 * @param xml  portlet xml
	 */
	@Override
	public void load(String xml) {
		try {
			SAXReader saxReader = new SAXReader();
			Document document = saxReader.read(new StringReader(xml));
			Element root = document.getRootElement();
			if(root != null) {
				load(root);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void load(Element e) {
		if(e == null){
			return;
		}
		this.id = e.attributeValue("id", this.id);
		this.name = e.attributeValue("name", this.name);
		
		this.parentId = e.attributeValue("parentId", this.parentId);
		this.namespace = e.attributeValue("namespace", this.namespace);
		
		this.dataSetProviderBeanName = e.attributeValue("dataSetProviderBeanName", this.dataSetProviderBeanName);
		
		this.desc = e.elementText("Desc");
		Element child = e.element("Category");
		if(child != null){
			this.categoryId = child.attributeValue("id", this.categoryId);
			this.categoryName = child.attributeValue("name", this.categoryName);
		}
		
		child = e.element("Height");
		if(child != null){
			this.height = Integer.valueOf(child.attributeValue("value", this.height + ""));
			this.heightMode = SizeMode.getMode(child.attributeValue("mode"));
		}
		
		child = e.element("Width");
		if(child != null){
			this.width = Integer.valueOf(child.attributeValue("value", this.width + ""));
			this.widthMode = SizeMode.getMode(child.attributeValue("mode"));
		}
		
		child = e.element("Margin");
		this.margin = new PortletMargin();
		this.margin.load(child);
		
		child = e.element("Ref");
		if(child != null){
			this.entityId = child.attributeValue("id");
			
			this.entityName = child.attributeValue("name");
			// 兼容处理,支持HTML标签
			Element nameEle = e.element("Name");
			if(nameEle != null) {
				String _name = nameEle.getText();
				if(StringUtil.isEmpty(_name)) {
					this.entityName = _name;
				}
			}
		}
		
		// 子节点
		child = e.element("Children");
		if(child != null){
			List<?> childEles = child.elements("Portlet");
			if(childEles != null){
				for(int i = 0; i < childEles.size(); i++){
					Element c = (Element) childEles.get(i);
					String portletType = c.attributeValue("type");
					Portlet childPortlet = null; //PortletRegistry.getPortlet(portletType);
					// 递归加载
					childPortlet.load(c);
					this.addChild(childPortlet);
				}
			}
		}
		
	}
	
	@Override
	public void save(Element e) {
		if(e == null) return;
		e.addAttribute("id", this.id);
		e.addAttribute("name", this.name);
		
		e.addAttribute("namespace", this.namespace);
		e.addAttribute("parentId", this.parentId);
		e.addAttribute("type", this.getType().toString());
		if(!StringUtil.isEmpty(this.getDataSetProviderBeanName())) {
			e.addAttribute("dataSetProviderBeanName", this.getDataSetProviderBeanName());
		}
		
		Element child = e.addElement("Desc");
		if(!StringUtil.isEmpty(this.desc)){
			child.setText(this.desc);
		}
		
		child = e.addElement("Category");
		child.addAttribute("id", categoryId);
		child.addAttribute("name", categoryName);
		
		child = e.addElement("Height");
		child.addAttribute("value", height +"");
		child.addAttribute("mode", heightMode.toString());
		
		child = e.addElement("Width");
		child.addAttribute("value", width + "");
		child.addAttribute("mode", widthMode.toString());
		
		child = e.addElement("Margin");
		this.margin.save(child);
		
		child = e.addElement("Ref");
		if(child != null){
			child.addAttribute("id", entityId);
			
			Element nameEle = child.addElement("Name");
			//child.addAttribute("name", entityName);
			nameEle.addCDATA(entityName);
		}
		
		
		// 子节点
		Element childrenEle = e.addElement("Children");
		for(Component p : this.children){
			Element portletEle = childrenEle.addElement("Portlet");
			p.save(portletEle);
		}
	}
	
	/**
	 * 添加子节点
	 * @param child
	 */
	public void addChild(Component child){
		if(child instanceof Portlet){
			Portlet p = (Portlet) child;
			if(!StringUtil.isEmpty(this.id)){
				p.parentId = this.id;
			}
			p.parent = this;
			p.renderMode = this.renderMode;
			super.addChild(p);
		}else {
			super.addChild(child);
		}
	}
	
	/**
	 * 删除子节点
	 * @param portlet
	 * @return
	 */
	public boolean removeChild(Portlet portlet){
		return this.children.remove(portlet);
	}
	
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public SizeMode getWidthMode() {
		return widthMode;
	}

	public void setWidthMode(SizeMode widthMode) {
		this.widthMode = widthMode;
	}
	
	public int getRealWidth() {
		if(realWidth <= 0){
			return width;
		}
		return realWidth;
	}

	public void setRealWidth(int realWidth) {
		this.realWidth = realWidth;
	}

	public int getRealHeight() {
		if(realHeight <= 0){
			return height;
		}
		return realHeight;
	}

	public void setRealHeight(int realHeight) {
		this.realHeight = realHeight;
	}

	public SizeMode getHeightMode() {
		return heightMode;
	}

	public void setHeightMode(SizeMode heightMode) {
		this.heightMode = heightMode;
	}

	public PortletMargin getMargin() {
		return margin;
	}

	public void setMargin(PortletMargin margin) {
		this.margin = margin;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public PortletRenderMode getRenderMode() {
		return renderMode;
	}

	public void setRenderMode(PortletRenderMode renderMode) {
		this.renderMode = renderMode;
	}

	public boolean isRoot() {
		return ComponentConstants.PORTLET_ROOT_ID.equalsIgnoreCase(this.id);
	}
	
	/**
	 * 构建portlet在设计模式下的json格式
	 * @return
	 * @throws ComponentException
	 */
	public JSONObject toDesignerJSON() throws ComponentException {
		JSONObject json = new JSONObject();
		json.put("id", this.getId());
		json.put("name", this.getName());
		json.put("type", this.getType().toString());
		json.put("typeName", this.getType().getDesc());
		json.put("categoryId", this.getCategoryId());
		json.put("width", this.getWidth());
		json.put("widthMode", this.getWidthMode());
		json.put("height", this.getHeight());
		json.put("heightMode", this.getHeightMode());
		json.put("margin", this.getMargin());
		JSONObject ref = new JSONObject();
		ref.put("id", this.getEntityId());
		ref.put("name", this.getEntityName());
		json.put("ref", ref);
		return json;
	}
}
