package juc;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestThreadFixedSchedule {

    public static void main(String[] args) {

        //initialDelay 代表当前时间和周四的时间差值
        //period 一周的时间间隔
        //获得当前时间
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now);

        //获得周四时间
        LocalDateTime thursday = now.withHour(18).withMinute(0).withHour(0).withSecond(0).withNano(0).with(DayOfWeek.THURSDAY);
        System.out.println(thursday);

        //如果当前时间 > 本周周四，必须找到下周周四
        if (now.compareTo(thursday) > 0) {
            thursday = thursday.plusWeeks(1);
        }

        long initialDelay = Duration.between(now, thursday).toMillis();

        long period = 1000 * 60 * 60 * 24 * 7;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            System.out.println("正在执行....");
        }, initialDelay, period, TimeUnit.MICROSECONDS);

    }

}
