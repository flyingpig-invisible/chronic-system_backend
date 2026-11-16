package com.chronic.common.exception;

import com.chronic.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author chronic
 */
@Getter
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public ServiceException(String message) {
        super(message);
        this.code = ResultCode.FAIL;
    }

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public static ServiceException of(String message) {
        return new ServiceException(message);
    }
}
