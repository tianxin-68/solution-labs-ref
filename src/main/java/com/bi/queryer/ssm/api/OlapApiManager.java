package com.bi.queryer.ssm.api;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.entity.OlapApiKeyEntity;
import com.bi.queryer.ssm.api.entity.OlapApiKeyUserEntity;
import com.bi.queryer.ssm.api.entity.OpenapiUserHostnameMappingEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class OlapApiManager {

    //key = api_key:user_name
    private static Map<String, OlapApiKeyUserEntity> apiKeyUserMap = new HashMap();

    //key = user_name, value = api_key，用于getApiKeyByUserName快速查找及回写DB兜底查询结果，避免缓存穿透
    private static final Map<String, String> userNameApiKeyMap = new ConcurrentHashMap<>();

    //负缓存：记录DB也查不到的user_name，容量上限10000、60秒过期，防止不存在的user_name反复穿透查库
    private static final Cache<String, Boolean> apiKeyNotFoundCache = CacheUtil.newLRUCache(10000, 60_000);

    //validate()中DB兜底查询命中后会回写，非initialize()单线程场景，需用线程安全实现
    private static Map<String, OlapApiKeyEntity> apiKeyMap = new ConcurrentHashMap<>();

    //key = user_name:hostname，个人扩展机器映射；仅 initialize() 全量加载，未命中不查库
    private static final Map<String, Boolean> userHostnameMappingMap = new ConcurrentHashMap<>();

    public static void initialize() {
        BaseDao dao = DBUtil.getBaseDao();
        if (apiKeyUserMap != null) {
            apiKeyUserMap.clear();
        }
        userNameApiKeyMap.clear();

        List<OlapApiKeyUserEntity> list = (List<OlapApiKeyUserEntity>) dao.queryObjectList("ssm.olap.api.queryAllApiKeyUser", new HashMap<>());

        if (CollUtil.isNotEmpty(list)) {
            for (OlapApiKeyUserEntity olapApiKeyUserEntity : list) {
                String key = olapApiKeyUserEntity.getApiKey() + ":" + olapApiKeyUserEntity.getUserName();
                apiKeyUserMap.put(key, olapApiKeyUserEntity);
                userNameApiKeyMap.put(olapApiKeyUserEntity.getUserName(), olapApiKeyUserEntity.getApiKey());
            }
        }

        if (apiKeyMap != null) {
            apiKeyMap.clear();
        }
        List<OlapApiKeyEntity> apiKeyList = (List<OlapApiKeyEntity>) dao.queryObjectList("ssm.olap.api.queryAllApiKey", new HashMap<>());

        if (CollUtil.isNotEmpty(apiKeyList)) {
            for (OlapApiKeyEntity olapApiKeyEntity : apiKeyList) {
                apiKeyMap.put(olapApiKeyEntity.getApiKey(), olapApiKeyEntity);
            }
        }

        userHostnameMappingMap.clear();
        List<OpenapiUserHostnameMappingEntity> mappingList =
                (List<OpenapiUserHostnameMappingEntity>) dao.queryObjectList("ssm.olap.api.queryAllUserHostnameMapping", new HashMap<>());
        if (CollUtil.isNotEmpty(mappingList)) {
            for (OpenapiUserHostnameMappingEntity mapping : mappingList) {
                if (StrUtil.isEmpty(mapping.getUserName()) || StrUtil.isEmpty(mapping.getHostname())) {
                    continue;
                }
                userHostnameMappingMap.put(buildUserHostnameKey(mapping.getUserName(), mapping.getHostname()), Boolean.TRUE);
            }
        }
    }

    /**
     * 判断用户-机器名扩展映射是否存在（仅读本地缓存，未命中不查库）
     *
     * @param userName 用户名
     * @param hostname 机器名（tag，不含 .local）
     * @return 缓存中存在则 true
     */
    public static boolean hasUserHostnameMapping(String userName, String hostname) {
        if (StrUtil.isEmpty(userName) || StrUtil.isEmpty(hostname)) {
            return false;
        }
        return userHostnameMappingMap.containsKey(buildUserHostnameKey(userName, hostname));
    }

    private static String buildUserHostnameKey(String userName, String hostname) {
        return userName + ":" + hostname;
    }

    /**
     * 获取用户最大查询行数
     * @param apiKey
     * @return
     */
    public static Integer getUserMaxQueryRowNum(String apiKey) {

        Integer olapApiRowLimit = Integer.parseInt(SC.v("ssm.olap.api.search.limit", "500000"));

        //ssm-server.example.com 域名为多维公用集群，需要按默认行数限制
        //olap-api.example.com 域名为多维API集群，需要按olap-api-user配置行数限制
        //不同集群通过机器ip区分
        String serverIP = BIUtil.getServerIP();
        String ssmServerIPs = SC.v("ssm.olap.api.server.iptable", "");
        if (StrUtil.isNotEmpty(ssmServerIPs)) {
            List<String> olapApiServerIpList = Arrays.asList(ssmServerIPs.split(","));
            if (!olapApiServerIpList.contains(serverIP)) {
                return olapApiRowLimit;
            }
        }

        User user = UserManager.get();
        String key = apiKey + ":" + user.getName();
        OlapApiKeyUserEntity olapApiKeyUserEntity = apiKeyUserMap.get(key);
        if (olapApiKeyUserEntity != null) {
            if (olapApiKeyUserEntity.getMaxQueryRowNum() != null && olapApiKeyUserEntity.getMaxQueryRowNum() > 0) {
                olapApiRowLimit = olapApiKeyUserEntity.getMaxQueryRowNum();
            }
        }

        return olapApiRowLimit;

    }


    /**
     * 获取api key信息
     * @param apiKey
     * @return
     */
    public static OlapApiKeyEntity get(String apiKey) {
        return apiKeyMap.get(apiKey);
    }

    /**
     * 将DB兜底查询命中的api_key信息回写缓存，避免重复查库
     * @param apiKey
     * @param entity
     */
    public static void put(String apiKey, OlapApiKeyEntity entity) {
        if (StrUtil.isNotEmpty(apiKey) && entity != null) {
            apiKeyMap.put(apiKey, entity);
        }
    }

    /**
     * 根据用户名获取其绑定的api key（不含user_name=all的公共key）
     * 用户绑定了多个api_key时，取任意一个
     * @param userName
     * @return 用户绑定的api_key，未绑定则返回null
     */
    public static String getApiKeyByUserName(String userName) {
        String apiKey = userNameApiKeyMap.get(userName);
        if (apiKey != null) {
            return apiKey;
        }

        // 命中负缓存，说明近期已确认该user_name在DB中也不存在，直接返回null，避免穿透
        if (apiKeyNotFoundCache.containsKey(userName)) {
            return null;
        }

        // 缓存未命中，查库兜底（应对initialize()刷新周期内新增的绑定关系），命中后回写缓存避免重复查库
        BaseDao dao = DBUtil.getBaseDao();
        apiKey = (String) dao.queryObject("ssm.olap.api.queryApiKeyByUserName", userName);
        if (apiKey != null) {
            userNameApiKeyMap.put(userName, apiKey);
        } else {
            apiKeyNotFoundCache.put(userName, Boolean.TRUE);
        }
        return apiKey;
    }

}
