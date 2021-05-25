package juc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TestThreadPool {

    public static void main(String[] args) {

        ThreadPool threadPool = new ThreadPool(1, 1000, TimeUnit.MICROSECONDS, 1,(queue,task)->{
            //策略是死等
//            queue.put(task);
//                2)带超时事件
//            queue.offer(task, 1500, TimeUnit.MICROSECONDS);
//                3)让调用者放弃任务执行
//            System.out.println("放弃任务的执行....");
//                4)让调用者抛出异常
//            throw new RuntimeException("没有Worker执行任务，执行失败!");
//                5)让调用者自己执行任务
            task.run();
        });

        for (int i = 0; i < 4; i++) {
            int j = i;
            new Thread(() -> {
                threadPool.execute(()->{
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("打印数字：" + j);
                });
            }).start();
        }

    }


}


//拒绝策略
@FunctionalInterface
interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue, T task);
}



class ThreadPool{

    //任务队列
    private BlockingQueue<Runnable> taskQueue;

    //线程集合
    private HashSet<Worker> workers = new HashSet<>();

    //核心的线程数
    private int coreSize;

    //时间单位
    private TimeUnit timeUnit;

    private long timeout;

    private RejectPolicy<Runnable> rejectPolicy;


    class Worker extends Thread {
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
//            执行任务
//            1）当task不为空，执行任务
//            2）当task执行完毕，再接着从任务队列获取任务并执行
            while (task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
                try {
                    System.out.println(Thread.currentThread().getName() + " 正在执行...." + task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            synchronized (workers) {
                System.out.println(Thread.currentThread().getName() + " 正在移除..." + this);
                workers.remove(this);
            }
        }
    }

    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int capacity, RejectPolicy<Runnable> rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
        this.taskQueue = new BlockingQueue<>(capacity);
        this.coreSize = coreSize;
        this.timeUnit = timeUnit;
        this.timeout = timeout;
    }


    public void execute(Runnable task) {
        //当任务数没有超过coreSize时，直接交给worker对象执行
        //如果任务数超过coreSize时，加入任务队列暂存
        synchronized (workers) {
            if (workers.size() < coreSize) {
                Worker worker = new Worker(task);
                System.out.println(Thread.currentThread().getName() + " 新增worker" + worker);
                workers.add(worker);
                worker.start();
            } else{
                System.out.println(Thread.currentThread().getName() + " 加入任务队列" + task);

//                1)死等  taskQueue.put(task);
//                2)带超时事件
//                3)让调用者放弃任务执行
//                4)让调用者抛出异常
//                5)让调用者自己执行任务

                taskQueue.tryPut(rejectPolicy, task);

            }
        }
    }

}


class BlockingQueue<T> {

    //1.任务队列
    private Deque<T> queue = new ArrayDeque<>();
    //2.锁
    private ReentrantLock reentrantLock = new ReentrantLock();
    //3.生产者条件变量
    private Condition fullWaitSet = reentrantLock.newCondition();
    //4.消费者条件变量
    private Condition emptyWaitSet = reentrantLock.newCondition();
    //5.线程池容量
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    //改进 -- 带超时的阻塞获取
    public T poll(long timeout, TimeUnit unit) {
        reentrantLock.lock();
        try{
            //将超时事件，统一转换为纳秒
            long nanos = unit.toNanos(timeout);
            while (queue.isEmpty()) {
                try {
                    //返回的是剩余的时间，比如等待1秒，然后实际等待了0.5s，那么返回的时间就是0.5s
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            reentrantLock.unlock();
        }

    }


    /**
     * 阻塞获取
     * @return
     */
    public T take() {
        reentrantLock.lock();
        try{
            while (queue.isEmpty()) {
                try {
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * 获取队列大小
     * @return
     */
    public void put(T element) {
        reentrantLock.lock();
        try{
            while (queue.size() == capacity) {
                try {
                    System.out.println("等待加入任务队列：" + element);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("加入任务队列：" + element);
            queue.addLast(element);
            emptyWaitSet.signal();
        } finally {
            reentrantLock.unlock();
        }
    }

    //带超时事件的阻塞添加
    public boolean offer(T task, long timeout, TimeUnit timeUnit) {
        reentrantLock.lock();
        try{
            long nanos = timeUnit.toNanos(timeout);
            while (queue.size() == capacity) {
                try {
                    System.out.println("等待加入任务队列：" + task);
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("加入任务队列：" + task);
            queue.addLast(task);
            emptyWaitSet.signal();
            return true;
        } finally {
            reentrantLock.unlock();
        }
    }


    /**
     * 获取队列大小
     * @return
     */
    public int size() {
        reentrantLock.lock();
        try {
            return queue.size();
        } finally {
            reentrantLock.unlock();
        }
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        reentrantLock.lock();

        try {
            //判断队列是否已满
            if (queue.size() == capacity) {
                rejectPolicy.reject(this, task);
            } else {    //队列还有空闲
                System.out.println("加入任务队列：" + task);
                queue.addLast(task);
                emptyWaitSet.signal();
            }

        } finally {
            reentrantLock.unlock();
        }
    }
}