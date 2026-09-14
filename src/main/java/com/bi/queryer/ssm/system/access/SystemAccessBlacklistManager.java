package com.bi.queryer.ssm.system.access;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 系统访问黑名单（启动时全量加载，与 {@link com.bi.queryer.ssm.api.OlapApiManager} 相同策略）。
 */
public abstract class SystemAccessBlacklistManager {

    private static HashSet<String> blackUserNameList = new HashSet<>();

    public static void initialize() {
        BaseDao dao = DBUtil.getBaseDao();
        blackUserNameList.clear();
        List<String> list =
                (List<String>) dao.queryObjectList("ssm.system.access.blacklist.queryAll", new HashMap<>());
        if (CollUtil.isNotEmpty(list)) {
            blackUserNameList.addAll(list);
        }
    }

    /**
     * 用户名是否在系统访问黑名单中（与库中 user_name 精确一致）。
     */
    public static boolean isBlacklisted(String userName) {
        if (StrUtil.isEmpty(userName)) {
            return false;
        }
        return blackUserNameList.contains(userName);
    }


}
