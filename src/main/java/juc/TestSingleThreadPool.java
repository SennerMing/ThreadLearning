package juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class TestSingleThreadPool {

    public static void main(String[] args) {

        test2();

    }


    public static void test2() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName()+" 1");
            int i = 1 / 0;
        });

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName()+" 2");
        });

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 3");
        });


    }


    private static void test1() {
        ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private AtomicInteger t = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "my_pool_t" + t.getAndIncrement());
            }
        });


    }

}
