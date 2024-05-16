package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 信号报警器
 */
public class App {
    public static void main(String[] args) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        AtomicInteger seconds = new AtomicInteger();
        boolean bidReport = false;
        boolean askReport = true;
        StockConfig[] stockConfigs = {
                new StockConfig("居然之家", "sz000785", 2.90, 3.1),
                new StockConfig("广电网络", "sh600831", 3.5, 3.8),    // 3.71 6500
                new StockConfig("航天动力", "sh600343", 8.05, 8.5),   // 7.27 3500
                new StockConfig("人福医药", "sh600079", 19.8, 21.0)
        };

        String stockCode =
                Arrays.stream(stockConfigs).map(item -> item.getStockCode()).collect(Collectors.joining(","));

        String url = "http://hq.sinajs.cn/list=" + stockCode; // 字段参考：https://www.cnblogs.com/ytkah/p/8510222.html

        Runnable task = () -> {
            try {
                seconds.getAndIncrement();
                HttpClient client = HttpClients.createDefault();
                HttpGet request = new HttpGet(url);
                request.setHeader(HttpHeaders.REFERER, "http://finance.sina.com.cn");
                HttpResponse response = client.execute(request);
                String result = EntityUtils.toString(response.getEntity());
                List<String> results = Arrays.asList(result.replace("\n", "").split(";"))
                        .stream().map(item -> item.split("=")[1].replace("\"", "")).collect(Collectors.toList());

                // 每隔一分钟报一次最新价格
                if (seconds.get() % 60 == 0) {
                    System.out.println("-------------------------------------------------------------------------");
                    results.forEach(item -> {
                        String[] itemDetails = item.split(",");
                        System.out.printf("%6s    现价%8s    今开%8s    昨收%8s\n", itemDetails[0] ,itemDetails[3] ,itemDetails[1] ,itemDetails[2]);
                    });
                }

                results.forEach(item -> {
                    String[] itemDetails = item.split(",");
                    if (bidReport) {
                        Arrays.stream(stockConfigs)
                                .filter(i -> i.getStockName().trim().equals(itemDetails[0]))
                                .forEach(j -> {if (Double.valueOf(itemDetails[3])  <= j.getExpectedBid()) {
                                    System.out.printf("【%s  买入提醒】 %6s    现价%8s    今开%8s    昨收%8s\n", LocalDateTime.now().format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), itemDetails[0] ,itemDetails[3] ,itemDetails[1] ,itemDetails[2]);
                                }});
                    }
                    if (askReport) {
                        Arrays.stream(stockConfigs)
                                .filter(i -> i.getStockName().trim().equals(itemDetails[0]))
                                .forEach(j -> {if (Double.valueOf(itemDetails[3])  >= j.getExpectedAsk()) {
                                    System.out.printf("【%s  卖出提醒】 %6s    现价%8s    今开%8s    昨收%8s\n", LocalDateTime.now().format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), itemDetails[0] ,itemDetails[3] ,itemDetails[1] ,itemDetails[2]);
                                }});
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        executorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

}
@Data
@AllArgsConstructor
class StockConfig{
    private String stockName;
    private String stockCode;
    private Double expectedBid; // 期望买价
    private Double expectedAsk; // 期望卖价
}