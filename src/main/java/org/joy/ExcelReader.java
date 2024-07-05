package org.joy;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ExcelReader {
    @Value("${joy.alarm.interval:12}")
    private int interval;

    @Resource
    private AlarmTaskIncreaseRate alarmTaskIncreaseRate;
    List<StockConfig> stockConfigs = new ArrayList<>();
    static AtomicInteger seconds = new AtomicInteger();
    public List<StockConfig> getStockConfigs() {
        // 每隔一分钟更新一下stock配置。
        if ((!stockConfigs.isEmpty()) && (seconds.getAndIncrement() % (60/interval) != 0)) {
            return stockConfigs;
        }
        List<StockConfig> newStockConfigs = new ArrayList<>();

        String excelFilePath = "D:\\workcode\\joy\\stockinfo.xlsx";
        try (InputStream inputStream = new FileInputStream(new File(excelFilePath));
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            for (Row row : sheet) { // 迭代每一行
                if (row.getRowNum() != 0) {
                    newStockConfigs.add(new StockConfig(
                            row.getCell(0).getStringCellValue(),
                            row.getCell(1).getStringCellValue(),
                            row.getCell(2).getNumericCellValue(),
                            row.getCell(3).getNumericCellValue(),
                            row.getCell(4).getBooleanCellValue()
                    ));
                    // 用stock code 作为key.
                    alarmTaskIncreaseRate.addPriceDequeIntoCache(row.getCell(0).getStringCellValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stockConfigs = newStockConfigs;
        return stockConfigs;
    }
}