package com.bi.queryer.sys.rmi;

import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * rmi服务
 * @author contributor
 *
 */
public class RMIServer {

	/**
	 * 多服务器同步调用
	 *
	 * @param param
	 * @return
	 */
	public static Map<String, Object> syncInvoke(Class<? extends IRMIService> serviceClass, Object param) {
		Map<String, Object> result = new ConcurrentHashMap<>();
		String[] serverIPTable = BIUtil.getServerIPTable();
		List<CompletableFuture<Void>> futureList = new ArrayList<>(serverIPTable.length);
		for (String ip : serverIPTable) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					IRMIService rmi = (IRMIService) RMIServiceRegistry.getService(serviceClass, ip);
					Object r = rmi.invoke(param);
					result.put(ip, r);
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
			futureList.add(future);
		}

		CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
		return result;
	}

	public static Object syncInvoke(Class<? extends IRMIService> serviceClass, String ip, Object param) {
		try {
			IRMIService rmi = (IRMIService) RMIServiceRegistry.getService(serviceClass, ip);
			return rmi.invoke(param);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
