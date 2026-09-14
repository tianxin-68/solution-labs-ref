package com.bi.queryer.sys.authapply;

import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.cache.CacheService;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.dim.vo.Dim;
import com.bi.queryer.sys.role.vo.Role;
import com.bi.queryer.sys.user.constant.AuthApplyType;
import com.bi.queryer.sys.user.constant.SsdDataAuthApplyType;
import com.bi.queryer.sys.user.model.SysAuthApplyRecord;
import com.bi.queryer.sys.user.vo.SsdDataAuthVo;
import com.bi.queryer.sys.user.vo.SsmAuthApplyVo;
import com.bi.queryer.sys.user.vo.UserAuth;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthApplyService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private CacheService cacheService;

    public void authApprove(SysAuthApplyRecord record) {
        if (record == null || record.getApproveFinishTime() != null) {
            return;
        }
        new ApproveThread(dao, cacheService, record).start();
    }

    /**
     * 直接生效行级权限变更（不走工单），用于仅删除行级权限等场景。
     */
    public void applyRowDataAuthImmediately(String userName, List<SsdDataAuthVo> dataAuthList) {
        if (BIUtil.isEmpty(userName) || BIUtil.isEmpty(dataAuthList)) {
            return;
        }
        List<DataAuth> rowDataAuthDeleteList = new ArrayList<>();
        List<DataAuth> rowDataAuthInsertList = new ArrayList<>();
        collectRowDataAuthChanges(userName, dataAuthList, rowDataAuthDeleteList, rowDataAuthInsertList);
        if (rowDataAuthDeleteList.isEmpty() && rowDataAuthInsertList.isEmpty()) {
            return;
        }
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                if (BIUtil.isNotEmpty(rowDataAuthDeleteList)) {
                    dao.delete("dim.deleteUserDataAuthByItems", rowDataAuthDeleteList);
                }
                if (BIUtil.isNotEmpty(rowDataAuthInsertList)) {
                    dao.insert("dim.insertDataAuthWithActiveDurationDays", rowDataAuthInsertList);
                }
            }
        });
        cacheService.flushByUserName(userName);
    }

    private void collectRowDataAuthChanges(String userName,
                                           List<SsdDataAuthVo> dataAuthList,
                                           List<DataAuth> rowDataAuthDeleteList,
                                           List<DataAuth> rowDataAuthInsertList) {
        int rowAuthActiveDays = 9999;
        for (SsdDataAuthVo rowAuth : dataAuthList) {
            if (BIUtil.isEmpty(rowAuth.getItemCode())) {
                continue;
            }
            if (SsdDataAuthApplyType.isDelete(rowAuth.getApplyType())) {
                rowDataAuthDeleteList.add(buildRowDataAuth(userName, rowAuth.getModuleCode(), rowAuth.getModuleName(),
                        rowAuth.getDimCode(), rowAuth.getDimName(), rowAuth.getItemCode(), rowAuth.getItemValue()));
            } else if (SsdDataAuthApplyType.isAdd(rowAuth.getApplyType())) {
                rowDataAuthInsertList.add(buildRowDataAuth(userName, rowAuth.getModuleCode(), rowAuth.getModuleName(),
                        rowAuth.getDimCode(), rowAuth.getDimName(), rowAuth.getItemCode(), rowAuth.getItemValue(),
                        rowAuthActiveDays));
            }
        }
    }

    private DataAuth buildRowDataAuth(String userName, String moduleCode, String moduleName, String dimCode,
                                      String dimName, String itemCode, String dimItemName) {
        DataAuth dataAuth = new DataAuth();
        dataAuth.setAuthId(Guid.id());
        dataAuth.setOwnerType("User");
        dataAuth.setOwnerId(userName);
        dataAuth.setModuleCode(moduleCode);
        dataAuth.setModuleName(moduleName);
        dataAuth.setDimCode(dimCode);
        dataAuth.setDimName(dimName);
        dataAuth.setItemCode(itemCode);
        dataAuth.setItemValue(resolveItemValue(dimItemName));
        dataAuth.setDimDisplayName(buildDimDisplayName(moduleName, dimName, dimItemName));
        dataAuth.setCreatedBy(userName);
        dataAuth.setActiveDurationDays(9999);
        return dataAuth;
    }

    private DataAuth buildRowDataAuth(String userName, String moduleCode, String moduleName, String dimCode,
                                      String dimName, String itemCode, String dimItemName, Integer activeDurationDays) {
        DataAuth dataAuth = buildRowDataAuth(userName, moduleCode, moduleName, dimCode, dimName, itemCode, dimItemName);
        dataAuth.setActiveDurationDays(activeDurationDays);
        return dataAuth;
    }

    private String resolveItemValue(String dimItemName) {
        if (BIUtil.isEmpty(dimItemName)) {
            return "";
        }
        String[] itemNames = dimItemName.split("/");
        return itemNames[itemNames.length - 1];
    }

    private String buildDimDisplayName(String moduleName, String dimName, String dimItemName) {
        if (BIUtil.isEmpty(dimItemName)) {
            return "";
        }
        if (dimItemName.contains("/")) {
            return "【" + moduleName + "/" + dimName + "】"
                    + dimItemName.substring(dimItemName.indexOf("/") + 1);
        }
        return "【" + moduleName + "/" + dimName + "】" + dimItemName;
    }

    class ApproveThread extends Thread {
        private final BaseDao dao;
        private final CacheService cacheService;
        private final SysAuthApplyRecord record;

        ApproveThread(BaseDao dao, CacheService cacheService, SysAuthApplyRecord record) {
            this.dao = dao;
            this.cacheService = cacheService;
            this.record = record;
        }

        @Override
        public void run() {
            synchronized (("auth_apply_" + record.getApplyId()).intern()) {
                SysAuthApplyRecord fresh = queryRecordByApplyId(record.getApplyId());
                if (fresh == null || fresh.getApproveFinishTime() != null) {
                    return;
                }
                Map<String, String> map = new HashMap<>();
                map.put("applyId", record.getApplyId());
                dao.update("authority.updateApplyFinishTime", map);
                record.setApproveFinishTime(new Date());
            }
            AuthApplyType applyType = AuthApplyType.getType(record.getAuthType());
            if (applyType == AuthApplyType.SSM) {
                dealSsmAuth();
            }
        }

        private SysAuthApplyRecord queryRecordByApplyId(String applyId) {
            Map<String, String> map = new HashMap<>();
            map.put("taskId", record.getTaskId());
            return (SysAuthApplyRecord) dao.queryObject("authority.queryApplyRecordByTaskId", map);
        }

        private void dealSsmAuth() {
            List<UserAuth> userAuthList = new ArrayList<>();
            List<Role> menuRole = new ArrayList<>();
            List<Role> ssdRole = new ArrayList<>();
            List<DataAuth> dataAuthInsertList = new ArrayList<>();
            SsmAuthApplyVo ssmAuthApplyVo = JSON.parseObject(record.getAuthInfo(), SsmAuthApplyVo.class);
            if (ssmAuthApplyVo == null) {
                return;
            }

            String menuId = (String) dao.queryObject("menu.queryMenuIdByCode", "ssm");
            if (BIUtil.isNotEmpty(menuId)) {
                UserAuth userAuth = new UserAuth();
                userAuth.setUserName(record.getCreatedBy());
                userAuth.setResId(menuId);
                userAuth.setActiveDurationDays(9999);
                userAuthList.add(userAuth);
            }

            List<SsdDataAuthVo> ctgList = ssmAuthApplyVo.getCtgList();
            if (ctgList == null) {
                ctgList = Collections.emptyList();
            }
            Dim ctgDim = getDim("ssd_ctg", "ssm_ctg");
            if (ctgDim != null) {
                ctgList.forEach(a -> dataAuthInsertList.add(buildDataAuth(ctgDim.getModuleCode(), ctgDim.getModuleName(),
                        ctgDim.getDimCode(), ctgDim.getDimName(), a.getItemCode(), a.getItemValue(),
                        getActiveDurationDays(a.getMonths()))));
            }

            List<SsdDataAuthVo> dataAuthList = ssmAuthApplyVo.getDataAuthList();
            if (dataAuthList == null) {
                dataAuthList = Collections.emptyList();
            }
            List<DataAuth> rowDataAuthDeleteList = new ArrayList<>();
            List<DataAuth> rowDataAuthInsertList = new ArrayList<>();
            collectRowDataAuthChanges(record.getCreatedBy(), dataAuthList, rowDataAuthDeleteList, rowDataAuthInsertList);

            dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                @Override
                public void execute() {
                    if (BIUtil.isNotEmpty(userAuthList)) {
                        this.dao.insert("user.updateUserAuthListByWorkOrder", userAuthList);
                    }
                    insertMenuRole(menuRole);
                    insertSsdRole(ssdRole);
                    if (BIUtil.isNotEmpty(rowDataAuthDeleteList)) {
                        dao.delete("dim.deleteUserDataAuthByItems", rowDataAuthDeleteList);
                    }
                    if (BIUtil.isNotEmpty(dataAuthInsertList)) {
                        dao.insert("dim.insertDataAuthWithActiveDurationDays", dataAuthInsertList);
                    }
                    if (BIUtil.isNotEmpty(rowDataAuthInsertList)) {
                        dao.insert("dim.insertDataAuthWithActiveDurationDays", rowDataAuthInsertList);
                    }
                }
            });
            cacheService.flushByUserName(record.getCreatedBy());
        }

        private void insertMenuRole(List<Role> roleList) {
            if (BIUtil.isNotEmpty(roleList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("roles", roleList);
                map.put("userName", record.getCreatedBy());
                map.put("activeDuration", 9999);
                dao.insert("role.updateUserRoleByWorkOrder", map);
            }
        }

        private void insertSsdRole(List<Role> roleList) {
            if (BIUtil.isNotEmpty(roleList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("roles", roleList);
                map.put("userName", record.getCreatedBy());
                dao.insert("role.insertSsdRole", map);
            }
        }

        private Dim getDim(String moduleCode, String dimCode) {
            Map<String, String> map = new HashMap<>();
            map.put("moduleCode", moduleCode);
            map.put("dimCode", dimCode);
            return (Dim) dao.queryObject("dim.getDimInfoByCode", map);
        }

        private int getActiveDurationDays(Integer activeMonth) {
            if (activeMonth == null || activeMonth > 12) {
                activeMonth = 12;
            }
            if (activeMonth < 0) {
                activeMonth = 0;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MONTH, activeMonth);
            return (int) ((calendar.getTimeInMillis() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24));
        }

        private DataAuth buildDataAuth(String moduleCode, String moduleName, String dimCode, String dimName,
                                       String itemCode, String dimItemName) {
            DataAuth dataAuth = new DataAuth();
            dataAuth.setAuthId(Guid.id());
            dataAuth.setOwnerType("User");
            dataAuth.setOwnerId(record.getCreatedBy());
            dataAuth.setModuleCode(moduleCode);
            dataAuth.setModuleName(moduleName);
            dataAuth.setDimCode(dimCode);
            dataAuth.setDimName(dimName);
            dataAuth.setItemCode(itemCode);
            dataAuth.setItemValue(resolveItemValue(dimItemName));
            dataAuth.setDimDisplayName(buildDimDisplayName(moduleName, dimName, dimItemName));
            dataAuth.setCreatedBy(record.getCreatedBy());
            dataAuth.setActiveDurationDays(9999);
            return dataAuth;
        }

        private DataAuth buildDataAuth(String moduleCode, String moduleName, String dimCode, String dimName,
                                       String itemCode, String dimItemName, Integer activeDurationDays) {
            DataAuth dataAuth = buildDataAuth(moduleCode, moduleName, dimCode, dimName, itemCode, dimItemName);
            dataAuth.setActiveDurationDays(activeDurationDays);
            return dataAuth;
        }

        private String resolveItemValue(String dimItemName) {
            if (BIUtil.isEmpty(dimItemName)) {
                return "";
            }
            String[] itemNames = dimItemName.split("/");
            return itemNames[itemNames.length - 1];
        }

        private String buildDimDisplayName(String moduleName, String dimName, String dimItemName) {
            if (BIUtil.isEmpty(dimItemName)) {
                return "";
            }
            if (dimItemName.contains("/")) {
                return "【" + moduleName + "/" + dimName + "】"
                        + dimItemName.substring(dimItemName.indexOf("/") + 1);
            }
            return "【" + moduleName + "/" + dimName + "】" + dimItemName;
        }
    }
}
