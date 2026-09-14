package com.bi.queryer.sys.startup;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;

/**
 * 系统初始化器
 * @author contributor
 *
 */
public abstract class SystemInitializer {
	
	public static final String INIT_MODULE_KEY = "_modules_key";
	
	/**
	 * 初始化
	 * @param initParams
	 */
	public static void initialize(Map<String, ?> initParams) throws BIException{
		if(initParams == null || initParams.isEmpty()){
			return;
		}
		String initModuleCode = (String) initParams.get(INIT_MODULE_KEY);
		if(StringUtil.isEmpty(initModuleCode)){
			return;
		}
		InitializableModule initModule = InitializableModule.get(initModuleCode);
		if(initModule == InitializableModule.Unknow){
			throw new BIException("初始化" + initModuleCode + "失败：未知模块");
		}
		for(InitializableModule module : InitializableModule.values()){

			Initializable i = null;

			String className = module.getClassName();
			String constructer = module.getConstructer();
			String name = module.name();
			String value = BIUtil.getRuntimeEnv().getValue();
			if(StringUtil.isEmpty(className)){
				continue;
			}
			try{
				if("default".equalsIgnoreCase(constructer)){
					i = (Initializable)Class.forName(className).newInstance();
				}else if("spring".equalsIgnoreCase(constructer)){
					i = (Initializable) SpringContextUtil.getBean(className);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			if(i == null){
				continue;
			}
			try{
				if(initModule == InitializableModule.All){
					i.initialize(initParams);
				}else{
					if(initModule == module){
						i.initialize(initParams);
					}
				}
			}catch(Exception e){
				throw new BIException(e);
			}
		}
	}

	/**
	 * 初始化所有
	 */
	public static void initialize(){
		initialize(InitializableModule.All);
	}
	
	/**
	 * 同步其他服务器缓存刷新
	 */
	public static void syncInitialize(HttpServletRequest request){
		// 先刷新自己
		String localIP = BIUtil.getServerIP();
		try {
			SystemInitializer.initialize();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		// 同时刷新其他服务器
		if(RuntimeEnv.Product == BIUtil.getRuntimeEnv()) {
			String[] serverIPTable = BIUtil.getServerIPTable();
			for(String ip : serverIPTable){
				if(ip.equals(localIP)){
					continue;
				}
				try{
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void initialize(String module){
		Map<String, String> modules = new HashMap<String, String>();
		modules.put(INIT_MODULE_KEY, module);
		initialize(modules);
	}
	
	/**
	 * 初始化指定模块
	 * @param module
	 */
	public static void initialize(InitializableModule module){
		initialize(module.toString());
	}
}
