package atomic;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class TestAtomicField {

    public static void main(String[] args) {
        Student student = new Student();

        AtomicReferenceFieldUpdater atomicReferenceFieldUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");

        atomicReferenceFieldUpdater.compareAndSet(student, null, "张三");

        System.out.println(atomicReferenceFieldUpdater.get(student));

    }

}

class Student {

    volatile String name;

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                '}';
    }
}
