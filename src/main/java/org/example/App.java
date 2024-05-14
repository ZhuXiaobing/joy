package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 信号报警器
 */
public class App {
    public static void main(String[] args) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        StockConfig[] stockConfigs = {
                new StockConfig("居然之家", "sz000785", 3.90, 100.0),
                new StockConfig("广电网络", "sh600831", 3.6, 100.0),
                new StockConfig("航天动力", "sh600343", 8.10, 100.0),
                new StockConfig("人福医药", "sh600079", 19.6, 100.0)
        };

        String stockCode =
                Arrays.stream(stockConfigs).map(item -> item.getStockCode()).collect(Collectors.joining(","));
        String url = "http://hq.sinajs.cn/list=" + stockCode;

        Runnable task = () -> {
            try {
                HttpClient client = HttpClients.createDefault();
                HttpGet request = new HttpGet(url);
                request.setHeader(HttpHeaders.REFERER, "http://finance.sina.com.cn");
                HttpResponse response = client.execute(request);
            /*
            0：”XXXX”，Stock名字；
            1：”27.55″，今日开盘价；
            2：”27.25″，昨日收盘价；
            3：”26.91″，当前价格；
            4：”27.55″，今日最高价；
            5：”26.20″，今日最低价；
            6：”26.91″，竞买价，即“买一”报价；
            7：”26.92″，竞卖价，即“卖一”报价；
            8：”22114263″，成交的股票数，由于股票交易以一百股为基本单位，所以在使用时，通常把该值除以一百；
            9：”589824680″，成交金额，单位为“元”，为了一目了然，通常以“万元”为成交金额的单位，所以通常把该值除以一万；
            */
                String result = EntityUtils.toString(response.getEntity());
                List<String> results = Arrays.asList(result.replace("\n", "").split(";"))
                        .stream().map(item -> item.split("=")[1].replace("\"", "")).collect(Collectors.toList());
                System.out.println("----------------------------------");
                results.forEach(item -> {
                    String[] itemDetails = item.split(",");
                    System.out.printf("%6s    现价%8s    今开%8s    昨收%8s\n", itemDetails[0] ,itemDetails[3] ,itemDetails[1] ,itemDetails[2]);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };


        // 周一至周五的上午9:30开始
        Calendar startTimeMondayMorning = Calendar.getInstance();
        startTimeMondayMorning.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startTimeMondayMorning.set(Calendar.HOUR_OF_DAY, 9);
        startTimeMondayMorning.set(Calendar.MINUTE, 30);
        startTimeMondayMorning.set(Calendar.SECOND, 0);
        startTimeMondayMorning.set(Calendar.MILLISECOND, 0);

        // 上午11:30结束
        Calendar endTimeMondayMorning = Calendar.getInstance();
        endTimeMondayMorning.set(Calendar.HOUR_OF_DAY, 11);
        endTimeMondayMorning.set(Calendar.MINUTE, 30);
        endTimeMondayMorning.set(Calendar.SECOND, 0);
        endTimeMondayMorning.set(Calendar.MILLISECOND, 0);

        // 下午1点开始
        Calendar startTimeAfternoon = Calendar.getInstance();
        startTimeAfternoon.set(Calendar.HOUR_OF_DAY, 13);
        startTimeAfternoon.set(Calendar.MINUTE, 0);
        startTimeAfternoon.set(Calendar.SECOND, 0);
        startTimeAfternoon.set(Calendar.MILLISECOND, 0);

        // 下午3点结束
        Calendar endTimeAfternoon = Calendar.getInstance();
        endTimeAfternoon.set(Calendar.HOUR_OF_DAY, 15);
        endTimeAfternoon.set(Calendar.MINUTE, 0);
        endTimeAfternoon.set(Calendar.SECOND, 0);
        endTimeAfternoon.set(Calendar.MILLISECOND, 0);

        // 周一至周五的上午9:30-11:30 和 下午1点-3点 每隔两秒执行一次
        scheduleTask(executorService, task, startTimeMondayMorning, endTimeMondayMorning, 5000L);
        scheduleTask(executorService, task, startTimeAfternoon, endTimeAfternoon, 5000L);

        // 如果你想在一定时间后关闭执行器服务，可以使用以下代码
        // executorService.shutdown();
    }

    private static void scheduleTask(ScheduledExecutorService executorService, Runnable task,
                                     Calendar startTime, Calendar endTime, long interval) {
        long initialDelay = getInitialDelay(startTime);
        long period = interval - (endTime.getTimeInMillis() % interval); // 调整时间偏移

        executorService.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MICROSECONDS);
    }

    private static long getInitialDelay(Calendar startTime) {
        long currentTime = System.currentTimeMillis();
        long initialDelay = startTime.getTimeInMillis() - currentTime;
        return initialDelay < 0 ? 0 : initialDelay;
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