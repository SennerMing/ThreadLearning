package juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TestReentrantLock {
    private static final ReentrantLock reentrantLock = new ReentrantLock();


    /**
     * 测试reentrantLock的尝试获取锁
     * @param args
     */
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName()+"尝试获取锁....");
//            if (!reentrantLock.tryLock()) {
            try {
                if (!reentrantLock.tryLock(2, TimeUnit.SECONDS)) {
                    System.out.println(Thread.currentThread().getName() + "获取不到锁！");
                    return;
                }
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + "获取不到锁！");
                e.printStackTrace();
                return;
            }
            try{
                System.out.println(Thread.currentThread().getName()+"获取到锁！");
            } finally {
                reentrantLock.unlock();
            }

        },"t1");


        reentrantLock.lock();
        t1.start();
        try {
            TimeUnit.SECONDS.sleep(1);
            reentrantLock.unlock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * 测试reentrantLock的可打断
     * @param args
     */
    public static void main2(String[] args) {

        Thread t1 = new Thread(() -> {
            try {
                //如果没有竞争那么此方法就会获取lock对象锁
                //如果有竞争，就进入阻塞队列，可以被其他线程用interrupt方法打断
                System.out.println(Thread.currentThread().getName()+"尝试获得锁，进入可打断状态.....");
                reentrantLock.lockInterruptibly();
//                reentrantLock.lock();

            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println(Thread.currentThread().getName() + "没有获得锁.....");
                return;
            }
            try {
                System.out.println(Thread.currentThread().getName()+"获得到锁！");
            } finally {
                reentrantLock.unlock();
            }

        }, "t1");



        t1.start();

        reentrantLock.lock();

        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println(Thread.currentThread().getName()+"进行t1的打断操作");
            t1.interrupt();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
          reentrantLock.unlock();
        }

    }




    /**
     * 测试reentrantLock可重入特性
     * @param args
     */
    public static void main1(String[] args) {
        reentrantLock.lock();
        try{
            System.out.println(Thread.currentThread().getName() + "主方法");
            m1();
        } finally {
            reentrantLock.unlock();
        }
    }

    public static void m1() {
        reentrantLock.lock();
        try {
            System.out.println(Thread.currentThread().getName()+"进入m1");
            m2();
        } finally {
            reentrantLock.unlock();
        }
    }


    public static void m2() {
        reentrantLock.lock();
        try {
            System.out.println(Thread.currentThread().getName()+"进入m2");
        } finally {
            reentrantLock.unlock();
        }
    }


}
