package com.bi.queryer.ssm.query.interceptor;

import cn.hutool.json.JSONUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.cross.CrossDimensionItemBuilder;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.mail.MailItem;
import com.bi.queryer.ssm.mail.MailServer;
import com.bi.queryer.ssm.monitor.ServiceMonitor;
import com.bi.queryer.ssm.query.SSDQueryManager;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 12:55
 * Description:
 */
@Component
@Aspect
public class ControllerInterceptor {

	@Pointcut(value = "execution(* com.bi.queryer.ssm.query.SSDQueryController.*(..)) " +
			"|| execution(* com.bi.queryer.ssm.query.chart.SSMChartQueryController.*(..))" +
			"|| execution(* com.bi.queryer.ssm.query.grid.header.SSMGridHeaderQueryController.*(..))" +
			"|| execution(* com.bi.queryer.ssm.query.field.QueryFieldController.*(..))" +
			"|| execution(* com.bi.queryer.ssm.custom.CustomFieldController.*(..))" +
			"|| execution(* com.bi.queryer.ssm.export.SSDExporterController.*(..))" +
			"|| execution(* com.bi.queryer.ssm.qa.SSDQuestAndAnswerController.*(..))" +
			"|| execution(* com.bi.queryer.ssm.sensitive.SensitiveApplyController.*(..))")
	public void pointCut() {

	}

	@Around(value = "pointCut()")
	public Object handleControllerMethod(ProceedingJoinPoint pjp) {
		Object result = null;
		Map<String, String> paramMap = null;
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		try {

			BaseController controller = (BaseController) pjp.getTarget();
			controller.initStreamParameters();
			// 执行方法
			MethodSignature ms = (MethodSignature) pjp.getSignature();
			Class<?> targetCls = pjp.getTarget().getClass();
			String targetClsName = targetCls.getName();
			//String targetObjectMethodName = targetClsName + "." + ms.getName();
			ResponseMessage checkResult =  this.checkEnable(controller, targetCls, ms);// this.checkServerHealth(targetClsName, ms.getName());
			if(checkResult.getSuccess()) {
				paramMap = controller.toStringMap();
				result = pjp.proceed(pjp.getArgs());
			}else {
				result = checkResult;
			}
		} catch (Throwable throwable) {
			result = new ResponseMessage(throwable);
			Class exception = throwable.getClass();
			if (exception.equals(SSDException.class)) {
				SSDException ssdException = (SSDException) throwable;
				// 无效模型异常：携带结构化模型信息供前端展示
				if (!ssdException.getInvalidModelInfo().isEmpty()) {
					((ResponseMessage) result).setData(ssdException.getInvalidModelInfo());
				}
				// 若是自定义字段合法性校验，则不发邮件
				if (SSDException.Custom_Field_Check_Error.equalsIgnoreCase(ssdException.getType())
						|| SSDException.Manual_Kill_Error.equalsIgnoreCase(ssdException.getType())) {
					return result;
				}
			}
			// 发送错误邮件
			this.sendExceptionMail(throwable, request, paramMap);
		}finally {
			// 移除数据源
			DataSourceRouter.removeCurrentDataSourceType();

			// 清理列维度数据集缓存
			CrossDimensionItemBuilder.clearResultDataSetThreadCache();
		}
		return result;
	}

	public String getRemoteHost(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
	}

	protected void sendExceptionMail(Throwable exception, HttpServletRequest request, Map<String, String> paramMap){
		// 发送错误邮件
		MailItem mailItem = new MailItem();
		User user = UserManager.get();
		if (user != null) {
			mailItem.setUserName(user.getName());
		}

		//IP地址
		String ipAddr = getRemoteHost(request);
		mailItem.setSubject(SC.appName + "-后台错误");
		mailItem.setInfo(BIUtil.getStackTrace(exception));
		mailItem.setBeginTime(Calendar.getInstance().getTime());
		mailItem.setEndTime(Calendar.getInstance().getTime());
		String passBackParams = SSDUtil.getMapToString(paramMap);
		mailItem.setQuerySQL("请求ip:" + ipAddr + "，请求参数:" + passBackParams);
		QueryEngine queryEngine = SSDQueryManager.getQueryEngine();

		if (queryEngine != null && queryEngine.getModels() != null) {
			Set<String> tableOwners = queryEngine.getModels().stream().map(StarModel::getFactTable)
					.map(k -> k.getMeta().getTableOwner())
					.collect(Collectors.toSet());
			Set<String> tableList = queryEngine.getModels().stream().map(StarModel::getFactTable)
					.map(k -> k.getMeta().getName()).collect(Collectors.toSet());
			mailItem.setQuerySQL("请求ip:" + ipAddr + "，请求参数:" + passBackParams + "，查询表:" + JSONUtil.toJsonStr(tableList));
			mailItem.getMailTo().addAll(tableOwners);
			SSDQueryManager.remove();
		}

		if(exception instanceof SSDException) {
			SSDException ssdException = (SSDException) exception;
			mailItem.getMailTo().addAll(ssdException.getMailTo());
			if(BIUtil.isNotEmpty(ssdException.getSql())){
				mailItem.setQuerySQL("sql:" + ssdException.getSql() + "，" + mailItem.getQuerySQL());
			}
		}

		MailServer.send(mailItem);
	}

	/**
	 * 当前请求是否可用
	 * @param controller
	 * @return
	 */
	protected ResponseMessage checkEnable(BaseController controller, Class<?> clazz, MethodSignature ms){
		ResponseMessage checkResult = this.checkServerHealth(clazz.getName(), ms.getName());
		if(!checkResult.getSuccess()){
			return checkResult;
		}
		// 限流
		// checkResult = RateLimiter.limit(controller, clazz, ms);
		return checkResult;
	}

	protected ResponseMessage checkServerHealth(String clazzName, String methodName){
		ResponseMessage result = new ResponseMessage();
		String[] methodCheckList = new String[]{"SSDQueryController.buildQueryResultGrid", "SSDQueryController.buildResultSQL", "SSDQueryController.buildResultQueryCount", "SSDQueryController.buildFilterData"};
		boolean isInCheckList = false;
		String methodFullName = clazzName + "." + methodName;
		for(String m : methodCheckList){
			if(methodFullName.endsWith(m)){
				isInCheckList = true;
				break;
			}
		}
		if(!isInCheckList){
			return result;
		}
		result = ServiceMonitor.monitorServerHealth();
		return result;
	}
}
