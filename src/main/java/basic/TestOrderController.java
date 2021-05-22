package basic;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class TestOrderController {


    static Thread at;
    static Thread bt;
    static Thread ct;

    /**
     * park unpack
     * @param args
     */
    public static void main(String[] args) {

        OrderController orderController = new OrderController(5);
        at = new Thread(() -> {
            orderController.print("a", bt);
        }, "a");

        bt = new Thread(() -> {
            orderController.print("b", ct);
        }, "b");

        ct = new Thread(() -> {
            orderController.print("c", at);
        }, "c");

        at.start();
        bt.start();
        ct.start();

        LockSupport.unpark(at);


    }

    /**
     * Condition方式
     * @param args
     */
    public static void main2(String[] args) {
//        OrderController orderController = new OrderController(5);
//        Condition ac = orderController.newCondition();
//        Condition bc = orderController.newCondition();
//        Condition cc = orderController.newCondition();
//
//        Thread a = new Thread(() -> {
//            orderController.print("a", ac, bc);
//        }, "a");
//
//        Thread b = new Thread(() -> {
//            orderController.print("b", bc, cc);
//        }, "b");
//
//        Thread c = new Thread(() -> {
//            orderController.print("c", cc, ac);
//        }, "c");
//
//        a.start();
//        b.start();
//        c.start();
//
//        try{
//            orderController.lock();
//            ac.signalAll();
//        } finally {
//            orderController.unlock();
//        }


    }


    /**
     * wait and notify
     * @param args
     */
    public static void main1(String[] args) {

//        for (int i = 0; i < 50; i++) {
//            System.out.println(i%3);
//        }

//        OrderController orderController = new OrderController(1, 5);
//
//        Thread a = new Thread(() -> {
//            orderController.print("a", 1, 2);
//        }, "a");
//
//        Thread b = new Thread(() -> {
//            orderController.print("b", 2, 3);
//        }, "b");
//
//        Thread c = new Thread(() -> {
//            orderController.print("c", 3, 1);
//        }, "c");
//
//        a.start();
//        b.start();
//        c.start();
    }


}


//class OrderController extends ReentrantLock {         //Condition方式需配合继承自ReentrantLock
class OrderController{

    //==============================LockSupport=====================================
    private int loopNumber;

    public OrderController(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String str, Thread next) {
        for (int i = 0; i < loopNumber; i++) {

            LockSupport.park();
            System.out.println(Thread.currentThread().getName() + " 线程 " + str);
            LockSupport.unpark(next);
        }

    }





    //==============================Condition=====================================
    /*private int loopNumber;

    public OrderController(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String str, Condition current, Condition next) {
        for (int i = 0; i < loopNumber; i++) {
            lock();
            try {
                current.await();
                System.out.println(Thread.currentThread().getName()+" 线程打印 "+str);
                next.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                unlock();
            }
        }
    }*/


    //=========================wait and notify====================================
    /*public void print(String str, int waitFlag, int nextFlag) {

        for (int i = 0; i < loopNumber; i++) {
            synchronized (this) {
                while (flag != waitFlag) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(Thread.currentThread().getName()+" 打印内容  "+str);
                flag = nextFlag;
                this.notifyAll();
            }
        }

    }

    private int flag;
    private int loopNumber;

    public OrderController(int flag, int loopNumber) {
        this.flag = flag;
        this.loopNumber = loopNumber;
    }*/
}