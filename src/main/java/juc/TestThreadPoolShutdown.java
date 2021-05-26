package juc;

import sun.jvm.hotspot.runtime.Thread;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestThreadPoolShutdown {

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<Integer> future1 = executorService.submit(() -> {
            System.out.println("task 1 running ....");
            TimeUnit.SECONDS.sleep(1);
            System.out.println("task 1 finished...");
            return 1;
        });

        Future<Integer> future2 = executorService.submit(() -> {
            System.out.println("task 2 running ....");
            TimeUnit.SECONDS.sleep(1);
            System.out.println("task 2 finished...");
            return 2;
        });

        Future<Integer> future3 = executorService.submit(() -> {
            System.out.println("task 3 running ....");
            TimeUnit.SECONDS.sleep(1);
            System.out.println("task 3 finished...");
            return 3;
        });

        System.out.println("shutdown"); //已经提交的还是回去执行的
//        executorService.shutdown();
//        try {
//            executorService.awaitTermination(3, TimeUnit.SECONDS);
//            System.out.println("other....");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        List<Runnable> runnableList = executorService.shutdownNow();//虽然任务1，任务2已经启动了，但是还是被无情打断了


    }


}
