package com.bi.queryer.ssm.meta.flush;

import com.bi.queryer.ssm.meta.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MetaCache {

    private List<MetaField> metaFieldList = new ArrayList<>();

    private List<MetaTableEtlJob> metaTableEtlJobList = new ArrayList<>();

    private List<MetaTable> tablesList = new ArrayList<>();

    private List<MetaFieldDataAuth> fieldDataAuthList = new ArrayList<>();

    private List<MetaTableRelation> tableRelationList = new ArrayList<>();

    private List<MetaTablePriSubCfg> tablePriSubCfgList = new ArrayList<>();

    private List<MetaFieldValueMap> fieldItemMaps = new ArrayList<>();


    private List<MetaFieldCategory> categoryList = new ArrayList<>();

}
