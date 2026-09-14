package com.bi.queryer.sys.base;

/**
 * 事物
 * @author contributor
 *
 */
public abstract class AbstractTransaction {
	
	protected BaseDao dao = null;
	
	public BaseDao getDao() {
		return dao;
	}

	public void setDao(BaseDao dao) {
		this.dao = dao;
	}
	
	/**
	 * 事物执行内容，由子类实现
	 */
	abstract public void execute();
}
