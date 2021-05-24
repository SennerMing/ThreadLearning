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

### 异步模式之生产者/消费者模式

要点：

- 与前面的保护性暂停中的GuradedObject不同，不需要产生结果和消费者结果的线程一一对应
- 消费队列可以用来平衡生产和消费的线程资源
- 生产者仅负责产生结果数据，不关心数据该如何进行处理，而消费者专心处理结果数据
- 消息队列是容量限制的，队列满时不会再加入数据，空时不会再消耗数据
- JDK中各种阻塞队列，采用的就是这种模式

## LockSupport

### park、unpack

与Object的wait和notify相比

- wait，notify和notifyAll必须配合Object Monitor来一起使用，而unpark不必
- park & unpack是以线程为单位来[阻塞]和[唤醒]线程，而notify只能随机唤醒一个等待线程，notifyAll是唤醒所有等待线程，就不那么精确了
- park & unpack可以先unpark，而wait & notify不能先notify

#### 原理分析

每个线程都有自己的一个Parker对象，由三部分组成_counter(干粮)，_codn(帐篷)和_mutex(互斥锁)，举个例子

- 线程就像一个旅人，Parker就像他随身携带的背包，条件变量就好比背包中的帐篷。_counter就好比背包中的备用干粮（0为耗尽，1为充足）
- 调用park就是要看需不需要停下来歇息，就是检查下背包
  - 如果备用干粮耗尽，那么就钻进帐篷休息
  - 如果备用干粮充足，那么就不需要停留，继续前进
- 调用unpark，就好比令干粮充足
  - 如果这时线程还在帐篷，就唤醒旅人让他继续前进
  - 如果这是线程还在运行，那么下次他调用park时，仅是消耗掉备用干粮，不需要停留继续前进，因为背包空间有限，多次调用unpark仅会补充一份备用干粮

## 对线程状态转换的理解

- 情况1 NEW ----> RUNNABLE

  当调用t.start()方法时，由NEW --> RUNNABLE

- 情况2 RUNNABLE <----> WAITING

  t线程使用synchronized(obj)获取了对象锁之后

  - 调用obj.wait()方法时，t线程从RUNNABLE --> WAITING
  - 调用obj.notify()、obj.notifyAll()，t.interrupt()方法时
    - 竞争锁成功，t线程从WAITING-->RUNNABLE
    - 竞争锁失败，t线程从WAITING-->BLOCKED

- 情况3 RUNNABLE <----> WAITING

  - 当前线程调用t.join()方法时，当前线程从RUNNABLE ---> WAITING
    - 注意是当前线程在t线程对象的监视器上等待
  - t线程运行结束时，或调用了当前线程的interrupt()方法时，当前线程从WAITING ---> RUNNABLE

- 情况4 RUNNBALE <---> WAITING

  - 当前线程调用LockSupport.park()方法会让当前线程从RUNNABLE ----> WAITING
  - 调用LockSupport.unpack(目标线程)或调用了线程的interrupt()，会让目标线程从WAITING --> RUNNABLE

- 情况5 RUNNABLE <---> TIMED_WAITING

  t线程用synchronized(obj)获取了对象锁之后

  - 调用了obj.wait(long n)方法时，t线程从RUNNABLE ----> TIMED_WAITING
  - t线程等待时间超过了n毫秒，或调用obj.notify()，obj.notifyAll()，t.interrupt()时
    - 竞争锁成功，t线程从TIMED_WAITING ---> RUNNABLE
    - 竞争锁失败，t线程从TIMED_WAITING ---> BLOCKED

- 情况6 RUNNABLE <---->TIMED_WAITING

  - 当前线程调用t.join(long n)方法时，当前线程从RUNNABLE -----> TIMED_WAITING
    - 注意是当前线程在t线程对象的监视器上等待
  - 当前线程等待时间超过了n毫秒，或t线程运行结束，或调用了当前线程的interrupt()方法时，当前线程从TIMED_WAITING ----> RUNNABLE

- 情况7 RUNNABLE <----> TIMED_WAITING

  - 当前线程调用了Thread.sleep(long n)，当前线程从RUNNABLE ---> TIMED_WAITING
  - 当前线程等待时间超过了n毫秒，当前线程从TIMED_WAITING ----> RUNNABLE

