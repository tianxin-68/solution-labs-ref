package com.bi.queryer.ssm.custom.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.meta.MetaFieldAuthWhiteList;
import com.bi.queryer.util.Guid;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.InitializableModule;
import com.bi.queryer.sys.startup.SystemInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class CustomAuthService {

    private static final List<String> TARGET_ROLE_NAMES = Arrays.asList("城市经理", "战区经理"); // 目标角色
    private static final String SYS_CREATED_BY = "sys"; // 系统同步创建人标识

    @Autowired
    private BaseDao dao;

    /**
     * 同步城市经理/战区经理权限
     * <p>
     * 按「城市经理」「战区经理」角色查出用户后，在同一事务内：
     * 1）删除 created_by=sys 的字段权限白名单，再为上述用户批量插入 shop_cnt/all 白名单；
     * 2）从系统访问黑名单中移除这些用户。
     * 事务提交后通过 RMI 广播刷新所有机器的 SystemConfig 相关内存缓存（含 Acl 与黑名单），使权限立即生效。
     * </p>
     *
     * @return 同步结果（含处理用户数）
     */
    public SSMResponseMessage syncCityRegionManagerAuth() {
        // 1. 按角色查用户（事务外只读查询）
        List<String> userNames = (List<String>) dao.queryObjectList("ssm.role.ext.queryUserNamesByRoleNames", TARGET_ROLE_NAMES);
        userNames = CollUtil.isEmpty(userNames) ? Collections.emptyList()
                : userNames.stream().filter(StrUtil::isNotEmpty).distinct().collect(Collectors.toList());

        List<String> finalUserNames = userNames;
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                // 2. 白名单：先删 created_by = sys，再按用户批量插入
                dao.delete("ssm.field.deleteFieldAuthWhiteListByCreatedBy", SYS_CREATED_BY, DataSourceType.Default);
                if (CollUtil.isNotEmpty(finalUserNames)) {
                    List<MetaFieldAuthWhiteList> entities = finalUserNames.stream().map(u -> {
                        MetaFieldAuthWhiteList w = new MetaFieldAuthWhiteList();
                        w.setPkid(Guid.id());
                        w.setUserName(u);
                        w.setFieldCode("all");
                        w.setFieldTitle("all");
                        w.setFieldGroup("shop_cnt");
                        w.setCreatedBy(SYS_CREATED_BY);
                        return w;
                    }).collect(Collectors.toList());
                    dao.insert("ssm.field.batchInsertFieldAuthWhiteList", entities, DataSourceType.Default);
                }

                // 3. 黑名单：删除这些用户
                if (CollUtil.isNotEmpty(finalUserNames)) {
                    dao.delete("ssm.system.access.blacklist.deleteByUserNames", finalUserNames, DataSourceType.Default);
                }
            }
        });

        // 4. 事务提交后通过 RMI 广播刷新所有机器的内存缓存，权限立即生效
        Map<String, String> rmiParamMap = new HashMap<>();
        rmiParamMap.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.SystemConfig.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, rmiParamMap);

        return SSMResponseMessage.success("同步城市经理/战区经理权限成功，共处理 " + userNames.size() + " 个用户");
    }

}
