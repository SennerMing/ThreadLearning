package basic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class TestLockSupport {
    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
//            System.out.println("park....");
//            LockSupport.park();
//            System.out.println("not park....");
//            System.out.println("打断状态：" + Thread.currentThread().isInterrupted());

            System.out.println("park....");
            LockSupport.park();
            System.out.println("not park....");
            //如果继续按照上面的方法进行打断状态的判断，那么再执行park()方法的话，就无法再停止线程
            //那么就换成interrupted()

//            System.out.println("打断状态：" + Thread.currentThread().isInterrupted());
            System.out.println("打断状态：" + Thread.interrupted());

            LockSupport.park();
            System.out.println("not park....");

        }, "t1");

        t1.start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        t1.interrupt();

    }

}
