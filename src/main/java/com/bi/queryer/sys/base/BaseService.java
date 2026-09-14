package com.bi.queryer.sys.base;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.bi.queryer.sys.common.ReturnMsg;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.util.SpringContextUtil;

public class BaseService<T> implements Initializable{
	
	transient protected Logger logger = Logger.getLogger(getClass());
	
	protected BaseDao dao = null;

	@Override
	public void initialize(Map<String, ?> initParams) throws BIException {
		
	}
	
	/**
	 * 查询所有记录
	 * @param sqlId
	 * @param param
	 * @return
	 */
	public List<T> queryList(String sqlId, Object param){
		List<T> result = (List<T>) dao.queryObjectList(sqlId, param);
		return result;
	}
	
	/**
	 * 通过id查询单条记录
	 * @param sqlId
	 * @param param
	 * @return
	 */
	public T queryObject(String sqlId, Object param){
		T result = (T) dao.queryObject(sqlId, param);
		return result;
	}
	
	public ReturnMsg insert(String sqlId, T param, DataSourceType dsType){
		ReturnMsg result = new ReturnMsg();
		try{
			dao.insert(sqlId, param, dsType);
		}catch(Throwable e){
			result = new ReturnMsg(e);
			logger.error(e);
			throw new BIException(e);
		}
		return result;
	}
	
	/**
	 * 插入
	 * @param sqlId
	 * @param param
	 * @return
	 */
	public ReturnMsg insert(String sqlId, T param){
		return insert(sqlId, param, DataSourceType.Default);
	}
	
	/**
	 * 更新
	 * @param sqlId
	 * @param param
	 * @return
	 */
	public ReturnMsg update(String sqlId, T param){
		ReturnMsg result = new ReturnMsg();
		try{
			dao.update(sqlId, param);
		}catch(Throwable e){
			result = new ReturnMsg(e);
			logger.error(e);
			throw new BIException(e);
		}
		return result;
	}
	
	/**
	 * 删除
	 * @param sqlId
	 * @param param
	 * @return
	 */
	public ReturnMsg delete(String sqlId, Object param){
		return delete(sqlId, param, DataSourceType.Default);
	}
	
	/**
	 * 删除
	 * @param sqlId
	 * @param param
	 * @return
	 */
	public ReturnMsg delete(String sqlId, Object param, DataSourceType dsType){
		ReturnMsg result = new ReturnMsg();
		try{
			dao.delete(sqlId, param, dsType);
		}catch(Throwable e){
			result = new ReturnMsg(e);
			logger.error(e);
			throw new BIException(e);
		}
		return result;
	}
	
	public Integer count(String sqlId, Object param){
		Integer count = dao.queryCount(sqlId, param);
		return count;
	}
	
	public BaseDao getDao() {
		if(dao == null){
			dao = (BaseDao) SpringContextUtil.getBean("baseDao");
		}
		return dao;
	}

	public void setDao(BaseDao dao) {
		this.dao = dao;
	}
}
