package com.bi.queryer.ssm.query.interceptor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.mail.MailItem;
import com.bi.queryer.ssm.mail.MailServer;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 12:55
 * Description:
 */
@Component
@Aspect
public class SSMControllerInterceptor {

    @Pointcut(value = "execution(* com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryController.*(..))" +
            "|| execution(* com.bi.queryer.ssm.query.template.TemplateController.*(..))")
    public void pointCut() {

    }

    @Around(value = "pointCut()")
    public Object handleControllerMethod(ProceedingJoinPoint pjp) {
        Object result = null;
        JSONObject paramMap = new JSONObject();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        //IP地址
        String ipAddr = getRemoteHost(request);
        try {
            result = pjp.proceed(pjp.getArgs());
            try {
                for (Object arg : pjp.getArgs()) {
                    paramMap = JSONUtil.parseObj(arg);
                }
            } catch (Exception e) {
               // System.out.println("解析报错：" + e.getMessage());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            result = new SSMResponseMessage(false, throwable.getMessage());
            // 发送错误邮件
            MailItem mailItem = new MailItem();
            User user = UserManager.get();
            if (user != null) {
                mailItem.setUserName(user.getName());
            }
            mailItem.setSubject(SC.appName + "-后台错误");
            /*
            Class exception = throwable.getClass();
            if (exception.equals(SSDException.class)) {
                SSDException ssdException = (SSDException) throwable;
                if (SSDException.UnAuth_Key.equalsIgnoreCase(ssdException.getType())) {
                    mailItem.setSubject(SC.appName + "-权限不足");
                }
                return result;
            }
             */
            mailItem.setInfo(BIUtil.getStackTrace(throwable));
            mailItem.setBeginTime(Calendar.getInstance().getTime());
            mailItem.setEndTime(Calendar.getInstance().getTime());
            String passBackParams = paramMap.toString();
            mailItem.setQuerySQL("请求ip:" + ipAddr + "，请求参数:" + passBackParams);
            MailServer.send(mailItem);
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
}
