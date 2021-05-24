package atomic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class TestAtomicStampedReference {

    public static void main(String[] args) {

        AtomicStampedReference<String> reference = new AtomicStampedReference<String>("A", 0);

        System.out.println("main start....");
        String prev = reference.getReference();
        int m_stamp = reference.getStamp();
        System.out.println("0.版本为" + m_stamp);

        new Thread(() -> {
            //每次都在上一个版本的基础上进行更新，更新成功版本+1
            int stamp = reference.getStamp();
            System.out.println("1.stamp:"+stamp);
            System.out.println("change A -> B " +
                    reference.compareAndSet("A", "B", stamp, stamp + 1));

            //不再是盲区
            System.out.println("do sth unknown");
            //每次都得在上一个版本的基础上更新，更新成功版本+1
            stamp = reference.getStamp();
            System.out.println("2.stamp:"+stamp);
            System.out.println("change B -> A " +
                    reference.compareAndSet("B", "A", stamp, stamp + 1));

        }, "t1").start();
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("3.stamp:"+reference.getStamp());
        System.out.println("change A -> C " + reference.compareAndSet(prev, "C", m_stamp, m_stamp + 1));

    }

}
