package com.bi.queryer.sys.menu;

import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserService;
import com.bi.queryer.sys.user.model.HrEmployee;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MenuService {

    private static final String ROOT_CTG_ID = "-1";

    @Autowired
    private BaseDao dao;

    @Autowired
    private UserService userService;

    /**
     * 查找目录 owner：
     * 1. 分析师（rpt_dev_owner）：当前目录 → 父目录
     * 2. 开发owner（data_dev_owner）：当前目录 → 父目录
     * 3. 兜底人
     */
    public String ssmCtgOwnerByCtgId(String ctgId, String defaultOwner) {
        if (BIUtil.isEmpty(ctgId) || ROOT_CTG_ID.equals(ctgId)) {
            return defaultOwner;
        }
        String analyst = ctgOwner(queryCtgOwnerInfoWithParent(ctgId), null);
        if (analyst != null) {
            return analyst;
        }
        String devOwner = ctgOwner(queryCtgDevOwnerInfoWithParent(ctgId), null);
        if (devOwner != null) {
            return devOwner;
        }
        return defaultOwner;
    }

    /**
     * 查找目录 业务owner：
     * 1. 业务负责人（biz_owner）：当前目录或模块
     * 2. 分析师（rpt_dev_owner）：当前目录 → 父目录
     * 3. 兜底人
     */
    public String ssmCtgBusinessOwnerByCtgId(String ctgId, String defaultOwner) {
        if (BIUtil.isEmpty(ctgId) || ROOT_CTG_ID.equals(ctgId)) {
            return defaultOwner;
        }
        String businessOwner = ctgOwner(queryCtgBusinessOwnerInfoWithParent(ctgId), null);
        if (businessOwner != null) {
            return businessOwner;
        }
        String analyst = ctgOwner(queryCtgOwnerInfoWithParent(ctgId), null);
        if (analyst != null) {
            return analyst;
        }
        return defaultOwner;
    }

    public String queryCtgOwnerLeader(String ctgOwnerEmail) {
        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", ctgOwnerEmail);
        HrEmployee ctgOwnerHrEmployee = (HrEmployee) dao.queryObject("user.queryEmployeeByEmail", emailMap);
        String managerEmail = SC.v("ssd.default.approver", "contributor@example.com");
        if (ctgOwnerHrEmployee != null) {
            String managerUserName = ctgOwnerHrEmployee.getManagerUserName();
            if (BIUtil.isNotEmpty(managerUserName)) {
                User manager = userService.queryByName(managerUserName);
                if (manager != null && BIUtil.isNotEmpty(manager.getEmail())) {
                    managerEmail = manager.getEmail();
                }
            }
        }
        return managerEmail;
    }

    public String getDirectLeaderEmail(HrEmployee hrEmployee) {
        if (hrEmployee != null && BIUtil.isNotEmpty(hrEmployee.getManagerUserName())) {
            User manager = userService.queryByName(hrEmployee.getManagerUserName());
            if (manager != null && BIUtil.isNotEmpty(manager.getEmail())) {
                return manager.getEmail();
            }
        }
        String defaultLeader = SC.v("ssm.default.directLeader", "");
        if (BIUtil.isNotEmpty(defaultLeader)) {
            return defaultLeader;
        }
        return SC.v("ssd.default.approver", "contributor@example.com");
    }

    private String queryCtgOwnerInfoWithParent(String ctgId) {
        String ctgOwnerInfo = (String) dao.queryObject("menu.querySsdCtgOwnerWithParentByCtgId",
                ctgId, DataSourceType.Default);
        if (BIUtil.isEmpty(ctgOwnerInfo)) {
            ctgOwnerInfo = (String) dao.queryObject("menu.querySsdCtgOwnerWithParentByCtgId",
                    ctgId, SSDUtil.getMgpDataSourceType());
        }
        if (BIUtil.isNotEmpty(ctgOwnerInfo)) {
            String[] owners = ctgOwnerInfo.split(",");
            if (owners.length > 0) {
                ctgOwnerInfo = owners[0];
            }
        }
        return ctgOwnerInfo;
    }


    private String queryCtgDevOwnerInfoWithParent(String ctgId) {
        String devOwnerInfo = (String) dao.queryObject("menu.querySsdCtgDevOwnerWithParentByCtgId",
                ctgId, DataSourceType.Default);
        if (BIUtil.isEmpty(devOwnerInfo)) {
            devOwnerInfo = (String) dao.queryObject("menu.querySsdCtgDevOwnerWithParentByCtgId",
                    ctgId, SSDUtil.getMgpDataSourceType());
        }
        if (BIUtil.isNotEmpty(devOwnerInfo)) {
            String[] owners = devOwnerInfo.split(",");
            if (owners.length > 0) {
                devOwnerInfo = owners[0];
            }
        }
        return devOwnerInfo;
    }

    private String queryCtgBusinessOwnerInfoWithParent(String ctgId) {
        String businessOwnerInfo = (String) dao.queryObject("menu.querySsdCtgBusinessOwnerByCtgId",
                ctgId, DataSourceType.Default);
        if (BIUtil.isEmpty(businessOwnerInfo)) {
            businessOwnerInfo = (String) dao.queryObject("menu.querySsdCtgBusinessOwnerByCtgId",
                    ctgId, SSDUtil.getMgpDataSourceType());
        }
        if (BIUtil.isNotEmpty(businessOwnerInfo)) {
            String[] owners = businessOwnerInfo.split(",");
            if (owners.length > 0) {
                businessOwnerInfo = owners[0];
            }
        }
        return businessOwnerInfo;
    }

    private String ctgOwner(String ctgOwnerInfo, String defaultOwner) {
        try {
            if (BIUtil.isNotEmpty(ctgOwnerInfo)) {
                if (ctgOwnerInfo.contains("（") && ctgOwnerInfo.contains("）")) {
                    ctgOwnerInfo = ctgOwnerInfo.substring(ctgOwnerInfo.indexOf("（") + 1, ctgOwnerInfo.indexOf("）"));
                }
                if (BIUtil.isNotEmpty(ctgOwnerInfo)) {
                    User user = userService.queryByName(ctgOwnerInfo);
                    if (user != null) {
                        return user.getEmail();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return defaultOwner;
    }
}
