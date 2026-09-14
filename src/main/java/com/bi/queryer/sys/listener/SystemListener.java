package com.bi.queryer.sys.listener;

import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.rmi.RMIServiceRegistry;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.parser.ParserConfig;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class SystemListener implements ApplicationListener<ContextRefreshedEvent>{

	@Override
	public void onApplicationEvent(ContextRefreshedEvent appEvent) {
		if(SpringContextUtil.getContext() == null) {
			SpringContextUtil.initApplicationContext(appEvent.getApplicationContext());
			SystemInitializer.initialize();
			RMIServiceRegistry.initialize(); // rmi服务注册

			//打开SafeMode功能
			ParserConfig.getGlobalInstance().setSafeMode(true);

			// 初始化redis缓存客户端
			RuntimeEnv env = BIUtil.getRuntimeEnv();
			if(env == RuntimeEnv.UT || env == RuntimeEnv.Product || env == RuntimeEnv.Dev) {
				RedisCacheManager.getRedisClient();
			}

			System.out.println("...........initialize system config successfully............");
		}
	}

}
