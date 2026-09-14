package com.bi.queryer.util.dataBaseImport;

/**
 * @author contributor
 */
public enum DataBaseType {

    Mysql("mysql","dataBaseImportMysql"),
    Sqlserver("sqlserver","dataBaseImportSqlserver");


    private String id;

    private String className;

    private DataBaseType(String id,String className){
        this.id = id;
        this.className = className;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public static DataBaseType get(String id){
        for(DataBaseType m : DataBaseType.values()){
            if(m.getId().equalsIgnoreCase(id)){
                return m;
            }
        }
        return Mysql;
    }
}
