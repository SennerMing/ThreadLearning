package atomic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class TestAtomicMarkableReference {

    public static void main(String[] args) {
        GarbageBag bag = new GarbageBag("装满了垃圾");
        AtomicMarkableReference<GarbageBag> atomicMarkableReference = new AtomicMarkableReference<>(bag, true);

        System.out.println("starting....");
        GarbageBag prev = atomicMarkableReference.getReference();
        System.out.println(prev.toString());

        //加入保洁阿姨的线程
        new Thread(() -> {
            System.out.println("保洁阿姨starting...");
            bag.setDesc("保洁阿姨换了个空垃圾袋");
            atomicMarkableReference.compareAndSet(bag, bag, true, false);//更新了新的mark 为 false了，下面再CAS就就没办法了
            System.out.println(bag.toString());
        }, "保洁阿姨").start();


        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("想换一只新的垃圾袋？");
        boolean success = atomicMarkableReference
                .compareAndSet(prev, new GarbageBag("空垃圾袋"), true, false);
        System.out.println("换了么？" + success);
        System.out.println(atomicMarkableReference.getReference().toString());

    }

}

class GarbageBag{

    String desc;

    public GarbageBag(String desc) {
        this.desc = desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "GarbageBag{" +
                "desc='" + desc + '\'' +
                '}';
    }
}