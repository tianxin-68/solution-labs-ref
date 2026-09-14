package com.bi.queryer.util.dataBaseImport;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.dataBaseImport.vo.TableField;
import com.bi.queryer.util.dataBaseImport.vo.TableInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
@Service
public class DataBaseImportPresto implements DataBaseImportInterface{

    @Autowired
    protected BaseDao dao;

    @Override
    public List<TableInfo> getAllTable(DataSourceType dataSourceType) {
      return null;
    }

    @Override
    public List<TableField> getField(Map<String,Object> map, DataSourceType dataSourceType) {
       return null;
    }

}
