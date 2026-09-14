package com.bi.queryer.util.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.api.ComponentRequest;
import com.bi.queryer.util.component.message.ComponentMessage;
import com.bi.queryer.util.component.message.ComponentMessageBody;
import com.bi.queryer.util.component.message.ComponentMessageScope;

/**
 * 组件渲染器 
 * @author contributor
 *
 * @param <T>
 */
public abstract class ComponentRender<T extends Component> {
	protected final Log log = LogFactory.getLog(getClass());
	
	protected T component = null;
	
	/**
	 * 用于html元素的data-options使用
	 */
	protected JSONObject dataOptions = new JSONObject();
	protected JSONArray  dataMsgs = new JSONArray();
	
	
	public ComponentRender(T component){
		this.component = component;
		init();
	}
	
	/**
	 * 构建组件html
	 * @return
	 */
	public String buildHtml(){
		String html = buildBeginHtml() + buildEndHtml();
		return html;
	}
	
	/**
	 * 构建开始html片断代码
	 * @return
	 */
	public String buildBeginHtml(){
		return "";
	}
	
	/**
	 * 构建结束html片断代码
	 * @return
	 */
	public String buildEndHtml(){
		return "";
	}
	
	/**
	 * 构建组件属性选项,用于javascript使用
	 * @return json格式
	 */
	public abstract JSONObject buildOptions();
	
	protected void init(){
		
	}
	
	/**
	 * 格式：data-options='"name1":"value1","name2":"value2"....'
	 * @return
	 */
	public String buildDataOptions(JSONObject json, boolean append){
		StringBuilder html = new StringBuilder();
		if(json == null){
			json = new JSONObject();
		}
		if(append){
			json.put(ComponentConstants.Parameter_Component_ID, component.getId());// 发布时查找使用
			json.put(ComponentConstants.Parameter_Component_Code, component.getCode());// SQL查询使用
			json.put(ComponentConstants.Parameter_Component_Namespace, component.getNamespace());// SQL查询使用
			json.put(ComponentConstants.Parameter_Component_Type, component.getType());// 前台JS初始化数据使用
			if(component.getParent() != null){
				json.put(ComponentConstants.Parameter_Component_PID, component.getParent().getId());
			}
		}
//		json.put(ComponentConstants.Parameter_Module_Id, component.getRequest().getString(ComponentConstants.Parameter_Module_Id));
		json.put("entityId", component.entityId);
		json.put("entityName", component.entityName);
		String opts = json.toString();
		opts = opts.replaceAll("\"", "\\\"");
		html.append(" data-options=").append("'").append(opts).append("' ");
		return html.toString();
	}
	
	public String buildDataOptions(JSONObject json){
		return buildDataOptions(json, true);
	}
	
	protected void buildMessage(){
		ComponentMessage message = component.getMessage();
		if(message == null){
			return ;
		}
		ComponentRequest request = component.getRequest();
		String[] recs = message.getReceivers();
		List<String> receivers = new ArrayList<String>();
		receivers.addAll(Arrays.asList(recs));
		for(Component c : message.getRecevierComponents()){
			if(!receivers.contains(c.getId())){
				receivers.add(c.getId());
			}
		}
		for(String receiver : receivers){
			JSONObject item = new JSONObject();
			if(ComponentMessageScope.Page == message.getScope()){// 如果是页面跳转消息，则需添加其父模块id，用于导航回退
				item.put(ComponentConstants.Parameter_Module_Pid, request.getString(ComponentConstants.Parameter_Module_Id));
			}
			item.put(ComponentConstants.Parameter_Msg_Receiver, receiver);
			item.put(ComponentConstants.Parameter_Msg_Scope, message.getScope().toString());
			for(ComponentMessageBody body : message.getContents()){
				if(StringUtil.isEmpty(body.getName())) {
					continue;
				}
				item.put(body.getName(), body.getValue());
			}
			dataMsgs.add(item);
		}
	}
	
	/**
	 * 构建消息选项
	 * @return
	 */
	public String buildMessageOptions(){
		StringBuilder html = new StringBuilder();
		ComponentMessage message = component.getMessage();
		if(message == null){
			return "";
		}
		buildMessage();
		html.append(" data-msgs=").append("'").append(dataMsgs.toString()).append("' ");
		return html.toString();
	}
	
	/**
	 * 子类组件根据需要构建自己格式json数据
	 * @return
	 */
	public JSONObject buildDataSet(){
		return component.getStaticValue();
	}
	
	@Override
	public String toString() {
		String str = "";
		if(component != null){
			str = component.toString() + "Render";
		}
		return str;
	}
	
	/**
	 * 构建自定义属性
	 * @return
	 */
	public JSONObject buildCustomAttribute(){
		JSONObject attributeObj = new JSONObject();
		try{
			// 添加默认自定义属性
			component.getAttributes().put("guid", component.getGuid());
			component.getAttributes().put("id", component.getId());
			component.getAttributes().put("componentType", component.getType().toString());
			
			for(Entry<String, Object> entry : component.getAttributes().entrySet()){
				attributeObj.put(entry.getKey(), entry.getValue());
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return attributeObj;
	}
	
	public JSONObject getDataOptions() {
		return dataOptions;
	}

	public void setDataOptions(JSONObject dataOptions) {
		this.dataOptions = dataOptions;
	}
}
