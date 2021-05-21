package basic;

import java.util.concurrent.TimeUnit;

public class TestWaitNotify {

    static final Object lock = new Object();

    public static void main(String[] args) {

//        synchronized (lock) {
//            try {
//                lock.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }


//        new Thread(() -> {
//            synchronized (lock) {
//                System.out.println("执行....");
//                try {
//                    lock.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("其他程序....");
//            }
//        }, "t1").start();
//
//        new Thread(() -> {
//            synchronized (lock) {
//                System.out.println("执行....");
//                try {
//                    lock.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("其他程序....");
//            }
//        }, "t2").start();
//
//        try {
//            TimeUnit.SECONDS.sleep(2);
//            System.out.println("唤醒lock的一个线程");
//            synchronized (lock) {
////                lock.notify();
//                lock.notifyAll();
//            }
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }, "t3").start();

        new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(1000, 2);//可以点进去看看，如果nanos>0那么timeout就++
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }, "t4").start();


    }
}