- 情况8 RUNNABLE <----> TIMED_WAITING

  - 当前线程调用LockSupport.parkNanos(long nanos)或LockSupport.parkUntil(long millis)时，当前线程从RUNNABLE ----> TIMED_WAITING
  - 调用LockSupport.unpark(目标线程)或调用了线程的interrupt()，或是等待超时，会让目标线程从TIMED_WAITING ----> RUNNABLE

- 情况9 RUNNABLE <----> BLOCKED

  - t线程使用synchronized(obj)获取了对象锁时如果竞争失败，从RUNNABLE ----> BLOCKED
  - 持有obj锁线程的同步代码块执行完毕，会唤醒该对象上所有BLOCKED的线程重新竞争，如果其中t线程竞争到对象锁的话，那么就从BLOCKED ----> RUNNABLE，其他失败的线程仍然BLOCKED

- 情况10 RUNNABLE <----> TERMINATED

  - 当前线程所有代码运行完毕，进入TERMINATED

## 线程状态查看

```java
1.jps  2.jstack [pid]
```

## ReentrantLock

相对于synchronized它具备如下特点

- 可中断
- 可以设置超时时间
- 可以设置为公平锁
- 支持多个条件变量

与synchronized一样，都支持可重入

可重入指的是同一个线程如果首次获得了这把锁，那么因为它是这把锁的拥有者，因此有权力再次获取这把锁，如果是不可重入锁，那么第二次获取锁时，自己也会被锁挡住

### 基本语法

```java
//获取锁
reentrantLock.lock();
try{
  //临界区
}finally{
  //释放锁
  reentrantLock.unlock();
}
```

### 条件变量

synchronized中也有条件变量，就是我们讲原理时那个waitSet休息室，当条件不满足时，进入waitSet等待ReentrantLock的条件变量比synchronized强大之处于，它是支持多个条件变量的，这就好比

- synchronized是那些不满足条件的线程都在一间休息室等消息
- 而ReentrantLock支持多间休息室，有专门等烟的休息室、专门等早餐的休息室、唤醒时也是按休息室来唤醒

使用流程

- await前需要获得锁
- await执行后，会释放锁，进入conditionObject等待
- await的线程被唤醒（或打断或超时）去重新竞争lock锁
- 竞争lock锁成功后，从await后继续执行

## 多线程设计模式

### 同步模式之顺序控制

固定运行顺序比如，必须先2后1的打印，可以使用wait、notify，也可使park、unpack，可以看basic.TestFixedOrder

### 同步模式之多线程交替顺序控制

交替输出比如，线程1输出a 5次，线程2 输出b 5次，线程3输出c 5次。现在要求输出abcabcabcabcabc怎么实现，可以使用wait、notify，也可以使用park、unpack，还可以使用ReentrantLock，参考basic.TestOrderController

#### 要点总结

- 分析多线程访问共享资源的时候，那些代码片段属于临界区
- 使用synchronized互斥解决临界区的线程安全问题
  - 掌握synchronized锁对象的语法
  - 掌握synchronized家在成员方法和静态方法语法
  - 掌握wait & notify同步方法
- 使用lock互斥解决临界区的安全问题
  - 掌握lock的使用细节：可打断、锁超时、公平锁、条件变量
- 学会分析变量的线程安全性、掌握常见线程安全类的使用
- 了解线程活跃性问题：死锁、活锁、饥饿
- 引用方面
  - 互斥：使用synchronized或lock打到共享资源互斥效果
  - 同步：使用wait & notify或lock的condition变量来达到线程通信的效果
- 原理方面
  - monitor、synchronized、wait & notify原理
  - synchronized进阶原理，偏向，轻量，锁膨胀，重量级锁，批量偏向，批量撤销偏向
  - park & unpark原理
- 模式方面
  - 同步模式之保护性暂停
  - 异步模式之生产者消费
  - 同步模式之顺序控制

## JMM（Java Memory Model）

### 共享模型之内存

上面讲到了Monitor主要关注的是访问共享变量时，保证临界区代码的原子性，下面将进一步了解共享变量在多线程之间的[可见性]问题与多条指令执行时的[有序性]问题

### Java内存模型

JMM即Java Memory Model，他定义了主存、工作区内存抽象的概念，底层对应着CPU寄存器、缓存、硬件内存、CPU指令优化等。

JMM体现在一下几个方面

