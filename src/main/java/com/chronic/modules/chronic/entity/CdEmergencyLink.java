package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 急救联动记录
 *
 * @author chronic
 */
@Data
@TableName("cd_emergency_link")
@Schema(description = "急救联动记录")
public class CdEmergencyLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long linkId;

    private Long warningId;

    private Long patientId;

    private String patientName;

    @Schema(description = "联动渠道：120/hospital/family/community")
    private String channel;

    private String receiver;

    private String linkResult;

    private String linkBy;

    private LocalDateTime linkTime;

    private String remark;
}
