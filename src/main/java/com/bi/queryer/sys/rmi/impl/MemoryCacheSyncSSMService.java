package com.bi.queryer.sys.rmi.impl;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import com.bi.queryer.sys.rmi.IRMIService;
import com.bi.queryer.sys.rmi.RMI;
import com.bi.queryer.sys.startup.SystemInitializer;

@RMI
public class MemoryCacheSyncSSMService extends UnicastRemoteObject implements IRMIService{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MemoryCacheSyncSSMService() throws RemoteException {
		super();
	}

	@Override
	public Object invoke(Object param) throws RemoteException {
		SystemInitializer.initialize((Map)param);
		return Boolean.TRUE;
	}
}
