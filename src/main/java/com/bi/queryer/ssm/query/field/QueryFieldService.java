package com.bi.queryer.ssm.query.field;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.cache.LocalCacheManager;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.acl.AclManager;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.meta.event.MetadataCacheLoadedEvent;
import com.bi.queryer.ssm.mgr.fieldCtg.FieldCtgService;
import com.bi.queryer.ssm.mgr.fieldCtg.model.AuthApplyCtgTreeRsp;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgAuthAcl;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgListReq;
import com.bi.queryer.ssm.mgr.fieldCtg.model.DatasetCtgDataAuthRsq;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.query.field.model.FieldDataUpdateTimeReq;
import com.bi.queryer.ssm.query.field.model.FieldDataUpdateTimeRsp;
import com.bi.queryer.ssm.query.field.model.FieldMetaDataReq;
import com.bi.queryer.ssm.query.field.model.QueryFieldReq;
import com.bi.queryer.ssm.system.access.SystemAccessBlacklistManager;
import com.bi.queryer.ssm.query.log.SSMFilterQueryLogEntity;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.TableDataUpdateTimeUtil;
import com.bi.queryer.sys.authority.AuthorityService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tx.cache.common.exception.KVException;
import com.tx.cache.redis.RedisCacheClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:22 2023-11-09
 * @Description TODO
 **/
@Service
@Scope("prototype")
public class QueryFieldService {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BaseDao dao = null;

    @Autowired
    private FieldCtgService fieldCtgService;

    public static final String FIELD_REDIS_SPACE_KEY = "ssm_field@";

    public static final String FIELD_TREE_REDIS_VALUE_KEY_PREFIX = "tree:";

    /**
     * 永久授权结束日期（特殊部门授权、后台配置授权）
     */
    private static final String PERMANENT_AUTH_END_DATE = "2099-12-31";

    /**
     * 构建字段树
     * @return
     */
    public List<JSONObject> buildFieldTree(CategoryType ctgType,String datasetId) {
        List<JSONObject> tree = new ArrayList<>();
        Map<String, MetaFieldCategory> categoryMap = null;
        String userName = UserManager.get() == null ? "" : UserManager.get().getName();

        // 先从cache中获取
        tree = this.getTreeByCache(datasetId, userName);
        if (BIUtil.isNotEmpty(tree)) {
            return tree;
        }

        //是否要加更多目录
        if (ctgType == CategoryType.Front) {
            categoryMap = SSDMetaCacheManager.getFrontCategories();
        } else {
            categoryMap = SSDMetaCacheManager.getBackCategories();
        }

        if (BIUtil.isEmpty(categoryMap)) {
            return tree;
        }

        if (StrUtil.isEmpty(datasetId)) {
            datasetId = SC.v("ssm.default.datasetId", "");
        }

        List<MetaFieldCategory> rootFrontCategories;
        if (ctgType == CategoryType.Front) {
            rootFrontCategories = getRootFrontCategories(datasetId);
        } else {
            // 从目录开始构建
            List<MetaFieldCategory> rootFrontCategoriesTmp = categoryMap.values().stream().filter(category -> {
                return BIConsts.Category_Root_Id.equalsIgnoreCase(category.getParentId());
            }).collect(Collectors.toList());

            //按数据集过滤目录
            rootFrontCategories = new ArrayList<>();
            if (StrUtil.isNotEmpty(datasetId)) {
                for (MetaFieldCategory metaFieldCategory : rootFrontCategoriesTmp) {

                    if (StrUtil.isEmpty(metaFieldCategory.getDatasetId())) {
                        continue;
                    }
                    if (!metaFieldCategory.getDatasetId().contains(datasetId)) {
                        continue;
                    }

                    rootFrontCategories.add(metaFieldCategory);
                }
            } else {
                rootFrontCategories = rootFrontCategoriesTmp;
            }

            //非ut环境过滤测试目录
            RuntimeEnv runtimeEnv = BIUtil.getRuntimeEnv();
            if (RuntimeEnv.Product == runtimeEnv || RuntimeEnv.Test == runtimeEnv) {
                rootFrontCategories = rootFrontCategories.stream().filter(a -> !Enabled.value(a.getIsTestCtg())).collect(Collectors.toList());
            }
        }

        // 字段权限列表（取消字段权限）
//        List<String> aclFieldCodes = getAuthFieldCodes(userName, SSDUtil.Field_Auth_Module, SSDUtil.Field_Auth_Dim);
//        List<String> aclFieldCodes = new ArrayList<>();

        String CtgAuthDim = SSDUtil.Ctg_Auth_Dim;

        Map<String, Integer> inheritMap = fieldCtgService.queryAllInherit();

        // 目录权限列表（统一走 resolveCtgAcls，内含特殊部门权限逻辑）
        List<String> aclCtgList = resolveCtgAcls(userName, SSDUtil.Ctg_Auth_Module, CtgAuthDim,
                datasetId, rootFrontCategories, inheritMap);

        Map<String,String> aclCtgMap = new HashMap<>();
        for(String k : aclCtgList) {
            aclCtgMap.put(k, k);
        }

        // 合并字段和目录权限
//        List<String> aclCodes = mergeAclCodes(aclFieldCodes, aclCtgCodes);

        //目录对应作业更新时间
        List<String> jobNameList = new ArrayList<>();

        for(MetaFieldCategory category : categoryMap.values()) {
            if (!StringUtil.isEmpty(category.getEtlJob())) {
                jobNameList.addAll(Arrays.asList(category.getEtlJob().split(",")));
            }
        }

        List<SysEtlJobInfo> ctgEtlJobInfo = new ArrayList<>();

        if(CollUtil.isNotEmpty(jobNameList)){
            jobNameList = jobNameList.stream().distinct().collect(Collectors.toList());
            ctgEtlJobInfo = (List<SysEtlJobInfo>) dao.queryObjectList("ssm.query.querySysEtlJobInfo", jobNameList, DataSourceType.ETL);
        }

        Set<String> blacklistCodes = AclManager.getUnauthFieldCodeByBlackList(userName);


        boolean isRtDataSet = false;
        //判断数据集类型
        MetaDataset metaDataset = SSDMetaCacheManager.getDataset(datasetId);
        if(metaDataset != null ){
            if(DataTypeEnum.REAL_TIME == DataTypeEnum.codeOf(metaDataset.getDatasetType())){
                isRtDataSet = true;
            }
        }

        this.buildFieldTreeCascade(rootFrontCategories, tree, aclCtgMap, ctgEtlJobInfo, false, inheritMap, blacklistCodes,isRtDataSet);

        this.addTreeToCache(tree, datasetId, userName);
        return tree;
    }

    /**
     * 获取前台根目录列表
     * <p>
     * 供 buildFieldTree、buildAuthApplyCtgTree 等复用：
     * 取 parentId 为根节点的目录，按 datasetId 过滤，非 UT 环境排除测试目录。
     * </p>
     *
     * @param datasetId 数据集 ID，为空时返回全部根目录
     * @return 前台根目录列表
     */
    public List<MetaFieldCategory> getRootFrontCategories(String datasetId) {
        Map<String, MetaFieldCategory> categoryMap = SSDMetaCacheManager.getFrontCategories();
        if (BIUtil.isEmpty(categoryMap)) {
            return new ArrayList<>();
        }
        List<MetaFieldCategory> rootFrontCategoriesTmp = categoryMap.values().stream()
                .filter(category -> BIConsts.Category_Root_Id.equalsIgnoreCase(category.getParentId()))
                .collect(Collectors.toList());

        List<MetaFieldCategory> rootFrontCategories = new ArrayList<>();
        if (StrUtil.isNotEmpty(datasetId)) {
            for (MetaFieldCategory metaFieldCategory : rootFrontCategoriesTmp) {
                if (StrUtil.isEmpty(metaFieldCategory.getDatasetId())) {
                    continue;
                }
                if (!metaFieldCategory.getDatasetId().contains(datasetId)) {
                    continue;
                }
                rootFrontCategories.add(metaFieldCategory);
            }
        } else {
            rootFrontCategories = rootFrontCategoriesTmp;
        }

        RuntimeEnv runtimeEnv = BIUtil.getRuntimeEnv();
        if (RuntimeEnv.Product == runtimeEnv || RuntimeEnv.Test == runtimeEnv) {
            rootFrontCategories = rootFrontCategories.stream()
                    .filter(category -> !Enabled.value(category.getIsTestCtg()))
                    .collect(Collectors.toList());
        }
        return rootFrontCategories;
    }