- 原子性 - 保证指令不受到线程上下文切换的影响
- 可见性 - 保证指令不受到CPU缓存的影响
- 有序性 - 保证指令不受到CPU指令并行优化的影响

```java
static boolean run = true;
psvm(){
  Thread t = new Thread(()->{
    while(run){
      //代码块
    }
  });
  t.start();
  TimeUnit.SECONDS.sleep(1);
  run = false;
}
```

上面的代码他结束不了，为什么呢？来分析分析

在程序运行之初，存在着主内存(static boolean run = true)，还有主线程和t线程

1. 初始状态，t线程刚开始从主内存读取了run的值到工作内存
2. 因为t线程要频繁的从主内存中读取run的值，JIT编译器会将run的值缓存到自己的工作内存（t线程的高速缓存）中的高速缓存中，减少对主内存中run的访问，以提高效率
3. 1秒钟之后，main线程修改了run的值，并同步导主内存中，而t线程是从自己工作内存的高速缓存中读取这个run的变量，结果永远都是旧的值！

修改，将run变量编程volatile，会去主内存中获得最新的值,synchronized(obj){临界区中的代码，也是可见性的}

可见性 & 原子性

前面的例子体现的时机就是可见性，他可以保证是在多个线程之间，一个线程对volatile变量的修改对另一个线程可见，不能保证原子性，仅用在一个写线程，多个读线程的情况：

上例中从字节码理解是这样的：

```java
getstatic run //线程t获取run true
getstatic run //线程t获取run true
getstatic run //线程t获取run true
getstatic run //线程t获取run true
putstatic run //线程main修改run为false，仅此一次
getstatic run //线程t获取run为false
```

比较一下之前我们将线程安全时举的例子：两个线程一个i++一个i--，只能保证看到最新值，不能解决指令交错

```java
//假设i的初始值为0
getstatic i	//线程2 - 获取静态变量i的值 线程内i=0

getstatic i //线程1 - 获取静态变量i的值 线程内i=0
iconst_1 		//线程1 - 准备常量1
iadd				//线程1 -	自增 线程内i=1
putstatic i	//线程1 - 将修改后的值存入静态变量i 静态变量i=1

iconst_1		//线程2 - 准备常量1
isub 				//线程2 - 自减 线程内i=-1
putstatic		//线程2 -	将修改后的值存入静态变量i	静态变量i=-1
```

### Balking模式

```java
private static boolean hasStart = false;
psvm(){
  synchronized(this){
    if(hasStart){
      return;
    }
  }
}
```

单例模式也比较常用的

### 指令重排序

指令重排序是为了性能优化，提高CPU使用率，现代处理器会设计为一个时钟周期完成一条执行时间最长的CPU指令。为什么这么做呢？可以想到指令还可以在划分成一个个更小的阶段，例如，每条指令都可以分为：取指令--指令译码--内存访问--数据写回 这5个阶段

### 支持流水线的处理器

现代CPU支持多级指令流水线，例如支持同时执行 取指令 -- 指令译码 --  执行指令 -- 内存访问 -- 数据写回 的处理器，就可以称之为 **五级指令流水线**。这时CPU可以在一个时钟周期内，同时运行五条之灵的不同阶段（相当于一条执行时间最长的复杂指令），IPC=1，本质上流水线技术并不能缩短单条指令的执行时间，但是它变相地提高了指令的吞吐率。

奔腾4（Pentium4）支持高达35级流水线，但由于功耗太高被废弃。

在不改变程序结果的前提下，这些指令的各个阶段可以通过 **重排序**和 **组合**来实现 **指令级并行**，这一技术在上世纪80年代中叶到90年代中叶占据了计算机架构的重要地位。

**分阶段，分工是提升效率的关键！**  任务分解难度大鸭

指令重排的前提是，重拍指令不能影响结果，例如：

```java
//可以重排序的例子
int a = 10;
int b = 20;
sout(a+b);

//不能重排序的例子
int a = 10;
int b = a - 5;
```

### 防止重排序

上代码：

```java
public class ConcurrentTest{
  int num = 0;
  volatile boolean ready = false;
  
  public void m1(){
    if(ready){
      result = num + num;
    } else{
      result = 1; 
    }
  }
  
  public void m2(){
    num = 2;
    ready = true;
  }
}
```

