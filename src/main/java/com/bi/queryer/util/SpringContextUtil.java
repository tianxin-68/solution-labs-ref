package com.bi.queryer.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringContextUtil  implements ApplicationContextAware {
	
	private static ApplicationContext cxt;
	
	public static Object getBean(String beanId) {
		/*
		SpringServiceLocator s = SpringServiceLocator.getInstance();
		return s.getContext().getBean(beanName);
		*/
		/*if(cxt == null){
			cxt = SpringServiceLocator.getInstance().getContext();
		}*/
		if (cxt == null) {
			@SuppressWarnings("resource")
			ApplicationContext classPathCxt = new ClassPathXmlApplicationContext(new String[]{"spring-applicationContext.xml", "spring-db-pool.xml"});
			Object bean = classPathCxt.getBean(beanId);
			return bean;
           // throw new NullPointerException("ApplicationContext is null");
        }
        return cxt.getBean(beanId);
	}

//	public static void setApplicationContext(ApplicationContext springCxt) throws BeansException {
//		cxt = springCxt;
//	}
	
	public static ApplicationContext getContext(){
		return cxt;
	}

	@Override
	public void setApplicationContext(ApplicationContext springCxt) throws BeansException {
		cxt = springCxt;
	}
	
	public static void initApplicationContext(ApplicationContext springCxt) throws BeansException {
		cxt = springCxt;
	}
}