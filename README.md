## Monitor

Monitor被翻译成**监视器**或者**管程**

每个Java对象都可以关联一个Monitor对象，如果使用synchronized给对象上锁（重量级）之后，该对象头的Mark Word中就被设置指向Monitor对象的指针

Monitor是JVM实现的对象，当使用synchronized(obj)对一个对象进行加锁的时候，那么这个对象头中的MarkWord中就保存了一份对JVM中Monitor对象的引用。

Monitor中有EntrySet、Owner，Owner是Monitor的所有者（当前获得锁的线程），EntryList中存放的是等待队列，放的是别的线程（处于BLOCKED的状态），当Owner的线程执行完毕让出Owner的位置，那么EntryList中的线程就开始进行锁的竞争，争抢Owner的位置。

举例：当前有四个线程Thread0、Thread1、Thread2、Thread3、Thread4、Thread5

- 刚开始的时候，Monitor对象中的Owner为Null
- 当Thread2首先执行synchronized(obj)的时候，就会将Monitor的Owner设置为Thread-2，Monitor中只能有一个Owner
- 在Thread2上锁的过程中，如果Thread3、Thread4、Thread5也来进行synchronized(obj)的话，那么就会进入Monitor中的EntryList中进行BLOCKED
- Thread2执行完同步代码块的内容，然后唤醒EntryList中正处于阻塞状态的线程来竞争锁，竞争是非公平的（就不一定先进来的先拿到锁，看JVM心情O(∩_∩)O哈哈~）
- Monitor中还有一个域叫做WaitSet，其中存放了之前获取过锁的线程(比如Thread0、Thread1)，但由于某些条件不满足而进入WAITING状态的线程，可以结合后面的wait-notify进行理解
  - 注意：
    - synchronized必须是进入同一个对象的Monitor才会有上述的效果
    - 不加synchronized的对象不会关联Monitor，不遵从以上的规则

## 轻量级锁

轻量级锁的使用场景：如果一个对象虽然有多线程访问，但是多线程访问的时间是错开的（也就是虽然写了synchronized()，但是并没有出现竞争），那么就可以使用轻量级锁来进行优化。

轻量级锁对使用者是透明的，即语法仍然是synchronized。

假设有两个方法同步块，利用同一个锁对象进行加锁操作

```java
static final Object obj = new Object();
public static void method1(){
  synchronized(obj){
		//同步块A
    method2();
  }
}
public static void method2(){
  synchronized(obj){
    //同步块B
  }
}
```

- 创建锁记录（Lock Record）对象，每个线程的栈帧都会包含一个锁记录的结构，内部可以存储锁定对象的Mark Word，Lock Record中有两个域，一个保存是当前线程的锁地址信息Lock Record Address，还有一个Object Reference保存的是Object对象的引用
- 让锁记录中的Object Reference指向锁对象，并尝试用CAS替换Object的Mark Word，将Mark Word的值存入锁记录；那么，未加锁的Object对象头中Mark Word中Lightweight Locked位置为：锁记录地址为空，标记为00，CAS成功之后，锁记录地址就为加锁成功的Lock Record Address，00也就变成了01
  - 那么如果CAS失败，会出现两种情况
    - 如果是其他线程已经持有了该Object对象的轻量级锁，这时表明有竞争，那么会进入锁膨胀过程
    - 如果是自己执行了synchronized锁重入，那么再添加一条Lock Record作为重入的计数，Lock Record作为加锁的记录，其中的Lock Record Address为null
- 当退出synchronized代码块（解锁时）如果有取值为null的锁记录时，表示有重入，这时重置锁记录，标识重置计数减一
- 当退出synchronized代码块时（解锁时）锁记录的值不为null，这时使用CAS将Mark Word的值恢复给对象头
  - 成功，则解锁成功
  - 失败，说明轻量级锁进行了锁膨胀或者已经升级为重量级锁，进入重量级锁解锁流程

## 锁膨胀

如果在尝试加轻量级锁的过程中，CAS操作无法成功，这时一种情况就是有其他线程为此对象加上了轻量级锁（有竞争力），那么这个时候需要进行锁膨胀，将轻量级锁变为重量级锁。

```java
static Object obj = new Object();
public static void method1(){
  synchronized(obj){
    //同步代码块
  }
}
```

- 例如有两个线程Thread0，Thread1，当Thread1进行轻量级加锁时，Thread0已经对该obj对象进行了轻量级锁
- 这时Thread1加轻量级锁失败，进入锁膨胀流程
  - 即为Object对象申请Monitor（重量级）锁，并且让Object中Lock Record中指向轻量级锁地址改为指向重量级锁地址
  - 接着自己也就是Thread1，进入Monitor的EntryList中进行BLOCKED
