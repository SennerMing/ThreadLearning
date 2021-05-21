package basic;

import java.util.concurrent.TimeUnit;

public class TestRoom {
    private static final Object room = new Object();
    private static boolean hasCigarette = false;
    private static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                System.out.println("小王：有没有烟？ " + hasCigarette);
                if (!hasCigarette) {
                    System.out.println("小王：没有烟我先睡一会吧...");
                    try {
//                        TimeUnit.SECONDS.sleep(2);
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("小王：睡完了，还有没有烟啊？" + hasCigarette);
                if (hasCigarette) {
                    System.out.println("小王：哎呀有烟了，终于可以干活了");
                }
            }
        }, "小王").start();

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                synchronized (room) {
                    System.out.println("其他人：可以开始干活了");
                }
            }, "其他人").start();
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            synchronized (room) {
                hasCigarette = true;
                room.notify();
                System.out.println("送烟的：烟送到");
            }
        }, "送烟的").start();
    }

}
