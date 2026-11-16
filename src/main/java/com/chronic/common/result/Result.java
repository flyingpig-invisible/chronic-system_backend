package com.chronic.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author chronic
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码：200成功")
    private Integer code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "业务数据")
    private T data;

    public static <T> Result<T> success() {
        return build(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return build(200, "操作成功", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return build(200, msg, data);
    }

    public static <T> Result<T> error(String msg) {
        return build(500, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return build(code, msg, null);
    }

    public static <T> Result<T> build(Integer code, String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
