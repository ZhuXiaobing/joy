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
public class AlarmTaskInstantPrice {

    private static final Logger logger = LoggerFactory.getLogger(AlarmTaskInstantPrice.class);
    @Value("${joy.myself.bid:true}")
    boolean bidReport;
    @Value("${joy.myself.ask:true}")
    boolean askReport;
    @Value("${joy.alarm.interval:12}")
    private int interval;
    @Resource
    public ExcelReader excelReader;
    @Resource
    public AlarmTaskIncreaseRate alarmTaskIncreaseRate;
    static AtomicInteger seconds = new AtomicInteger();

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        while (true) {
            if (Utils.isDealTime()) {
                report();
            }
            logger.info("mark......");
            try {
                Thread.sleep(interval * 1000);
            } catch (Exception e) {
                e.printStackTrace();
                logger.info(e.getMessage());
            }
        }
    }


    public void report() {
        List<StockConfig> stockConfigs = excelReader.getStockConfigs();
        String stockCode = stockConfigs.stream().map(item -> item.getStockCode()).collect(Collectors.joining(","));
        // 字段参考：https://www.cnblogs.com/ytkah/p/8510222.html
        String url = "http://hq.sinajs.cn/list=" + stockCode;
        try {
            HttpClient client = HttpClients.createDefault();
            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.REFERER, "http://finance.sina.com.cn");
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());
            List<String> results = Arrays.asList(result.replace("\n", "").split(";"))
                    .stream().map(item -> item.split("=")[1].replace("\"", "")).collect(Collectors.toList());
            alarmTaskIncreaseRate.addInstantPriceIntoPriceCache(now ,results);
            // 定时报最新价
            seconds.getAndIncrement();
            if (seconds.get() % (60/interval) == 0) {
                String spilter = String.format("----------------------------------[%s]----------------------------------",now);
                logger.info(spilter);
                System.out.println(spilter);
                results.forEach(item -> {
                    String[] itemDetails = item.split(",");
                    double priceGap = Double.valueOf(itemDetails[3]) - Double.valueOf(itemDetails[1]);
                    String newPrice = String.format("%6s" +
                                    "   MIN%10s" +
                                    "   NOW%10s" +
                                    "[%s %6.2f]" +
                                    "   MAX%10s" +
                                    "   OPEN%10s" +
                                    "   CLOSE%10s",
                            itemDetails[0],
                            itemDetails[5],
                            itemDetails[3],
                            (priceGap > 0) ? "↑" : (priceGap < 0) ? "↓" : "=", priceGap,
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
                                                    "    MAX%10s" +
                                                    "    MIN%10s" +
                                                    "    NOW%10s" +
                                                    "    OPEN%10s" +
                                                    "    CLOSE%10s",
                                            now,
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
                                                    "    MAX%10s" +
                                                    "    MIN%10s" +
                                                    "    NOW%10s" +
                                                    "    OPEN%10s" +
                                                    "    CLOSE%10s",
                                            now,
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
