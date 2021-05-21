package design.guarded;

import utils.DownLoader;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GuardedObject {

    private int id;

    //存储结果对象
    private Object result;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GuardedObject(int id) {
        this.id = id;
    }

    public GuardedObject() {
    }

    //    public Object getResult() {
//        synchronized (this) {
//            while (result == null) {
//                try {
//                    this.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            return result;
//        }
//    }

    public Object getResult(long timeout) {
        synchronized (this) {
            //记录开始时间
            long begin = System.currentTimeMillis();
            //记录经历的时间
            long passedTime = 0l;
            while (result == null) {
                //这一轮循环应该等待的时间
                long waitTime = timeout - passedTime;
                if (waitTime <= 0) {
                    break;
                }
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //求得经历的时间
                passedTime = System.currentTimeMillis() - begin;
            }
            return result;
        }
    }


    public void complete(Object result) {
        synchronized (this) {
            this.result = result;
            this.notifyAll();
        }
    }


    public static void main(String[] args) {
        GuardedObject guardedObject = new GuardedObject();

//        new Thread(() -> {
//            List<String> result = (List<String>) guardedObject.getResult(2000);
//            System.out.println(result.size());
//        }, "t1").start();
//
//        new Thread(() -> {
//            try {
//                guardedObject.complete(DownLoader.download());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }, "t2").start();


        new Thread(() -> {
            List<String> result = (List<String>) guardedObject.getResult(2000);
            System.out.println(result.size());
        }, "t3").start();

        new Thread(() -> {
            try {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    guardedObject.complete(DownLoader.download());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }, "t4").start();

    }


}
