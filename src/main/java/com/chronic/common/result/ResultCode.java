package com.chronic.common.result;

/**
 * 响应状态码
 *
 * @author chronic
 */
public interface ResultCode {

    /** 成功 */
    int SUCCESS = 200;

    /** 参数错误 */
    int BAD_REQUEST = 400;

    /** 未认证 */
    int UNAUTHORIZED = 401;

    /** 无权限 */
    int FORBIDDEN = 403;

    /** 资源不存在 */
    int NOT_FOUND = 404;

    /** 业务失败 */
    int FAIL = 500;

    /** 业务校验失败 */
    int VALIDATE_FAIL = 601;
}
