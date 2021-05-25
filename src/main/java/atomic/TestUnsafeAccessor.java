package atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class TestUnsafeAccessor {

    public static void main(String[] args) {

//        Unsafe unsafe = Unsafe.getUnsafe();
//        System.out.println(unsafe);


        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            System.out.println(unsafe);

            //1.获取感兴趣字段，在类中的偏移量
            long id_offset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("id"));
            long name_offset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("name"));

            Teacher teacher = new Teacher();

            unsafe.compareAndSwapInt(teacher, id_offset, 0, 1);
            unsafe.compareAndSwapObject(teacher, name_offset, null, "张三");

            System.out.println(unsafe.getInt(teacher, id_offset));
            System.out.println(unsafe.getObject(teacher, name_offset));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }




    }

}

class Teacher{
    volatile int id;
    volatile String name;
}