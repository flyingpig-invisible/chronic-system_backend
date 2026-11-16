package com.chronic.common.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 导出工具
 *
 * @author chronic
 */
@Slf4j
public class ExcelUtils {

    /**
     * 导出 Excel
     *
     * @param response  响应
     * @param fileName  文件名（不含扩展名）
     * @param sheetName sheet 名称
     * @param headers   表头
     * @param rows      数据行
     */
    public static void export(HttpServletResponse response, String fileName, String sheetName,
                              String[] headers, List<Object[]> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // 表头样式
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // 数据行
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Object[] data = rows.get(i);
                for (int j = 0; j < data.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(data[j] == null ? "" : data[j].toString());
                }
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fullName = fileName + "_" + timestamp + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                    + URLEncoder.encode(fullName, StandardCharsets.UTF_8));

            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        } catch (Exception e) {
            log.error("Excel 导出失败", e);
            throw new RuntimeException("Excel 导出失败：" + e.getMessage());
        }
    }
}
