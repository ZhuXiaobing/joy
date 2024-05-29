package org.joy;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.*;
import java.time.format.DateTimeFormatter;
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
    public boolean isWorkingDay() {
        LocalDate currentDate = LocalDate.now();
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        // DayOfWeek类中，MONDAY至FRIDAY表示周一至周五
        return (dayOfWeek.compareTo(DayOfWeek.MONDAY) >= 0 &&
                dayOfWeek.compareTo(DayOfWeek.FRIDAY) <= 0);
    }

    public boolean isDealTime() {
        LocalTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalTime();
        boolean isSWDealTime =  (now.isAfter(LocalTime.of(9, 25)) && now.isBefore(LocalTime.of(11, 30)));
        boolean isXWDealTime =  (now.isAfter(LocalTime.of(13, 0)) && now.isBefore(LocalTime.of(15, 0)));
        return isSWDealTime || isXWDealTime;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        while (true) {
            if (isWorkingDay() && isDealTime()) {
                report();
                try {
                    Thread.sleep(interval * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.info(e.getMessage());
                }
            }
        }
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
            if (seconds.get() % (60/interval) == 0) {
                String spilter = String.format("----------------------------------[%s]----------------------------------",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                logger.info(spilter);
                System.out.println(spilter);
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
            logger.info(e.getMessage());
        }
    }
}
