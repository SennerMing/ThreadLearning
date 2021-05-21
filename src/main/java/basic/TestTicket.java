package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class TestTicket {

    public static void main(String[] args) throws InterruptedException {

        for (int n = 0; n < 10; n++) {
            List<Thread> threads = new ArrayList<>();

            TicketWindow ticketWindow = new TicketWindow(1200);

            //卖出票数统计，放在一个集合中吧
            List<Integer> amountList = new Vector<>();

            for (int i = 0; i < 2000; i++) {

                Thread thread = new Thread(()->{
                    int amount = ticketWindow.sell(randomAmount());
                    amountList.add(amount);
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                });
                threads.add(thread);
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            //统计卖出的票数和剩余的票数
            System.out.println("剩余的票数："+ticketWindow.getCount());//剩余的票数
            System.out.println("购买的票数："+amountList.stream().mapToInt(i -> i).sum());

        }


    }


    static Random random = new Random();

    public static int randomAmount() {
        return random.nextInt(5) + 1;
    }


}

class TicketWindow {

    private int count;

    public TicketWindow(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public synchronized int sell(int amount) {

        if (this.count >= amount) {
            this.count -= amount;
//            System.out.println("成功购票[" + amount + "]张");
            return amount;
        }else{
//            System.out.println("还有[" + count + "]张余票");
            return 0;
        }

    }

}
