package com.bi.queryer.sys.rmi.impl;

import com.bi.queryer.sys.base.proxy.ConnectionProxy;
import com.bi.queryer.sys.rmi.IRMIService;
import com.bi.queryer.sys.rmi.RMI;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * @Auther: contributor
 * @Date: 2024/8/2 16:35
 * @Description:
 */
@RMI
public class QueryKillRmiService extends UnicastRemoteObject implements IRMIService {
    private static final long serialVersionUID = 1L;

    public QueryKillRmiService() throws RemoteException {
        super();
    }
    @Override
    public Object invoke(Object param) throws RemoteException {
        Map<String, String> map = (Map<String, String>) param;
        ConnectionProxy.killQuery(map.get("sessionId") + "");
        return true;
    }
}
