package com.bi.queryer.ssm.mgr.exportImport;

/**
 * @author contributor
 */
public enum MgrType {

    TableDef("tableDef","表管理.xlsx"),

    TableRel("tableRel","表关联.xlsx"),

    FieldDef("fieldDef","字段管理.xlsx"),

    FieldExclude("fieldExclude","字段互斥管理.xlsx"),

    FieldCtg("fieldCtg","字段目录管理.xlsx");

    private String id;
    private String name;

    private MgrType(String id,String name){
        this.id = id;
        this.name = name;
    }

    public static  MgrType get(String id){
        for(MgrType t : values()){
            if(t.getId().equalsIgnoreCase(id)){
                return t;
            }
        }
        return TableDef;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
