package basic;

import org.openjdk.jol.info.ClassLayout;

public class TestBiasedUpgrade {

    public static void main(String[] args) {

        Dog dog = new Dog();

        new Thread(() -> {

            System.out.println(ClassLayout.parseInstance(dog).toPrintable());
            synchronized (dog) {
                System.out.println(ClassLayout.parseInstance(dog).toPrintable());
            }
            System.out.println(ClassLayout.parseInstance(dog).toPrintable());

            synchronized (TestBiasedUpgrade.class) {
                TestBiasedUpgrade.class.notify();
            }

        }, "t1").start();

        new Thread(() -> {
            synchronized (TestBiasedUpgrade.class) {
                try {
                    TestBiasedUpgrade.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(ClassLayout.parseInstance(dog).toPrintable());
            synchronized (dog) {
                System.out.println(ClassLayout.parseInstance(dog).toPrintable());
            }
            System.out.println(ClassLayout.parseInstance(dog).toPrintable());

        }, "t2").start();



    }


}
