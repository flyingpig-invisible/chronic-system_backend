package com.chronic.modules.chronic.support.huawei;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入过程中的一条问题记录（用于前端展示 / 写入导入日志）
 *
 * @author chronic
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportIssue {

    /** 序号（解析到的第几条记录，从 1 开始） */
    private Integer seq;

    /** 问题原因 */
    private String reason;

    public static ImportIssue of(int seq, String reason) {
        return new ImportIssue(seq, reason);
    }
}
