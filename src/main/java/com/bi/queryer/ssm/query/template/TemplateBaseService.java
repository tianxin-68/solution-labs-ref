package com.bi.queryer.ssm.query.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.encryption.AES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class TemplateBaseService {
    @Autowired
    protected BaseDao dao = null;

    public static String encryptStr = "AES:";

    public static String keyByteStr = "pYyPs7JhbzPpd7Hf";

    /**
     * 根据模版ID查询模版详细信息
     * @param req
     * @return
     */
    public List<TemplateRsp> getTemplateRspList(TemplateShareReq req) {
        return (List<TemplateRsp>) dao.queryObjectList("ssm.template.queryTemplateByIds", req.getTplIdList());
    }

    /**
     * 将传入的用户列表规范化
     * @param userNames
     * @return
     */
    public List<String> normalizeUserNames(String userNames) {
        List<String> result = new ArrayList<>();
        if (BIUtil.isEmpty(userNames)) {
            return result;
        }

        String[] replaceSymbols = new String[]{"，", ";", "；", "\n", "\r\n", " ", "@example.com", "@bi_queryer.com"};
        for (String s : replaceSymbols) {
            userNames = userNames.replace(s, ",");
        }
        result = Arrays.asList(userNames.split(","));
        result = result.stream().filter(s -> !BIUtil.isEmpty(s)).collect(Collectors.toList());
        return result;
    }

    /**
     * 根据用户名查询用户信息
     * @param userNames
     * @return
     */
    public List<User> getUserListByNames(String userNames) {
        List<String> nameList = normalizeUserNames(userNames);
        return (List<User>) dao.queryObjectList("user.queryUserListByNames", nameList);
    }


    /**
     * 根据用户名查询用户信息
     * @param userNames
     * @return
     */
    public List<User> getUserListByNames(List<String> userNames) {
        if(CollectionUtil.isEmpty(userNames)){
            return new ArrayList<>();
        }
        return (List<User>) dao.queryObjectList("user.queryUserListByNames", userNames);
    }

    /**
     * 获取部门下的所有用户（含子孙部门）
     * @param deptId
     * @return
     */
    public List<User> getUserListByDept(String deptId) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("deptId", deptId);
        List<String> userNames = (List<String>)dao.queryObjectList("user.queryUserNamesByDeptId", queryParams);
        return getUserListByNames(userNames);
    }

    /**
     * 获取部门相关信息
     */
    public Map<String, DeptEntity> getDeptMap(String deptId) {
        List<String> deptIds = ListUtil.list(false);
        deptIds.add(deptId);
        return getDeptMap(deptIds);
    }

    /**
     * 获取部门相关信息
     */
    public Map<String, DeptEntity> getDeptMap(List<String> deptIds) {
        List<DeptEntity> deptList = (List<DeptEntity>) dao.queryObjectList("user.queryDeptListByIds", deptIds);
        return deptList.stream().collect(Collectors.toMap(DeptEntity::getDeptId, k -> k));
    }

    public String decryptTplConfig(TemplateAddReq req) {
        try {
            String tplConfig = req.getTplConfig();
            if (tplConfig.contains(encryptStr)) {
                tplConfig = tplConfig.replace(encryptStr, "");
                byte[] keyByte = keyByteStr.getBytes("utf-8");
                tplConfig = new String(AES.decrypt(tplConfig, keyByte), "utf-8");
            }
            return tplConfig;
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 通过模板id查询模板更新时间
     * @return
     */
    public List<TemplateEntity> batchQueryTemplateUpdateTime(List<String> tplIdList) {

        List<TemplateEntity> templateEntityList = new ArrayList<>();

        if (CollUtil.isEmpty(tplIdList)) {
            return templateEntityList;
        }

        templateEntityList = (List<TemplateEntity>)dao.queryObjectList("ssm.template.batchQueryTemplateUpdateTime",tplIdList);

        return templateEntityList;
    }

}
