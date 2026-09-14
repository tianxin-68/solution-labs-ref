package com.bi.queryer.sys.config;

import com.bi.queryer.ssm.api.OlapApiManager;
import com.bi.queryer.ssm.system.access.SystemAccessBlacklistManager;
import com.bi.queryer.ssm.engine.acl.AclManager;
import com.bi.queryer.ssm.engine.interceptor.QueryInterceptorManager;
import com.bi.queryer.ssm.engine.analysis.lunaryearweek.LunarYearWeekManager;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.sys.config.option.OptionService;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 系统配置
 * 
 * @author contributor
 *
 */
public class SystemConfig implements Initializable{
	
	protected static Log logger = LogFactory.getLog(SystemConfig.class);
	
	public static String appName = "";
	
	public static String appMobileName = "";

	public static boolean isDebug = false;
	
	public static boolean isLocal = false; // 本地开发模式

	public static String serverPort = "";

	public static String uploadDir = "";
	
	public static String theme = "";
	
	public static Map<String, SystemOption> options = new LinkedHashMap<String, SystemOption>();
	
	public static String version = "";
	
//	public static String serverIPTable[] = null; // 服务器ip列表
	
	public static void initialize() {
		try{
			options.clear();
			isLocal = "local".equalsIgnoreCase(System.getProperty("bi.dev"));
			isDebug = "debug".equalsIgnoreCase(System.getProperty("bi.env"));

			//加载sys_option
			OptionService service = (OptionService) SpringContextUtil.getBean("optionService");
			List<SystemOption> listOption = service.getAllSystemOption();
			for (SystemOption systemOption:listOption) {
				options.put(systemOption.getCode(),systemOption);
			}

			// 加载系统配置文件
			loadProperties("/app.properties");
			
			String jdbcFileName = BIUtil.getJDBCFileName();
			if(BIUtil.isNotEmpty(jdbcFileName)) {
				loadProperties("/" + jdbcFileName);
			}
			
			appName = value("app.name", "BI数据服务");
			theme = value("app.theme", "default").toLowerCase();
			serverPort = value("server.port", "8080");
			uploadDir = value("upload.dir", "");
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String defaultValue = sdf.format(Calendar.getInstance().getTime());
			version = value("app.version", defaultValue);
			
			RuntimeEnv env = RuntimeEnv.getEnv(value("app.runtime.env"));
			isDebug = (env != RuntimeEnv.Product);

			// 加载session settings
			QuerySessionSettingManager.initialize();

			// 加载权限黑白名单
			AclManager.refresh();

			// 查询拦截器缓存
			QueryInterceptorManager.refreshConfigureCache();

			//业务日历配置
			PromotionManager.refresh();

			//农历年周配置
			LunarYearWeekManager.refresh();

			//apikey配置
			OlapApiManager.initialize();

			//系统访问黑名单
			SystemAccessBlacklistManager.initialize();

		}catch(Exception e){
			e.printStackTrace();
			logger.error(e);
		}
	}
	
	protected static void loadProperties(String fileName){
		Properties pop = new Properties();
		try {
			pop.load(SystemConfig.class.getResourceAsStream(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("加载配置文件" + fileName + "出错");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("加载配置文件" + fileName + "出错");
		}
		for(Object key : pop.keySet()){
			SystemOption opt = new SystemOption();
			opt.setCode(key + "");
			opt.setValue(pop.getProperty(key + ""));
			options.put(opt.getCode(), opt);
		}
	}

	public static String value(String key) {
		return value(key, "");
	}
	
	public static String value(String key, String defaultValue){
		if(options == null || !options.containsKey(key)){
			return defaultValue;
		}
		String value = options.get(key).getValue();
		if(StringUtil.isEmpty(value)){
			return defaultValue;
		}
		
		return value;
	}

	public void initialize(Map<String, ?> initParams) throws BIException {
		initialize();
	}
	
	/**
	 * 设置
	 * @param key
	 * @param value
	 */
	public static void set(String key, String value){
		if(!options.containsKey(key)){
			SystemOption opt = new SystemOption();
			opt.setCode(key);
			opt.setValue(value);
			options.put(key, opt);
		}else{
			options.get(key).setValue(value);
		}
	}
	
}
