package basic;

import java.util.concurrent.locks.LockSupport;

public class TestFixedOrder {

    static final Object lock = new Object();
    static boolean t2_completed = false;


    /**
     * park and unpark
     * @param args
     */
    public static void main(String[] args) {
        Thread t1 = new Thread(()->{
            LockSupport.park();
            System.out.println("线程1执行完毕!");
        },"t1");


        Thread t2 = new Thread(()->{
            System.out.println("线程2执行完毕！");
            LockSupport.unpark(t1);
        },"t2");

        t1.start();
        t2.start();
    }

    /**
     * wait and notify
     * @param args
     */
    public static void main1(String[] args) {
        Thread t1 = new Thread(()->{
            synchronized (lock) {
                while (!t2_completed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("T-1执行完毕");
            }


        },"t1");


        Thread t2 = new Thread(()->{
            synchronized (lock) {
                System.out.println("T-2执行完毕");
                t2_completed = true;
                lock.notifyAll();
            }
        },"t2");

        t1.start();
        t2.start();
    }


}
