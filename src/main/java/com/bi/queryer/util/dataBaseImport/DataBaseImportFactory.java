package com.bi.queryer.util.dataBaseImport;

import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.dataBaseImport.vo.TableField;
import com.bi.queryer.util.dataBaseImport.vo.TableInfo;
import com.bi.queryer.util.SpringContextUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
public abstract class DataBaseImportFactory {

    public static List<TableInfo> getAllTable(String dataSourceKey){

        List<TableInfo> result = new ArrayList<>();

        try {

            Map<String,Object> map = getCurrentInterfaceClass(dataSourceKey);
            DataBaseImportInterface dataBaseImportInterface = (DataBaseImportInterface)map.get("DataBaseImportInterface");

            if(dataBaseImportInterface!=null) {
                result = dataBaseImportInterface.getAllTable((DataSourceType)map.get("DataSourceType"));
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return  result;
    }

    public static List<TableField> getField(String tableSchema,String name,String dataSourceKey){

        List<TableField> tableField = new ArrayList<>();

        try {

            Map<String,Object> map = getCurrentInterfaceClass(dataSourceKey);
            DataBaseImportInterface dataBaseImportInterface = (DataBaseImportInterface)map.get("DataBaseImportInterface");

            if(dataBaseImportInterface!=null) {
                Map<String,Object> mapField = new HashMap<>();
                mapField.put("tableSchema",tableSchema);
                mapField.put("name",name);
                tableField = dataBaseImportInterface.getField(mapField,(DataSourceType)map.get("DataSourceType"));
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return tableField;
    }


    protected static Map<String,Object> getCurrentInterfaceClass(String dataSourceKey) {

        Map<String,Object> map = new HashMap<>();

        try{

            DataBaseImportInterface d = null;

            DataSourceType dataSourceType = DataSourceType.getTypeById(dataSourceKey);
            String type = dataSourceType.getDialect();

            DataBaseType dataBaseType = DataBaseType.get(type);
            d  = (DataBaseImportInterface) SpringContextUtil.getBean(dataBaseType.getClassName());

            map.put("DataSourceType",dataSourceType);
            map.put("DataBaseImportInterface",d);

        }catch (Exception e){
            e.printStackTrace();
        }

        return map;
    }

    public static String getFieldDataType(String type){

        if(BIUtil.isEmpty(type)) return "";

        String str = "";

        switch (type.toLowerCase()) {
            case "varchar":
            case "nvarchar":
            case "char":
            case "longtext":

                str = "String";
                break;

            case "int":
            case "tinyint":
            case "smallint":
            case "bigint":

                str = "Integer";
                break;

            case "decimal":
            case "numeric":
            case "float":

                str = "Double";
                break;

            case "datetime":

                str = "Datetime";
                break;
            case "date":
                str = "Date";
                break;

        }
        return str;
    }
}
