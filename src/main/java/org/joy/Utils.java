package org.joy;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Utils {

    // 全局共享的线程池。
    static ExecutorService executorService = Executors.newCachedThreadPool();

    private static boolean isWorkingDay() {
        LocalDate currentDate = LocalDate.now();
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        // DayOfWeek类中，MONDAY至FRIDAY表示周一至周五
        return (dayOfWeek.compareTo(DayOfWeek.MONDAY) >= 0 &&
                dayOfWeek.compareTo(DayOfWeek.FRIDAY) <= 0);
    }

    public static boolean isDealTime() {
        LocalTime now = LocalTime.now();
        boolean isSWDealTime =  (now.isAfter(LocalTime.of(9, 25)) && now.isBefore(LocalTime.of(11, 30)));
        boolean isXWDealTime =  (now.isAfter(LocalTime.of(13, 0)) && now.isBefore(LocalTime.of(15, 0)));
        return isWorkingDay() && (isSWDealTime || isXWDealTime);
    }

    public static boolean isTimeToCleanPriceCache() {
        LocalTime now = LocalTime.now();
        return isWorkingDay() &&
                (now.isAfter(LocalTime.of(9, 24)) && now.isBefore(LocalTime.of(9, 25)));
    }
}
