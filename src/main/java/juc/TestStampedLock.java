package juc;

import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

public class TestStampedLock {

    public static void main(String[] args) {
        DataContainerStamped dataContainerStamped = new DataContainerStamped(1);

        new Thread(() -> {
            System.out.println(dataContainerStamped.read(2));
        }, "t1").start();

        try {
            TimeUnit.SECONDS.sleep(1);
            new Thread(() -> {
                dataContainerStamped.write(8);
            }, "t2").start();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            System.out.println(dataContainerStamped.read(0));
        }, "t2").start();

    }

}


class DataContainerStamped {
    private int data;
    private final StampedLock lock = new StampedLock();

    public DataContainerStamped(int data) {
        this.data = data;
    }

    public int read(int readTime) {
        long stamp = lock.tryOptimisticRead();
        System.out.println(Thread.currentThread().getName() + " " + stamp);
        try {
            TimeUnit.SECONDS.sleep(readTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (lock.validate(stamp)) {
            System.out.println("read finish...." + stamp);
            return data;
        }
        //锁升级 - 读锁
        try{
            stamp = lock.readLock();
            System.out.println("read lock " + stamp);
            TimeUnit.SECONDS.sleep(readTime);
            System.out.println("read finish " + stamp);
            return data;
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("线程被打断....");
            return -1;
        } finally {
            System.out.println("read unlock " + stamp);
            lock.unlock(stamp);
        }
    }


    public void write(int newData) {
        long stamp = lock.writeLock();
        System.out.println(Thread.currentThread().getName() + " " + stamp);
        try {
            TimeUnit.SECONDS.sleep(2);
            this.data = newData;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + " " + stamp);
            lock.unlockWrite(stamp);
        }

    }


}