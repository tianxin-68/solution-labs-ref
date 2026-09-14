package com.bi.queryer.sys.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.bi.queryer.sys.env.EnvVariableManager;

public class StartUp implements ServletContextListener{

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		start(event);
	}
	
	protected void start(ServletContextEvent event){
		EnvVariableManager.initialize(event);
	}
}
