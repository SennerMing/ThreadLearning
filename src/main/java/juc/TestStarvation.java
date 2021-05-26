package juc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestStarvation {
    static final List<String> MENU = Arrays.asList("宫保鸡丁", "地三鲜", "辣子鸡", "鲍鱼");
    static Random RANDOM = new Random();

    static String cooking() {
        return MENU.get(RANDOM.nextInt(MENU.size()));
    }

    public static void main(String[] args) {
        ExecutorService waiter = Executors.newFixedThreadPool(2);
        ExecutorService cooker = Executors.newFixedThreadPool(2);

        waiter.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 处理点餐");
            Future<String> future = cooker.submit(() -> {
                System.out.println(Thread.currentThread().getName() + " 做菜");
                return cooking();
            });
            try{
                System.out.println(Thread.currentThread().getName() + " 上菜 " + future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });

        waiter.execute(()->{
            System.out.println(Thread.currentThread().getName() + " 处理点餐");
            Future<String> future = cooker.submit(() -> {
                System.out.println(Thread.currentThread().getName() + " 做菜");
                return cooking();
            });
            try{
                System.out.println(Thread.currentThread().getName() + " 上菜 " + future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });

//        executorService.shutdown();

    }

}
