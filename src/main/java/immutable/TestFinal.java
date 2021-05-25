package immutable;

import org.w3c.dom.ls.LSOutput;

public class TestFinal {

    final static int A = 10;
    final static int B = Short.MAX_VALUE + 1;

    final int a = 20;
    final int b = Integer.MAX_VALUE;

    final void test() {

    }



}


class UserFinal{

    public void test() {
        System.out.println(TestFinal.A);
        System.out.println(TestFinal.B);
        System.out.println(new TestFinal().a);
        System.out.println(new TestFinal().b);
        new TestFinal().test();
    }

}

class UseFinal1{
    public void test() {
        System.out.println(TestFinal.A);
    }
}