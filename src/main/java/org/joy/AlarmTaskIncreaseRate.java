package org.joy;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
* 如果Price在一定时间类快速增长到一定比率，提醒ASK操作。
* */

@Component
@Data
public class AlarmTaskIncreaseRate {
    private static final Logger logger = LoggerFactory.getLogger(AlarmTaskIncreaseRate.class);
    private Map<String, Deque<StockInstantPrice>> priceCache = new HashMap<>();

    @Value("${joy.alarm.interval:12}")
    private int interval;

    public void addPriceDequeIntoCache(String key){
        // 用stock code 作为key.
        if (!priceCache.containsKey(key)) {
            priceCache.put(key, new ArrayDeque<StockInstantPrice>(300 / interval));
        }
    }

    public void addInstantPriceIntoPriceCache(String time, List<String> results) {
        results.forEach(item -> {
            String[] itemDetails = item.split(",");
            String stockName = itemDetails[0];
            Double nowPrice = Double.valueOf(itemDetails[3]);
            Double openPrice = Double.valueOf(itemDetails[1]);
            addPriceDequeIntoCache(stockName);
            priceCache.get(stockName).offer(new StockInstantPrice(time, nowPrice, openPrice));
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        Utils.executorService.submit(()->{
            while (true) {
                if (Utils.isTimeToCleanPriceCache()) {
                    // 当前最多监控10个stock的信息。
                    priceCache = new HashMap<>(10);
                }
                if (Utils.isDealTime()) {
                    // 执行分析
                    doIncreaseReteAnalyse();
                }
                logger.info("StockIncreaseRateAlarm mark......");
                try {
                    Thread.sleep(interval * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.info(e.getMessage());
                }
            }
        });
    }

    private void doIncreaseReteAnalyse() {
        
    }
}
