package basic;

import java.util.concurrent.TimeUnit;

public class TestVolatile {

    private static boolean run = true;
    private final static Object lock = new Object();


    public static void main(String[] args) {

        new Thread(() ->{
            while (true) {
                synchronized (lock) {
                    if (!run) {
                        System.out.println("线程停止！");
                        break;
                    }
                }
            }
        }).start();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (lock) {
            run = false;
        }

    }


}
