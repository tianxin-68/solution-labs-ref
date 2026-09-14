package com.bi.queryer.util.component.exception;

public class PortletException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PortletException() {

	}

	public PortletException(String message) {
		super(message);
	}

	public PortletException(Throwable e) {
		super(e);
	}

	public PortletException(String message, Throwable e) {
		super(message, e);
	}
}