package design.guarded;

import java.util.concurrent.TimeUnit;

public class TestPeoplePostman {

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new People().start();
        }

        try {
            TimeUnit.SECONDS.sleep(1);

            for (Integer id : MailBox.getIds()) {
                new Postman(id, "内容：" + id).start();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

}
