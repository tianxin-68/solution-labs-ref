package com.bi.queryer.sys.interceptor;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionInterceptor {
	
	@ExceptionHandler(value= {Exception.class, RuntimeException.class})
	@ResponseBody
	public SSMResponseMessage catchException(Exception e) {
		// TODO log
		e.printStackTrace();
		return SSMResponseMessage.operationFailed(e.getMessage());
	}
	
	private String getStackTrace(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			throwable.printStackTrace(pw);
			return sw.toString();
		} finally {
			pw.close();
		}
	}
}
