package juc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class TestThreadPoolSubmit {

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(2);


        try {
            String result = executorService.invokeAny(Arrays.asList(
                    ()->{
                        System.out.println(Thread.currentThread().getName() + " begin 1....");
                        TimeUnit.SECONDS.sleep(1);
                        return "1";
                    },()->{
                        System.out.println(Thread.currentThread().getName() + " begin 2....");
                        TimeUnit.SECONDS.sleep(1);
                        return "2";
                    },()->{
                        System.out.println(Thread.currentThread().getName() + " begin 3....");
                        TimeUnit.SECONDS.sleep(1);
                        return "3";
                    }
            ));
            System.out.println(result);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

//        invokeAllTest(executorService);


//        futureTest();

    }

    private static void invokeAllTest(ExecutorService executorService) {
        try {
            List<Future<String>> futureList = executorService.invokeAll(Arrays.asList(
                    ()->{
                        System.out.println(Thread.currentThread().getName() + " begin....");
                        TimeUnit.SECONDS.sleep(1);
                        return "1";
                    },()->{
                        System.out.println(Thread.currentThread().getName() + " begin....");
                        TimeUnit.SECONDS.sleep(1);
                        return "2";
                    },()->{
                        System.out.println(Thread.currentThread().getName() + " begin....");
                        TimeUnit.SECONDS.sleep(1);
                        return "3";
                    }
            ));

            futureList.forEach(f->{
                try {
                    System.out.println(f.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void futureTest() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                TimeUnit.SECONDS.sleep(1);
                return "ok";
            }
        });

        try {
            System.out.println(future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}
