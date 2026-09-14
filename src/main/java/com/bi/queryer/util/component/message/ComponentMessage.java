package com.bi.queryer.util.component.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bi.queryer.util.component.Component;

/**
 * 组件消息：暂时只支持单个接受者
 * @author contributor
 *
 */
public class ComponentMessage implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	protected String id = "msg";
	
	protected Component sender = null;
	
	protected String senderId = null;
	
	protected List<String> receivers = new ArrayList<String>();
	
	protected List<Component> recevierComponents = new ArrayList<Component>();
	
	/**
	 * 消息发送范围:
	 * scope=Page，则receiver必须为报表beanName
	 * scope=Component，则receiver为组件或组件ID
	 */
	protected ComponentMessageScope scope = ComponentMessageScope.Component;
	
	protected List<ComponentMessageBody> bodys = new ArrayList<ComponentMessageBody>();
	
	public ComponentMessage(){
		
	}
	public ComponentMessage(String id){
		this.id = id;
	}
	
	public ComponentMessage(ComponentMessageScope scope){
		this.scope = scope;
	}
	
	@Override
	public String toString() {
		String sid = senderId;
		if(sender != null){
			sid = sender.getId();
		}
		return "id:" + id + "\t sender:" + sid;
	}
	
	/**
	 * 如果scope=page时，receiver为page的beanName
	 * @param c
	 * @return
	 */
	public ComponentMessage addReceiver(String receiverId){
		if(receivers.contains(receiverId)){
			receivers.remove(receiverId);
		}
		receivers.add(receiverId);
		return this;
	}
	
	/**
	 * @param c
	 * @return
	 */
	public ComponentMessage addReceiver(Component c){
		recevierComponents.add(c);
		return this;
	}
	
	public ComponentMessage addContent(ComponentMessageBody body){
		if(bodys.contains(body)){
			bodys.remove(body);
		}
		bodys.add(body);
		return this;
	}
	
	public ComponentMessage addContent(String messageName, String messageDefaultValue){
		ComponentMessageBody body = new ComponentMessageBody(messageName, messageDefaultValue);
		return addContent(body);
	}
	public ComponentMessage addContent(String messageName, String messageDefaultValue, String valueType){
		ComponentMessageBody body = new ComponentMessageBody(messageName, messageDefaultValue);
		body.setValueType(valueType);
		return addContent(body);
	}
	
	public void destroy(){
		sender = null;
		receivers.clear();
		bodys.clear();
	}
	
	public ComponentMessage clone(){
		ComponentMessage copy = new ComponentMessage();
		copy.id = this.id;
		copy.sender = this.sender;
		copy.senderId = this.senderId;
		copy.receivers = new ArrayList<String>();
		copy.receivers.addAll(this.receivers);
		copy.recevierComponents = new ArrayList<Component>();
		copy.recevierComponents.addAll(this.recevierComponents);
		copy.scope = this.scope;
		copy.bodys = new ArrayList<ComponentMessageBody>();
		if(this.bodys != null){
			for(ComponentMessageBody body : this.bodys){
				copy.addContent(body.clone());
			}
		}
		return copy;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		ComponentMessage msg = (ComponentMessage) obj;
		return this.id.equals(msg.getId());
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Component getSender() {
		return sender;
	}

	public void setSender(Component sender) {
		this.sender = sender;
		this.senderId = sender.getId();
	}
	
	public void setSender(String senderId){
		this.senderId = senderId;
	}

	public ComponentMessageScope getScope() {
		return scope;
	}

	public void setScope(ComponentMessageScope scope) {
		this.scope = scope;
	}

	public ComponentMessageBody[] getContents() {
		ComponentMessageBody[] bs = new ComponentMessageBody[bodys.size()];
		bodys.toArray(bs);
		return bs;
	}

	public void setBodys(List<ComponentMessageBody> bodys) {
		this.bodys = bodys;
	}

	public String[] getReceivers() {
		String[] rs = new String[receivers.size()];
		return receivers.toArray(rs);
	}

	public void setReceivers(List<String> receivers) {
		this.receivers = receivers;
	}
	public List<Component> getRecevierComponents() {
		return recevierComponents;
	}
	public void setRecevierComponents(List<Component> recevierComponents) {
		this.recevierComponents = recevierComponents;
	}
	public String getSenderId() {
		return senderId;
	}
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}
}
