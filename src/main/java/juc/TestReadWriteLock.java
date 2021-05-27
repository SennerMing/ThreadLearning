package juc;

import javax.xml.crypto.Data;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestReadWriteLock {

    public static void main(String[] args) {
        DataContainer dataContainer = new DataContainer();

        new Thread(() -> {
            dataContainer.read();
        }, "t1").start();

        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            dataContainer.write();
        }, "t2").start();

    }

}

class DataContainer {
    private Object data;
    private ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock r = rw.readLock();
    private ReentrantReadWriteLock.WriteLock w = rw.writeLock();

    public Object read() {
        System.out.println(Thread.currentThread().getName() + " 获取读锁...");
        r.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 读取");
            TimeUnit.SECONDS.sleep(1);
            return data;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + " 释放读锁...");
            r.unlock();
        }
        return null;
    }

    public void write() {
        System.out.println(Thread.currentThread().getName() + " 获取写锁...");
        w.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 写入");
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + " 释放写锁...");
            w.unlock();
        }
    }
}