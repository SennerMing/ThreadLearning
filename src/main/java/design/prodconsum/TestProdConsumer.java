package design.prodconsum;

import java.util.concurrent.TimeUnit;

public class TestProdConsumer {

    public static void main(String[] args) {

        MessageQueue messageQueue = new MessageQueue(4);

        //生产者线程
        for (int i = 0; i < 6; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                messageQueue.put(new Message(id, "值：" + id));
            }, "生产者线程").start();
        }

        //消费者线程
        new Thread(() -> {
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(1);
                    Message message = messageQueue.take();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "消费者线程").start();


    }


}
