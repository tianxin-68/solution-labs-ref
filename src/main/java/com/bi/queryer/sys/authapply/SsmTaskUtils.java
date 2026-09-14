package com.bi.queryer.sys.authapply;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.bi.queryer.ssm.enums.DataSensitiveLevel;
import com.bi.queryer.ssm.meta.MetaDataset;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.authapply.model.BaseTemplate;
import com.bi.queryer.sys.authapply.model.ChildTemplateList;
import com.bi.queryer.sys.authapply.model.TaskParam;
import com.bi.queryer.sys.authapply.model.ssm.SsmModulePermissionChildTemplate;
import com.bi.queryer.sys.authapply.model.ssm.SsmRowPermissionChildTemplate;
import com.bi.queryer.sys.authapply.model.ssm.SsmWorkOrderMainTemplate;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.menu.MenuService;
import com.bi.queryer.sys.user.constant.SsdDataAuthApplyType;
import com.bi.queryer.sys.user.model.HrEmployee;
import com.bi.queryer.sys.user.vo.SsdDataAuthVo;
import com.bi.queryer.sys.user.vo.SsmAuthApplyVo;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SsmTaskUtils {

    private static final String ROW_FORM_NAME = "申请行级权限";
    private static final String MODULE_FORM_NAME = "申请模块权限";

    private SsmTaskUtils() {
    }

    public static Integer createTask(String url, TaskParam taskParam) {
        Map<String, String> headerMap = new java.util.HashMap<>();
        headerMap.put("Content-Type", "application/json");
        try {
            System.out.println("创建SSM工单参数：" + JSON.toJSONString(taskParam));
            System.out.println("创建SSM工单地址：" + url);
            HttpResponse response = HttpRequest.post(url)
                    .addHeaders(headerMap)
                    .body(JSON.toJSONString(taskParam))
                    .execute();
            System.out.println("创建SSM工单response：" + response);
            String result = response.body();
            System.out.println("创建SSM工单结果：" + result);
            if (BIUtil.isNotEmpty(result)) {
                com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(result);
                if (object.containsKey("success") && object.getBoolean("success")) {
                    com.alibaba.fastjson.JSONObject data = object.getJSONObject("data");
                    return data.getInteger("taskId");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SsmWorkOrderApprovalRoute buildApprovalRoute(MenuService menuService, HrEmployee hrEmployee,
                                                             SsdDataAuthVo ctgVo) {
        Map<String, MetaFieldCategory> categoryMap = SSDMetaCacheManager.getFrontCategories();
        SsmWorkOrderApprovalRoute route = new SsmWorkOrderApprovalRoute();
        route.setDirectLeader(menuService.getDirectLeaderEmail(hrEmployee));
        String businessOwner = menuService.ssmCtgBusinessOwnerByCtgId(ctgVo.getItemCode(),
                SC.v("ssm.default.approver", "contributor@example.com"));
        route.setBusinessOwner(businessOwner);
        String dataOwner = menuService.ssmCtgOwnerByCtgId(ctgVo.getItemCode(),
                SC.v("ssm.default.approver", "contributor@example.com"));
        route.setDataOwner(dataOwner);
        route.setDataOwnerLeader(menuService.queryCtgOwnerLeader(dataOwner));
        String sensitiveLevel = resolveCtgSensitiveLevel(ctgVo, categoryMap);
        route.setDataSensitiveLevel(sensitiveLevel);
        route.setSecurityOwner(resolveDefaultSecurityOwner());
        return route;
    }

    private static String resolveDefaultSecurityOwner() {
        return SC.v("ssm.default.securityOwner",
                SC.v("ssm.download.securityEMail", "contributor@example.com"));
    }

    /**
     * @param group 已合并的工单分组（route + 目录列表，分组阶段已算好审批链路，此处不再重复计算）
     */
    public static TaskParam createSsmTaskParam(User user, HrEmployee hrEmployee, SsmAuthApplyService.WorkOrderGroup group,
                                               SsmAuthApplyVo ssmAuthApplyVo) {
        Map<String, MetaFieldCategory> categoryMap = SSDMetaCacheManager.getFrontCategories();
        SsmWorkOrderApprovalRoute route = group.getRoute();
        List<SsdDataAuthVo> ctgList = group.getDisplayCtgList();

        TaskParam taskParam = new TaskParam();
        SsmWorkOrderMainTemplate mainTemplate = new SsmWorkOrderMainTemplate();
        mainTemplate.setApplyUserEmail(user.getEmail());
        mainTemplate.setDirectLeader(nvlEmail(route.getDirectLeader()));
        mainTemplate.setBusinessOwner(nvlEmail(route.getBusinessOwner()));
        mainTemplate.setApplyerName(user.getRealName());
        mainTemplate.setApplyerDept(getEmployeeDept(hrEmployee));
        mainTemplate.setApplyDataset(buildApplyDataset(ctgList, categoryMap));
        mainTemplate.setApplyReason(ssmAuthApplyVo.getApplyReason());
        mainTemplate.setDataOwner(nvlEmail(route.getDataOwner()));
        mainTemplate.setDataOwnerLeader(nvlEmail(route.getDataOwnerLeader()));
        mainTemplate.setDataSensitiveLevel(formatSensitiveLevel(route.getDataSensitiveLevel()));
        mainTemplate.setSecurityOwner(nvlEmail(route.getSecurityOwner()));
        taskParam.setTemplateList(mainTemplate.buildTemplateList());
        taskParam.setChildrenTemplateList(buildChildrenTemplateList(group, ssmAuthApplyVo, categoryMap));
        return taskParam;
    }

    private static List<ChildTemplateList> buildChildrenTemplateList(SsmAuthApplyService.WorkOrderGroup group,
                                                                     SsmAuthApplyVo ssmAuthApplyVo,
                                                                     Map<String, MetaFieldCategory> categoryMap) {
        List<ChildTemplateList> children = new ArrayList<>();
        ChildTemplateList rowPermissionChild = buildRowPermissionChildList(ssmAuthApplyVo);
        children.add(rowPermissionChild != null ? rowPermissionChild : buildEmptyRowPermissionChildList());
        ChildTemplateList modulePermissionChild = buildModulePermissionChildList(group.getCtgList(), categoryMap);
        children.add(modulePermissionChild != null ? modulePermissionChild : buildEmptyModulePermissionChildList());
        return children;
    }

    private static ChildTemplateList buildEmptyRowPermissionChildList() {
        ChildTemplateList childTemplateList = new ChildTemplateList();
        childTemplateList.setFormName(ROW_FORM_NAME);
        SsmRowPermissionChildTemplate template = new SsmRowPermissionChildTemplate();
        template.setRowDim("");
        template.setHasRowPermissions("");
        template.setAddedRowPermissions("");
        template.setRemovedRowPermissions("");
        childTemplateList.setTemplateBaseEntitiesList(Collections.singletonList(template.buildChildTemplate()));
        return childTemplateList;
    }

    private static ChildTemplateList buildEmptyModulePermissionChildList() {
        ChildTemplateList childTemplateList = new ChildTemplateList();
        childTemplateList.setFormName(MODULE_FORM_NAME);
        SsmModulePermissionChildTemplate template = new SsmModulePermissionChildTemplate();
        template.setDataContentName("");
        template.setDataUsageScenario("");
        template.setSensitiveLevel("");
        template.setPermissionDuration("");
        childTemplateList.setTemplateBaseEntitiesList(Collections.singletonList(template.buildChildTemplate()));
        return childTemplateList;
    }

    private static ChildTemplateList buildRowPermissionChildList(SsmAuthApplyVo ssmAuthApplyVo) {
        // dataAuthList 为空但用户已有行级权限时，仍需把已有权限带进工单（仅当二者都为空才跳过）
        if (BIUtil.isEmpty(ssmAuthApplyVo.getDataAuthList()) && BIUtil.isEmpty(ssmAuthApplyVo.getOwnAuthList())) {
            return null;
        }
        Set<String> dimNames = collectRowDimNames(ssmAuthApplyVo);
        if (dimNames.isEmpty()) {
            return null;
        }
        ChildTemplateList childTemplateList = new ChildTemplateList();
        childTemplateList.setFormName(ROW_FORM_NAME);
        List<List<BaseTemplate>> rows = new ArrayList<>();
        for (String dimName : dimNames) {
            String hasRowPermissions = buildRowPermissionValues(ssmAuthApplyVo.getOwnAuthList(), dimName, null);
            String addedRowPermissions = buildRowPermissionValues(ssmAuthApplyVo.getDataAuthList(), dimName, true);
            String removedRowPermissions = buildRowPermissionValues(ssmAuthApplyVo.getDataAuthList(), dimName, false);
            // 无新增/删除，但有已有权限时也保留该维度行，确保已有权限展示在工单中
            if (BIUtil.isEmpty(addedRowPermissions) && BIUtil.isEmpty(removedRowPermissions)
                    && BIUtil.isEmpty(hasRowPermissions)) {
                continue;
            }
            SsmRowPermissionChildTemplate template = new SsmRowPermissionChildTemplate();
            template.setRowDim(dimName);
            template.setHasRowPermissions(hasRowPermissions);
            template.setAddedRowPermissions(addedRowPermissions);
            template.setRemovedRowPermissions(removedRowPermissions);
            rows.add(template.buildChildTemplate());
        }
        if (rows.isEmpty()) {
            return null;
        }
        childTemplateList.setTemplateBaseEntitiesList(rows);
        return childTemplateList;
    }

    private static ChildTemplateList buildModulePermissionChildList(List<SsdDataAuthVo> ctgList,
                                                                      Map<String, MetaFieldCategory> categoryMap) {
        if (BIUtil.isEmpty(ctgList)) {
            return null;
        }
        ChildTemplateList childTemplateList = new ChildTemplateList();
        childTemplateList.setFormName(MODULE_FORM_NAME);
        List<List<BaseTemplate>> rows = new ArrayList<>();
        for (SsdDataAuthVo ctgVo : ctgList) {
            MetaFieldCategory category = categoryMap.get(ctgVo.getItemCode());
            SsmModulePermissionChildTemplate template = new SsmModulePermissionChildTemplate();
            String dataContentName = BIUtil.isNotEmpty(ctgVo.getItemValue()) ? ctgVo.getItemValue()
                    : (category != null ? category.getName() : ctgVo.getItemCode());
            // 未开启权限继承的目录（继承标记由前台传入 isInherited=0），名称后追加标注
            if (Enabled.NO.getId().equals(ctgVo.getIsInherited())) {
                dataContentName = dataContentName + "（该目录未开启权限继承）";
            }
            template.setDataContentName(dataContentName);
            template.setDataUsageScenario(resolveDataUsageScenario(ctgVo, category));
            template.setSensitiveLevel(formatSensitiveLevel(resolveCtgSensitiveLevel(ctgVo, categoryMap)));
            template.setPermissionDuration(formatPermissionDuration(ctgVo.getMonths()));
            rows.add(template.buildChildTemplate());
        }
        childTemplateList.setTemplateBaseEntitiesList(rows);
        return childTemplateList;
    }

    private static Set<String> collectRowDimNames(SsmAuthApplyVo ssmAuthApplyVo) {
        Set<String> dimNames = new LinkedHashSet<>();
        collectDimNames(dimNames, ssmAuthApplyVo.getOwnAuthList());
        collectDimNames(dimNames, ssmAuthApplyVo.getDataAuthList());
        return dimNames;
    }

    private static void collectDimNames(Set<String> dimNames, List<SsdDataAuthVo> authList) {
        if (authList == null) {
            return;
        }
        authList.stream()
                .map(SsdDataAuthVo::getDimName)
                .filter(BIUtil::isNotEmpty)
                .forEach(dimNames::add);
    }

    private static String buildRowPermissionValues(List<SsdDataAuthVo> authList, String dimName, Boolean addType) {
        if (authList == null || authList.isEmpty()) {
            return "";
        }
        List<SsdDataAuthVo> filtered = authList.stream()
                .filter(vo -> dimName.equals(vo.getDimName()))
                .filter(vo -> {
                    if (addType == null) {
                        return true;
                    }
                    return addType ? SsdDataAuthApplyType.isAdd(vo.getApplyType())
                            : SsdDataAuthApplyType.isDelete(vo.getApplyType());
                })
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return "";
        }
        return filtered.stream()
                .map(SsdDataAuthVo::getItemValue)
                .filter(BIUtil::isNotEmpty)
                .collect(Collectors.joining("、"));
    }

    private static String buildApplyDataset(List<SsdDataAuthVo> ctgList, Map<String, MetaFieldCategory> categoryMap) {
        Set<String> datasetNames = new LinkedHashSet<>();
        for (SsdDataAuthVo ctgVo : ctgList) {
            MetaFieldCategory category = categoryMap.get(ctgVo.getItemCode());
            if (category == null || BIUtil.isEmpty(category.getDatasetId())) {
                continue;
            }
            MetaDataset dataset = SSDMetaCacheManager.getDataset(category.getDatasetId());
            if (dataset != null && BIUtil.isNotEmpty(dataset.getDatasetName())) {
                datasetNames.add(dataset.getDatasetName());
            }
        }
        if (datasetNames.isEmpty()) {
            return "---";
        }
        return StringUtils.join(datasetNames, "、");
    }

    private static String resolveDataUsageScenario(SsdDataAuthVo ctgVo, MetaFieldCategory category) {
        if (BIUtil.isNotEmpty(ctgVo.getDataUsageScenario())) {
            return ctgVo.getDataUsageScenario();
        }
        if (category == null) {
            return "---";
        }
        if (BIUtil.isNotEmpty(category.getDataDesc())) {
            return category.getDataDesc();
        }
        if (BIUtil.isNotEmpty(category.getCtgDesc())) {
            return category.getCtgDesc();
        }
        MetaDataset dataset = SSDMetaCacheManager.getDataset(category.getDatasetId());
        if (dataset != null && BIUtil.isNotEmpty(dataset.getDataDesc())) {
            return dataset.getDataDesc();
        }
        return "---";
    }

    public static String resolveCtgSensitiveLevel(SsdDataAuthVo ctgVo, Map<String, MetaFieldCategory> categoryMap) {
        if (BIUtil.isNotEmpty(ctgVo.getSensitiveLevel())) {
            return ctgVo.getSensitiveLevel();
        }
        MetaFieldCategory category = categoryMap.get(ctgVo.getItemCode());
        return resolveCategorySensitiveLevel(category);
    }

    public static String resolveCategorySensitiveLevel(MetaFieldCategory category) {
        if (category == null || BIUtil.isEmpty(category.getFields())) {
            return DataSensitiveLevel.C1.getCode();
        }
        return category.getFields().stream()
                .map(MetaField::getSensitiveLevel)
                .filter(BIUtil::isNotEmpty)
                .map(DataSensitiveLevel::get)
                .max(Comparator.comparingInt(DataSensitiveLevel::getLevel))
                .orElse(DataSensitiveLevel.C1)
                .getCode();
    }

    public static String resolveMaxSensitiveLevel(List<SsdDataAuthVo> authList) {
        if (authList == null || authList.isEmpty()) {
            return DataSensitiveLevel.C1.getCode();
        }
        return authList.stream()
                .map(SsdDataAuthVo::getSensitiveLevel)
                .filter(BIUtil::isNotEmpty)
                .map(DataSensitiveLevel::get)
                .max(Comparator.comparingInt(DataSensitiveLevel::getLevel))
                .orElse(DataSensitiveLevel.C1)
                .getCode();
    }

    private static String formatPermissionDuration(Integer months) {
        if (months == null || months <= 0) {
            return "12个月";
        }
        return months + "个月";
    }

    private static String formatSensitiveLevel(String sensitiveLevel) {
        if (BIUtil.isEmpty(sensitiveLevel)) {
            return DataSensitiveLevel.C1.getCode().toUpperCase();
        }
        return sensitiveLevel.toUpperCase();
    }

    private static String nvlEmail(String email) {
        return BIUtil.isEmpty(email) ? "" : email.trim();
    }

    private static String getEmployeeDept(HrEmployee hrEmployee) {
        if (hrEmployee == null || BIUtil.isEmpty(hrEmployee.getDeptName())) {
            return "";
        }
        // 部门全称直接取 deptName
        return hrEmployee.getDeptName();
    }
}
