package juc;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

public class TestThreadSchedule {



    /**
     * JUC的ScheduledExecutorService 异常处理，异常默认不处理
     * 1.自己主动加try catch块
     * 2.用future获得
     * @param args
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService scheduledExecutorService = Executors.newFixedThreadPool(2);
        System.out.println("开始....");

        Future<Boolean> future =  scheduledExecutorService.submit(() -> {
            System.out.println("task 1");
            int i = 1 / 0;
            return true;
        });

        System.out.println(future.get());

    }

    /**
     * JUC的ScheduledExecutorService升级用法
     * @param args
     */
    public static void main3(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        System.out.println("开始....");


        scheduledExecutorService.scheduleAtFixedRate(() -> {
            System.out.println("task 1");
            try {
                TimeUnit.SECONDS.sleep(2);  //加了延时时间了，任务执行完了，才能执行下一次
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 2, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            System.out.println("running...");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS); //任务间隔时间


    }


    /**
     * JUC的ScheduledExecutorService
     * @param args
     */
    public static void main2(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

        scheduledExecutorService.schedule(() -> {
            System.out.println("task 1");
            try {
                TimeUnit.SECONDS.sleep(2);
                int i = 1 / 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 1, TimeUnit.SECONDS);


        scheduledExecutorService.schedule(() -> {
            System.out.println("task 2");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 1, TimeUnit.SECONDS);

    }


    /**
     * 脆弱的Timer实现方式
     * @param args
     */
    public static void main1(String[] args) {
        Timer timer = new Timer();
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                System.out.println("task 1");
                int i = 1 / 0;  //这样的话任务1出现异常，任务二直接没法执行
//                try {
//                    TimeUnit.SECONDS.sleep(2);  //加上了的话，会阻塞任务2的执行计划
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        };

        TimerTask task2 = new TimerTask() {
            @Override
            public void run() {
                System.out.println("task 2");
            }
        };

        //使用timer添加两个任务，希望他们都在1s后执行
        //但是由于timer内只有一个线程来顺序执行队列中的任务，因此[任务1]的延迟，影响了[任务二]的执行
        System.out.println("start...");
        timer.schedule(task1, 1000);
        timer.schedule(task2, 1000);


    }


}
