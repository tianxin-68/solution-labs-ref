package com.bi.queryer.sys.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRMIService extends Remote{
	//public RMIServiceType getServiceType() throws RemoteException;
	public Object invoke(Object param) throws RemoteException;
	
}
