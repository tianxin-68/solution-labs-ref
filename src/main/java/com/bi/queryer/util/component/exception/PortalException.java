package com.bi.queryer.util.component.exception;

public class PortalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PortalException() {

	}

	public PortalException(String message) {
		super(message);
	}

	public PortalException(Throwable e) {
		super(e);
	}

	public PortalException(String message, Throwable e) {
		super(message, e);
	}
}
