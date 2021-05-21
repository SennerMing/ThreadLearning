package design.guarded;

public class People extends Thread{
    @Override
    public void run() {
        GuardedObject guardedObject = MailBox.createGuardedObject();
        System.out.println(Thread.currentThread().getName()+"开始收信id:[" + guardedObject.getId() + "]");
        Object mail = guardedObject.getResult(5000);
        System.out.println(Thread.currentThread().getName()+"收到信件：[" + guardedObject.getId() + "]，内容：[" + mail + "]");
    }
}
