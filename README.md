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

修改，将run变量编程volatile，会去主内存中获得最新的值

