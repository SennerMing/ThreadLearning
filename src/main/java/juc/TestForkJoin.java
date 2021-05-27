package juc;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class TestForkJoin {

    public static void main(String[] args) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        System.out.println(forkJoinPool.invoke(new MyTask(5)));

        //new MyTask(5) + new MyTask(4) + new MyTask(3) .... + new MyTask(1)
        System.out.println(forkJoinPool.invoke(new MyTaskUpgrade(1, 5)));
    }


}


//求1~n之间的整数的和
class MyTaskUpgrade extends RecursiveTask<Integer> {
    private int begin;
    private int end;

    public MyTaskUpgrade(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        if (begin == end) {
            System.out.println("join() " + begin);
            return begin;
        }

        if (end - begin == 1) {
            System.out.println("join() " + begin + " + " + end + " = " + (begin + end));
            return end + begin;
        }

        int mid = (end + begin) / 2;

        MyTaskUpgrade taskUpgrade = new MyTaskUpgrade(begin, mid);
        taskUpgrade.fork();

        MyTaskUpgrade taskUpgrade1 = new MyTaskUpgrade(mid + 1, end);
        taskUpgrade1.fork();

        int result = taskUpgrade.join() + taskUpgrade1.join();
        System.out.println("join() " + taskUpgrade + " + " + taskUpgrade1 + " = " + result);
        return result;
    }

    @Override
    public String toString() {
        return "MyTaskUpgrade{" +
                "begin=" + begin +
                ", end=" + end +
                '}';
    }
}


//求1~n之间的整数的和
class MyTask extends RecursiveTask<Integer> {
    private int n;

    public MyTask(int n) {
        this.n = n;
    }

    @Override
    protected Integer compute() {
        if (n == 1) {
            return 1;
        }

        MyTask task = new MyTask(n - 1);
        task.fork();    //对任务进行拆分，让一个线程去执行（ForkJoinPool）
        int result = n + task.join();    //获取任务结果

        return result;
    }
}