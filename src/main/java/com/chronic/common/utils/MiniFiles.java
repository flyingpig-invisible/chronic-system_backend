package com.chronic.common.utils;

import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 临时文件清理工具
 * <p>
 * 小程序侧上传的华为运动健康导出包体积较大，解析完成后必须及时删除，
 * 避免临时目录不断堆积。
 *
 * @author chronic
 */
public final class MiniFiles {

    private MiniFiles() {
    }

    /** 静默删除文件或目录，异常不抛出 */
    public static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                FileSystemUtils.deleteRecursively(path);
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // 清理失败不影响业务结果
        }
    }
}
