package immutable;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class TestSdf {

    public static void main(String[] args) {


        //方法抽取 快捷键：option+command+m
//        test();

        //jdk8提供的不可变类的 DateFormatter
        DateTimeFormatter stf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {

                TemporalAccessor temporalAccessor = stf.parse("1958-06-08");
                System.out.println(temporalAccessor);
            }).start();
        }

    }

    private static void test() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        for(int i = 0;i < 10; i++){
//            new Thread(()->{
//                try{
//                    System.out.println((sdf.parse("1958-06-08")));
//                } catch(Exception e){
//                    System.out.println((e));
//                }
//            }).start();
//        }

        //==========================改进====================================
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for(int i = 0;i < 10; i++){
            new Thread(()->{
                synchronized (sdf){
                    try{
                        System.out.println((sdf.parse("1958-06-08")));
                    } catch(Exception e){
                        System.out.println((e));
                    }
                }
            }).start();
        }
    }

}
