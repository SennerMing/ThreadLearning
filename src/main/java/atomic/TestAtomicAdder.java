package atomic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TestAtomicAdder {

    public static void main(String[] args) {

        for (int i = 0; i < 5; i++) {
            demo(
                    () -> new AtomicLong(0),
                    (adder) -> adder.getAndIncrement()
            );
        }

        for (int i = 0; i < 5; i++) {
            demo(
                    () -> new LongAdder(),
                    (longAdder) -> longAdder.increment()
            );
        }
        //结果
//        /*
//          2000000 cost: 40
//          2000000 cost: 37
//          2000000 cost: 36
//          2000000 cost: 37
//          2000000 cost: 35
//          2000000 cost: 9
//          2000000 cost: 4
//          2000000 cost: 4
//          2000000 cost: 5
//          2000000 cost: 4
//
//          Process finished with exit code 0*/

    }

    /**
     *
     * @param supplier ()->结果 累加器对象
     * @param consumer (参数)->void 执行累加操作
     * @param <T>
     */
    private static <T> void demo(Supplier<T> supplier, Consumer<T> consumer) {
        T adder = supplier.get();
        List<Thread> threadList = new ArrayList<>();
        //创建四个线程每个累加50万次
        for (int i = 0; i < 4; i++) {
            threadList.add(new Thread(()->{
                for (int j = 0; j < 500000; j++) {
                    consumer.accept(adder);
                }
            }));
        }
        long start = System.nanoTime();
        threadList.forEach(t -> t.start());
        threadList.forEach(t->{
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        System.out.println(adder + " cost: " + (end - start) / 1000_000);



    }


}
