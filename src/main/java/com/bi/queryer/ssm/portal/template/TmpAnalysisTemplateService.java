package com.bi.queryer.ssm.portal.template;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.portal.enums.AnalysisTplType;
import com.bi.queryer.ssm.portal.template.entity.TmpAnalysisTplCtgRelEntity;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.query.template.model.TemplateCtgTreeRsp;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2026/4/20 15:09
 * @Description:
 */

@Service
@Scope("prototype")
public class TmpAnalysisTemplateService {
    @Autowired
    protected BaseDao dao;

    @Autowired
    private QueryTemplateCategoryService categoryService;

    @Autowired
    private TemplateConfigService configService;

    /**
     * 我的空间下、当前用户有权限的目录中挂载的临时看板列表。
     * 目录与挂载关系与 {@link TemplateConfigService#getCategoryTplTree}（isMySpace=true）一致；
     * 返回字段与 {@link #getProdAnalysisTplByPortalId(String)} 对齐（analysisTplId、analysisTplName、hasAiSummary；临时看板无门户菜单 menuId 为 null）。
     */
    public List<AnalysisTemplateVO> listMySpaceMountedTmpAnalysisTpl() {
        List<String> ctgRootIds = new ArrayList<>(4);
        ctgRootIds.add(QueryTemplateCategoryType.MY.getId());
        ctgRootIds.add(QueryTemplateCategoryType.FAV.getId());
        ctgRootIds.add(QueryTemplateCategoryType.SHARE.getId());

        List<QueryTemplateCategory> categories = categoryService.getAllCategories();

        List<QueryTemplateCategory> ctgTreeRsps = new ArrayList<>();
        for (QueryTemplateCategory ctg : categories) {
            if (ctgRootIds.contains(ctg.getParentId())) {
                ctgTreeRsps.add(ctg);
            }
        }

        List<TemplateCtgTreeRsp> result = configService.getSubCtgIds(ctgTreeRsps);
        List<String> allCtgIds = result.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
        allCtgIds.addAll(ctgRootIds);

        Map<String, Object> queryMap = new HashMap<>(4);
        queryMap.put("userName", UserManager.get().getName());
        queryMap.put("ctgIds", allCtgIds);
        List<TmpAnalysisTplCtgRelEntity> rows = dao.queryObjectList(
                "ssd.tmpAnalysisTplCtgRel.listMountedTmpAnalysisByUserAndCtgIds",
                queryMap,
                TmpAnalysisTplCtgRelEntity.class);
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        List<AnalysisTemplateVO> resultList = new ArrayList<>();
        for (TmpAnalysisTplCtgRelEntity row : rows) {
            AnalysisTemplateVO vo = new AnalysisTemplateVO();
            vo.setAnalysisTplId(row.getAnalysisTplId());
            vo.setAnalysisTplName(row.getAnalysisTplName());
            vo.setHasAiSummary(row.getHasAiSummary());
            vo.setAnalysisTplType(AnalysisTplType.TMP.getCode());
            resultList.add(vo);
        }
        return resultList;
    }
}
