package atomic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TestAtomicArray {


    public static void main(String[] args) {

        demo(
                ()->new int[10],
                (array)->array.length,
                (array,index)->array[index]++,
                (array)-> System.out.println(Arrays.toString(array))
        );
        demo(
                ()->new AtomicIntegerArray(10),
                (array)->array.length(),
                (array,index)->array.getAndIncrement(index),
                (array)-> System.out.println(array)
        );

    }

    /**
     * 测试方法
     * @param supplier  提供数组，可以是线程不安全数组或线程安全数组
     * @param function  获取数组长度的方法
     * @param biConsumer    自增方法，回传array，index
     * @param consumer  打印数组的方法
     * @param <T>
     */
    // Supplier 函数式接口，表示提供者，特点无中生有 没有参数:()->结果，给他一个结果

    // Function 函数，一个参数一个结果:(参数)->结果;
    // BiFunction 函数，两个参数一个结果的：(参数1，参数2)->结果。

    // Consumer 消费者，一个参数，但是没有结果:(它会传过来一个参数)->void(你可以拿到这个参数进行操作，不用给他返回结果)
    // BiConsumer (参数1，参数2)->
    private static <T> void demo(Supplier<T> supplier, Function<T, Integer> function,
                                 BiConsumer<T, Integer> biConsumer, Consumer<T> consumer) {
        List<Thread> threads = new ArrayList<>();
        T array = supplier.get();
        int length = function.apply(array);
        for (int i = 0; i < length; i++) {
            //每个线程对数组做10000次操作
            threads.add(new Thread(()->{
                for (int j = 0; j < 10000; j++) {
                    biConsumer.accept(array, j % length);
                }
            }));
        }
        threads.forEach(t -> t.start());
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        consumer.accept(array);
    }


}