    /**
     * 构建权限申请目录树
     * <p>
     * 编排流程：获取用户身份 → 查询目录继承配置 → 过滤前台根目录 → 解析目录权限 → 组装响应树。
     * </p>
     *
     * @param req 请求参数，datasetId 指定数据集范围，为空时使用默认数据集
     * @return 权限申请目录树节点列表
     */
    public SSMResponseMessage<List<AuthApplyCtgTreeRsp>> buildAuthApplyCtgTree(CtgListReq req) {
        // 当前登录用户
        String userName = UserManager.get() == null ? "" : UserManager.get().getName();
        // 目录权限继承配置：ctgId -> 是否继承（Enabled.YES/NO）
        Map<String, Integer> inheritMap = fieldCtgService.queryAllInherit();
        String datasetId = req.getDatasetId();
        if (StrUtil.isEmpty(datasetId)) {
            datasetId = SC.v("ssm.default.datasetId", "");
        }
        // 前台根目录：按数据集过滤，非 UT 环境排除测试目录
        List<MetaFieldCategory> rootFrontCategories = getRootFrontCategories(datasetId);
        // 用户已拥有的目录权限及结束时间（含特殊部门权限逻辑）
        Map<String, String> aclCtgMap = resolveCtgAclsWithEndDate(userName, SSDUtil.Ctg_Auth_Module, SSDUtil.Ctg_Auth_Dim,
                datasetId, rootFrontCategories, inheritMap);
        List<AuthApplyCtgTreeRsp> treeData = doBuildAuthApplyCtgTree(rootFrontCategories, aclCtgMap, inheritMap);
        return SSMResponseMessage.success("构建权限申请目录树成功", treeData);
    }

    /**
     * 获取当前登录用户在指定数据集下第一个有权限且在元数据缓存中存在的目录 id
     * <p>
     * 复用 resolveCtgAcls 解析权限列表，按顺序返回第一个在 SSDMetaCacheManager 中存在的目录 id。
     * 无可用目录时 data 为空，仍返回成功。
     * </p>
     *
     * @param datasetId 数据集 id，为空时使用默认数据集
     * @return 第一个有权限且在缓存中存在的目录 id，找不到时 data 为 null
     */
    public SSMResponseMessage<String> getFirstAuthCtgId(String datasetId) {
        String userName = UserManager.get() == null ? "" : UserManager.get().getName();
        Map<String, Integer> inheritMap = fieldCtgService.queryAllInherit();
        if (StrUtil.isEmpty(datasetId)) {
            datasetId = SC.v("ssm.default.datasetId", "");
        }
        List<MetaFieldCategory> rootFrontCategories = getRootFrontCategories(datasetId);
        List<String> aclCtgList = resolveCtgAcls(userName, SSDUtil.Ctg_Auth_Module, SSDUtil.Ctg_Auth_Dim,
                datasetId, rootFrontCategories, inheritMap);
        String firstAuthCtgId = null;
        if (CollUtil.isNotEmpty(aclCtgList)) {
            for (String ctgId : aclCtgList) {
                if (SSDMetaCacheManager.getCategoryById(ctgId) != null) {
                    firstAuthCtgId = ctgId;
                    break;
                }
            }
        }
        return SSMResponseMessage.success("查询首个有权限目录成功", firstAuthCtgId);
    }

    /**
     * 组装权限申请目录树
     *
     * @param rootFrontCategories 前台根目录列表
     * @param aclCtgMap           用户已拥有的目录权限 Map，key=ctgId，value=authEndDate
     * @param inheritMap          目录权限继承配置
     * @return 权限申请目录树节点列表
     */
    private List<AuthApplyCtgTreeRsp> doBuildAuthApplyCtgTree(List<MetaFieldCategory> rootFrontCategories,
                                                                Map<String, String> aclCtgMap,
                                                                Map<String, Integer> inheritMap) {
        return buildAuthApplyCtgTreeNodes(rootFrontCategories, aclCtgMap, inheritMap, false, null);
    }

    /**
     * 递归组装权限申请目录树节点
     * <p>
     * hasAuth 计算规则与 buildFieldTreeCascade 一致：
     * (父目录有权限 && 当前目录继承) || 当前目录在 aclCtgMap 中。
     * authEndDate：直接授权取 aclCtgMap 中的日期；继承授权取父级日期；多来源取较晚日期。
     * 递归时将当前节点算出的 hasAuth、authEndDate 作为子节点的 hasParentAuth、parentAuthEndDate 向下传递。
     * </p>
     *
     * @param categories        待组装的目录列表
     * @param aclCtgMap         用户已授权的目录 Map，key=ctgId，value=authEndDate
     * @param inheritMap        目录权限继承配置
     * @param hasParentAuth     父目录是否拥有权限
     * @param parentAuthEndDate 父目录生效的权限结束时间
     * @return 权限申请目录树节点列表
     */
    private List<AuthApplyCtgTreeRsp> buildAuthApplyCtgTreeNodes(List<MetaFieldCategory> categories,
                                                                 Map<String, String> aclCtgMap,
                                                                 Map<String, Integer> inheritMap,
                                                                 boolean hasParentAuth,
                                                                 String parentAuthEndDate) {
        if (CollUtil.isEmpty(categories)) {
            return new ArrayList<>();
        }
        List<MetaFieldCategory> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);

