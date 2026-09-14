package com.bi.queryer.sys.rmi;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.annotation.AnnotationScanner;


public class RMIServiceRegistry {
	
	public static Integer port = 8083;
	
	/**
	 * 此处必须使用静态变量，避免gc回收
	 */
	public static Map<String, Remote> services = new HashMap<String, Remote>();
	
	public static String ip = "";
	
	/**
	 * 注册服务
	 */
	public static void initialize(){
		try {
			
			ip = BIUtil.getServerIP();
			// 设置本机的rmi地址
			System.setProperty("java.rmi.server.hostname", ip);
			
			port = BIUtil.nvl(SC.v("ssm.server.rmi.port"), port);
			LocateRegistry.createRegistry(port);
			
			String pkgName = RMIServiceRegistry.class.getPackage().getName() + ".impl";
	        // 查找包含RestController和Controller注解的类
			Set<Class<?>> rmiClassSet = AnnotationScanner.scanClassesByAnnotations(pkgName, true,  RMI.class);
	        if (rmiClassSet == null || rmiClassSet.isEmpty()){
	            return;
	        }
	        rmiClassSet.forEach(clazz -> {
	        	String clazzName = BIUtil.firstLowerCase(clazz.getSimpleName());
	        	
	        	Remote service;
				try {
					service = createService(clazzName);
					String url = "rmi://" + ip + ":" + port + "/" + clazzName;
					Naming.rebind(url, service);
					System.out.println("rmi monitor service:" + url);
				} catch (Exception e) {
					e.printStackTrace();
				}
	        });
		} catch (Exception e) {
			System.err.println("rmi service registe error : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建服务
	 * @param serviceType
	 * @return
	 * @throws RemoteException
	 */
	public static Remote createService(String serviceName) throws RemoteException{
		Remote service = services.get(serviceName);
		if(service == null){
			service = (Remote) SpringContextUtil.getBean(serviceName);
			services.put(serviceName, service);
		}
		return service;
	}
	
	/**
	 * 获取服务
	 * @param serviceType
	 * @return
	 * @throws Exception
	 */
	public static Remote getService(Class<? extends IRMIService> serviceClass) throws Exception{
		return getService(serviceClass, ip);
	}
	
	public static Remote getService(Class<? extends IRMIService> serviceClass, String ip) throws Exception{
		String serviceName = BIUtil.firstLowerCase(serviceClass.getSimpleName());
		return getService(serviceName, ip);
	}
	
	/**
	 * 获取服务
	 * @param serviceType
	 * @return
	 * @throws Exception
	 */
	public static Remote getService(String serviceName) throws Exception{
		return getService(serviceName, ip);
	}
	
	public static Remote getService(String serviceName, String ip) throws Exception{
		Remote service = Naming.lookup("rmi://" + ip + ":" + port + "/" + serviceName);
		return service;
	}
	
	
}
