package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestAccount {
    public static void main(String[] args) {
        Account1 account = new AccountUnsafe(10000);
        Account1.demo(account);


        Account1 account1 = new AccountSafe(10000);
        Account1.demo(account1);
    }

}

class AccountUnsafe implements Account1{

    private Integer balance;

    public AccountUnsafe(Integer balance) {
        this.balance = balance;
    }

    @Override
    public Integer getBalance() {
        synchronized (this) {
            return this.balance;
        }
    }

    @Override
    public void withdraw(Integer amount) {
        synchronized (this){
            this.balance -= amount;
        }
    }
}

class AccountSafe implements Account1 {
    private AtomicInteger balance;

    public AccountSafe(Integer balance) {
        this.balance = new AtomicInteger(balance);
    }

    @Override
    public Integer getBalance() {
        return this.balance.get();
    }

    @Override
    public void withdraw(Integer amount) {
        while (true) {
            int prev = this.balance.get();
            int next = prev - amount;
            if (this.balance.compareAndSet(prev, next)) {
                break;
            }
        }
    }
}





interface Account1 {
    //获取余额
    Integer getBalance();

    //取款
    void withdraw(Integer amount);

    static void demo(Account1 account) {
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            threadList.add(new Thread(()->{
                account.withdraw(10);
            }));
        }
        long start = System.nanoTime();
        threadList.forEach(Thread::start);
        threadList.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        System.out.println(account.getBalance()+"  cost  "+(end-start)/1000_000 + "ms");
    }
}