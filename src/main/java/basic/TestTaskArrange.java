package basic;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class TestTaskArrange {

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            System.out.println("洗水壶中...");
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("烧开水中...");
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"老刘");

        Thread t2 = new Thread(()->{
            System.out.println("洗茶壶中...");
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("洗茶杯...");
                TimeUnit.SECONDS.sleep(2);
                System.out.println("拿茶叶中...");
                TimeUnit.SECONDS.sleep(1);

                t1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"小刘");

        t1.start();
        t2.start();

    }


}
