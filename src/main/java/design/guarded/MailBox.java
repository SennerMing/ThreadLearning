package design.guarded;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MailBox {

    private static Map<Integer, GuardedObject> boxes = new Hashtable<>();
    private static AtomicInteger atomicInteger = new AtomicInteger();

    private static int generateId() {
        return atomicInteger.incrementAndGet();
    }

    public static GuardedObject createGuardedObject() {

        GuardedObject guardedObject = new GuardedObject(generateId());
        boxes.put(guardedObject.getId(), guardedObject);
        return guardedObject;
    }

    public static Set<Integer> getIds() {
        return boxes.keySet();
    }

    public static GuardedObject getGuardedObject(int id) {
        return boxes.remove(id);
    }


}
