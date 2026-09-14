package com.bi.queryer.sys.base;

import cn.hutool.core.io.IoUtil;
import com.bi.queryer.ssm.engine.session.QuerySessionManager;
import com.bi.queryer.sys.base.proxy.ConnectionProxy;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceContextHolder;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.exception.QueryCancelException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import io.trino.jdbc.TrinoConnection;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.*;
import java.util.*;

@Service
@Scope("prototype")
public class BaseDao {

    protected ThreadLocal<SqlSession> txSession = new ThreadLocal<SqlSession>();

    @Autowired
    @Qualifier("sqlSessionFactory")
    protected SqlSessionFactory sessionFactory = null;

    @Autowired
    @Qualifier("transactionTemplate")
    protected TransactionTemplate txTemplate = null;

    public TransactionTemplate getTxTemplate() {
        return txTemplate;
    }

    public void setTxTemplate(TransactionTemplate txTemplate) {
        this.txTemplate = txTemplate;
    }

    public SqlSessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void executeTranscation(DataSourceType dsType, final AbstractTransaction myTx) {
        try {
            DataSourceContextHolder.setType(dsType.getId());
            myTx.setDao(this);
            this.txTemplate.execute(new TransactionCallback<Object>() {
                public Object doInTransaction(TransactionStatus ts) {
                    myTx.execute();
                    return null;
                }
            });
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            if (txSession.get() != null) {
                txSession.get().close();
            }
            this.txSession.remove();
        }
    }

	/*
	public void startTransaction(DataSourceType dsType) {
		DataSourceContextHolder.setType(dsType.getId());
		SqlSession sqlSession = sessionFactory.openSession(false);
		sqlSession.clearCache();
		this.txSession.set(sqlSession);
	}

	public void endTransaction() {
		SqlSession sqlSession = this.txSession.get();
		if(sqlSession == null) {
			System.out.println("事物结束异常：sqlSession为空");
		}
		sqlSession.commit();
	}

	public void rollback() {
		SqlSession sqlSession = this.txSession.get();
		if(sqlSession == null) {
			System.out.println("事物回滚异常：sqlSession为空");
		}
		sqlSession.rollback();
	}
	*/

    /**
     * 获取session
     *
     * @param dsType
     * @return
     */
    protected SqlSession getSession(DataSourceType dsType) {
        SqlSession session = this.txSession.get();
        if (session == null) {
            DataSourceContextHolder.setType(dsType.getId());
            session = sessionFactory.openSession(true); // 自动事物提交
        }
        return session;
    }

    /**
     * 只关闭非事物中的session，事物中的session由事物关闭
     *
     * @param sqlSession
     */
    protected void closeSession(SqlSession sqlSession) {
        if (sqlSession != null && this.txSession.get() == null) {
            sqlSession.close();
        }
    }