上面对ready加了volatile之后，result就不会出现0的情况了，volatile修饰的字段之前的操作，都不会进行重排序，他会对代码块加上写屏障，保证他之前的代码不会在它之后执行，sout()也会出现正确的结果。

### Volatile原理

volatile的底层实现是内存屏障，Memory Barrier（Memory Fence）

- 对volatile变量的写指令后会加入写屏障
- 对volatile变量的读指令前会加入读屏障

#### 如何保证可见性

写屏障（sfence）保障在该屏障之前的，对共享变量的改动，都同步到主存当中

```java
public void m2(){
  num = 2;
  ready = true;//ready是volatile赋值带写屏障
  //写屏障 方向↑
}
```

而读屏障（lfence）保证在该屏障之后，对共享变量的读取，加载的是主内存中最新的数据

```java
public void m1(){
  //ready是volatile读取值带读屏障，读屏障 方向↓
  if(ready){
    result = num + num;
  }else{
    result = 1
  }
}
```

```java
sequence diagram;
participant t1 as t1 线程;
participant num as num = 0;
participant ready as volatile ready = false;
participant t2 as t2 线程;
t1 -- >> t1 : num = 2;
t1 -- >> ready : ready = true;
Note over t1,ready : 写屏障;
Note over num,t2 : 读屏障;
t2 -- >> ready : 读取ready = true;
t2 -- >> num : 读取num = 2;
```

#### 如何保证有序性

写屏障会确保指令重排序时，不会将写屏障之前的代码排在写屏障之后

```java
public void m2(){
  num = 2;
  ready = true; //ready 是 volatile复制带写屏障
  //写屏障 ↑
}
```

读屏障会确保指令重排序时，不会将读屏障之后的代码排在读屏障之前

```java
public void m1(){
  //ready 是 volatile 读取值带读屏障 读屏障↓
  if(ready){
    result = num + num;
  }else{
    result = 1;
  }
}
```

不能解决指令交错（多线程情况会出问题）：

- 写屏障仅仅保证之后的读能够读到最新的结果，但不能保证读跑到他前面去
- 而有序性的保证也只是保证了本线程内相关代码不被重排序

#### Double-checked Locking

上代码

```java
public final class Singleton{
  private Singleton(){} 
  private static Singleton INSTANCE = null;
  public static Singleton getInstance(){
    if(INSTANCE == null){
      synchronized(Singleton.class){
        if(INSTANCE == null){
          INSTANCE = new Singleton();
        }
      }
    }
    return INSTANCE;
  }
}
```

以上实现的特点：

- 懒惰实例化
- 首次使用getInstance()才使用synchronized加锁，后续使用时无需加锁
- 有隐含的，但很关键的一点：第一个if使用了INSTANCE变量，实在同步块之外

在多线程环境下，上面代码是有问题的，getInstance方法对应的字节码为：

```java
0: getstatic #2;
3: ifnonnull 37;
6: ldc			 #3;		//获得类对象
8: dup;
9: astore_0;				//放入操作数栈
10: monitorenter;		//检查Monitor对象
11: getstatic #2;		//拿到INSTANCE对象
14: ifnonnull 27;
17: new 			#3;
20: dup;
21: invokespecial #4;
24: putstatic			#2;
27: aload_0;
28: monitorexit;
29: goto 					37;
32: astore_1;
33: aload_0;
34: monitorexit;
35: aload_1;
36: athrow;
37: getstatic 		#2;
40: areturn
```

其中

- 17表示创建对象，将对象引用入栈	//new Singleton
- 20表示复制一份对象引用 	 //根据引用地址
- 21表示利用一个对象引用，调用构造方法
- 24表示利用一个对象引用，赋值给static INSTANCE

也许JVM会优化为：先执行24，再执行21。如果两个线程t1、t2按如下事件序列执行：

| THREAD-1                          | INSTANCE | THREAD-2                                         |
| --------------------------------- | -------- | ------------------------------------------------ |
| 17：new                           |          |                                                  |
| 20：dup                           |          |                                                  |
| 24：putstatic（给INSTANCE赋值）   |          |                                                  |
|                                   |          | 0：getstatic（获取INSTANCE引用）                 |
|                                   |          | 3：ifnonnull 37（判断不为空，跳转至37行）        |
|                                   |          | 37：getstatic（获取INSTANCE引用）                |
|                                   |          | 40：areturn（返回）                              |
|                                   |          | **使用INSTANCE对象了**（然鹅，对象还没构造完呢） |
| 21：invokespecial（调用构造方法） |          |                                                  |

