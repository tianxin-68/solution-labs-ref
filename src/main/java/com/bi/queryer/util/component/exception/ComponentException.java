package com.bi.queryer.util.component.exception;

public class ComponentException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ComponentException() {

	}

	public ComponentException(String message) {
		super(message);
	}

	public ComponentException(Throwable e) {
		super(e);
	}

	public ComponentException(String message, Throwable e) {
		super(message, e);
	}
}
