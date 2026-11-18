package com.gzu.chronic.common;

/** 业务异常：统一由 GlobalExceptionHandler 转成 {code,msg} */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        this(2001, msg);
    }

    public int getCode() {
        return code;
    }
}