- 当Thread0退出同步块（解锁）时，使用CAS将MarkWord的值恢复给对象头，失败。这时会进入重量级解锁流程，即按照Monitor地址找到Monitor对象（WaitSet、EntryList、Owner），设置Owner为null，唤醒EntryList中BLOCKED的线程

## 自旋优化

重量级锁进行竞争的时候，还可以使用自旋来进行优化，如果当前线程自旋成功（即这时候持有锁的线程已经退出了同步块，释放了锁），这时当前线程就可以避免阻塞，多核CPU才有意义。

自旋重试成功的情况：

| 线程1（CPU1上）         | 对象Mark                   | 线程2（CPU2上）         |
| ----------------------- | -------------------------- | ----------------------- |
| -                       | 10（重量级锁）             | -                       |
| 访问同步块，获取Monitor | 10（重量级锁）重量级锁指针 | -                       |
| 成功（加锁）            | 10（重量级锁）重量级锁指针 | -                       |
| 执行同步块              | 10（重量级锁）重量级锁指针 | -                       |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 访问同步块，获取Monitor |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 自旋重试                |
| 执行完毕                | 10（重量级锁）重量级锁指针 | 自旋重试                |
| 成功（解锁）            | 01（无锁状态）             | 自旋重试                |
| -                       | 10（重量级锁）重量级锁指针 | 成功（加锁）            |
| -                       | 10（重量级锁）重量级锁指针 | 执行同步块              |
| -                       | ...                        | ...                     |

自旋重试失败的情况：

| 线程1（CPU1上）         | 对象Mark                   | 线程2（CPU2上）         |
| ----------------------- | -------------------------- | ----------------------- |
| -                       | 10（重量级锁）             | -                       |
| 访问同步块，获取Monitor | 10（重量级锁）重量级锁指针 | -                       |
| 成功（加锁）            | 10（重量级锁）重量级锁指针 | -                       |
| 执行同步块              | 10（重量级锁）重量级锁指针 | -                       |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 访问同步块，获取Monitor |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 自旋重试                |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 自旋重试                |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 自旋重试                |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 阻塞                    |
| 执行同步块              | 10（重量级锁）重量级锁指针 | 阻塞                    |
| ...                     | ...                        | ...                     |

- 在Java6之后自旋锁是自适应的，比如对象刚刚的一次自选操作成功过，那么认为这次自旋成功的可能性会高，就会多自旋几次；反之，就少自旋甚至不自旋，总之，比较智能。
- 自旋会占用CPU事件，单核CPU自旋的话就是在浪费CPU资源，多核CPU进行自旋才能发挥出优势
- Java7之后不能控制是否开启自旋功能

## 偏向锁

轻量级锁在没有竞争时（就自己这个线程加锁），每次锁重入的时候仍然要进行CAS操作。

Java6中引入了偏向锁来做进一步优化：只有第一次使用CAS将线程ID设置到对象obj的Mark Word头（那么轻量级锁的话，是每一次都CAS相当于翻书包，才能确定线程锁），之后发现这个线程ID是自己的就表示没有竞争，不用重新进行CAS操作。以后只要不发生竞争，这个对象就归该线程所有

例如：

```java
static final Object obj = new Object();
public static void m1(){
  synchronized(obj){
    //同步代码块A
    m2();
  }
}
public static void m2(){
  synchronized(obj){
    //同步代码块B
    m3();
  }
}
public static void m3(){
  synchronized(obj){
    //同步代码块C
  }
}
```

### 偏向状态

对象头：

| MarkWord(64 bits)          |             |          |       |               |      | State              |
| -------------------------- | :---------: | -------- | ----- | ------------- | ---- | ------------------ |
| Unused:25                  | Hashcode:31 | Unused:1 | Age:4 | Biased_lok:0  | 01   | Normal             |
| Thread:54                  |   Epoch:2   | Unused:1 | Age:4 | Biased_lock:1 | 01   | Biased             |
| Ptr_to_lock_record         |      :      | 62       |       |               | 00   | Lightweight Locked |
| Prt_to_heavyweight_monitor |      :      | 62       |       |               | 10   | Heavyweight Locked |
|                            |             |          |       |               | 11   | Marked for GC      |

Normal中：biased_lock-0未启用偏向锁，Biased中Biased_lock-1启用偏向锁

一个对象创建时：

- 如果开启了偏向锁（默认开启），那么对象创建后，MarkWord值为0x05即最后3位为101，这个时候他的Thread、epoch、age都为0

