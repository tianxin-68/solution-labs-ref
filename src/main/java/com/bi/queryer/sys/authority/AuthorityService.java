package com.bi.queryer.sys.authority;

import com.bi.queryer.sys.authapply.AuthApplyService;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgAuthAcl;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.model.SysAuthApplyRecord;
import com.bi.queryer.sys.user.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthorityService {

    private static final long serialVersionUID = 1L;

    @Autowired
    private BaseDao dao = null;

    @Autowired
    private AuthApplyService authApplyService;

    /**
     * 获取所有数据权限item_code,item_value
     *
     * @param userName
     * @param moduleCode
     * @param dimCode
     * @return
     */
    public List<KeyValuePair> getDataItems(String userName, String moduleCode, String dimCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("moduleCode", moduleCode);
        map.put("dimCode", dimCode);

        User user = UserManager.get();
        map.put("deptId", user == null ? "" : user.getDeptId());
        List<KeyValuePair> list = (List<KeyValuePair>) dao.queryObjectList("authority.getDataItems", map);
        return list;
    }

    /**
     * 获取数据权限 item_code 及权限结束时间
     *
     * @param userName   用户名
     * @param moduleCode 模块编码
     * @param dimCode    维度编码
     * @return 目录权限及结束时间列表
     */
    public List<CtgAuthAcl> getDataItemsWithAuthEndDate(String userName, String moduleCode, String dimCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("moduleCode", moduleCode);
        map.put("dimCode", dimCode);

        User user = UserManager.get();
        map.put("deptId", user == null ? "" : user.getDeptId());
        return (List<CtgAuthAcl>) dao.queryObjectList("authority.getDataItemsWithAuthEndDate", map);
    }

    public ResponseMessage authApplyApprove(String taskId) {
        SysAuthApplyRecord record = queryAuthApplyRecord(taskId);
        if (record == null) {
            return new ResponseMessage(false);
        }
        if (record.getApproveFinishTime() != null) {
            return new ResponseMessage(true, "该工单已处理", null);
        }
        authApplyService.authApprove(record);
        return new ResponseMessage(true);
    }

    public SysAuthApplyRecord queryAuthApplyRecord(String taskId) {
        Map<String, String> map = new HashMap<>();
        map.put("taskId", taskId);
        return (SysAuthApplyRecord) dao.queryObject("authority.queryApplyRecordByTaskId", map);
    }

}
