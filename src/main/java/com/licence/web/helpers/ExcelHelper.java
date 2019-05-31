package com.licence.web.helpers;

import com.licence.web.models.pojo.KeyspaceContentObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelHelper {
    private Integer maxCellLength = 32000;
    private String cellLengthTooLongMessage = "... -> *** Data too long! Export this table as JSON to view the content ***";

    public ExcelHelper(Integer maxCellLength, String cellLengthTooLongMessage) {
        this.maxCellLength = maxCellLength;
        this.cellLengthTooLongMessage = cellLengthTooLongMessage;
    }

    public XSSFWorkbook createTable(KeyspaceContentObject key) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        if (!key.getContent().isEmpty()) {
            Integer nrColumns = key.getColumnDefinitions().size();

            CreationHelper creationHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet(key.getTableName());

            // Create a Font for styling header cells
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerFont.setFontName("Arial");
            //headerFont.setColor(IndexedColors.RED.getIndex());
            // Create a CellStyle with the font
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Create a Row
            Row headerRow = sheet.createRow(0);

            // Create cells
            for (int i = 0; i < nrColumns; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(key.getColumnDefinitions().getName(i));
                cell.setCellStyle(headerCellStyle);
                sheet.setColumnWidth(i, 5000);
            }
            for (int i = 0; i < key.getContent().size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<Object> rowContent = key.getContent().get(i).entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
                for (int c = 0; c < nrColumns; c++) {
                    Cell cell = row.createCell(c);
                    if (rowContent.get(c) != null) {
                        try {
                            if(rowContent.get(c).toString().length() > maxCellLength) {
                                cell.setCellValue(rowContent.get(c).toString().substring(0, maxCellLength) + cellLengthTooLongMessage);
                            } else {
                                cell.setCellValue(rowContent.get(c).toString());
                            }
                        } catch (IllegalArgumentException e) {
                            cell.setCellValue("Row too long!");
                        }
                    }
                }
            }
            // Resize all columns to fit the content size
//            for(int i = 1; i < nrColumns;  i++) {
//                sheet.autoSizeColumn(i);
//            }
        }
        return workbook;
    }
}