- 偏向锁是默认延迟的，不会在程序启动时立即生效，如果想避免延迟，可以加以下VM参数来禁用延迟

  ```java
  -XX:BiasedLockingStartupDelay=0
  ```

- 如果没有开启偏向锁，那么对象创建后，MarkWord值为0x01即最后3位为001，这是他的hashcode、age都为0，第一次用到hashcode时才会赋值

如果程序中基本不存在这种偏向锁，就是一个线程自己给Object对象加锁，加来加去的也没啥意思，那么可以使用一个VM参数来禁用掉这个偏向锁

```java
-XX:-UseBiaseddLoocking
```

禁用了偏向锁，那么系统默认的锁就是轻量级锁，轻量级锁加了，但是存在多线程的竞态，那么就会发生锁膨胀，升级为重量级锁

### hashCode

这个函数会禁用掉偏向锁，当对象调用了hashCode之后，对象头中MarkWord需要增加hashcode字段，因为hashcode长度为31位，长度太大，就会舍掉可偏向对象的偏向锁状态，轻量级锁的hashcode会存在线程栈帧里面，重量级锁的hashcode会存在Monitor对象中，所以他们不会被调用hashCode影响

**不是一个同一个对象的话，且线程之间调用是错开的话，偏向锁会升级为轻量级锁，解锁后，对象的话从可偏向状态，变为不可偏向；调用wait 或者 notify会撤销偏向锁，这个只有重量级锁才有，甭管你是偏向还是轻量级都转为重量级锁**

## 批量重偏向

如果对象虽然被多个线程访问，但没有竞争，这时偏向了线程T1的对象仍有机会重新偏向T2，重偏向会重置对象的ThreadID.

当撤销偏向锁阈值超过20次之后，JVM会这样觉得，我是不是偏向错了呢？就很搞笑，预于是就会再给这些对象加锁时重新偏向至加锁线程（批量的操作！），为什么会这样呢？因为重偏向的话也是比较消耗性能的，所以就要批量去，一把梭，把对象中的锁全给他偏向到新的加锁的线程。

## 批量撤销

当撤销偏向锁阈值超过40次之后，就是上面的操作计数，JVM就会觉得自己之前的偏向锁是真的加错了。于是就将整个类的所有对象都变为不可偏向的，新建的对象也是不可偏向的

## 锁消除

```java
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations=3)
@Mesaurement(iterations=5)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MyBenchmark{
  static int x = 0;
  
  @Benchmark
  public void a() throws Exception{
    x++;
  }
  
  @Benchmark
  public void b() throws Exception{
    Object o = new Object();
    synchronized(o){
      x++;
    }
  }
}
```

```java
java -jar thread-learning.jar
```

JIT即时编译器，会对字节码进行再一次优化上面代码Object o并未发生逃逸，并不会被共享，直接干掉这个锁了。

```java
java -XX:-EliminateLocks -jar thread-learning.jar //取消锁消除
```

## Wait/Notify原理

之前将的Monitor对象，还记得吗，就是有WaitSet、EntryList、Owner三个域的重量级锁监视器。

- 当Owner线程发现条件不满足，调用wait方法，即可进如WaitSet中，变为WAITING状态
- BLOCKED和WAITING的线程都处于阻塞状态，不占用CPU的时间片
- BLOCKED线程会在Owner线程释放锁时唤醒
- WAITING线程会在Owner线程调用notify或者notifyAll时唤醒，但唤醒后并不意味着立刻获得锁，仍需进入EntryList重新进行锁的竞争

### 相关API

- obj.wait()让进入object监视器的线程到waitSet中等待
- obj.notify()在object上正在waitSet等待的线程中挑一个唤醒
- obj.notifyAll()让object上正在waitSet等待的线程全部唤醒
- 他们都是线程之间进行协作的手段，都属于object对象的方法，必须获得此对象的锁才能调用这几个方法

### Wait and Notify使用

先来看看sleep(long n)和wait(long n)的区别

1. sleep是Thread方法，而wait是Object的方法
2. sleep不需要强制和synchronized配合使用，但wait需要和synchronized一起使用
3. sleep在睡眠的同时，不会释放对象锁的，但wait在等待的时候会释放对象锁

## 多线程设计模式

### 同步模式之保护性暂停

定义：即Guarded Suspension，用在一个线程等待另一个线程的执行结果

要点：

- 有一个结果需要从一个线程传递到另一个线程，让他们关联同一个GuardedObject
- 如果有结果不断从一个线程到另一个线程那么可以使用消息队列（见生产者/消费者）
- JDK中，join的实现、Future的实现，采用的就是Guarded Suspension设计模式
- 因为要等待另一个线程的结果，因此归类到同步模式