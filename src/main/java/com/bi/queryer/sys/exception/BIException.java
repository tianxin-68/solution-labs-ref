package com.bi.queryer.sys.exception;

public class BIException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public static String CODE_ERROR = "error";

	public static String CODE_WARN = "warn";

	public static String CODE_INFO = "info";

	private String code = CODE_ERROR;

	public BIException() {

	}

	public BIException(String message) {
		super(message);
	}

	public BIException(String message, String code) {
		this(message);
		this.code = code;
	}

	public BIException(Throwable e) {
		super(e);
	}

	public BIException(String message, Throwable e) {
		super(message, e);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
