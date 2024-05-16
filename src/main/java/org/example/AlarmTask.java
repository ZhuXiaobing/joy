package org.example;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class AlarmTask {
    @Value("${joy.myself.bid:true}")
    boolean bidReport;
    @Value("${joy.myself.ask:true}")
    boolean askReport;
    @Value("${joy.alarm.interval:60}")
    private int interval;
    @Resource
    public ExcelReader excelReader;
    static List<StockConfig> stockConfigs = new ArrayList<>();
    static String stockCode;
    static AtomicInteger seconds = new AtomicInteger();
    @Scheduled(cron="0/1 25-59 9  * * MON-FRI") // 周一到周五 上午9:25-10:00
    @Scheduled(cron="0/1 * 10-11  * * MON-FRI") // 周一到周五 上午10:00-11:00
    @Scheduled(cron="0/1 0-30 11  * * MON-FRI") // 周一到周五 上午11:00-11:30
    @Scheduled(cron="0/1 * 13-15  * * MON-FRI") // 周一到周五 下午1:00-3:00
    @Scheduled(cron="0/1 * *  * * MON-FRI") // 周一到周五 下午1:00-3:00
    public void yugao() {
        if (stockConfigs.isEmpty()) {
            stockConfigs.addAll(excelReader.getStockConfigs());
            stockCode = stockConfigs.stream().map(item -> item.getStockCode()).collect(Collectors.joining(","));
        }
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
                System.out.printf("----------------------------------[%s]----------------------------------\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                results.forEach(item -> {
                    String[] itemDetails = item.split(",");
                    System.out.printf("%6s   MAX%8s   MIN%8s   NOW%8s   OPEN%8s   CLOSE%8s\n", itemDetails[0],
                            itemDetails[4], itemDetails[5], itemDetails[3], itemDetails[1], itemDetails[2]);
                });
            }

            results.forEach(item -> {
                String[] itemDetails = item.split(",");
                if (bidReport) {
                    stockConfigs.stream()
                            .filter(i -> i.getStockName().trim().equals(itemDetails[0]))
                            .forEach(j -> {
                                if (Double.valueOf(itemDetails[3]) <= j.getExpectedBid()) {
                                    System.out.printf(
                                            "【%s  BID notity】 %6s    MAX%8s    MIN%8s    NOW%8s    OPEN%8s    CLOSE%8s\n",
                                            LocalDateTime.now()
                                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                            itemDetails[0], itemDetails[4], itemDetails[5], itemDetails[3],
                                            itemDetails[1], itemDetails[2]);
                                }
                            });
                }
                if (askReport) {
                    stockConfigs.stream()
                            .filter(i -> i.getStockName().trim().equals(itemDetails[0]))
                            .forEach(j -> {
                                if ((Double.valueOf(itemDetails[3]) >= j.getExpectedAsk()) && j.isHavePosition()) {
                                    System.out.printf(
                                            "【%s  ASK notity】 %6s    MAX%8s    MIN%8s    NOW%8s    OPEN%8s    CLOSE%8s\n",
                                            LocalDateTime.now()
                                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                            itemDetails[0], itemDetails[4], itemDetails[5], itemDetails[3],
                                            itemDetails[1], itemDetails[2]);
                                }
                            });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
