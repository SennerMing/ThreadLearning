package design.guarded;

public class Postman extends Thread {
    private int id;
    private String mail;

    public Postman(int id, String mail) {
        this.id = id;
        this.mail = mail;
    }

    @Override
    public void run() {
        GuardedObject guardedObject = MailBox.getGuardedObject(id);
        System.out.println(Thread.currentThread().getName()+"送信的信id["+id+"]");
        guardedObject.complete(mail);
    }
}