        List<AuthApplyCtgTreeRsp> resultList = new ArrayList<>();
        for (MetaFieldCategory category : sortedCategories) {
            // 是否继承父目录权限，缺省为继承
            Integer isInherited = inheritMap.get(category.getId());
            if (isInherited == null) {
                isInherited = Enabled.YES.getId();
            }
            // 拥有父目录权限且继承，或自身在授权列表中
            boolean hasDirectAuth = aclCtgMap.containsKey(category.getId());
            boolean hasAuth = (hasParentAuth && Enabled.value(isInherited)) || hasDirectAuth;
            String authEndDate = resolveNodeAuthEndDate(hasDirectAuth, aclCtgMap.get(category.getId()),
                    hasParentAuth, isInherited, parentAuthEndDate);

            AuthApplyCtgTreeRsp node = new AuthApplyCtgTreeRsp();
            node.setCtgId(category.getId());
            node.setCtgName(category.getName());
            node.setRptDevOwner(extractFirstCommaValue(category.getRptDevOwner()));
            node.setRptDevOwnerDesc(extractFirstCommaValue(category.getRptDevOwnerDesc()));
            node.setBizOwner(extractFirstCommaValue(category.getBizOwner()));
            node.setBizOwnerDesc(extractFirstCommaValue(category.getBizOwnerDesc()));
            node.setUseSceneDesc(category.getCtgDesc());
            // 取缓存阶段聚合的目录敏感等级
            node.setSensitiveLevel(category.getSensitiveLevel());
            node.setIsInherited(isInherited);
            node.setHasAuth(hasAuth ? Enabled.YES.getId() : Enabled.NO.getId());
            node.setAuthEndDate(authEndDate);
            node.setAuthMap(category.getAuthMap());
            if (CollUtil.isNotEmpty(category.getChildren())) {
                node.setChildren(buildAuthApplyCtgTreeNodes(category.getChildren(), aclCtgMap, inheritMap,
                        hasAuth, hasAuth ? authEndDate : null));
            }
            resultList.add(node);
        }
        return resultList;
    }

    /**
     * tree是否可以缓存
     * @return
     */
    protected boolean canTreeCache(){
        return "true".equals(SC.v("ssm.tree.cache.enable", "true"));
    }

    /**
     * tree缓存key
     * @param datasetId
     * @param userName
     * @return
     */
    protected String getTreeCacheKey(String datasetId, String userName){
        String cacheKey = FIELD_TREE_REDIS_VALUE_KEY_PREFIX + datasetId + ":" + userName;
        return cacheKey;
    }

    /**
     * 通过缓存获取tree
     * @param datasetId
     * @param userName
     * @return
     */
    protected List<JSONObject> getTreeByCache(String datasetId, String userName){
        List<JSONObject> tree = new ArrayList<>();
        if(!this.canTreeCache()){
            return tree;
        }
        try {
            // 先从cache中获取
            String cacheKey = this.getTreeCacheKey(datasetId, userName);

            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            Object fileName = redisCacheClient.get(cacheKey);
            String redisTree = LocalCacheManager.getCache(fileName + "");
            if (BIUtil.isNotEmpty(redisTree)) {
                tree.clear();
                tree.addAll(JSON.parseArray(redisTree, JSONObject.class));
                return tree;
            }
        }catch (Exception e){
            System.out.println("从cache中tree失败" + e.getMessage());
        }
        return tree;
    }

    /**
     * tree添加到缓存（异步）
     * @param tree
     * @param datasetId
     * @param userName
     */
    protected void addTreeToCache(List<JSONObject> tree, String datasetId, String userName) {
        if (!this.canTreeCache()) {
            return;
        }
        try {
            String cacheKey = this.getTreeCacheKey(datasetId, userName);
            // 添加到缓存中
            new Thread(() -> {
                RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
                try {
                    redisCacheClient.set(cacheKey, cacheKey, 5 * 60, TimeUnit.SECONDS);
                    redisCacheClient.hset(FIELD_REDIS_SPACE_KEY, cacheKey, cacheKey, 1 * 60 * 60);
                } catch (KVException e) {
                    throw new RuntimeException(e);
                }
                LocalCacheManager.addCache(tree.toString(), cacheKey);
            }).start();
        } catch (Exception e) {
            System.out.println("tree写入cache失败" + e);
        }
    }

    /**
     * 清理所有用户的tree缓存
     */
    @EventListener(MetadataCacheLoadedEvent.class)
    public void clearTreeCache(MetadataCacheLoadedEvent event){
        try {
            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            Set<String> keys = RedisCacheManager.hscanKeys(FIELD_REDIS_SPACE_KEY).stream().filter(k -> BIUtil.isNotEmpty(k)).collect(Collectors.toSet());
            if(CollUtil.isEmpty(keys)) {
                return;
            }

            //cache限制批量最多删除200个
            //此处将keys进行分组，每次最多删除100个
            List<String> deleteKeys = new ArrayList<>();
            deleteKeys.addAll(keys);
            List<List<String>> splitDeleteKeys = BIUtil.splitList(deleteKeys, 100);
            for(List<String> groupDeleteKeyList : splitDeleteKeys) {
                if (CollUtil.isEmpty(groupDeleteKeyList)) {
                    continue;
                }

                Set<String> setKeys = new HashSet<>(groupDeleteKeyList);
                redisCacheClient.multiDelete(setKeys);
                redisCacheClient.hdel(FIELD_REDIS_SPACE_KEY, setKeys.toArray(new String[setKeys.size()]));
            }

        } catch (KVException e) {
            e.printStackTrace();
        }
    }

    // 构建字段树目录信息
    public JSONObject getCtgInfo(String ctgId) {
        if (BIUtil.isEmpty(ctgId)) {
            return new JSONObject();
        }
        MetaFieldCategory category = SSDMetaCacheManager.getCategoryById(ctgId);

        if (category == null) {
            return new JSONObject();
        }

        //目录对应作业更新时间
//        List<SysEtlJobInfo> ctgEtlJobInfo = new ArrayList<>();
//        if (BIUtil.isNotEmpty(category.getEtlJob())) {
//            List<String> jobNameList = Arrays.stream(category.getEtlJob().split(",")).distinct().collect(Collectors.toList());
//            ctgEtlJobInfo = (List<SysEtlJobInfo>) dao.queryObjectList("ssm.query.querySysEtlJobInfo", jobNameList, DataSourceType.ETL);
//        }

        // 权限范围信息查询
        Map<String, String> dimAuthMap = SSDMetaCacheManager.getAllMetaFieldDataAuthCfg().stream()
                .collect(Collectors.toMap(DataAuth::getDimCode, DataAuth::getDimName, (v1, v2) -> v1));
        String ctgAuthData = null;
        if (BIUtil.isNotEmpty(category.getAuthMap())) {
            ctgAuthData = category.getAuthMap().entrySet().stream()
                    .map(v -> dimAuthMap.getOrDefault(v.getKey(), v.getKey()) + "=" + v.getValue().stream().map(e -> {
                        if (DataAuthItemType.ALL.getValue().equals(e)) {
                            return DataAuthItemType.ALL.getName();
                        } else {
                            return e;
                        }
                    }).collect(Collectors.joining(",")))
                    .collect(Collectors.joining("; "));
        }

        JSONObject node = new JSONObject();
        node.put("id", category.getId());
        node.put("label", category.getName());
        node.put("ctgDesc", category.getCtgDesc());
        node.put("parentId", category.getParentId());
        node.put("rptDevOwner", category.getRptDevOwnerDesc());
        node.put("bizOwner", category.getBizOwnerDesc());
        node.put("dataDevOwner", category.getDataDevOwnerDesc());
        //node.put("remark", getCtgEtlJobInfo(category, ctgEtlJobInfo));
        node.put("dataDate", category.getDataDate());
        node.put("dataDesc", category.getDataDesc());
        node.put("ctgDataType", category.getDataType());
        node.put("ctgAuthData", ctgAuthData);
        return node;
    }

    /**
     * 递归构建字段树
     * @param categories
     * @param tree
     */
    protected void buildFieldTreeCascade(List<MetaFieldCategory> categories, List<JSONObject> tree, Map<String,String> aclCtgMap,
                                         List<SysEtlJobInfo> ctgEtlJobInfo, Boolean hasParentAuth, Map<String, Integer> inheritMap, Set<String> blacklistCodes,
                                         boolean isRtDataSet) {
        if (BIUtil.isEmpty(categories)) {
            return;
        }

        //重新赋值，避免并发查询异常
        List<MetaFieldCategory> metaFieldCategoryList = new ArrayList<>();
        metaFieldCategoryList.addAll(categories);

        Collections.sort(metaFieldCategoryList);

        for (MetaFieldCategory c : metaFieldCategoryList) {

            //非ut环境过滤测试目录
            RuntimeEnv runtimeEnv = BIUtil.getRuntimeEnv();
            if(RuntimeEnv.Product == runtimeEnv || RuntimeEnv.Test == runtimeEnv) {
                if (Enabled.value(c.getIsTestCtg())) {
                    continue;
                }
            }

            JSONObject node = new JSONObject();
            node.put("id", c.getId());
            node.put("label", c.getName());
            node.put("type", "ctg");
            node.put("ctgDesc", c.getCtgDesc());
            node.put("parentId", c.getParentId());
            node.put("rptDevOwner", c.getRptDevOwnerDesc());
            node.put("bizOwner", c.getBizOwnerDesc());
            node.put("dataDevOwner", c.getDataDevOwnerDesc());
            //实时数据集在目录树上不展示数据更新时间，在表格上方展示实时数据更新时间
            node.put("remark", isRtDataSet ? "" : getCtgEtlJobInfo(c, ctgEtlJobInfo));
            node.put("dataDate", c.getDataDate());
            node.put("dataDesc", c.getDataDesc());
            node.put("ctgDataType",c.getDataType());
            List<MetaFieldCategory> children = c.getChildren();
            List<JSONObject> childTree = new ArrayList<>();

            //是否继承，默认继承
            Integer isInherited = inheritMap.get(c.getId());
            if(isInherited == null) {
                isInherited =  Enabled.YES.getId();
            }

            //判断是否拥有改目录权限--拥有父目录权限或该目录权限
            Boolean hasAuth = (hasParentAuth && Enabled.value(isInherited)) || aclCtgMap.containsKey(c.getId());

            // 添加字段:去掉不显示的字段
            List<MetaField> ctgFields = c.getFields().stream().filter(f->Enabled.isTrue(f.getIsShow())).collect(Collectors.toList());
            List<MetaField> fields = FieldUtil.getMaxWeightFields(ctgFields,c.getId()); //SSDMetaCacheManager.getCategoryMaxWeightFields(c.getId());
//            System.out.println(ctgFields.size() + "\t" + fields.size());
//            List<MetaField> fields = this.getMaxWeightFields(c.getFields());
            if (!fields.isEmpty()) {
                fields = fields.stream().filter(f -> Enabled.isTrue(f.getIsShow())).collect(Collectors.toList());
                for (MetaField m : fields) {
                    int hasFieldAuth;
                    if (CollUtil.isEmpty(blacklistCodes)) {
                        hasFieldAuth = hasAuth ? 1 : 0;
                    } else {
                        hasFieldAuth = hasAuth && !blacklistCodes.contains(m.getCode()) ? 1 : 0;
                    }
                    JSONObject fieldNode = metaFieldToJSONObject(c, m, hasFieldAuth);
                    if (fieldNode != null) {
                        childTree.add(fieldNode);
                    }
                }
            }

            if (BIUtil.isNotEmpty(children)) {
                buildFieldTreeCascade(children, childTree, aclCtgMap, ctgEtlJobInfo, hasAuth,inheritMap, blacklistCodes,isRtDataSet);
            }
            node.put("children", childTree);
            tree.add(node);
        }
    }

    /**
     * 判断是否是计算字段
     * @param field
     * @return dim维度，measure指标
     */
    private Boolean checkMetaFieldIsCalculated(MetaField field) {
        Boolean flag = false;
        try {
            if (field != null && StringUtils.isNotBlank(field.getAggExpression())) {
                String aggExpression = field.getAggExpression();
                String regex = "\\[[\\s\\S]*\\]";
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(aggExpression);
                while (m.find()) {
                    flag = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flag;
    }

    private JSONObject metaFieldToJSONObject(MetaFieldCategory metaFieldCategory, MetaField m, Integer hasAuth) {
        if ((Enabled.value(m.getIsFilter()) || Enabled.value(m.getIsResult()))) {
            JSONObject fieldNode = new JSONObject();
            fieldNode.put("id", m.getId());
            fieldNode.put("label", m.getTitle());
            fieldNode.put("type", "field");
            fieldNode.put("parentId", metaFieldCategory.getId());
            fieldNode.put("moduleCtgId",metaFieldCategory.getModuleCtgId());
            fieldNode.put("name", m.getName());
            fieldNode.put("dataType", m.getDataType());
            fieldNode.put("code", m.getCode());
            fieldNode.put("title", m.getTitle());
            fieldNode.put("aggExpression", m.getAggExpression());
            fieldNode.put("isFilter", m.getIsFilter());
            fieldNode.put("isResult", m.getIsResult());
            fieldNode.put("filterShowType", m.getFilterShowType());
            fieldNode.put("filterValueMode", m.getFilterValueMode());
            fieldNode.put("isMeasure", m.getIsMeasure());
            fieldNode.put("remark", m.getRemark());
            fieldNode.put("hasAuth", hasAuth);
            fieldNode.put("filterTips", m.getFilterTips());
            fieldNode.put("isSensitive", m.getIsSensitive());
            fieldNode.put("dataAuthModuleCode", m.getDataAuthModuleCode());
            fieldNode.put("dataAuthDimCode", m.getDataAuthDimCode());
            fieldNode.put("dataAuthMode", m.getDataAuthMode());
            fieldNode.put("canColDim", m.getCanColDim());
            fieldNode.put("isCommonDate", m.getIsCommonDate());
            if("true".equals(SC.v("ssm.field.tree.samecode.simplify", "true"))) {
                List<JSONObject> sameCodeFieldList = m.getSameCodeFieldList().stream().map(t -> {
                    JSONObject item = new JSONObject();
                    item.put("id", t.getId());
                    return item;
                }).collect(Collectors.toList());
                fieldNode.put("sameCodeFieldList", sameCodeFieldList);
            }else{
                fieldNode.put("sameCodeFieldList", m.getSameCodeFieldList());
            }
            fieldNode.put("showFormatExpression",m.getShowFormatExpression());
            fieldNode.put("kpiNo", m.getKpiNo());
            fieldNode.put("fieldType",m.getFieldType());

            String dateGranularity = m.getDateGranularity();
            List<String> availableDateGranularityList = new ArrayList<>();
            String tableId = StrUtil.isNotEmpty(m.getFactTableId()) ? m.getFactTableId() : m.getDimTableId();
            MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
            if (metaTable != null) {
                dateGranularity = metaTable.getDateGranularity();
                availableDateGranularityList = metaTable.getAvailableDateGranularityList();
            }
            fieldNode.put("dateGranularity", dateGranularity);
            fieldNode.put("availableDateGranularityList", availableDateGranularityList);

            buildCrossModelMeasureAttribute(fieldNode, m);

            fieldNode.put("isShow", m.getIsShow());
            return fieldNode;
        }
        return null;
    }

    /**
     * 构建跨模型指标属性
     * @return
     */
    public void buildCrossModelMeasureAttribute (JSONObject fieldNode,MetaField m) {

        if (FieldType.CROSS_MODEL_MEASURE != FieldType.get(m.getFieldType())) {
            return;
        }

        try {

            String dateGranularity = m.getDateGranularity();
            List<String> availableDateGranularityList = new ArrayList<>();

            if (CollUtil.isEmpty(m.getCalcAtomFields())) {
                return;
            }

            availableDateGranularityList = getCrossModelMeasureAvailableDateGranularityList(m);

            if (CollUtil.isNotEmpty(availableDateGranularityList)) {
                dateGranularity = availableDateGranularityList.get(0);
            }

            fieldNode.put("dateGranularity", dateGranularity);
            fieldNode.put("availableDateGranularityList", availableDateGranularityList);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取跨模型指标的可用日期粒度列表
     * @param m
     * @return
     */
    public List<String> getCrossModelMeasureAvailableDateGranularityList(MetaField m){

        List<String> availableDateGranularityList = new ArrayList<>();

        int idx = 0;
        for (MetaField calcAtomField : m.getCalcAtomFields()) {
            String tableId = StrUtil.isNotEmpty(calcAtomField.getFactTableId()) ? calcAtomField.getFactTableId() : calcAtomField.getDimTableId();
            MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
            if (metaTable == null) {
                continue;
            }
            if (idx == 0) {
                availableDateGranularityList = metaTable.getAvailableDateGranularityList();
            } else {

                //取支持的日期粒度的交集
                Collection<String> intersection = CollectionUtils.intersection(availableDateGranularityList, metaTable.getAvailableDateGranularityList());
                availableDateGranularityList = (List<String>) intersection;
            }

            idx++;

        }

        if(CollUtil.isNotEmpty(availableDateGranularityList)){

            Collections.sort(availableDateGranularityList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    // 定义排序顺序
                    String order = "dwmqy";
                    return order.indexOf(o1) - order.indexOf(o2);
                }
            });

        }

        return availableDateGranularityList;
    }

    /**
     * 获取目录对应作业的信息
     * @param ctgEtlJobInfo
     * @return
     */
    public String getCtgEtlJobInfo(MetaFieldCategory category, List<SysEtlJobInfo> ctgEtlJobInfo) {
        String jobNames = category.getEtlJob();
        if (BIUtil.isEmpty(ctgEtlJobInfo) || StringUtils.isBlank(jobNames)) {
            return "";
        }

        //非模块不展示
        if(!Enabled.value(category.getIsModule())){
            return "";
        }

        String result = "";
        Date updateTimeMax = null;
        String[] jobArray = jobNames.split(",");
        for (String jobName : jobArray) {
            Optional<SysEtlJobInfo> optional = ctgEtlJobInfo.stream().filter(a -> jobName.equalsIgnoreCase(a.getEtlJob())).findAny();
            if (optional.isPresent()) {
                SysEtlJobInfo sysEtlJobInfo = optional.get();
                //查询更新时间最大值
                try {
                    Date updateTime = sysEtlJobInfo.getEtlTime();
                    if (updateTime != null) {
                        updateTimeMax = getJobUpdateTime(updateTimeMax, updateTime);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (updateTimeMax != null) {
            result = "数据更新时间：" + simpleDateFormat.format(updateTimeMax);
        }

        return result;
    }

    // 取更新时间方法，先比较日期，取日期小的，日期相同，取时间大的
    public Date getJobUpdateTime(Date time1, Date time2) {
        if (time1 == null) {
            return time2;
        }
        //日期相同，取时间大的
        if (DateUtil.format(time1, "yyyy-MM-dd").equals(DateUtil.format(time2, "yyyy-MM-dd"))) {
            return DateUtil.compare(time1, time2) > 0 ? time1 : time2;
        }
        //日期不同，取日期小的
        return DateUtil.compare(time1, time2) > 0 ? time2 : time1;
    }


    /**
     * 根据开关和用户部门/角色条件，决定走常规权限逻辑还是特殊权限逻辑，返回目录权限列表（ctgAcls）。
     * 开关：ssm.auth.special.dept.enable = true
     * 特殊条件：
     *   1. 用户部门 deptId（/分隔）包含 ssm.auth.special.dept.ids 中的任一部门 id
     *   2. 用户在 ssm_role_user_ext 表中存在记录
     * 满足条件走特殊逻辑，否则走常规逻辑。
     */
    public List<String> resolveCtgAcls(String userName, String authModule, String authDim,
                                       String datasetId, List<MetaFieldCategory> rootFrontCategories,
                                       Map<String, Integer> inheritMap) {
        // 系统访问黑名单用户无目录权限
        if (SystemAccessBlacklistManager.isBlacklisted(userName)) {
            return new ArrayList<>();
        }

        // 开关判断
        boolean specialEnable = Boolean.parseBoolean(SC.v("ssm.auth.special.dept.enable", "false"));
        if (specialEnable && isSpecialDeptUser(userName)) {
            List<String> specialCtgAcls = getSpecialCtgAcls(userName);
            return specialCtgAcls;
        }

        // 常规逻辑
        List<String> ctgAcls = getAuthCtgList(userName, authModule, authDim);
        List<String> ctgAuthList = getConfigAuthCtgList(datasetId, rootFrontCategories, inheritMap);
        if (CollUtil.isNotEmpty(ctgAuthList)) {
            ctgAcls.addAll(ctgAuthList);
        }
        return ctgAcls;
    }

    /**
     * 判断用户是否满足特殊权限条件：
     *   1. 用户部门 deptId（/分隔）包含 ssm.auth.special.dept.ids 中的任一部门 id
     *   2. 用户在 ssm_role_user_ext 表中存在记录
     *   如：连锁运营部
     */
    private boolean isSpecialDeptUser(String userName) {
        String specialDeptIds = SC.v("ssm.auth.special.dept.ids", "200384");
        if (StrUtil.isBlank(specialDeptIds)) {
            return false;
        }
        User user = UserManager.get();
        if (user == null || StrUtil.isBlank(user.getDeptId())) {
            return false;
        }
        List<String> userDeptIds = Arrays.asList(user.getDeptId().split("/"));
        List<String> configDeptIds = Arrays.asList(specialDeptIds.split(","));
        boolean deptMatch = userDeptIds.stream().anyMatch(configDeptIds::contains);
        if (!deptMatch) {
            return false;
        }
        List<String> roleNames = (List<String>) dao.queryObjectList("ssm.role.ext.queryRoleNamesByUserName", userName);
        return CollUtil.isNotEmpty(roleNames);
    }

    /**
     * 特殊权限逻辑：
     *   1. 从 ssm_role_user_ext 取 role_name
     *   2. 从 ssm_role_ctg_auth_ext 取 ctg_full_name
     *   3. 切换到 OLAP 数据源，从 v_ssd_field_ctg 通过 ctg_name_path 取 ctg_id，作为目录权限列表返回
     */
    public List<String> getSpecialCtgAcls(String userName) {
        // 第一步：查询用户关联的角色名
        List<String> roleNames = (List<String>) dao.queryObjectList("ssm.role.ext.queryRoleNamesByUserName", userName);
        if (CollUtil.isEmpty(roleNames)) {
            return new ArrayList<>();
        }
        // 第二步：根据角色名查 ctg_full_name（走默认数据源）
        List<String> ctgFullNames = (List<String>) dao.queryObjectList("ssm.role.ext.queryCtgFullNamesByRoleNames", roleNames);
        if (CollUtil.isEmpty(ctgFullNames)) {
            return new ArrayList<>();
        }
        // 第三步：切换到 OLAP 数据源，从 v_ssd_field_ctg 查 ctg_id
        List<String> ctgIds = (List<String>) dao.queryObjectList("ssm.role.ext.queryCtgIdsByCtgFullNames", ctgFullNames, DataSourceType.OLAP);
        return CollUtil.isEmpty(ctgIds) ? new ArrayList<>() : ctgIds;
    }

    /**
     * 获取有权限的目录列表
     * @return
     */
    public List<String> getAuthCtgList(String userName, String authModule, String authDim) {
        // 权限表存储为字段id权限，转为字段编码
        AuthorityService authorityService = (AuthorityService) SpringContextUtil.getBean("authorityService");
        List<KeyValuePair> ctgAcls = authorityService.getDataItems(userName, authModule, authDim);
        List<String> aclCtgList = new ArrayList<String>();
        for (KeyValuePair kv : ctgAcls) {
            if (StringUtils.isNotBlank(kv.getKey())) {
                aclCtgList.add(kv.getKey());
            }
        }
        aclCtgList = aclCtgList.stream().distinct().collect(Collectors.toList());
        return aclCtgList;
    }

    /**
     * 解析目录权限及结束时间，同一 ctgId 多来源命中时取较晚的 authEndDate
     */
    public Map<String, String> resolveCtgAclsWithEndDate(String userName, String authModule, String authDim,
                                                         String datasetId, List<MetaFieldCategory> rootFrontCategories,
                                                         Map<String, Integer> inheritMap) {
        // 系统访问黑名单用户无目录权限
        if (SystemAccessBlacklistManager.isBlacklisted(userName)) {
            return new HashMap<>();
        }

        Map<String, String> aclCtgMap = new HashMap<>();
        boolean specialEnable = Boolean.parseBoolean(SC.v("ssm.auth.special.dept.enable", "false"));
        if (specialEnable && isSpecialDeptUser(userName)) {
            mergeCtgAuthAcl(aclCtgMap, getSpecialCtgAclsWithEndDate(userName));
            return aclCtgMap;
        }
        mergeCtgAuthAcl(aclCtgMap, getAuthCtgListWithEndDate(userName, authModule, authDim));
        mergeCtgAuthAcl(aclCtgMap, getConfigAuthCtgListWithEndDate(datasetId, rootFrontCategories, inheritMap));
        return aclCtgMap;
    }

    /**
     * 获取有权限的目录列表及真实计算的权限结束时间
     */
    public List<CtgAuthAcl> getAuthCtgListWithEndDate(String userName, String authModule, String authDim) {
        AuthorityService authorityService = (AuthorityService) SpringContextUtil.getBean("authorityService");
        List<CtgAuthAcl> ctgAcls = authorityService.getDataItemsWithAuthEndDate(userName, authModule, authDim);
        if (CollUtil.isEmpty(ctgAcls)) {
            return new ArrayList<>();
        }
        return ctgAcls;
    }

    /**
     * 特殊部门权限目录列表，authEndDate 固定为永久授权日期
     */
    private List<CtgAuthAcl> getSpecialCtgAclsWithEndDate(String userName) {
        List<String> ctgIds = getSpecialCtgAcls(userName);
        return buildPermanentCtgAuthAclList(ctgIds);
    }

    /**
     * 后台配置授权目录列表，authEndDate 固定为永久授权日期
     */
    public List<CtgAuthAcl> getConfigAuthCtgListWithEndDate(String datasetId, List<MetaFieldCategory> rootFrontCategories,
                                                            Map<String, Integer> inheritMap) {
        List<String> ctgIds = getConfigAuthCtgList(datasetId, rootFrontCategories, inheritMap);
        return buildPermanentCtgAuthAclList(ctgIds);
    }

    /**
     * 合并目录权限，同一 ctgId 保留较晚的 authEndDate（yyyy-MM-dd 定长字符串可直接比较）
     */
    private void mergeCtgAuthAcl(Map<String, String> aclCtgMap, List<CtgAuthAcl> ctgAuthAclList) {
        if (CollUtil.isEmpty(ctgAuthAclList)) {
            return;
        }
        for (CtgAuthAcl ctgAuthAcl : ctgAuthAclList) {
            if (ctgAuthAcl == null || StrUtil.isBlank(ctgAuthAcl.getCtgId())) {
                continue;
            }
            String ctgId = ctgAuthAcl.getCtgId();
            String authEndDate = ctgAuthAcl.getAuthEndDate();
            String existingAuthEndDate = aclCtgMap.get(ctgId);
            if (existingAuthEndDate == null) {
                aclCtgMap.put(ctgId, authEndDate);
                continue;
            }
            if (StrUtil.isNotEmpty(authEndDate) && authEndDate.compareTo(existingAuthEndDate) > 0) {
                aclCtgMap.put(ctgId, authEndDate);
            }
        }
    }

    /**
     * 构建永久授权目录权限列表
     */
    private List<CtgAuthAcl> buildPermanentCtgAuthAclList(List<String> ctgIds) {
        if (CollUtil.isEmpty(ctgIds)) {
            return new ArrayList<>();
        }
        List<CtgAuthAcl> resultList = new ArrayList<>();
        for (String ctgId : ctgIds) {
            CtgAuthAcl ctgAuthAcl = new CtgAuthAcl();
            ctgAuthAcl.setCtgId(ctgId);
            ctgAuthAcl.setAuthEndDate(PERMANENT_AUTH_END_DATE);
            resultList.add(ctgAuthAcl);
        }
        return resultList;
    }

    /**
     * 取逗号分隔字符串的第一个值
     */
    private String extractFirstCommaValue(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return StrUtil.trim(value.split(",")[0]);
    }

    /**
     * 计算节点生效的权限结束时间：直接授权与继承授权同时命中时取较晚日期
     */
    private String resolveNodeAuthEndDate(boolean hasDirectAuth, String directAuthEndDate,
                                          boolean hasParentAuth, Integer isInherited, String parentAuthEndDate) {
        String authEndDate = null;
        if (hasDirectAuth) {
            authEndDate = directAuthEndDate;
        }
        if (hasParentAuth && Enabled.value(isInherited) && StrUtil.isNotEmpty(parentAuthEndDate)) {
            if (authEndDate == null || parentAuthEndDate.compareTo(authEndDate) > 0) {
                authEndDate = parentAuthEndDate;
            }
        }
        return authEndDate;
    }

    /**
     * 获取后台配置的权限
     * @return
     */
    public List<String> getConfigAuthCtgList(String datasetId, List<MetaFieldCategory> rootFrontCategories, Map<String, Integer> inheritMap) {
        List<String> configAuthCtgList = new ArrayList<String>();

        User user = UserManager.get();
        Map<String, Object> map = new HashMap<>();
        map.put("userName", user.getName());
        map.put("deptId", user.getDeptId());

        List<DatasetCtgDataAuthRsq> dataAuthRsqList = (List<DatasetCtgDataAuthRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.selectUserAuth", map,DataSourceType.Default);
        if(dataAuthRsqList == null) {
            dataAuthRsqList = new ArrayList<>();
        }

        DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
        List<DatasetCtgDataAuthRsq> mgpDataAuthRsqList = (List<DatasetCtgDataAuthRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.selectUserAuth", map,mgpDataSourceType);
        if(CollUtil.isNotEmpty(mgpDataAuthRsqList)){
            dataAuthRsqList.addAll(mgpDataAuthRsqList);
        }

        if (CollUtil.isNotEmpty(dataAuthRsqList)) {

            for (DatasetCtgDataAuthRsq dataAuthRsq : dataAuthRsqList) {

                DataAuthItemType dataAuthItemType = DataAuthItemType.get(dataAuthRsq.getItemType());
                switch (dataAuthItemType) {
                    case ALL:

                        Integer isInherited = inheritMap.get(datasetId);
                        if (isInherited == null) {
                            isInherited = Enabled.YES.getId();
                        }

                        if (Enabled.value(isInherited)) {
                            configAuthCtgList.addAll(addRootCtgAuth(rootFrontCategories, inheritMap));
                        }

                        break;
                    case DATASET:
                        if(datasetId.equalsIgnoreCase(dataAuthRsq.getItemValue())){
                            configAuthCtgList.addAll(addRootCtgAuth(rootFrontCategories, inheritMap));
                        }
                        break;
                    case CTG:
                        configAuthCtgList.add(dataAuthRsq.getItemValue());
                        break;
                }
            }
        }

        // 去重
        configAuthCtgList = configAuthCtgList.stream().distinct().collect(Collectors.toList());
        return configAuthCtgList;
    }

    //添加第一级目录的权限
    public List<String> addRootCtgAuth(List<MetaFieldCategory> rootFrontCategories,Map<String, Integer> inheritMap) {
        List<String> result = new ArrayList<>();

        for (MetaFieldCategory mfc : rootFrontCategories) {

            Integer isInherited = inheritMap.get(mfc.getId());
            if (isInherited == null) {
                isInherited =  Enabled.YES.getId();
            }

            if (Enabled.value(isInherited)) {
                result.add(mfc.getId());
            }
        }

        return result;
    }

    /**
     * 获取字段值排序集合
     * @param fieldCodes
     * @return
     */
    public ResponseMessage getFieldValueSortNumList(String fieldCodes) {
        ResponseMessage result = new ResponseMessage();

        Map<String, List<MetaFieldValueSort>> fieldValueSortMap = new HashMap<>();

        if (StrUtil.isNotEmpty(fieldCodes)) {

            List<String> fieldCodeList = Arrays.asList(fieldCodes.split(","));

            //以fieldcode为key封装结果集
            for (String fieldCode : fieldCodeList) {

                List<MetaFieldValueSort> fieldValueSortList = new ArrayList<>();

                //如果是公共日期dt，则是使用在业务日历排序，返回业务日历的枚举值
                if (BIConsts.DATE_CODE.equalsIgnoreCase(fieldCode)) {
                    fieldValueSortList = PromotionManager.getAllFieldValueSort();
                } else {
                    fieldValueSortList = SSDMetaCacheManager.getFieldValueSort(fieldCode);
                }

                fieldValueSortMap.put(fieldCode, fieldValueSortList);
            }

        }

        result.setData(fieldValueSortMap);
        return result;
    }

    /**
     * 查询敏感字段
     * @return String
     */
    public List<MetaSensitiveField> querySensitiveField() {
        List<MetaSensitiveField> list = new ArrayList<>();

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("isSensitive", 1);

            DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
            list = (List<MetaSensitiveField>) dao.queryObjectList("fieldDef.querySensitiveFieldList", map,dataEnvDataSourceType);
            if (!list.isEmpty()) {
                for (MetaSensitiveField entity : list) {
                    entity.setCategoryPath(getCtgPath(entity.getCategoryId()));
                }
                //按照路径排序
                Collections.sort(list);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return list;
    }

    /**
     * 获取敏感字段目录
     * @param ctgId
     * @return
     */
    private String getCtgPath(String ctgId) {
        String resultPath = "";
        Map<String, MetaFieldCategory> categoriesMap = SSDMetaCacheManager.getCategories();
        resultPath = getCtgPath(resultPath, categoriesMap, ctgId);
        if (StringUtils.isNotBlank(resultPath)) {
            resultPath = resultPath.substring(0, resultPath.length() - 1);
        }
        return resultPath;
    }

    private String getCtgPath(String resultPath, Map<String, MetaFieldCategory> categoriesMap, String ctgId) {
        if (StringUtils.isNotBlank(ctgId) && !"-1".equalsIgnoreCase(ctgId)) {
            if (categoriesMap.containsKey(ctgId)) {
                MetaFieldCategory metaFieldCategory = categoriesMap.get(ctgId);
                resultPath = metaFieldCategory.getName() + "/" + resultPath;
                resultPath = getCtgPath(resultPath, categoriesMap, metaFieldCategory.getParentId());
            }
        }
        return resultPath;
    }

    /**
     * 获取字段元信息集合
     * @param fieldIds
     * @return
     */
    public ResponseMessage getFieldMetaDataList(String fieldIds, String moduleCtgId) {

        ResponseMessage result = new ResponseMessage();
        List<MetaField> fieldList = new ArrayList<>();

        //查询字段元信息
        List<String> fieldIdList = new ArrayList<>();
        if(StrUtil.isNotEmpty(fieldIds)){
            fieldIdList = Arrays.asList(fieldIds.split(","));
        }

        for (String fieldId : fieldIdList) {
            MetaField metaField = SSDMetaCacheManager.getField(fieldId);
            if (metaField == null) {
                continue;
            }
            fieldList.add(metaField);
        }

        fieldList = appendCommonDate(fieldList, moduleCtgId);

        result.setData(fieldList);

        return result;
    }

    /**
     * 获取字段元信息集合V2
     * @param fieldMetaDataReq
     * @return
     */
    public ResponseMessage getFieldMetaDataListV2(FieldMetaDataReq fieldMetaDataReq) {
        ResponseMessage result = new ResponseMessage();

        String moduleCtgId = fieldMetaDataReq.getModuleCtgId();
        List<MetaField> fieldList = new ArrayList<>();

        //查询字段元信息
        if (CollUtil.isNotEmpty(fieldMetaDataReq.getFieldList())) {
            for (QueryFieldReq qf : fieldMetaDataReq.getFieldList()) {
                MetaField metaField = SSDMetaCacheManager.getField(qf.getId());

                //1 字段不存在
                //2 字段不显示，
                //通过code再获取一次
                if (metaField == null || !Enabled.value(metaField.getIsShow()) ) {

                    List<MetaField> metaFieldList = SSDMetaCacheManager.getFieldByCode(qf.getCode());

                    if(CollUtil.isEmpty(metaFieldList)){
                        continue;
                    }

                    //获取第一个显示的字段
                    for(MetaField mf:metaFieldList ){
                        if(Enabled.value(mf.getIsShow())){
                            metaField = mf;
                            break;
                        }
                    }

                }

                if (metaField == null) {
                    continue;
                }

                MetaField mf = metaField.clone();
                String ctgId = qf.getModuleCtgId();
                if(StrUtil.isEmpty(ctgId)){
                    if(CollUtil.isNotEmpty(metaField.getCategoryIdList())){
                        ctgId = metaField.getCategoryIdList().get(0);
                    }
                }
                mf.setModuleCtgId(ctgId);
                MetaFieldCategory metaFieldCategory = SSDMetaCacheManager.getCategoryById(ctgId);
                if (metaFieldCategory != null) {
                    mf.setCtgDataType(metaFieldCategory.getDataType());
                }

                String tableId = StrUtil.isNotEmpty(mf.getFactTableId()) ? mf.getFactTableId() : mf.getDimTableId();
                MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
                if (metaTable != null) {
                    mf.setAvailableDateGranularityList(metaTable.getAvailableDateGranularityList());
                }

                if (FieldType.CROSS_MODEL_MEASURE == FieldType.get(mf.getFieldType())) {
                    mf.setAvailableDateGranularityList(getCrossModelMeasureAvailableDateGranularityList(mf));
                }
                
                fieldList.add(mf);
            }
        }

        fieldList = appendCommonDate(fieldList, moduleCtgId);

        result.setData(fieldList);

        return result;
    }

    /**
     * 附加公共日期字段
     * @param fieldList
     * @param moduleCtgId
     * @return
     */
    public List<MetaField> appendCommonDate(List<MetaField> fieldList, String moduleCtgId) {
        boolean hasCommonDate = false;
        //添加公共日期
        for (MetaField metaField : fieldList) {

            List<MetaField> commonDateFieldList = new ArrayList<>();
            List<String> ctgIdList = new ArrayList<>();

            //moduleCtgId 优先从字段内部属性获取
            if (StrUtil.isNotEmpty(metaField.getModuleCtgId())) {
                ctgIdList.add(metaField.getModuleCtgId());
            }else{
                if (StrUtil.isNotEmpty(moduleCtgId)) {
                    ctgIdList.add(moduleCtgId);
                } else {
                    if (CollUtil.isEmpty(metaField.getCategoryIdList())) {
                        continue;
                    }
                    ctgIdList.addAll(metaField.getCategoryIdList());
                }
            }

            for (String ctgId : ctgIdList) {
                MetaFieldCategory metaFieldCategory = SSDMetaCacheManager.getCategoryById(ctgId);
                FieldUtil.getCtgCommonDateField(metaFieldCategory, commonDateFieldList);

                if (CollUtil.isNotEmpty(commonDateFieldList)) {
                    fieldList.add(commonDateFieldList.get(0));
                    hasCommonDate = true;
                    break;
                }
            }

            if (hasCommonDate) {
                break;
            }

        }

        //如果字段的目录中一个公共日期都没有，从所有字段中找一个
        if (!hasCommonDate) {
            for (MetaField mf : SSDMetaCacheManager.getFieldsCache().values()) {
                if (Enabled.value(mf.getIsCommonDate())) {
                    fieldList.add(mf);
                    break;
                }
            }
        }

        return fieldList;
    }

    public void getFieldEnumDataList(Long pkId, Integer threadNum) {
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

        String sqlId = "fieldDef.getFieldDimEnumMeta";
        final String updateSqlId = "fieldDef.updateFieldDimEnumMeta";
        BaseDao dao = DBUtil.getBaseDao();
        List<Map> filedInfos = (List<Map>) dao.queryObjectList(sqlId, pkId);
        if (BIUtil.isEmpty(filedInfos)) {
            return;
        }

        for (Map filedInfo : filedInfos) {
            if (BIUtil.isEmpty(filedInfo)) {
                continue;
            }

            Integer executed = (Integer) filedInfo.get("executed");
            if (executed != null && executed == 1) {
                continue;
            }

            executorService.submit(() -> {
                Map<String, Object> param = new HashMap<>();
                try {
                    Integer id = (Integer) filedInfo.get("pkId");
                    String filterSql = (String) filedInfo.get("filterSql");
                    if (BIUtil.isEmpty(filterSql) || id == null) {
                        return;
                    }

                    param.put("pkId", id);
                    param.put("queryBeginTime", new Date());
                    String sql = String.format("/*id:%s*/ select a.name as name from ( %s )a limit 10", id, filterSql);
                    List<BIMap> enumDataList = dao.queryMapListBySQL(sql, DataSourceType.Trino_Slave02);
                    if (BIUtil.isEmpty(enumDataList)) {
                        param.put("fieldEnums", "");
                    } else {
                        String enumData = enumDataList.stream().map(v -> v.get("name"))
                                .filter(Objects::nonNull).map(Object::toString)
                                .collect(Collectors.joining(","));

                        param.put("fieldEnums", enumData);
                    }
                    param.put("queryEndTime", new Date());
                    dao.update(updateSqlId, param);
                } catch (Throwable e) {
                    e.printStackTrace();
                    try {
                        param.put("queryEndTime", new Date());
                        param.put("queryInfo", e.getMessage());
                        dao.update(updateSqlId, param);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    /**
     * 更新过滤日志
     * @param entity
     * @return
     */
    public ResponseMessage updateFilterQueryLog(SSMFilterQueryLogEntity entity){

        try {

            if(StrUtil.isNotEmpty(entity.getSessionId())){
                this.dao.update( "ssm.query.updateFilterQueryLog", entity);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return new ResponseMessage();
    }

    /**
     * 通过黑名单获取字段
     * @return
     */
    public List<MetaFieldAuthBlackList> getFieldAuthBlackList() {
        Map<String, String> params = new HashMap<>();
        List<MetaFieldAuthBlackList> list = dao.queryObjectList("ssm.field.getFieldAuthBlackList", params, MetaFieldAuthBlackList.class);
        if (list == null) list = new ArrayList<>();
        List<MetaFieldAuthBlackList> res = new ArrayList<>(list.size() * 3);
        final String all = "all";
        for (MetaFieldAuthBlackList item : list) {
            res.add(item);
            if (!all.equals(item.getFieldCode())) {
                res.add(new MetaFieldAuthBlackList(item.getUserName(), item.getFieldGroup(), item.getFieldCode()  + "_" + AggExpressionType.Avg_By_Day.getCode(), item.getFieldTitle()));
                res.add(new MetaFieldAuthBlackList(item.getUserName(), item.getFieldGroup(), item.getFieldCode()  + "_" + AggExpressionType.Avg_By_Day_Real.getCode(), item.getFieldTitle()));
            }
        }
        return res;
    }

    /**
     * 通过白名单获取字段
     * @return
     */
    public List<MetaFieldAuthWhiteList> getFieldAuthWhiteList() {
        Map<String, String> params = new HashMap<>();
        List<MetaFieldAuthWhiteList> list = dao.queryObjectList("ssm.field.getFieldAuthWhiteList", params, MetaFieldAuthWhiteList.class);
        if (list == null) list = new ArrayList<>();
        List<MetaFieldAuthWhiteList> res = new ArrayList<>(list.size() * 3);
        final String all = "all";
        for (MetaFieldAuthWhiteList item : list) {
            res.add(item);
            if (!all.equals(item.getFieldCode())) {
                res.add(new MetaFieldAuthWhiteList(item.getUserName(), item.getFieldGroup(), item.getFieldCode()  + "_" + AggExpressionType.Avg_By_Day.getCode(), item.getFieldTitle()));
                res.add(new MetaFieldAuthWhiteList(item.getUserName(), item.getFieldGroup(), item.getFieldCode()  + "_" + AggExpressionType.Avg_By_Day_Real.getCode(), item.getFieldTitle()));
            }
        }
        return res;
    }

    /**
     * 查询字段数据更新时间
     * @param req
     * @return
     */
    public String queryFieldDataUpdateTime(FieldDataUpdateTimeReq req) {

        String result = "";

        String fieldCode = req.getFieldCode();
        List<String> queryTableNames = req.getQueryTableNames();

        if (CollUtil.isEmpty(queryTableNames)) {
            return result;
        }

        //通过字段code获取字段元信息
        List<MetaField> metaFieldList = SSDMetaCacheManager.getFieldByCode(fieldCode);
        if (CollUtil.isEmpty(metaFieldList)) {
            return result;
        }

        //找出字段在当前查询中使用的数据表名
        List<String> queryEtlJobs = new ArrayList<>();
        List<String> viewDependTableList = new ArrayList<>();

        for (MetaField metaField : metaFieldList) {
            String tableId = metaField.getTableId();
            MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
            if (metaTable == null) {
                continue;
            }

            //依赖作业和视图依赖表都为空，不处理
            if (CollUtil.isEmpty(metaTable.getEtlJobs()) && StrUtil.isEmpty(metaTable.getViewDependTables())) {
                continue;
            }

            if (queryTableNames.contains(metaTable.getFullName())) {
                if (StrUtil.isNotEmpty(metaTable.getViewDependTables())) {
                    viewDependTableList.add(metaTable.getViewDependTables());
                } else {
                    queryEtlJobs.addAll(metaTable.getEtlJobs());
                }
            }
        }

        //查询数据表对应的更新时间
        if (CollUtil.isEmpty(queryEtlJobs) && CollUtil.isEmpty(viewDependTableList)) {
            return result;
        }

        //实时从doris元数据信息中获取更新时间
        if (CollUtil.isNotEmpty(viewDependTableList)) {
            result = (String) dao.queryObject("ssm.doris.information.schema.getMaxTableDataUpdateTime", viewDependTableList);
        } else {
            result = (String) dao.queryObject("ssm.etl.info.queryLastJobFinishTime", queryEtlJobs);
        }

        return result;
    }


    /**
     * 查询字段数据更新时间
     * @param req
     * @return
     */
    public FieldDataUpdateTimeRsp queryFieldDataUpdateTimeV2(FieldDataUpdateTimeReq req) {

        FieldDataUpdateTimeRsp result = new FieldDataUpdateTimeRsp();

        String fieldCode = req.getFieldCode();
        List<String> queryTableNames = req.getQueryTableNames();

        if (CollUtil.isEmpty(queryTableNames)) {
            return result;
        }

        //通过字段code获取字段元信息
        List<MetaField> metaFieldList = SSDMetaCacheManager.getFieldByCode(fieldCode);
        if (CollUtil.isEmpty(metaFieldList)) {
            return result;
        }

        boolean isRtDataSet = false;
        MetaDataset dataset = SSDMetaCacheManager.getDataset(req.getDatasetId());
        if (dataset != null && DataTypeEnum.REAL_TIME == DataTypeEnum.codeOf(dataset.getDatasetType())) {
            isRtDataSet = true;
            result.setIsRt(Enabled.YES.getId());
        }

        //找出字段在当前查询中使用的数据表名
        List<String> queryEtlJobs = new ArrayList<>();
        List<String> viewDependTableList = new ArrayList<>();

        Integer rtTableCnt = 0;

        //获取实时的表列表
        List<String> rtTableNameList = new ArrayList<>();
        String rtTableNames = SC.v("ssm.rt.table.list", "");
        if (StrUtil.isNotEmpty(rtTableNames)) {
            rtTableNameList = Arrays.asList(StrUtil.split(rtTableNames, ","));
        }

        for (MetaField metaField : metaFieldList) {
            String tableId = metaField.getTableId();
            MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
            if (metaTable == null) {
                continue;
            }

            //依赖作业和视图依赖表都为空，不处理
            if (CollUtil.isEmpty(metaTable.getEtlJobs()) && StrUtil.isEmpty(metaTable.getViewDependTables())) {
                continue;
            }

            if (queryTableNames.contains(metaTable.getFullName())) {

                if (rtTableNameList.contains(metaTable.getFullName())) {
                    rtTableCnt++;
                }

                if (queryTableNames.contains(metaTable.getFullName())) {
                    //只有实时表才取viewDependTables
                    DataTypeEnum dataType = DataTypeEnum.codeOf(metaTable.getDataProcessType());
                    if (StrUtil.isNotEmpty(metaTable.getViewDependTables()) && DataTypeEnum.REAL_TIME == dataType) {

                        //如果是1h的准实时，依赖的表是视图本身，从数仓提供的准时表时间维表获取时间
                        DataSliceGranularity sliceType = DataSliceGranularity.get(metaTable.getDataSliceCfgObj().getSliceType());
                        if (DataSliceGranularity.ONE_HOUR == sliceType) {
                            viewDependTableList.add(metaTable.getFullName());
                        }else{
                            viewDependTableList.add(metaTable.getViewDependTables());
                        }

                    } else {
                        queryEtlJobs.addAll(metaTable.getEtlJobs());
                    }
                }
            }
        }

        //实时数据暂时获取不到更新时间，展示提示文案
        //实时数据更新时间，可以通过指标【数据更新时间】查看
        //具体实时更新时间正在建设中
        if (rtTableCnt > 0 && !isRtDataSet) {
            result.setIsRt(Enabled.YES.getId());
            result.setIsShowRemark(Enabled.YES.getId());
            String remark = SC.v("ssm.rt.data.update.time.remark", "实时数据更新时间，可以通过指标【数据更新时间】查看");
            result.setRemark(remark);
        }

        //查询数据表对应的更新时间
        if (CollUtil.isEmpty(queryEtlJobs) && CollUtil.isEmpty(viewDependTableList)) {
            return result;
        }

        String dataUpdateTime = "";
        //实时从doris元数据信息中获取更新时间
        if (CollUtil.isNotEmpty(viewDependTableList)) {
            Map<String, String> dorisTableDataUpdateTimeMap = TableDataUpdateTimeUtil.buildDorisTableDataUpdateTimeMap(viewDependTableList);

            DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
            //获取准实时的更新时间
            String nearRealtimeLastBatch = (String) dao.queryObject("ssm.doris.information.schema.getNearRealtimeLastBatch", null, dataSourceType);

            for (String viewDependTable : viewDependTableList) {

                String dorisTableDataUpdateTime = dorisTableDataUpdateTimeMap.get(viewDependTable);

                //数仓规范准实时的表，更新时间一致
                //准实时判定条件，数据切分粒度 = 1h
                MetaTable metaTable = SSDMetaCacheManager.getTableByFullName(viewDependTable);
                if (metaTable != null) {
                    DataSliceGranularity sliceType = DataSliceGranularity.get(metaTable.getDataSliceCfgObj().getSliceType());
                    if (DataSliceGranularity.ONE_HOUR == sliceType) {
                        dorisTableDataUpdateTime = nearRealtimeLastBatch;
                    }
                }

                dataUpdateTime = com.bi.queryer.util.period.DateUtil.getMaxDate(dataUpdateTime, dorisTableDataUpdateTime);
            }
        } else {
            dataUpdateTime = (String) dao.queryObject("ssm.etl.info.queryLastJobFinishTime", queryEtlJobs);
        }
        result.setDataUpdateTime(dataUpdateTime);
        return result;
    }

    /**
     * 更新作业结束时间
     * 用于查询作业的最后更新时间
     * @param etlJobName
     * @param etlJobFinishTime
     */
    public void updateEtlLastEndTime(String etlJobName, String etlJobFinishTime) {

        //不在多维使用的etl_job列表中，不处理
        if (!SSDMetaCacheManager.getTableEtlJobSet().contains(etlJobName)) {
            return;
        }

        Integer etlJobCount = (Integer) dao.queryObject("ssm.etl.info.queryEtlJobCount", etlJobName);

        SysEtlJobInfo sysEtlJobInfo = new SysEtlJobInfo();
        sysEtlJobInfo.setEtlJob(etlJobName);
        sysEtlJobInfo.setLastEndTime(etlJobFinishTime);

        if (etlJobCount == 0) {
            dao.insert("ssm.etl.info.add", sysEtlJobInfo);
        } else {
            dao.insert("ssm.etl.info.update", sysEtlJobInfo);
        }

    }

}
