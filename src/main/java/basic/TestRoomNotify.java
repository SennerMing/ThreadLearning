package basic;

import java.util.concurrent.TimeUnit;

public class TestRoomNotify {

    private static final Object room = new Object();
    private static boolean hasCigarette = false;
    private static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {

                System.out.println("小王：烟送到了没啊？" + hasCigarette);
                if (!hasCigarette) {

                    System.out.println("小王：烟怎么还没送到啊？我先歇一会吧！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("小王：烟有没有送到啊？我的天！" + hasCigarette);
                if (hasCigarette) {
                    System.out.println("小王：兄弟们，终于可以干活了，我的天!");
                }else{
                    System.out.println("小王：我的天烟还没有送到，我要死了！");
                }
            }
        }, "小王").start();

        new Thread(() -> {
            synchronized (room) {

                System.out.println("小张：外卖送到了没啊？" + hasTakeout);
                while (!hasTakeout) {

                    System.out.println("小张：外卖怎么还没送到啊？我先歇一会吧！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("小张：外卖有没有送到啊？我的天！" + hasTakeout);
                if (hasTakeout) {
                    System.out.println("小张：兄弟们，终于可以干饭了，我的天!");
                }else{
                    System.out.println("小张：我的天饭还没有送到，我要死了！");
                }
            }
        }, "小张").start();


        try {
            TimeUnit.SECONDS.sleep(1);

            new Thread(() -> {
                synchronized (room) {
                    System.out.println("送烟的：烟来啦，烟来啦！");
                    hasCigarette = true;
                    room.notify();
                }
            }, "送烟的").start();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
