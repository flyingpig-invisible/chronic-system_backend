package com.chronic.modules.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库文件上传结果
 *
 * @author chronic
 */
@Data
@Schema(description = "知识库文件上传结果")
public class KbFileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "访问地址，如 /upload/kb/xxx.pdf")
    private String fileUrl;

    @Schema(description = "文件哈希 SHA-256（服务端自动计算）")
    private String fileHash;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件大小可读展示，如 1.2 MB")
    private String fileSizeText;

    @Schema(description = "文件后缀，如 pdf")
    private String fileExt;

    @Schema(description = "若解析出正文，则返回正文内容")
    private String content;

    @Schema(description = "是否重复上传（哈希已存在）")
    private boolean duplicate;

    @Schema(description = "重复时命中的已有文档ID")
    private Long duplicateDocId;

    @Schema(description = "重复时命中的已有文档标题")
    private String duplicateDocTitle;
}
