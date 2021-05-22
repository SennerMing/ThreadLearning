package juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TestReentrantCondition {

    static ReentrantLock reentrantLock = new ReentrantLock();
    static Condition waitCigarette = reentrantLock.newCondition();
    static Condition waitTakeout = reentrantLock.newCondition();

    private static final Object room = new Object();
    private static boolean hasCigarette = false;
    private static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            reentrantLock.lock();
            try{
                System.out.println("小王：烟送到了没啊？" + hasCigarette);
                if (!hasCigarette) {
                    System.out.println("小王：烟怎么还没送到啊？我先歇一会吧！");
                    try {
                        waitCigarette.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("小王：兄弟们，终于可以干活了，我的天!");

            } finally {
                reentrantLock.unlock();
            }
        }, "小王").start();

        new Thread(() -> {
            reentrantLock.lock();
            try {
                System.out.println("小张：外卖送到了没啊？" + hasTakeout);
                while (!hasTakeout) {
                    System.out.println("小张：外卖怎么还没送到啊？我先歇一会吧！");
                    try {
                        waitTakeout.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("小张：兄弟们，终于可以干饭了，我的天!");
            } finally {
                reentrantLock.unlock();
            }
        }, "小张").start();


        try {
            TimeUnit.SECONDS.sleep(1);

            new Thread(() -> {
                reentrantLock.lock();
                try {
                    System.out.println("送烟的：烟来啦，烟来啦！");
                    hasCigarette = true;
                    waitCigarette.signalAll();
                } finally {
                    reentrantLock.unlock();
                }
            }, "送烟的").start();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public static void main1(String[] args) {

        Condition condition1 = reentrantLock.newCondition();
        Condition condition2 = reentrantLock.newCondition();

        reentrantLock.lock();
        try {
            //进入休息室等待！
            condition1.await();
            condition2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        condition1.signal();
        condition2.signalAll();
    }

}
