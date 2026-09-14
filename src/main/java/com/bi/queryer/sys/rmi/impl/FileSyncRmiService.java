package com.bi.queryer.sys.rmi.impl;

import com.bi.queryer.sys.rmi.IRMIService;
import com.bi.queryer.sys.rmi.RMI;
import com.bi.queryer.sys.rmi.model.FileSyncRequest;
import com.bi.queryer.sys.rmi.model.FileSyncResponse;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @Author contributor
 * @Date 14:37 2025/1/13
 * @Description 文件同步写入RMI服务
 **/
@RMI
public class FileSyncRmiService extends UnicastRemoteObject implements IRMIService {
    protected FileSyncRmiService() throws RemoteException {
        super();
    }

    @Override
    public Object invoke(Object param) throws RemoteException {
        FileSyncResponse response = new FileSyncResponse();
        if(!(param instanceof FileSyncRequest)){
            return response;
        }
        FileSyncRequest parameter = (FileSyncRequest) param;
        try {
            String operate = parameter.getOperate();
            if(FileSyncRequest.OPERATE_DELETE.equalsIgnoreCase(operate)) {
                FileUtils.deleteQuietly(new File(parameter.getFileFullName()));
            }
            if(FileSyncRequest.OPERATE_WRITE.equalsIgnoreCase(operate)){
                // 写入文件
                FileUtils.writeByteArrayToFile(new File(parameter.getFileFullName()), parameter.getFileContent());
            }
            if(FileSyncRequest.OPERATE_READ.equalsIgnoreCase(operate)){
                File file = new File(parameter.getFileFullName());
                if(file.exists()){
                    byte[] bytes = FileUtils.readFileToByteArray(new File(parameter.getFileFullName()));
                    response.setFileContent(bytes);
                    response.setLastModifiedTime(file.lastModified());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
