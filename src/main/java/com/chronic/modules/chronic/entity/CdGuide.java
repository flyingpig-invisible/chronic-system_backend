package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 远程指导
 *
 * @author chronic
 */
@Data
@TableName("cd_guide")
@Schema(description = "远程指导")
public class CdGuide implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long guideId;

    private Long patientId;

    private String patientName;

    @Schema(description = "关联预警ID")
    private Long warningId;

    @Schema(description = "指导类型：1调药 2用药提醒 3饮食推荐")
    private String guideType;

    private String content;

    private Long doctorId;

    private String doctorName;

    private LocalDateTime guideTime;

    @Schema(description = "已读状态：0未读 1已读")
    private String readStatus;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
