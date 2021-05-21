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
                    messages.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            messages.addLast(message);
            messages.notifyAll();
        }
    }

    public Message take() {
        //检查队列是否为空
        synchronized (messages) {
            while (messages.isEmpty()) {
                try {
                    messages.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Message message = messages.removeFirst();
            messages.notifyAll();
            return message;
        }
    }

}
