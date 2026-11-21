package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 健康计划项
 *
 * @author chronic
 */
@Data
public class MpPlanItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;

    private String itemType;

    private String itemTypeName;

    private String content;

    private String frequency;

    private String targetValue;
}