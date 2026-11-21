package com.chronic.modules.miniapp.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 患者发起在线问诊
 *
 * @author chronic
 */
@Data
public class MpClinicDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long patientId;

    /** 1图文 2视频 */
    private String visitType;

    @NotBlank(message = "请填写病情描述")
    private String chiefComplaint;

    /** 主诉附图，逗号分隔的 /upload/xxx 相对路径，最多 6 张 */
    private String imageUrls;
}