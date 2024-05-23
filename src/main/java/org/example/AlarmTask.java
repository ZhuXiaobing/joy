package org.example;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 告警任务
 */
@Component
public class AlarmTask {

    private static final Logger logger = LoggerFactory.getLogger(AlarmTask.class);
    @Value("${joy.myself.bid:true}")
    boolean bidReport;
    @Value("${joy.myself.ask:true}")
    boolean askReport;
    @Value("${joy.alarm.interval:12}")
    private int interval;
    @Resource
    public ExcelReader excelReader;
    static AtomicInteger seconds = new AtomicInteger();
    // 上午 9:25 到 11:30 每 5 秒执行一次
    @Scheduled(cron = "*/5 * 9-11 * * MON-FRI")
    public void morningTask() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(9, 25)) || now.isAfter(LocalTime.of(11, 30))) {
            return;
        }
        report();
    }

    // 下午 13:00 到 15:00 每 5 秒执行一次
    @Scheduled(cron = "*/5 * 13-14 * * MON-FRI")
    public void afternoonTask() {
        report();
    }

    public void report() {
        List<StockConfig> stockConfigs = excelReader.getStockConfigs();
        String stockCode = stockConfigs.stream().map(item -> item.getStockCode()).collect(Collectors.joining(","));
        String url = "http://hq.sinajs.cn/list=" + stockCode; // 字段参考：https://www.cnblogs.com/ytkah/p/8510222.html
        try {
            HttpClient client = HttpClients.createDefault();
            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.REFERER, "http://finance.sina.com.cn");
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());
            List<String> results = Arrays.asList(result.replace("\n", "").split(";"))
                    .stream().map(item -> item.split("=")[1].replace("\"", "")).collect(Collectors.toList());

            // 定时报最新价
            seconds.getAndIncrement();
            if (seconds.get() % (interval) == 0) {
                logger.info("----------------------------------------------------------------");
                results.forEach(item -> {
                    String[] itemDetails = item.split(",");
                    String newPrice = String.format("%6s" +
                                    "   MIN%8s" +
                                    "   NOW%8s" +
                                    "[%s]" +
                                    "   MAX%8s" +
                                    "   OPEN%8s" +
                                    "   CLOSE%8s",
                            itemDetails[0],
                            itemDetails[5],
                            itemDetails[3],
                            (Double.valueOf(itemDetails[3]) > Double.valueOf(itemDetails[1])) ? "↑" :
                                    (Double.valueOf(itemDetails[3]) < Double.valueOf(itemDetails[1])) ? "↓" : "=",
                            itemDetails[4],
                            itemDetails[1],
                            itemDetails[2]);
                    logger.info(newPrice);
                    System.out.println(newPrice);
                });
            }

            results.forEach(item -> {
                String[] itemDetails = item.split(",");
                if (bidReport) {
                    stockConfigs.stream()
                            .filter(i -> i.getStockName().trim().equals(itemDetails[0]))
                            .forEach(j -> {
                                if (Double.valueOf(itemDetails[3]) <= j.getExpectedBid()) {
                                    String bidTip = String.format("【%s BID notity】" +
                                                    " %s " +
                                                    "    MAX%8s" +
                                                    "    MIN%8s" +
                                                    "    NOW%8s" +
                                                    "    OPEN%8s" +
                                                    "    CLOSE%8s",
                                            LocalDateTime.now()
                                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                            itemDetails[0],
                                            itemDetails[4],
                                            itemDetails[5],
                                            itemDetails[3],
                                            itemDetails[1],
                                            itemDetails[2]);
                                    logger.info(bidTip);
                                    System.out.println(bidTip);

                                }
                            });
                }
                if (askReport) {
                    stockConfigs.stream()
                            .filter(i -> i.getStockName().trim().equals(itemDetails[0]))
                            .forEach(j -> {
                                if ((Double.valueOf(itemDetails[3]) >= j.getExpectedAsk()) && j.isHavePosition()) {
                                    String askTip = String.format("【%s ASK notity】" +
                                                    " %s " +
                                                    "    MAX%8s" +
                                                    "    MIN%8s" +
                                                    "    NOW%8s" +
                                                    "    OPEN%8s" +
                                                    "    CLOSE%8s",
                                            LocalDateTime.now()
                                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                            itemDetails[0],
                                            itemDetails[4],
                                            itemDetails[5],
                                            itemDetails[3],
                                            itemDetails[1],
                                            itemDetails[2]);
                                    logger.info(askTip);
                                    System.out.println(askTip);
                                }
                            });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
