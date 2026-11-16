package com.chronic.modules.kb.service;

import com.chronic.modules.kb.vo.KbFileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文件服务
 *
 * @author chronic
 */
public interface KbFileService {

    /** 上传文件：保存到本地、服务端计算 SHA-256、按需提取正文、检测重复 */
    KbFileVO upload(MultipartFile file);
}