这样看来，synchronized不能阻止重排序，volatile可以。

解决办法

```java
public final class Singleton{
  private Singleton(){} 
  private static volatile Singleton INSTANCE = null;
  public static Singleton getInstance(){
    if(INSTANCE == null){
      synchronized(Singleton.class){
        if(INSTANCE == null){
          INSTANCE = new Singleton();
        }
      }
    }
    return INSTANCE;
  }
}
```

```java
//-----------------> 加入了对INSTANCE变量的读屏障 ↓
0: getstatic #2;
3: ifnonnull 37;
6: ldc			 #3;		//获得类对象
8: dup;
9: astore_0;				//放入操作数栈
10: monitorenter;		//检查Monitor对象			保证原子性、可见性
11: getstatic #2;		//拿到INSTANCE对象
14: ifnonnull 27;
17: new 			#3;
20: dup;
21: invokespecial #4;
24: putstatic			#2;
//------------------> 加入对INSTANCE变量的写屏障 ↑
27: aload_0;
28: monitorexit;		//									保证原子性、可见性
29: goto 					37;
32: astore_1;
33: aload_0;
34: monitorexit;
35: aload_1;
36: athrow;
37: getstatic 		#2;
40: areturn
```



| THREAD-1                                      | INSTANCE | THREAD-2                                       |
| --------------------------------------------- | -------- | ---------------------------------------------- |
| 17：new                                       |          |                                                |
| 20：dup                                       |          |                                                |
| 21：invokespecial（调用构造方法）             |          |                                                |
| 24：putstatic（**给INSTANCE赋值，带写屏障**） |          |                                                |
|                                               |          | 0：getstatic（**获取INSTANCE引用，带读屏障**） |
|                                               |          | 3：ifnonnull 37（判断不为空，跳转至37行）      |
|                                               |          | 37：getstatic（获取INSTANCE引用）              |
|                                               |          | 40：areturn（返回）                            |
|                                               |          | 使用INSTANCE对象了（就不会出现为null）         |

上面带读写屏障的步骤，Thread-1：24，Thread-2：0，即使顺序颠倒，也能保证Thread-2进入到synchronized语句块中，交由重量级锁去控制。

#### happens-before

Happens-before规定了对共享变量的写操作对其他线程的读操作可见，他是可见性与有序性的一套规则总结，抛开以下happens-before规则，JMM并不能保证一个线程对共享变量的写，对于其他线程对该共享变量的读可见

- 线程解锁m之前对变量的写，对于接下来对m加锁的其他线程对该变量的读可见

  ```java
  static int x;
  static Object m = new Object();
  
  new Thread(()->{
    synchronized(m){
      x = 10;
    }
  },"t1").start();
  
  new Thread(()->{
    synchronized(m){
      sout(x);
    }
  },"t2").start();
  ```

  

- 线程对volatile变量的写，对接下来其他线程对该变量的读可见

  ```java
  volatile static int x;
  
  new Thread(()->{
    x = 10;
  },"t1").start();
  
  new Thread(()->{
    sout(x);
  },"t2").start();
  ```

- 线程start前对变量的写，对该线程开始后对该变量的读可见

  ```java
  static int x;
  x = 10;
  new Thread(()->{
    sout(x);
  },"t2").start()
  ```

- 线程结果前对变量的写，对其他线程得知它结束后的读可见（比如其他线程调用t1.isAlive()或t1.join()等待他结束）

  ```java
  static int x;
  Thread t1 = new Thread(()->{
    x = 10;
  },"t1");
  t1.start();
  t1.join();
  sout(x);
  ```

- 线程t1打断线程t2(interrupt)前对变量的写，对于其他线程得知t2被打断后对变量的读可见（通过t2.interrupted或t2.isInterrupted）

  ```java
  static int x;
  psvm(){
    Thread t2 = new Thread(()->{
      while(true){
        if(Thread.currentThread().isInterrupted()){
          sout(x);
          break;
        }
      }
    },"t2");
    t2.start();
    
    new Thread(()->{
      TimeUnit.SECONDS.sleep(1);
      x = 10;
      t2.interrupt();
    },"t1").start();
    
    while(!t2.isInterrupted()){
      Thread.yield();
    }
    sout(x);
  }
  ```

