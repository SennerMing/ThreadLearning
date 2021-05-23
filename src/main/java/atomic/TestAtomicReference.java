package atomic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestAtomicReference {

    static AtomicReference<String> reference = new AtomicReference<>("A");

    public static void main(String[] args) {
        System.out.println("main start....");
        //获取值，判断不出来这个变量是否被其他线程修改过
        String prev = reference.get();
        other();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Thread.currentThread().getName()+" change A -> C " + reference.compareAndSet(prev, "c"));
    }

    private static void other(){
        new Thread(()->{
            System.out.println(Thread.currentThread().getName() + " change A -> B " + reference.compareAndSet(reference.get(), "B"));
        },"t1").start();
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " change B -> A " + reference.compareAndSet(reference.get(), "A"));
        }, "t2").start();
    }
}
