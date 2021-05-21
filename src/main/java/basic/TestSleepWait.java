package basic;

import java.util.concurrent.TimeUnit;

public class TestSleepWait {

    private static final Object obj = new Object();
    public static void main(String[] args) {

        new Thread(() -> {
            synchronized (obj) {
                System.out.println("Thread-1获得锁");
                try {
//                    TimeUnit.SECONDS.sleep(2);
                    obj.wait(); //这样主线程就让该线程进入了等待waitset
                    System.out.println("处于等待状态...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }, "t1").start();

        try {
            TimeUnit.SECONDS.sleep(1);
            synchronized (obj) {
                System.out.println("主线程获得锁");
                obj.notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