- 对变量默认值（0，false，null）的写，对其他线程对该变量的读可见

- 具有传递性，如果x hb -> y 并且 y hb -> z，那么有x hb -> z，配合volatile的防止指令重排，上代码

  ```java
  volatile static int x;
  static int y;
  new Thread(()->{
    y = 10;
    x = 20;
  },"t1").start();
  
  new Thread(()->{
    //x=20 对 t2可见，同时y=10 也对t2可见
    sout(x);
  },"t2").start();
  ```

  变量都是指成员变量或静态成员变量

#### 线程安全问题

单例模式有很多实现方法，饿汉、懒汉、静态内部类、枚举类，试着分析下每种实现下获取单例对象（即调用getInstance）时的线程安全，并思考注释中的问题

- 饿汉式：类加载就会导致该单实例对象被创建
- 懒汉式：类加载不会导致该单实例对象被创建，而是搜词使用该对象时才会创建

实现1：

```java
//问题1：为什么加final		答：防止子类覆盖父类方法破坏单例
//问题2：如果实现了序列化接口，还要怎么做才能防止反序列化破坏单例	
public final class Singleton implements Serializable{
  //问题3：为什么设置为私有？是否能防止反射创建新的实例？答：不能防止反射创建对象
  private Singleton(){}
  //问题4：这样初始化是否能保证单例对象创建时的线程安全？ 答：可以，静态成员变量都是线程安全的，由JVM加载
  private static final Singleton INSTANCE = new Singleton();
  //问题5：为什么提供静态方法而不是直接将INSTANCE设置为public，说出你知道的理由
  //答：提供更好的封装性，可以实现懒惰的初始化方式，还可以对创建单例对象时有更多的控制权，还可以支持泛型
  public static Singleton getInstance(){
    return INSTANCE;
  }
  //问题2：答:在反序列化的过程中，如果发现readResolve()方法，就会采用方法内的INSTANCE对象，而不是字节码对	//象，这样即使反序列化了，也是同一个对象
  public Object readResovle(){
    return INSTANCE;
  }
}
```

实现2：

```java
//问题1：枚举单例是如何实现限制实例个数的	答：就是一个继承自Enum的final static的成员变量
//问题2：枚举单例在创建时是否有并发问题		答：没有，在类加载阶段创建的	
//问题3：枚举单例能否被反射破坏单例				答：不能
//问题4：枚举单例能否被反序列化破坏单例		
//答：枚举类默认实现了Serializable接口的，可以被序列化和反序列化的，但是其考虑到这个问题了，所以内部进行了处理，不能破坏单例
//问题5：枚举单例属于懒汉式还是饿汉式		答:饿汉式，类加载阶段就创建对象
//问题6：枚举单例如果希望加入一些单例创建时的初始化逻辑该如何去做	答：可以写一个构造方法
enum Singleton{
  INSTACNE;
}
```

实现3:

```java
//懒汉式的一个单例
public final class Singleton{
	private Singleton(){}
  private static Singleton INSTANCE = null;
  //分析这里的线程安全，并说明有什么缺点 答：可以保证线程安全的 static 方法加synchronized的相当于锁了class类，缺点就是锁的范围有点大，每次调用都要加锁，性能比较低
  public static synchronized Singleton getInstance(){
    if(INSTANCE != null){
      return INSTACNE;
    }
    INSTANCE = new Singleton();
    return INSTANCE;
  }
}
```

实现4：

```java
public final class Singleton{
 private Singleton(){}
  //问题1：为什么要加volatile？ 答：synchronized内部代码还是会重排序的，影响在synchronized外面的(INSTANC != null)的判断，保证判断前，是在调用完构造函数之前的Singleton实例
  private static voliate Singleton INSATNCE = null;
  
  //问题2：对比实现3，说出这样做的意义 答：性能上会比较优越
  public static Singleton getInstance(){
    if(INSTANCE != null){
      return INSTANCE;
    }
    synchronized(Singleton.class){
      //问题3：为什么还要在这里加空判断，之前不是判断过了吗？
      //答：多线程模式下，防止首次第一次并发访问时候的重复创建问题
      if(INSTANCE != null){
        return INSTACNE;
      }
      INSTANCE = new Singleton();
      return INSTANCE;
    }
  }
  
}
```

