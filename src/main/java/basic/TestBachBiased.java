package basic;

import org.openjdk.jol.info.ClassLayout;

import java.util.Vector;

public class TestBachBiased {
    public static void main(String[] args) {
        Vector<Dog> list = new Vector<>();

        new Thread(() -> {
            for (int i = 0; i < 30; i++) {

                Dog d = new Dog();
                list.add(d);

                synchronized (d) {
                    System.out.println(ClassLayout.parseInstance(d).toPrintable());
                }

            }
            synchronized (list) {
                list.notify();
            }

        }, "t1").start();

        new Thread(() -> {
            synchronized (list) {
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("=============================");
            for (int i = 0; i < 30; i++) {
                System.out.println("第[" + i + "]");
                Dog d = new Dog();
                list.add(d);

                System.out.println(ClassLayout.parseInstance(d).toPrintable());

                synchronized (d) {
                    System.out.println(ClassLayout.parseInstance(d).toPrintable());
                }
                System.out.println(ClassLayout.parseInstance(d).toPrintable());

            }
            synchronized (list) {
                list.notify();
            }

        }, "t2").start();


    }
}
