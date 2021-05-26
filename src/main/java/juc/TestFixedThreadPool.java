package juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFixedThreadPool {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private final AtomicInteger atomicInteger = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "my_pool_t" + atomicInteger.getAndIncrement());
            }
        });




    }



    public static void main1(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 1");
        });

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 2");
        });

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 3");
        });

        executorService.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 4");
        });


    }


}