实现5：

```java
public final class Singleton{
  private Singleton(){}
  //问题1：属于懒汉式还是饿汉式 答：懒汉式，内部类加载第一次使用到getInstance()方法才会进行类加载和初始化操作
  private static class LazyHolder{
    static final Singleton INSTANCE = new Singleton();
  }
  //问题2：在创建时是否有并发问题 答：不会
  public static Singleton getInstance(){
    return LazyHodler.INSTANCE;
  }
}
```

### 小结

对于JMM

- 可见性 - 由JVM缓存优化引起
- 有序性 - 由JVM指令重排序优化引起
- happens-before规则
- 原理方面
  - CPU指令并行
  - volatile
- 模式方面
  - 两阶段终止模式的volatile改进
  - 同步模式之balking

## 共享模型之无锁

- CAS与volatile

- 原子整数

- 原子引用

- 原子累加器

- Unsafe

  面临的问题，像是银行账户的存取款问题。

### volatile与CAS

获取共享变量时，为了保证该变量的可见性，需要使用volatile修饰。

它可以用来修饰成员变量和静态成员变量，他可以避免线程从自己的工作缓存中查找变量的值，必须到主内存中获取他的值，线程操作volatile变量都是直接操作主内存的。即一个线程对volatile变量的修改，对另一个线程可见。

​	注意

- volatile仅仅保证了共享变量的可见性，让其他线程能够看到最新值，但不能解决指令交错问题（不能保证原子性）

CAS必须接住volatile才能读取到共享变量的最新值来实现Compare and swap的效果		

### CAS效率分析

为什么无锁效率高？

- 无锁的情况下，即使重试失败，线程始终在高速运行，没有停下，而synchronized会让线程在没有获得锁的情况下，会发生上下文的切换，进入阻塞
- 线程好像高速跑道上的赛车，高速运行时，速度快，一旦发生上下文的切换，就好比赛车要减速，熄火，等待被唤醒又得重新打火、启动、加速...恢复到高速运行的状态，代价比较大
- 但无锁的情况下，因为线程要保持运行，需要额外CPU的支持，CPU在这里好比高速跑道，没有额外的跑道，线程想高速运行也无从谈起，虽然不会进入阻塞状态，但是由于没有分到时间片，仍然会进入可运行状态，还是会导致上下文的切换，所以最好是线程数少于核心数是最好的。

CAS特点

结合CAS和volatile可以实现无锁并发，适用于线程数少、多核CPU的场景下

- CAS是基于乐观锁的思想：最乐观的估计，不怕别的线程来修改共享变量，就算改了也没关系，我再进行重试吧
- synchronized是基于悲观锁的思想：最悲观的估计，得防着其他线程来修改共享变量，我上了锁你们都别想改，我改完了解开锁，你们才有机会
- CAS体现的是无锁并发、无阻塞的并发
  - 因为没有使用synchronized，所以线程不会陷入阻塞，这时效率提升的因素之一
  - 但是如果竞争非常激烈，可以想到重试必然发生的非常频繁，反而会影响效率

### 原子整数

JUC并发包提供了：

- AtomicBoolean
- AtomicInteger
- AtomicLong

以AtomicInteger为例：

```java
AtomicInteger i = new AtomicInteger(0);
//获取并自增(i = 0,结果i = 1,返回0)，类似于i++
sout(i.getAndIncrement());

//自增并获取(i = 1,结果i = 2,返回2)，类似于++i
sout(i.incrementAndGet());

//自减并获取(i = 2,结果i = 1,返回1)，类似于--i
sout(i.decrementAndGet());

//获取并自减(i = 1,结果i = 0,返回1)，类似于i--
sout(i.getAndDecrement());

//获取并加值(i = 0,结果i = 5,返回0)
sout(i.getAndAdd(5));

//加值并获取(i = 5,结果i = 0,返回0)
sout(i.addAndGet(-5));
```

### 原子引用

- AtomicReference
- AtomicMarkableReference
- AtomicStampedReference

为什么需要原子引用类型？

因为我们要保护的类型并不都是Java已经实现的基本类型

#### ABA问题

