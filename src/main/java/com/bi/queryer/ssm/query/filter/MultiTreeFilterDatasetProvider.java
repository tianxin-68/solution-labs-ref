package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 12:45
 * Description:
 */
@Service
@Scope("prototype")
public class MultiTreeFilterDatasetProvider implements IFilterDatasetProvider{

    protected MetaField metaField = null;
    protected Map<String, Object> params = null;

    public static final String Root_Id = "-1.1";

    @Override
    public Object buildDataset(MetaField metaField, Map<String, Object> params) {
        this.metaField = metaField;
        this.params = params;

        // 构建sql
        String sql = buildTreeSQL();

        // 查询数据
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<BIMap> dataset = dao.queryMapListBySQL(sql, DBUtil.getDataSourceType());

        // 构建tree
        List<JSONObject> tree = buildTree(dataset);

        return tree;
    }

    /**
     * 构建字段树
     * @return
     */
    public List<JSONObject> buildTree(List<BIMap> dataset) {
        List<JSONObject> tree = new ArrayList<>();
        if(BIUtil.isEmpty(dataset)) {
            return tree;
        }

        this.buildTreeCascade(dataset, Root_Id, tree);
        return tree;
    }

    /**
     * 递归构建树
     * @param tree
     */
    protected void buildTreeCascade(List<BIMap> dataset, String parentId, List<JSONObject> tree){
        if(BIUtil.isEmpty(dataset)) {
            return;
        }
        List<BIMap> levelDataset = dataset.stream().filter(map -> {
            return parentId.equals(map.get("parentId") + "");
        }).collect(Collectors.toList());
        if(BIUtil.isEmpty(levelDataset)) {
            return;
        }
        for(BIMap c : levelDataset){
            JSONObject node = new JSONObject();
            node.putAll(c);
            List<JSONObject> childTree = new ArrayList<>();
            String childId = c.get("id") + "";
            buildTreeCascade(dataset, childId, childTree);
            node.put("children", childTree);
            tree.add(node);
        }
    }

    /**
     * 构建tree sql
     * @return
     */
    protected String buildTreeSQL(){
        HierarchyInfo hierarchy = SSDMetaCacheManager.getHierarchy(metaField.getId());
        if(hierarchy == null) {
            return null;
        }
        List<Level> levels = new ArrayList<>();
        levels.add(new Level(hierarchy.getLevel1FieldId(), hierarchy.getLevel1FieldKeyName(),  null, hierarchy.getLevel1FieldName(), 1));
        levels.add(new Level(hierarchy.getLevel2FieldId(),hierarchy.getLevel2FieldKeyName(),  hierarchy.getLevel1FieldKeyName(), hierarchy.getLevel2FieldName(), 2));
        levels.add(new Level(hierarchy.getLevel3FieldId(),hierarchy.getLevel3FieldKeyName(),  hierarchy.getLevel2FieldKeyName(), hierarchy.getLevel3FieldName(), 3));
        levels.add(new Level(hierarchy.getLevel4FieldId(),hierarchy.getLevel4FieldKeyName(),  hierarchy.getLevel3FieldKeyName(), hierarchy.getLevel4FieldName(),4));
        levels.add(new Level(hierarchy.getLevel5FieldId(),hierarchy.getLevel5FieldKeyName(),  hierarchy.getLevel4FieldKeyName(), hierarchy.getLevel5FieldName(),5));

        StringBuilder sql = new StringBuilder();
        sql.append("select tmp.* from ( ");
        Level parentLevel = null;
        int index = 0;

        DBType dbType = DBType.getType(DBUtil.getDataSourceType().getDialect());
        String charType = (dbType == DBType.MySQL) ? "char" : "varchar";

        List<String> keyFields = new ArrayList<>();
        for(Level level : levels){
            index++;
            if(BIUtil.isEmpty(level.fieldId) || BIUtil.isEmpty(level.keyField) || BIUtil.isEmpty(level.nameField)){
                continue;
            }
            MetaField levelField = SSDMetaCacheManager.getField(level.fieldId);
            if(levelField == null) {
                continue;
            }
            // 层次表
            String tableId = BIUtil.isEmpty(levelField.getDimTableId()) ? levelField.getFactTableId() : levelField.getDimTableId();
            MetaTable table = SSDMetaCacheManager.getTable(tableId);
            if(table == null) {
                continue;
            }

            String tableName = table.getFullName();
            StringBuilder levelSQL = new StringBuilder();

            String nodeParentIdExpress = BIUtil.listToStr(keyFields, ",");// 节点父id
            if(BIUtil.isEmpty(nodeParentIdExpress)) {
                nodeParentIdExpress = Root_Id;
            }
            keyFields.add(" CAST(" + level.keyField + " AS " + charType + ")");
            keyFields.add("'|'");
            String nodeIdExpress = BIUtil.listToStr(keyFields, ","); // 节点id
            String userName = getCurrentUserName();
            levelSQL.append("/* ").append(userName).append(" */ ");
            levelSQL.append(" select ").append("tmp_").append(level.index).append(".*")
                    .append(" from ").append("(");
                levelSQL.append(" select distinct ")
                        // 添加字段名前缀，避免id重复
                        .append("concat(").append(nodeIdExpress).append(")").append(" as id").append(", ")
                        .append(level.nameField).append(" as label").append(", ")
                        .append("concat(").append(nodeParentIdExpress).append(")").append(" as parentId").append(", ")
                        // 字段真实值id
                        .append(level.keyField).append(" as realId").append(", ")
                        // 层级字段id
                        .append("'").append(level.fieldId).append("'").append(" as levelFieldId").append(",")
                        // 层级字段名
                        .append("'").append(levelField.getName()).append("'").append(" as levelFieldName")
                        .append(" from ").append(tableName)
                        .append(" where 1=1")
                        .append("   and ").append(level.keyField).append(" is not null ");
            levelSQL.append(" ) ").append("tmp_").append(level.index);
            levelSQL.append(" where 1=1 ");
            this.addDataAuthFilter(level, levelSQL);
            if(index > 1) {
                sql.append(" union all ");
            }
            sql.append(levelSQL);
        }
        sql.append(" ) tmp")
            .append(" order by realId"); // 按真实值id排序
        return sql.toString();
    }

