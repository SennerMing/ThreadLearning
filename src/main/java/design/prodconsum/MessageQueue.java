package design.prodconsum;


import java.util.LinkedList;

//消息队列，用于java线程之间的通信
public class MessageQueue {

    private LinkedList<Message> messages = new LinkedList<>();
    //消息队列的容量
    private int capacity;

    public MessageQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(Message message) {
        synchronized (messages) {
            while (messages.size() == capacity) {
                try {
                    System.out.println("当前队列已满，PUT操作正在等待消费....");
                    messages.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(Thread.currentThread().getName()+"生产了："+message);
            messages.addLast(message);
            messages.notifyAll();
        }
    }

    public Message take() {
        //检查队列是否为空
        synchronized (messages) {
            while (messages.isEmpty()) {
                try {
                    System.out.println("当前队列已空，TAKE操作正在等待生产....");
                    messages.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Message message = messages.removeFirst();
            System.out.println(Thread.currentThread().getName()+"消费了："+message);
            messages.notifyAll();
            return message;
        }
    }

}
