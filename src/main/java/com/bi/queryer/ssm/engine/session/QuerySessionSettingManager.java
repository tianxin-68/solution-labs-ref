package com.bi.queryer.ssm.engine.session;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:01 2024-04-15
 * @Description 查询session setting管理器
 **/
public abstract class QuerySessionSettingManager {
    public static List<QuerySessionSetting> allSettings = new ArrayList<>();

    public static void initialize() {
        BaseDao dao = DBUtil.getBaseDao();
        if(allSettings != null){
            allSettings.clear();
        }
        allSettings = (List<QuerySessionSetting>) dao.queryObjectList("ssm.session.getAllSettings", new HashMap<>());
    }

    /**
     * 获取查询超时时间
     * @return
     */
    public static Integer getQueryTimeoutSec(){
        User user = UserManager.get();
        List<QuerySessionSetting> userSettings = allSettings.stream()
                        .filter(f->f.getOptionCode().equalsIgnoreCase("query.timeout.sec"))
                        .filter(f->user != null && f.getUserName().equalsIgnoreCase(user.getName())).collect(Collectors.toList());

        if(BIUtil.isEmpty(userSettings)){
            return BIConsts.QUERY_TIME_OUT_SEC;
        }

        Integer timeout = 0;
        for(QuerySessionSetting s : userSettings){
            Integer v = Integer.valueOf(s.getOptionValue());
            if(v > timeout){
                timeout = v;
            }
        }

        if(timeout == null || timeout <= 0) {
            timeout = BIConsts.QUERY_TIME_OUT_SEC;
        }
        if(timeout > BIConsts.QUERY_MAX_TIME_OUT_SEC){
            timeout = BIConsts.QUERY_MAX_TIME_OUT_SEC;
        }
        return timeout;
    }

    /**
     * 获取session查询属性参数
     * @param userName
     * @param templateId
     * @return
     */
    public static List<QuerySessionProperty> getSessionProperties(String userName, String templateId){
        List<QuerySessionProperty> properties = new ArrayList<>();

        /*
        BaseDao dao = DBUtil.getBaseDao();
        Map<String, String> params = new HashMap<>();
        if(BIUtil.isNotEmpty(userName)) {
            params.put("userName", userName);
        }
        if(BIUtil.isNotEmpty(templateId)) {
            params.put("templateId", templateId);
        }
        List<QuerySessionSetting> options = (List<QuerySessionSetting>) dao.queryObjectList("ssm.session.getSessionProperties", params);
        if(BIUtil.isEmpty(options)){
            return properties;
        }
        */
        if(BIUtil.isEmpty(allSettings)){
            return properties;
        }

        List<QuerySessionSetting> propertySettings = new ArrayList<>();
        for(QuerySessionSetting s : allSettings){
            if(!s.getOptionCode().equalsIgnoreCase("query.session.property")){
                continue;
            }
            QuerySessionSettingScope scope = QuerySessionSettingScope.get(s.getScope());
            if(scope == QuerySessionSettingScope.All
                    || (BIUtil.isNotEmpty(userName) && userName.equalsIgnoreCase(s.getUserName()))
                    || (BIUtil.isNotEmpty(templateId) && userName.equalsIgnoreCase(s.getTemplateId()))){
                propertySettings.add(s);
            }
        }

        for(QuerySessionSetting propertySetting : propertySettings){
            String optionValueStr = propertySetting.getOptionValue();
            String[] optionValues = optionValueStr.split(";");
            String scope = propertySetting.getScope();
            for(String optionValue : optionValues){
                QuerySessionProperty property = new QuerySessionProperty();
                property.setScope(scope);
                String[] keyValue = optionValue.trim().split("=");
                if(keyValue.length != 2){
                    continue;
                }
                property.setKey(keyValue[0].trim());
                property.setValue(keyValue[1].trim());
                property.setSupportQueryEngines(propertySetting.getSupportQueryEngines());
                properties.add(property);
            }
        }

        // 按scope优先级排序：all < user < template
        Collections.sort(properties, new Comparator<QuerySessionProperty>() {
            @Override
            public int compare(QuerySessionProperty o1, QuerySessionProperty o2) {
                int sortId1 = QuerySessionSettingScope.get(o1.getScope()).getSortId();
                int sortId2 = QuerySessionSettingScope.get(o2.getScope()).getSortId();
                return sortId2 - sortId1;
            }
        });

        // 按scope优先级排序去重：all < user < template
        List<QuerySessionProperty> distinctProperties = new ArrayList<>();
        for(QuerySessionProperty p : properties){
            if(distinctProperties.contains(p)){
                continue;
            }
            distinctProperties.add(p);
        }
        properties = distinctProperties;
        return properties;
    }

    /**
     * 获取查询行数限制
     * @return
     */
    public static Integer getQueryRowLimit(){
        Integer defaultRowLimit = Integer.parseInt(SC.v("ssm.search.limit", "20000"));
        User user = UserManager.get();
        List<QuerySessionSetting> userSettings = allSettings.stream()
                .filter(f->f.getOptionCode().equalsIgnoreCase("query.row.limit"))
                .filter(f->user != null && f.getUserName().equalsIgnoreCase(user.getName())).collect(Collectors.toList());

        if(BIUtil.isEmpty(userSettings)){
            return defaultRowLimit;
        }

        Integer rowLimit = defaultRowLimit;
        for(QuerySessionSetting s : userSettings){
            Integer v = Integer.valueOf(s.getOptionValue());
            if(v != null){
                rowLimit = v;
                break;
            }
        }

        if(rowLimit == null || rowLimit <= 0) {
            rowLimit = defaultRowLimit;
        }
        return rowLimit;
    }
}
