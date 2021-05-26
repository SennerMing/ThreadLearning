package juc;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class TestCacheThreadPool {


    public static void main(String[] args) {

        SynchronousQueue<Integer> integers = new SynchronousQueue<>();
        new Thread(()->{
            try{
                System.out.println("puting..."+1);
                integers.put(1);
                System.out.println("putted..."+1);

                System.out.println("putting..."+2);
                integers.put(2);
                System.out.println("putted..."+2);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        },"t1").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(()->{
            try{
                System.out.println("taking...."+1);
                integers.take();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        },"t2").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(()->{
            try{
                System.out.println("taking" + 2);
                integers.take();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        },"t3").start();
    }
}
