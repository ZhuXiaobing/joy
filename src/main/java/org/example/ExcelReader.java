package org.example;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelReader {

    @Autowired
    private ResourceLoader resourceLoader;
    static List<StockConfig> stockConfigs = new ArrayList<>();
    public List<StockConfig> getStockConfigs() {
        if (!stockConfigs.isEmpty()) {
            return stockConfigs;
        }
        String excelFilePath = "stockinfo.xlsx";
        List<StockConfig> stockConfigs = new ArrayList<>();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            for (Row row : sheet) { // 迭代每一行
                if (row.getRowNum() != 0) {
                    stockConfigs.add(new StockConfig(
                            row.getCell(0).getStringCellValue(),
                            row.getCell(1).getStringCellValue(),
                            row.getCell(2).getNumericCellValue(),
                            row.getCell(3).getNumericCellValue(),
                            row.getCell(4).getBooleanCellValue()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stockConfigs;
    }
}