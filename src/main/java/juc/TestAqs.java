package juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TestAqs {

    public static void main(String[] args) {
        MyLock lock = new MyLock();
        new Thread(() -> {
            System.out.println("尝试加锁");
            lock.lock();
            System.out.println("尝试二次加锁");
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()+" locking....");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                System.out.println(Thread.currentThread().getName()+" unlocking....");
                lock.unlock();
            }
        }, "t1").start();

//        new Thread(() -> {
//            lock.lock();
//            try {
//                System.out.println(Thread.currentThread().getName()+" locking....");
//
//            } finally {
//                System.out.println(Thread.currentThread().getName()+" unlocking....");
//                lock.unlock();
//            }
//        }, "t2").start();

    }




}

//自定义锁（不可重入）
class MyLock implements Lock {

    //独占锁
    class MySync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                //加上锁了，设置锁的Owner为当前线程
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            setExclusiveOwnerThread(null);
            setState(0); // state为volatile 写屏障↑
            return true;
        }

        //是否持有独占锁
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        public Condition newCondition() {
            return new ConditionObject();
        }
    }

    private MySync sync = new MySync();

    //加锁，不成功进入队列等待
    @Override
    public void lock() {
        sync.acquire(1);
    }

    //加锁，可打断
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);

    }

    //尝试加锁（尝试一次）
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    //尝试加锁，带超时时间
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    //解锁
    @Override
    public void unlock() {
        sync.release(1);
    }

    //创建条件
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}