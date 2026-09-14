package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.sys.enums.Enabled;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TemplatePageListReq {

    /**
     * 目录类型 查询所有模版时不传
     */
    private String categoryType;

    /**
     * 搜索关键字
     */
    private String templateSearchText;

    /**
     * 目录ID 他人分享传-share、我的模版传-my、查询所有模版时传-1或不传, 下钻目录
     */
    private String ctgId = "-1";

    //查询一批目录的下的内容， 不下钻目录
    private List<String> ctgIds;

    //模板id
    private List<String> tplIds;

    /**
     * 分页参数-第几页
     */
    private Integer currPageNo = 1;

    /**
     * 分页参数-每页数量
     */
    private Integer prePageSize = 30;

    /**
     * 显示所有
     */
    private Integer isShowAll = Enabled.NO.getId();

    private String sortKey;

    private String sortType;
    /**
     * 是否展示所有目录的模板（忽略权限）
     */
    private Integer showAllCtg = Enabled.NO.getId();

    //数据集数据类型
    private String datasetType;
}
