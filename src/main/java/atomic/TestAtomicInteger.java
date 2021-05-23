package atomic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class TestAtomicInteger {

    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(5);

        //这个操作是一个原子的操作，想做啥运算做啥运算，CAS操作，如果operand已经被改掉了，那么就会update失败
//        atomicInteger.updateAndGet(operand -> operand * 10);
//        atomicInteger.getAndUpdate(operand -> operand / 10);

        updateAndGet(atomicInteger, operand -> operand / 2);



        System.out.println(atomicInteger.get());
    }

    //就实现了上面的updateAndGet或者getAndUpdate
    public static void updateAndGet(AtomicInteger i, IntUnaryOperator operator){
        while (true) {
            int prev = i.get();
            int next = operator.applyAsInt(prev);
            if (i.compareAndSet(prev, next)) {
                break;
            }
        }
    }


}