代码参照atomic.TestAtomicReference，线程无法感知到别的线程对变量做得修改，主线程仅能判断出共享变量的值与最初值A是否相等，不能感知到这种从A改为B又改回A的情况，如果主线程希望：

只要有其他线程[动过了]共享变量，那么自己的CAS就算是失败，这时，仅比较值是不够的，需要再加一个版本号！

##### AtomicStampedReference

可以对引用修改增加版本号

实现参照atomic.TestAtomicStampedReference

##### AtomicMarkableReference

有时候只关心这个引用有没有被其他线程修改过，并不关心版本号

参照实现atomic.TestAtomicMarkableReference

### 原子数组



- AtomicIntegerArray
- AtomicLongArray
- AtomicReferenceArray

```java
// 创建给定长度的新 AtomicLongArray
AtomicLongArray(int length)
// 创建与给定数组具有相同长度的新 AtomicLongArray，并从给定数组复制其所有元素
AtomicLongArray(long[] array)
// 以原子方式将给定值添加为索引 i 的元素
long addAndGet(int i, long delta)
// 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值
boolean compareAndSet(int i, long expect, long update)
// 以原子方式将索引 i 的元素减1
long decrementAndGet(int i)
// 获取位置 i 的当前值
long get(int i)
// 以原子方式将给定值与索引 i 的元素相加
long getAndAdd(int i, long delta)
// 以原子方式将索引 i 的元素减 1
long getAndDecrement(int i)
// 以原子方式将索引 i 的元素加 1
long getAndIncrement(int i)
// 以原子方式将位置 i 的元素设置为给定值，并返回旧值
long getAndSet(int i, long newValue)
// 以原子方式将索引 i 的元素加1
long incrementAndGet(int i)
// 最终将位置 i 的元素设置为给定值。
void lazySet(int i, long newValue)
// 返回该数组的长度
int length()
// 将位置 i 的元素设置为给定值
void set(int i, long newValue)
// 返回数组当前值的字符串表示形式
String toString()
// 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值
boolean weakCompareAndSet(int i, long expect, long update)

```

```java
public class LongArrayTest {
    
    public static void main(String[] args){

        // 新建AtomicLongArray对象
        long[] arrLong = new long[] {10, 20, 30, 40, 50};
        AtomicLongArray ala = new AtomicLongArray(arrLong);

        ala.set(0, 100);
        for (int i=0, len=ala.length(); i<len; i++) 
            System.out.printf("get(%d) : %s\n", i, ala.get(i));

        System.out.printf("%20s : %s\n", "getAndDecrement(0)", ala.getAndDecrement(0));
        System.out.printf("%20s : %s\n", "decrementAndGet(1)", ala.decrementAndGet(1));
        System.out.printf("%20s : %s\n", "getAndIncrement(2)", ala.getAndIncrement(2));
        System.out.printf("%20s : %s\n", "incrementAndGet(3)", ala.incrementAndGet(3));

        System.out.printf("%20s : %s\n", "addAndGet(100)", ala.addAndGet(0, 100));
        System.out.printf("%20s : %s\n", "getAndAdd(100)", ala.getAndAdd(1, 100));

        System.out.printf("%20s : %s\n", "compareAndSet()", ala.compareAndSet(2, 31, 1000));
        System.out.printf("%20s : %s\n", "get(2)", ala.get(2));
    }
}
```

```java
public class AtomicReferenceArrayTest {
        public static void main(String[] args) {
            Long[] l = new Long[4];
            String[] s = new String[4];
            int[] i = new int[4];
            Integer[] in = new Integer[4];
            AtomicReferenceArray atomicReferenceArray = new AtomicReferenceArray(l);
            System.out.println(atomicReferenceArray.length());
            System.out.println(atomicReferenceArray.get(2));

            AtomicReferenceArray atomic = new AtomicReferenceArray(4);
            atomic.set(0,432141);
            atomic.set(2,"fsafefeq");
            atomic.set(3,i);
            System.out.println(atomic.toString());
        }
    }
/*
	输出结果：
	exclude patterns:
	4
	null
	[432141, null, fsafefeq, [I@357b2b99]

	Process finished with exit code 0
*/
```



#### 不安全数组

#### 安全的数组

### 字段更新器

### 原子累加器

#### 原子累加器性能比较

#### LongAdder原理分析

#### 伪共享问题