    /**
     * 添加数据权限过滤
     * @param levelSQL
     */
    protected void addDataAuthFilter(Level level, StringBuilder levelSQL) {
        // 添加权限
        String userName = getCurrentUserName();
        MetaField levelField = SSDMetaCacheManager.getField(level.fieldId);
        if(levelField == null) {
            return;
        }
        boolean hasDataAuthCfg = SSDMetaCacheManager.hasDataAuthCfg(levelField.getCode());
        if(hasDataAuthCfg) {
            // 添加数据权限过滤
            SSDQueryService queryService = (SSDQueryService) SpringContextUtil.getBean("SSDQueryService");
            // 查询用户所有模块维度-字段权限
            List<MetaFieldDataAuth> dataAclList = queryService.getDataAuthByUser(userName);
            dataAclList = dataAclList.stream().filter(da -> {
                return da.getDimCode().equals(levelField.getCode());
            }).collect(Collectors.toList());

            // 字段配置了权限，但用户无权限
            if(BIUtil.isEmpty(dataAclList)) {
                levelSQL.append(" AND 1 = 2");
            }else {
                String filterValues = "";
                List<String> authValues = new ArrayList<>();
                authValues = dataAclList.stream().map(da -> da.getItemCode()).collect(Collectors.toList());
                if(DataType.String == DataType.getType(levelField.getFieldKeyType())) {
                    filterValues = BIUtil.listToStr(authValues, ",", "'");
                }else {
                    filterValues = BIUtil.listToStr(authValues, ",");
                }
                levelSQL.append(" AND ").append("tmp_").append(level.index).append(".id").append(" in(").append(filterValues).append(")");
            }
        }
    }

    protected String getCurrentUserName(){
        // 添加权限
        User user = UserManager.get();
        String userName = user == null ? "_unknow_" : user.getName();
        return userName;
    }


    class Level{
        protected String fieldId;
        protected String keyField;
        protected String parentIdField;
        protected String nameField;
        protected Integer index;

        public Level(){

        }

        public Level(String fieldId,String keyField, String parentIdField, String nameField, Integer index) {
            this.keyField = keyField;
            this.parentIdField = parentIdField;
            this.nameField = nameField;
            this.fieldId = fieldId;
            this.index = index;
        }
    }
}
