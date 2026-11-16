package com.chronic.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 *
 * @author chronic
 */
@Data
@Schema(description = "分页响应结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页数据")
    private List<T> rows;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setTotal(page.getTotal());
        r.setRows(page.getRecords());
        return r;
    }

    public static <T> PageResult<T> of(Long total, List<T> rows) {
        PageResult<T> r = new PageResult<>();
        r.setTotal(total);
        r.setRows(rows);
        return r;
    }
}
