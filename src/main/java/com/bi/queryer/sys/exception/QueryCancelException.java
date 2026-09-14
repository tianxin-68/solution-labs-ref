package com.bi.queryer.sys.exception;

public class QueryCancelException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public QueryCancelException() {

    }

    public QueryCancelException(String message) {
        super(message);
    }

    public QueryCancelException(Throwable e) {
        super(e);
    }

    public QueryCancelException(String message, Throwable e) {
        super(message, e);
    }
}
