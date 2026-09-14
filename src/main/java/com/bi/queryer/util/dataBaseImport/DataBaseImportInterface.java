package com.bi.queryer.util.dataBaseImport;

import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.dataBaseImport.vo.TableField;
import com.bi.queryer.util.dataBaseImport.vo.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
public interface DataBaseImportInterface {

    List<TableInfo> getAllTable(DataSourceType dataSourceType) ;

    List<TableField> getField(Map<String, Object> map, DataSourceType dataSourceType);
}
