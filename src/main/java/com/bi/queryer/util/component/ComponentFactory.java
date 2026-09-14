package com.bi.queryer.util.component;

import java.lang.reflect.InvocationTargetException;

import com.bi.queryer.util.BIUtil;


public abstract class ComponentFactory {
	/**
	 * 创建组件
	 * @param type
	 * @return
	 */
	public static Component create(ComponentType type){
		Component c = null;
		if(type == ComponentType.Auto ||
				type == ComponentType.None){
			return c;
		}
		try {
			c = (Component)Class.forName(type.getModelClass()).newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return c;
	}
	
	public static XmlHelper<?> getXmlHelper(ComponentType componentType){
		XmlHelper<?> helper = null;
        Component c = create(componentType);
		XmlHelperType type = XmlHelperType.getType(c.getType().toString());
		if(type == null){
			return null;
		}
		try {
			String xmlHelperClassName = type.getXmlHelperClass();
			Class<?> xmlHelper = Class.forName(xmlHelperClassName);
			helper = (XmlHelper<?>) (xmlHelper.getConstructor(c.getClass()).newInstance(c));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return helper;
	}
	
	public static XmlHelper<?> getXmlHelper(Component c){
		XmlHelper<?> helper = null;
		if(c == null) {
			return null;
		}
		XmlHelperType type = XmlHelperType.getType(c.getType().toString());
		try {
			String xmlHelperClassName = type.getXmlHelperClass();
			Class<?> xmlHelper = Class.forName(xmlHelperClassName);
			helper = (XmlHelper<?>) (xmlHelper.getConstructor(c.getClass()).newInstance(c));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return helper;
	}	
	
	public static ComponentRender<?> getRender(Component c){
		ComponentRender<?> render = null;
		if(c == null) {
			return null;
		}
		ComponentType type = c.getType();
		try {
			String renderClassName = c.getRenderClassName();
			if(BIUtil.isEmpty(renderClassName)){
				renderClassName = type.getRenderClass();
			}
			Class<?> renderClass = Class.forName(renderClassName);
			render = (ComponentRender<?>) (renderClass.getConstructor(c.getClass()).newInstance(c));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return render;
	}
	
	public static ComponentRender<?> createRender(ComponentType type){
		ComponentRender<?> render = null;
		Component c = create(type);
		if(c == null) {
			return null;
		}
		render = getRender(c);
		return render;
	}
	
	/**
	 * 通过实体创建组件
	 * @param entityId
	 * @return
	 */
	public static Component createByEntity(ComponentType ctype, String entityId){
		Component c = null;
		/*
		switch(ctype) {
		case DataGrid:
			DataGridDesignerService ds = new DataGridDesignerService();
			c = ds.getDataGrid(entityId);
			break;
		case Filter:
			FilterDesignerService fs = new FilterDesignerService();
			c = fs.getFilter(entityId);
			break;
		default:
			break;
		}
		*/
		return c;
	}
	
	public static void main(String[] args) {
		Component c = create(ComponentType.DataGrid);
		System.out.println(c);
		ComponentRender<?> render = getRender(c);
		System.out.println(render);
		
	}
}