    public Connection getConn(DataSourceType dsType) throws SQLException {
        Connection conn = null;
        SqlSession sqlSession = null;
        try {
            sqlSession = getSession(dsType);
            conn = sqlSession.getConfiguration().getEnvironment().getDataSource().getConnection();
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return conn;
    }

    public <T> T queryObject(String sqlId, Object param, Class<T> tClass) {
        SqlSession sqlSession = null;
        T result;
        try {
            DataSourceContextHolder.setType(DataSourceType.Default.getId());
            sqlSession = sessionFactory.openSession();
            result = sqlSession.selectOne(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Object queryObject(String sqlId, Object param, DataSourceType dsType) {
        SqlSession sqlSession = null;
        Object result = null;
        try {
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            result = sqlSession.selectOne(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Object queryObject(String sqlId, Object param) {
        return this.queryObject(sqlId, param, DataSourceType.Default);
    }


    public List<?> queryObjectList(String sqlId, Object param, DataSourceType dsType) {
        DataSourceContextHolder.setType(dsType.getId());
        if (DBType.Presto == DBType.getType(dsType.getDialect())) {
            return this.queryObjectListByPresto(sqlId, (Map) param);
        } else if (DBType.Trino == DBType.getType(dsType.getDialect())) {
            return this.queryObjectListByTrino(sqlId, (Map) param, dsType);
        }
        SqlSession sqlSession = null;
        List<?> result = null;
        try {
            sqlSession = sessionFactory.openSession();
            result = sqlSession.selectList(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public <T> List<T> queryObjectList(String sqlId, Object param, Class<T> tClass) {
        SqlSession sqlSession = null;
        List<T> result;
        try {
            DataSourceContextHolder.setType(DataSourceType.Default.getId());
            sqlSession = sessionFactory.openSession();
            result = sqlSession.selectList(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }

        if (result == null) {
            return new ArrayList<>();
        }

        return result;
    }

    public List<?> queryObjectList(String sqlId, Object param) {
        return this.queryObjectList(sqlId, param, DataSourceType.Default);
    }


    public List<BIMap> queryMapList(String sqlId, Map<String, ?> param, DataSourceType dsType) {
        SqlSession sqlSession = null;
        List<BIMap> result = null;
        try {
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            result = sqlSession.selectList(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public List<BIMap> queryMapList(String sqlId, Map<String, ?> param) {
        return queryMapList(sqlId, param, DataSourceType.Default);
    }


    public Object insert(String sqlId, Map<String, ?> param, DataSourceType dsType) {
        SqlSession sqlSession = null;
        Object result = null;
        try {
            sqlSession = getSession(dsType);
            result = sqlSession.insert(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Object insert(String sqlId, Map<String, ?> param) {
        return this.insert(sqlId, param, DataSourceType.Default);
    }


    public Object insert(String sqlId, Object paramObject, DataSourceType dsType) {
        SqlSession sqlSession = null;
        Object result = null;
        try {
            sqlSession = getSession(dsType);
            result = sqlSession.insert(sqlId, paramObject);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Object insert(String sqlId, Object paramObject) {
        return this.insert(sqlId, paramObject, DataSourceType.Default);
    }


    public void update(String sqlId, Map<String, ?> param, DataSourceType dsType) {
        SqlSession sqlSession = null;
        try {
            sqlSession = getSession(dsType);
            sqlSession.update(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
    }

    public void update(String sqlId, Map<String, ?> param) {
        this.update(sqlId, param, DataSourceType.Default);
    }


    public void update(String sqlId, Object paramObject, DataSourceType dsType) {
        SqlSession sqlSession = null;
        try {
            sqlSession = getSession(dsType);
            sqlSession.update(sqlId, paramObject);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
    }

    public void update(String sqlId, Object paramObject) {
        this.update(sqlId, paramObject, DataSourceType.Default);
    }


    public void delete(String sqlId, Map<String, ?> param, DataSourceType dsType) {
        SqlSession sqlSession = null;
        try {
            sqlSession = getSession(dsType);
            sqlSession.delete(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
    }

    public void delete(String sqlId, Map<String, ?> param) {
        this.delete(sqlId, param, DataSourceType.Default);
    }


    public void delete(String sqlId, Object param, DataSourceType dsType) {
        SqlSession sqlSession = null;
        try {
            sqlSession = getSession(dsType);
            sqlSession.delete(sqlId, param);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
    }

    public void delete(String sqlId, Object param) {
        this.delete(sqlId, param, DataSourceType.Default);
    }


    public List<BIMap> queryMapListBySQL(String sql, DataSourceType dsType) {
        SqlSession sqlSession = null;
        List<BIMap> result = null;
        try {
            if (DBType.Presto == DBType.getType(dsType.getDialect())) {
                result = this.queryMapListByPrestoSQL(sql, new HashMap());
                return result;
            } else if (DBType.Trino == DBType.getType(dsType.getDialect())) {
                result = this.queryMapListByTrinoSQL(sql, new HashMap(), dsType);
                return result;
            }
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            Map<String, String> params = new HashMap<>();
            params.put("sql", sql);
            result = sqlSession.selectList("base.queryMapListBySQL", params);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public List<BIMap> queryMapListBySQL(String sql) {
        return this.queryMapListBySQL(sql, DataSourceType.Default);
    }


    public Object queryObjectBySQL(String sql, DataSourceType dsType) {
        SqlSession sqlSession = null;
        Object result = null;
        try {
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            Map<String, String> params = new HashMap<>();
            params.put("sql", sql);
            result = sqlSession.selectOne("base.queryObjectBySQL", params);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Object queryObjectBySQL(String sql) {
        return this.queryObjectBySQL(sql, DataSourceType.Default);
    }


    public Map<String, ?> queryMapBySQL(String sql, DataSourceType dsType) {
        SqlSession sqlSession = null;
        Map<String, ?> result = null;
        try {
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            Map<String, String> params = new HashMap<>();
            params.put("sql", sql);
            result = sqlSession.selectOne("base.queryMapBySQL", params);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }


    public List<?> queryListBySQL(String sql, DataSourceType dsType) {
        SqlSession sqlSession = null;
        List<?> result = null;
        try {
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            Map<String, String> params = new HashMap<>();
            params.put("sql", sql);
            result = sqlSession.selectList("base.queryObjectListBySQL", params);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Integer queryCountBySQL(String sql, DataSourceType dsType) {
        SqlSession sqlSession = null;
        Integer result = null;
        try {
            if (DBType.getType(dsType.getDialect()) == DBType.Presto) {
                Map<String, String> sqlMap = new HashMap<>();
//				sqlMap.put("sql", sql);
                List<?> list = queryObjectListByPrestoSQL(sql, sqlMap, dsType);
                Integer count = 0;
                if (BIUtil.isNotEmpty(list)) {
                    Map m = (Map) list.get(0);
                    count = Integer.valueOf(m.values().iterator().next() + "");
                }
                return count;
            } else if (DBType.getType(dsType.getDialect()) == DBType.Trino) {
                Map<String, String> sqlMap = new HashMap<>();
//				sqlMap.put("sql", sql);
                List<?> list = queryObjectListByTrinoSQL(sql, sqlMap, dsType);
                Integer count = 0;
                if (BIUtil.isNotEmpty(list)) {
                    Map m = (Map) list.get(0);
                    count = Integer.valueOf(m.values().iterator().next() + "");
                }
                return count;
            }
            DataSourceContextHolder.setType(dsType.getId());
            sqlSession = sessionFactory.openSession();
            Map<String, String> params = new HashMap<>();
            params.put("sql", sql);
            Object o = sqlSession.selectOne("base.queryObjectBySQL", params);
            result = Integer.valueOf(o + "");
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
        return result;
    }

    public Integer queryCount(String sqlId, Map<String, ?> param, DataSourceType dsType) {
        Integer result = (Integer) this.queryObject(sqlId, param, dsType);
        return result;
    }

    public Integer queryCount(String sqlId, Object objectParam, DataSourceType dsType) {
        Integer result = (Integer) this.queryObject(sqlId, objectParam, dsType);
        return result;
    }

    public Integer queryCount(String sqlId, Map<String, ?> param) {
        return this.queryCount(sqlId, param, DataSourceType.Default);
    }

    public Integer queryCount(String sqlId, Object objectParam) {
        Integer result = (Integer) this.queryObject(sqlId, objectParam, DataSourceType.Default);
        return result;
    }

    public void execute(String sqlId, Map<String, ?> param, DataSourceType dsType) {
        this.update(sqlId, param, dsType);
    }


    public void executeSQL(String sql, DataSourceType dsType) {
        SqlSession sqlSession = null;
        try {
            sqlSession = getSession(dsType);
            Map<String, String> param = new HashMap<>();
            param.put("sql", sql);
            sqlSession.update("base.executeSql", sql);
        } catch (Exception e) {
            throw new BIException(e);
        } finally {
            closeSession(sqlSession);
        }
    }

    public void executeSQL(String sql) {
        this.executeSQL(sql, DataSourceType.Default);
    }

    public List<BIMap> queryMapListByPrestoSQL(String sql, Map param) {
        return (List<BIMap>) this.queryObjectListByPrestoSQL(sql, param);
    }

    public List<?> queryObjectListByPrestoSQL(String sql, Map param, DataSourceType dsType) {
        BIException biException = null;
        Statement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        List<Map> dataset = new ArrayList<Map>();
        System.out.println("====================MyBaseDao Presto Query Data Begin...====================");
        System.out.println(sql);
        Long t1 = System.currentTimeMillis();

        try {
            conn = DBUtil.getConn(DataSourceType.Trino_Master);
            stmt = conn.createStatement();
            res = stmt.executeQuery(sql);
            ResultSetMetaData metaData = res.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (res.next()) {
                BIMap map = new BIMap();
                for (int i = 1; i <= columnCount; i++) {
                    map.put(metaData.getColumnLabel(i), res.getObject(i));
                }
                dataset.add(map);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            biException = new BIException(e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                biException = new BIException(BIConsts.QUERY_ERROR_TIP);
            }
        }
        Long t2 = System.currentTimeMillis();
        System.out.println("====================MyBaseDao Presto Query Data End, Consume " + ((t2 - t1) / 1000) + "s====================");

        if (biException != null) {
            throw biException;
        }
        return dataset;
    }

    public List<?> queryObjectListByPrestoSQL(String sql, Map param) {
        return this.queryObjectListByPrestoSQL(sql, param, DataSourceType.Trino_Master);
    }

    public List<?> queryObjectListByPresto(String sqlId, Map param) {
        String sql = this.getMyBatisSql(sqlId, param);
        return this.queryObjectListByPrestoSQL(sql, param);
    }

    public List<BIMap> queryMapListByTrinoSQL(String sql, Map param, DataSourceType dsType) {
        return (List<BIMap>) this.queryObjectListByTrinoSQL(sql, param, dsType);
    }

    public List<?> queryObjectListByTrinoSQL(String sql, Map param, DataSourceType dsType) {
        BIException biException = null;
        Statement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        List<Map> dataset = new ArrayList<Map>();
        System.out.println("====================MyBaseDao Trino Query Data Begin...====================");
        System.out.println(sql);
        Long t1 = System.currentTimeMillis();

        try {

            conn = DBUtil.getConn(dsType);

            TrinoConnection tc = conn.unwrap(TrinoConnection.class);
            // 设置查询超时时间为1分钟
            tc.setSessionProperty("query_max_run_time", "60s");
            stmt = conn.createStatement();

            res = stmt.executeQuery(sql);
            ResultSetMetaData metaData = res.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (res.next()) {
                BIMap map = new BIMap();
                for (int i = 1; i <= columnCount; i++) {
                    map.put(metaData.getColumnLabel(i), res.getObject(i));
                }
                dataset.add(map);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            biException = new BIException(e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                biException = new BIException(BIConsts.QUERY_ERROR_TIP);
            }
        }
        Long t2 = System.currentTimeMillis();
        System.out.println("====================MyBaseDao Trino Query Data End, Consume " + ((t2 - t1) / 1000) + "s====================");

        if (biException != null) {
            throw biException;
        }
        return dataset;
    }

    public List<?> queryObjectListByTrino(String sqlId, Map param, DataSourceType dsType) {
        String sql = this.getMyBatisSql(sqlId, param);
        return this.queryObjectListByTrinoSQL(sql, param, dsType);
    }

    /**
     * 获取mybatis运行时sql
     *
     * @param id
     * @param parameterMap
     * @return
     */
    public String getMyBatisSql(String id, Map<String, Object> parameterMap) {
        String mybatisSql = "";
        MappedStatement ms = sessionFactory.getConfiguration().getMappedStatement(id);
        BoundSql boundSql = ms.getBoundSql(parameterMap);
        String prepareSql = boundSql.getSql();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Object[] parameterArray = new Object[]{};
        if (parameterMappings != null) {
            parameterArray = new Object[parameterMappings.size()];
            ParameterMapping parameterMapping = null;
            Object value = null;
            Object parameterObject = null;
            MetaObject metaObject = null;
            PropertyTokenizer prop = null;
            String propertyName = null;
            String[] names = null;
            for (int i = 0; i < parameterMappings.size(); i++) {
                parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    propertyName = parameterMapping.getProperty();
                    names = propertyName.split("\\.");
                    if (propertyName.indexOf(".") != -1 && names.length == 2) {
                        parameterObject = parameterMap.get(names[0]);
                        propertyName = names[1];
                    } else if (propertyName.indexOf(".") != -1 && names.length == 3) {
                        parameterObject = parameterMap.get(names[0]); // map
                        if (parameterObject instanceof Map) {
                            parameterObject = ((Map) parameterObject).get(names[1]);
                        }
                        propertyName = names[2];
                    } else {
                        parameterObject = parameterMap.get(propertyName);
                    }
                    metaObject = parameterMap == null ? null
                            : MetaObject.forObject(parameterObject, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                            SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY);
                    prop = new PropertyTokenizer(propertyName);
                    if (parameterObject == null) {
                        value = null;
                    } else if (ms.getConfiguration().getTypeHandlerRegistry()
                            .hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX)
                            && boundSql.hasAdditionalParameter(prop.getName())) {
                        value = boundSql.getAdditionalParameter(prop.getName());
                        if (value != null) {
                            value = MetaObject.forObject(value, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                                            SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY)
                                    .getValue(propertyName.substring(prop.getName().length()));
                        }
                    } else {
                        value = metaObject == null ? null : metaObject.getValue(propertyName);
                    }
                    parameterArray[i] = value;
                }
            }
        }

        if (parameterArray == null || StringUtil.isEmpty(prepareSql)) {
            return "";
        }
        mybatisSql = prepareSql;
        List<Object> parameterList = Arrays.asList(parameterArray);
        List<Object> list = new ArrayList<Object>(parameterList);
        while (prepareSql.indexOf("?") != -1 && list.size() > 0 && parameterArray.length > 0) {
            mybatisSql = mybatisSql.replaceFirst("\\?", list.get(0).toString());
            list.remove(0);
        }
        mybatisSql = mybatisSql.replaceAll("(\r?\n(\\s*\r?\n)+)", "\r\n");

        return mybatisSql;
    }

    public void executeSqlByDoris(String sql, DataSourceType dsType){
        Statement stmt = null;
        Connection conn = null;
        System.out.println("====================MyBaseDao Doris Execute Data Begin...====================");
        System.out.println(sql);
        Long t1 = System.currentTimeMillis();

        try {
            conn = DBUtil.getConn(dsType);
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (Throwable e) {
            //e.printStackTrace();
            throw new BIException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Long t2 = System.currentTimeMillis();
        System.out.println("====================MyBaseDao Doris Query Data End, Consume " + ((t2 - t1) / 1000) + "s====================");
    }

    public List<Map> queryObjectListByDorisSQL(String sql, DataSourceType dsType) {
        BIException biException = null;
        Statement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        List<Map> dataset = new ArrayList<Map>();
        System.out.println("====================MyBaseDao Doris Query Data Begin...====================");
        System.out.println(sql);
        Long t1 = System.currentTimeMillis();

        try {

            conn = DBUtil.getConn(dsType);
            stmt = conn.createStatement();
            res = stmt.executeQuery(sql);
            ResultSetMetaData metaData = res.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (res.next()) {
                BIMap map = new BIMap();
                for (int i = 1; i <= columnCount; i++) {
                    map.put(metaData.getColumnLabel(i), res.getObject(i));
                }
                dataset.add(map);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            biException = new BIException(e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                biException = new BIException(BIConsts.QUERY_ERROR_TIP);
            }
        }
        Long t2 = System.currentTimeMillis();
        System.out.println("====================MyBaseDao Doris Query Data End, Consume " + ((t2 - t1) / 1000) + "s====================");

        if (biException != null) {
            throw biException;
        }
        return dataset;
    }


    @SuppressWarnings("unchecked")
    public List<BIMap> queryObjectListBySQL(DataSourceType dsType, String sql, String sessionId) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet res = null;
        try {
            conn = ConnectionProxy.getConn(dsType, sessionId);
            stmt = conn.createStatement();
            if (conn.isWrapperFor(TrinoConnection.class)) {
                TrinoConnection tc = conn.unwrap(TrinoConnection.class);
                tc.setSessionProperty("query_max_run_time", "60s");
            }

            // 记录sessionId+queryId，便于后面kill
            QuerySessionManager.add(sessionId, "", dsType.getKey());
            res = stmt.executeQuery(sql);

            List<BIMap> dataset = new ArrayList<>();
            ResultSetMetaData metaData = res.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (res.next()) {
                BIMap map = new BIMap();
                for (int i = 1; i <= columnCount; i++) {
                    map.put(metaData.getColumnLabel(i), res.getObject(i));
                }
                dataset.add(map);
            }
            return dataset;
        } catch (Throwable t) {
            try {
                if (stmt != null && stmt.isClosed()) {
                    throw new QueryCancelException("查询已经取消");
                }
            } catch (SQLException ignored) {
            }
            throw new BIException(t);
        } finally {
            IoUtil.close(res);
            IoUtil.close(stmt);
            IoUtil.close(conn);
            QuerySessionManager.delete(sessionId);
        }
    }
}

