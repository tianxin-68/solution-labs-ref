package com.bi.queryer.ssm.export.log;


import com.bi.queryer.sys.base.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
@Qualifier("SSDExportLogService")
public class SSDExportLogService {

	@Autowired
	protected BaseDao dao;

	public void log(SSDExportLogEntity entity){
		try{
			SSDExportLogWriter biSysLogWriter = new SSDExportLogWriter(this.dao, "ssm.query.insertExportLog", entity);
			biSysLogWriter.start();
		}catch(Throwable e){
			e.printStackTrace();
		}
	}

	public void submitLog(SSDExportLogEntity entity){
		try{
			SSDExportLogWriter biSysLogWriter = new SSDExportLogWriter(this.dao, "ssm.query.insertSubmitExportLog", entity);
			biSysLogWriter.start();
		}catch(Throwable e){
			e.printStackTrace();
		}
	}
}

/**
 * 启用单独的线程执行日志入库操作
 *
 * @author contributor
 *
 */
class SSDExportLogWriter extends Thread {
	private BaseDao dao = null;
	private Object params = null;
	private String sqlId = "";

	public SSDExportLogWriter(BaseDao dao, String sqlId, Object params) {
		this.dao = dao;
		this.sqlId = sqlId;
		this.params = params;
	}

	public void run() {
		try {
			dao.insert(sqlId, params);
		} catch (Exception e) {
			System.out.println("SSD导出日志添加出错！！");
			e.printStackTrace();
		}

	}

}
