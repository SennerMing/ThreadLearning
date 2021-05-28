package juc;

import java.util.Date;
import java.util.concurrent.*;

public class TestCyclicBarrier {

    public static void main(String[] args) {

////        1、会议需要三个人
//        CyclicBarrier cyclicBarrier = new CyclicBarrier(3, new Runnable() {
//                @Override
//                public void run(){
//    //            2、这是三个人都到齐之后会执行的代码
//                System.out.println("三个人都已到达会议室");
//            }
//        });
//
//        //3、定义三个线程，相当于三个参会的人
//          for (int i = 0; i < 3; i++) {
//                final int finalI = i;
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//        //                    4、模拟每人到会议室所需时间
//                            Thread.sleep((long) (Math.random()*5000));
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        System.out.println("第"+Thread.currentThread().getName()+"个人到达会议室");
//                        try {
//        //                    5、等待其他人到会议室
//                            cyclicBarrier.await();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        } catch (BrokenBarrierException e) {
//                            e.printStackTrace();
//                        }
//                        System.out.println(Thread.currentThread().getName()+"开始开会");
//                    }
//                }, String.valueOf(finalI)).start();
//            }


        cyclicBarrierTest();


//        onceTest();
    }

    private static void cyclicBarrierTest() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CyclicBarrier cb = new CyclicBarrier(2,()->{
            System.out.println("后续....");
        }); // 个数为2时才会继续执行
        for (int i = 0; i < 3; i++) {
            executorService.submit(()->{
                System.out.println(Thread.currentThread().getName()+"线程开始.."+new Date());
                try {
                    TimeUnit.SECONDS.sleep(1);
                    cb.await(); // 当个数不足时，等待
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
//                System.out.println("线程1继续向下运行..."+new Date());
            });
            executorService.submit(()->{
                System.out.println(Thread.currentThread().getName()+"线程开始.."+new Date());
                try { Thread.sleep(2000); } catch (InterruptedException e) { }
                try {
                    cb.await(); // 2 秒后，线程个数够2，继续运行
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
//                System.out.println("线程2继续向下运行..."+new Date());
            });
        }

        executorService.shutdown();
    }

    private static void onceTest() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch countDownLatch = new CountDownLatch(2);

        executorService.submit(() -> {
            System.out.println("task1 start....");
            try {
                TimeUnit.SECONDS.sleep(1);
                countDownLatch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        executorService.submit(() -> {
            System.out.println("task2 start....");
            try {
                TimeUnit.SECONDS.sleep(2);
                countDownLatch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("task 1 task2 finish...");
        executorService.shutdown();
    }
}
