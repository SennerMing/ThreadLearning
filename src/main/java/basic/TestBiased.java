package basic;

import org.openjdk.jol.info.ClassLayout;

import java.util.concurrent.TimeUnit;

public class TestBiased {

    public static void main(String args[]) throws InterruptedException {
        Dog dog = new Dog();
        dog.hashCode(); //会禁用掉这个对象的偏向锁，可以看对象头
        System.out.println(ClassLayout.parseInstance(dog).toPrintable());

        TimeUnit.SECONDS.sleep(4);

        System.out.println(ClassLayout.parseInstance(new Dog()).toPrintable());

    }

}

class Dog {

}
