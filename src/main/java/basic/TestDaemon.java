package basic;

import java.util.concurrent.TimeUnit;

public class TestDaemon {

    public static void main(String[] args) {

        Thread t1 = new Thread(()->{
            while (true) {

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }

            System.out.println("结束！");
        });

        //像是垃圾回收器就用的是守护线程
        t1.setDaemon(true);
        t1.start();

        try {
            TimeUnit.SECONDS.sleep(2);
            System.out.println("主线程结束了！");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
