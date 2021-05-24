package atomic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestAtomicCas {

//    0没加锁
//    1已加锁
    private AtomicInteger state = new AtomicInteger(0);

    public void lock() {
        while (true) {
            if (state.compareAndSet(0, 1)) {
                break;
            }
        }
    }

    public void unlock() {

        System.out.println("unlocking....");
        state.set(0);
    }

    public static void main(String[] args) {
        TestAtomicCas lock = new TestAtomicCas();
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName()+"begin....");
            lock.lock();

            try {
                System.out.println(Thread.currentThread().getName()+"locking....");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                lock.unlock();
            }

        }, "t1").start();

        new Thread(() -> {
            System.out.println(Thread.currentThread().getName()+"begin....");
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()+"locking....");
            } finally {
                lock.unlock();
            }
        }, "t2").start();
    }


}
