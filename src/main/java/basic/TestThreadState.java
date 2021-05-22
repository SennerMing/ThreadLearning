package basic;

import java.util.concurrent.TimeUnit;

public class TestThreadState {

    private final static Object lock = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (lock) {
                System.out.println(Thread.currentThread().getName() + " 执行....");
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(Thread.currentThread().getName() + " 其他代码....");
        }, "t1").start();

        new Thread(() -> {
            synchronized (lock) {
                System.out.println(Thread.currentThread().getName() + " 执行....");

                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 其他代码....");
            }
        }, "t2").start();

        try {
            TimeUnit.SECONDS.sleep(2);
            System.out.println("唤醒lock上的其他线程");
            synchronized (lock) {
                lock.notifyAll();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
