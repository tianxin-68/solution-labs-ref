package com.bi.queryer.ssm.engine.accelerate.cache;

import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.FileSyncRmiService;
import com.bi.queryer.sys.rmi.model.FileSyncRequest;
import com.bi.queryer.sys.rmi.model.FileSyncResponse;
import com.bi.queryer.util.BIUtil;
import com.github.luben.zstd.Zstd;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Author contributor
 * @Date 14:02 2025/1/13
 * @Description 本地缓存管理器
 **/
public abstract class LocalCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalCacheManager.class);
    public static long addCache(String cacheContent, String cacheFileName){
        if(BIUtil.isEmpty(cacheFileName) || BIUtil.isEmpty(cacheContent)){
            return -1;
        }
        // 写入文件
        long cacheSize = -1L;
        try {
            String cacheFileFullName = getFileFullName(cacheFileName);
            // 压缩
            // cacheContent = SSDUtil.compress(cacheContent);
            //FileSyncOperation syncOperation = new FileSyncOperation(FileSyncOperation.OPERATE_WRITE, fileFullName, compressArray);
            //RMIServer.syncInvoke(FileSyncRmiService.class, syncOperation);

            // 写单服务器
            byte[] compressArray = Zstd.compress(cacheContent.getBytes(Charset.forName("UTF-8")));
            // 默认20M
            Integer maxSize = SC.getInteger("ssm.local.cache.max.size.mb", 20) ;
            if(compressArray.length > maxSize * 1024 * 1024){
                logger.error("{}本地缓存写入跳过,压缩后大小超过{}M:{}", cacheFileName , maxSize, compressArray.length/1024/1024);
                return -1;
            }
            File compressFile = new File(cacheFileFullName);
            writeCacheFile(compressFile, compressArray);
            cacheSize = compressFile.length();

            // fileName + ip写入redis
            Integer localCacheTtlSeconds = SC.getInteger("ssm.local.cache.ttl.seconds", 60 * 60 * 2);
            RedisCacheManager.set(cacheFileFullName, BIUtil.getServerIP(), localCacheTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cacheSize;
    }

    private static void writeCacheFile(File cacheFile, byte[] content) throws IOException {
        File parentFile = cacheFile.getParentFile();
        if(parentFile != null && !parentFile.exists()){
            parentFile.mkdirs();
        }
        File tempFile = new File(cacheFile.getAbsolutePath() + "." + Thread.currentThread().getId() + ".tmp");
        FileUtils.writeByteArrayToFile(tempFile, content);
        try {
            Files.move(tempFile.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String getCache(String cacheFileName){
        return getCache(cacheFileName, -1);
    }

    public static String getCache(String cacheFileName, long expireSecondAfterWrite){
        long t1 = System.currentTimeMillis();
        if(BIUtil.isEmpty(cacheFileName)){
            return null;
        }
        String cacheContent = null;
        // 读取文件
        try {

            String cacheFileFullName = getFileFullName(cacheFileName);
            String cacheServerIp = RedisCacheManager.get(cacheFileFullName);
            if(BIUtil.isEmpty(cacheServerIp)){
                return null;
            }
            String currentServerIp = BIUtil.getServerIP();

            // 先从本机读取
            if(currentServerIp.equals(cacheServerIp)){
                return getCacheFromCurrentServer(cacheFileFullName, expireSecondAfterWrite);
            }else {
                return getCacheFromRemoteServer(cacheServerIp, cacheFileFullName, expireSecondAfterWrite);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long t2 = System.currentTimeMillis();
        System.out.println(String.format("LocalFileCacheManager.getCache(%s)耗时%s(ms)", cacheFileName, (t2 - t1)));
        return cacheContent;
    }

    /**
     * 从远程服务器获取文件
     * @param remoteServerIp
     * @param cacheFileName
     * @param expireSecondAfterWrite
     * @return
     */
    protected static String getCacheFromRemoteServer(String remoteServerIp, String cacheFileName, long expireSecondAfterWrite){
        FileSyncResponse response = (FileSyncResponse) RMIServer.syncInvoke(FileSyncRmiService.class, remoteServerIp, new FileSyncRequest(FileSyncRequest.OPERATE_READ, cacheFileName));
        if(response == null || response.getFileContent() == null || response.getFileContent().length == 0){
            return null;
        }
        // 判断是否过期
        if(expireSecondAfterWrite > 0) {
            boolean isOld = System.currentTimeMillis() - response.getLastModifiedTime() > expireSecondAfterWrite * 1000; //FileUtils.isFileOlder(file, System.currentTimeMillis() - expireSecondAfterWrite * 1000);
            if (isOld) {
                return null;
            }
        }
        return decompress(response.getFileContent(), cacheFileName);
    }


    /**
     * 通过当前服务器获取缓存文件
     * @param cacheFileName
     * @param expireSecondAfterWrite
     * @return
     * @throws IOException
     */
    protected static String getCacheFromCurrentServer(String cacheFileName, long expireSecondAfterWrite) throws IOException {
        File file = new File(cacheFileName);
        if(!file.exists()){
            return null;
        }
        if(expireSecondAfterWrite > 0) {
            boolean isOld = FileUtils.isFileOlder(file, System.currentTimeMillis() - expireSecondAfterWrite * 1000);
            if (isOld) {
                return null;
            }
        }
        byte[] content = FileUtils.readFileToByteArray(file);
        if(content == null || content.length == 0){
            return null;
        }
        String cacheContent = decompress(content, cacheFileName);
        if(cacheContent == null){
            FileUtils.deleteQuietly(file);
        }
        return cacheContent;
    }

    protected static String decompress(byte[] content) {
        return decompress(content, "");
    }

    private static String decompress(byte[] content, String cacheFileName) {
        if(content == null || content.length == 0){
            return null;
        }
        try {
            long decompressedSize = Zstd.decompressedSize(content);
            if(decompressedSize <= 0 || decompressedSize > Integer.MAX_VALUE){
                logger.error(cacheFileName + "本地缓存解压失败,非法解压大小:" + decompressedSize);
                return null;
            }
            int size = (int) decompressedSize;
            byte[] decompressArray = new byte[size];
            Zstd.decompress(decompressArray, content);
            return new String(decompressArray, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            logger.error(cacheFileName + "本地缓存解压失败", e);
            return null;
        }
    }

    public static void deleteCache(String cacheFileName){
        if(BIUtil.isEmpty(cacheFileName)){
            return;
        }
        try {
            String fileFullName = getFileFullName(cacheFileName);

            FileSyncRequest syncOperation = new FileSyncRequest(FileSyncRequest.OPERATE_DELETE, fileFullName);
            RMIServer.syncInvoke(FileSyncRmiService.class, syncOperation);

            //FileUtils.deleteQuietly(new File(fileFullName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteCache(Set<String> cacheFileNames){
        for(String cacheFileName : cacheFileNames) {
            deleteCache(cacheFileName);
        }
    }

    protected static String getFileFullName(String cacheFileName){
        if(BIUtil.isEmpty(cacheFileName)){
            return cacheFileName;
        }
        if(!cacheFileName.contains(".")){
            cacheFileName = cacheFileName + ".txt";
        }
        String fileFullName = "";
        if(!cacheFileName.contains("/")){
            String fileDir = getFileDir();
            fileFullName = fileDir + "/" + cacheFileName;
        }else {
            fileFullName = cacheFileName;
        }
        return fileFullName;
    }

    public static String getFileDir(){
        String dirName = SC.v("local.cache.file.dir", "/tmp/ssm/local/cache");
        File file = new File(dirName);
        if(!file.exists()){
            file.mkdirs();
        }
        if(dirName.endsWith("/")){
            // 去掉后面斜杠
            dirName = dirName.substring(0, dirName.length() - 1);
        }
        return dirName;
    }
}
