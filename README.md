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

可以参照atomic.TestAtomicArray，实现了非安全数组和安全数组

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

### 字段更新器

- AtomicReferenceFieldUpdater 	// 域 字段
- AtomicIntegerFieldUpdater
- AtomicLongFieldUpdater

利用字段更新器，可以针对对象的某个域(Field)进行原子操作，只能配合volatile修饰的字段使用，否则会出现异常

```java
Exception in thread "main" java.lang.IllegalArgumentException: Must be volatile type
```

代码参照atomic.TestAtomicField

### 原子累加器

- LongAdder
- LongAccumulator
- DoubleAdder
- DoubleAccumulator

性能对比，代码参照atomic.TestAtomicAdder

#### 原子累加器性能比较

那么LongAdder为什么会比AtomicLong性能高这么多呢?其实提升性能很简单,就是在有竞争时,设置多个累加单元,Thread-0累加Cell[0]，而Thread-1累加Cell[1]...最后将结果汇总。这样他们在累加的时候操作的不同Cell变量，因此减少了CAS重试失败，从而提高性能。

#### LongAdder原理分析

LongAdder是并发大师@Author Doug Lea (J.U.C作者)的作品，设计的非常精巧

LongAdder类有几个关键域

```java
//累加单元数组，懒惰初始化
transient volatile Cell[] cells;
//基础知识，如果没有竞争，则用CAS累加这个域
transient volatile long base;
//在cells创建或扩容时，置为1，表示加锁
transient volatile int cellsBusy;
```

结合atomic.TestAtomicCas

```java
//防止缓存行伪共享
@sun.misc.Contended static final class Cell{
  volatile long value;
  Cell(long x){value = x;}
  //最重要的方法，用来CAS方式进行累加，prev表示旧值，next表示新值
  final boolean cas(long cmp, long val) {
    return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
  }
}
```

什么是缓存行和伪共享

缓存与内存的速度比较

| CPU1 Core    |              | CPU2 Core    |              |
| :----------- | :----------: | :----------- | -----------: |
| 一级指令缓存 | 一级指令缓存 | 一级指令缓存 | 一级指令缓存 |
| 二级         |     缓存     | 二级         |         缓存 |
| 三           |      级      | 缓           |           存 |
| 内           |              |              |           存 |

速度比较

| 从CPU到          | 大约需要的时钟周期           |
| ---------------- | ---------------------------- |
| 寄存器           | 1 cycle(4GHz的CPU约为0.25ns) |
| L1(一级指令缓存) | 3~4 cycle                    |
| L2(二级指令缓存) | 10~20 cycle                  |
| L3(三级指令缓存) | 40~45 cycle                  |
| 内存             | 120~240 cycle                |

因为CPU与内存的速度差异很大，需要靠预读数据至缓存来提升效率。

而缓存以缓存行为单位，每个缓存行对应着一块内存，一般是64byte(8个long)。

缓存的加入会造成数据副本的产生，即同一份数据会缓存在不同核心的缓存行中

好比，内存中有个商品的价格，CPU1里面需要使用到商品，那么CPU1里面就有了一个商品价格的缓存，同样CPU2需要使用到商品，那么CPU2里面就有了一个商品价格的缓存，那这样的话，两个CPU核心都维护了一个相同商品的价格。

那么CPU1改了这个价格，CPU要保证数据的一致性，如果某个CPU核心更改了数据，其他CPU核心对应的**整个缓存行**必须失效。

因为Cell是数组形式，在内存中是连续存储的，一个Cell为24个字节（16个字节的对象头，Cell里面的value字段为long型的8个字节的value），因此缓存行可以存在2个Cell的对象，这样问题就来了：

- Core-0要修改Cell[0]
- Core-1要修改Cell[1]

**那么无论谁修改成功，都会导致另一个CPU Core的缓存行失效**，比如Core-0中Cell[0]=6000，Cell[1]=8000，要累加Cell[0]=6001，Cell[1]=8000，这时会让CPU Core-1的缓存行失效

解决的方法就是让Cell处于不同的缓存行

@sun.misc.Contended用来解决这个问题，他的原理是在使用此注解的对象或字段的**前后各增加128字节大小的padding**，从而**让CPU将对象预读至缓存时占用不同的缓存行**，这样不会造成对方的缓存行失效！

最后，什么是防止缓存行伪共享就是这个@sun.misc.Contended的作用

防止多个Cell，在同一个缓存行，导致修改失效

### Unsafe

概述：Unsafe对象提供了非常底层的，操作内存、线程的方法，Unsafe对象不能直接调用，只能通过反射获得

```java
public class UnsafeAccessor{
  static Unsafe unsafe;
  static{
    try{
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
    }catch(NoSuchFieldException | IllegalAccessException e){
      throw new RuntimeException(e);
    }
  }
  static Unsafe getUnsafe(){
    return unsafe;
  }
}
```

#### Unsafe的CAS操作

参照atomic.TestUnsafeAccessor

#### Unsafe AtomicInteger

自己使用Unsafe类实现了一个AtomicInteger类，参照atomic.TestMyAtomicInteger

### 小结

- CAS与Volatile
- API
  - 原子整数
  - 原子引用
  - 原子数组
  - 字段更新器
  - 原子累加器
- Unsafe
- 原理方面
  - LongAdder
  - 伪共享@sun.misc.Contended

## 不可变

- 不可变类的使用
- 不可变类的设计
- 无状态类的设计

### 日期转换的问题

下面代码运行时，由于SimpleDateFormat不是线程安全的

```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
for(int i = 0;i < 10; i++){
  new Thread(()->{
    try{
      sout(sdf.parse("1958-06-08"));
    } catch(Exception e){
      sout(e)
    }
  }).start();
}
```

运行结果参照immutable.TestSdf，改进代码也在其中

### 不可变类的设计

以String类为例

```java
public final class String implements java.io.Serializable,Compareable<String>,CharSequence{
  /**The value is used for character storage.*/
  private final char value[];
  
  /**Cache the hash code for the string*/
  private int hash;//Default to 0
  //...
}
```

final的使用

发现该类、类中所有属性都是final的

- 属性用final修饰保证了该属性是只读的，不能修改
- 类用final修饰保证了该类中的方法不能被覆盖，防止子类无意间破坏不可变性

构造新字符串对象，会生成新的char[] value，对内容进行复制。这种通过创建副本对象来避免共享的手段称之为[保护性拷贝(defensive copy)]

### 享元模式

定义：英文名称Flyweight patern.当需要重用数量有限的同一类对象时进行一个共享

**体现**

包装类，在JDK中Boolean、Byte、Short、Integer、Character等包装类提供了valueOf方法，例如Long的valueOf会缓存-128~127之间的Long对象，在这个范围内会重用对象，大于这个范围，参会新建Long对象

**注意**

- Byte、Short、Long缓存的范围都是-128~127
- Character缓存范围是0~127
- Integer默认范围是-128~127，最小值不能变，但是最大值可以通过调整虚拟机参数-Djava.lang.Integer.IntegerCache.high来调整
- Boolean缓存了true和false

String串池、BigDecimal和BigInteger都是享元模式，线程安全

**问题**

例如：一个线上商城应用，QPS达到数千，如果每次都重新创建和关闭数据库连接，性能会受到极大影响。这时预先创建好一批连接，放入连接池。一次请求到达后，从连接池中获取连接，使用完毕后再还回连接池，这样既节约了连接的创建和关闭时间，也实现了连接的重用，不至于让庞大的连接数压垮数据库。

参照immutable.TestConnectionPool

自己的实现中没有考虑到的问题：

- 连接的动态增长与收缩
- 连接保活（可用性监测）
- 等待超时处理
- 分布式hash

对于关系型数据库，有比较成熟的连接池实现，例如C3P0，Druid等

对于更通用的对象池，可以考虑使用Apache Commons Pool，例如redis连接池可以考虑Jedis中关于连接池的实现

### final原理

理解了volatile原理，再对比final的实现就比较简单了

```java
public class TestFinal{
  final int a = 20;
}
```

字节码

```java
0: aload_0;
1: invokespecial 	#1;
4: aload_0;
5: bipush 	  		20;
7: putfield 			#2;
// 写屏障 ↑
10: return
```

发现final变量的赋值也会通过putfield指令来完成，同样在这条指令之后也会加入写屏障，保证在其他线程读到他的值时不会出现为0的情况

### 无状态

在web应用中，设计Servlet时为了保证其线程安全，都会有这样的建议，不要为Servlet设置成员变量，这种没有任何成员变量的类是线程安全的

因为成员变量保存的数据可以成为状态信息，因此没有成员边阿玲就称之为[无状态]

### 小结

不可变类的使用

不可变类的设计

还有Final和享元

## 并发编程相关工具

### 自定义线程池

线程数并不是越多越好，多了，容易引发线程争抢，造成上下文的频繁切换，造成性能损耗

### 线程池的构成

- ThreadPool，线程池
- BlockingQueue，在生产者消费者模式下，平衡生产与消费速率的工具

步骤1：自定义拒绝策略接口

```java
@FunctionalInterface //拒绝策略
interface RejectPolicy<T>{
  void Reject(BlockingQueue<T> queue,T task);
}
```

自己实现了ThreadPool，代码见juc.TestThreadPool

### ThreadPoolExecutor

#### 线程池状态

ThreadPoolExecutor使用int的高3位来表示线程池状态，低29位表示线程数量

| 状态名     | 高3位 | 接收新任务 | 处理阻塞队列任务 | 说明                                     |
| ---------- | ----- | ---------- | ---------------- | ---------------------------------------- |
| RUNNING    | 111   | Y          | Y                |                                          |
| SHUTDOWN   | 000   | N          | Y                | 不会接收新任务，但会处理阻塞队列剩余任务 |
| STOP       | 001   | N          | N                | 会中断正在执行的任务，并抛弃阻塞队列任务 |
| TIDYING    | 010   | -          | -                | 任务全执行完毕，活动线程为0即将进入终结  |
| TERMINATED | 011   | -          | -                | 终结状态                                 |

从数字上比较，TERMINATED>TIDYING>STOP>SHUTDOWN>RUNNING

这些信息存储在一个原子变量ctl中，目的是将线程池状态与线程个数合二为一，这样就可以用一次CAS原子操作进行赋值

```java
//c为旧值，ctlOf返回结果为新值
ctl.compareAndSet(c,ctlOf(targetState,workerCountOf(c)));
//rs为高3位代表线程池状态，wc为低29位代表线程个数，ctl是合并他们
private static int ctlOf(int rs,int wc){return rs|wc;}
```

#### 构造方法

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximunPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workerQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutorHandler handler)
```

- corePoolSize核心线程数目（最多保留的线程数）
- maximunPoolSize最大线程数目
- keepAliveTime生存时间-针对救急线程
- unit时间单位-针对救急线程
- workQueue阻塞队列
- threadFactory线程工厂-可以为线程创建时起个好名字，区分线程池线程和其他
- handler拒绝策略

#### 工作方式

线程池中存在**核心**和**救急**两种线程，核心+救急=最大线程数，假设线程池中设置了两个核心线程，最大线程数为3，那么当五个任务来临，core thread - 1去执行了task1，core thread - 2去执行了task2，剩下的task3，task4放在了阻塞队列等待执行，阻塞队列的size=2，那么线程池会检查有无救急线程，如果可以则创建一个救急线程对task5进行处理，救急线程与核心线程最大的区别就是有生存时间，如果task5执行完毕，那么救急线程就被干掉了，当线程池中核心和救急线程都在忙着呢，阻塞队列也已经满了，那么再来task6、task7等等，就会去执行拒绝策略。

- 线程池中刚开始没有线程，当一个任务提交给线程池后，线程池会创建一个新线程来执行任务
- 当线程数打到corePoolSize并且没有空闲线程，这时再及嵌入任务，新家的任务就会被加入workQueue队列排队，直到有空闲的线程
- 如果队列选择了有界队列，那么任务超过了队列大小时，会创建maximunPoolSize - corePoolSize数目的线程来救急
- 如果线程打到了maximunPoolSize仍然有新任务这时会执行拒绝策略，拒绝策略JDK提供了4中实现，其他著名框架也提供了实现
  - AbortPolicy让调用者抛出RejectedExecutionException异常，这时默认策略
  - CallerRunsPolicy让调用者运行任务
  - DiscardPolicy放弃本次任务
  - DiscardOldestPolicy放弃队列中最早的任务，本任务取而代之
  - Dubbo的实现，在抛出RejectedExecutionException异常之前会记录日志，并dump线程栈信息，方便定位问题
  - Netty实现，是创建一个新县城来执行任务
  - ActiveMQ的实现，带超时等待（60s）尝试放入队列，类似上面自定义线程池中的拒绝策略
  - PinPoint的实现，他使用了一个拒绝策略链，会逐一尝试策略链中每种拒绝策略
- 当高峰过去以后，超过corePoolSize的救急线程如果有一段时间没有任务做，需要结束以节省资源，这个时间由keepAliveTime和unit来控制

JDK中四种拒绝策略，都实现了RejectedExecutionHandler，分别是DiscardPolicy、DiscardOldestPolicy、CallerRunsPolicy和AbortPolicy

根据这个构造方法，JDK Executors类中提供了众多工厂方法来创建各种用途的线程池

#### 工厂方法

##### newFixedThreadPool

```java
public static ExecutorService new FixedThreadPool(int nThreads){
  return new ThreadPoolExecutor(nThreads,
                                nThreads,
                                0L,
                                TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>())
}
```

特点

- 核心线程数 == 最大线程数（没有救急线程被创建），因此也无需超时事件

- 阻塞队列是无界的，可以放任意数量的任务

  **适用于任务量已知，相对耗时的任务**，代码参照juc.TestFixedThreadPool

##### newCachedThreadPool

```java
public static ExecutorService newCachedThreadPool(){
  return new ThreadPoolExecutor(0,
                                Integer.MAX_VALUE,
                                60L,
                                TimeUnit.SECONDS,
                                new SynchronousQueue<Runnable>());
}
```

特点

- 核心线程数是0，最大线程数是Integer.MAX_VALUE，救急线程的空闲生存时间是60s，意味着

  - 全部都是救急线程（60s后无任务可以回收）
  - 救急线程可以无限创建

- 队列采用了SynchronousQueue实现特点是，他没有容量，没有线程来取是放不进去的（一手交钱，一手交货）

  ```java
  SynchronousQueue<Integer> integers = new SynchronousQueue<>();
          new Thread(()->{
              try{
                  System.out.println("puting..."+1);
                  integers.put(1);
                  System.out.println("putted..."+1);
  
                  System.out.println("putting..."+2);
                  integers.put(2);
                  System.out.println("putted..."+2);
              }catch(InterruptedException e){
                  e.printStackTrace();
              }
          },"t1").start();
  
          try {
              TimeUnit.SECONDS.sleep(1);
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
  
          new Thread(()->{
              try{
                  System.out.println("taking...."+1);
                  integers.take();
              }catch(InterruptedException e){
                  e.printStackTrace();
              }
          },"t2").start();
  
          try {
              TimeUnit.SECONDS.sleep(1);
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
  
          new Thread(()->{
              try{
                  System.out.println("taking" + 2);
                  integers.take();
              }catch(InterruptedException e){
                  e.printStackTrace();
              }
          },"t3").start();
  ```

  参考代码juc.TestCahceThreadPool

  整个线程池表现为线程数会根据任务量不断增长，没有上线，当任务执行完毕，空闲1分钟后释放线程。适合任务数比较密集，但每个任务执行时间较短的情况

##### newSingleThreadExecutor

```java
public static ExecutorService newSingleThreadExecutor(){
  return new FinalizableDelegatedExecutorService(
    new ThreadPoolExecutor(1,1,0L,
                           TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>()));
}
```

使用场景

希望多个任务排队执行。线程数固定为1，任务数多于1时，会放入无界队列排队。任务执行完毕，这唯一的线程也不会被释放。

区别

- 自己创建一个单线程串行执行任务，如果任务执行失败而终止那么没有任何补救措施，而线程池还会新建一个线程，保证池的正常工作
- Executors.newSingleThreadExecutor()线程个数始终为1，不能修改
  - FinalizableDelegateExecutorService应用的是装饰器模式，只对外暴露了ExecutorService接口，因此不能调用ThreadPoolExecutor中特有的方法
- Executors.newFixedThreadPool(1)初始时为1，以后还可以修改
  - 对外暴露的是ThreadPoolExecutor对象，可以强转后调用setCorePoolSize等方法进行修改

参照juc.TestSingleThreadPool

##### 提交任务

```java
//执行任务
void execute(Runnable command);

//提交任务task中所有任务
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws IntegerruptedException;

//提交tasks中所有任务，带超时时间
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,long timeout,TimeUnit unit) throws InterruptedException;

//提交tasks中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其他任务取消
<T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,ExecutionException;

//提交tasks中所有任务，哪个任务先车工执行完毕，返回此任务执行结果，其他任务取消，带超时时间
<T> T invokeAny(Collection<? extends Callable<T>> tasks,long timeout,TimeUnit unit) throws InterruptedException,ExecutionException,TimeoutException;
```

##### 关闭线程池

###### shutdown

```java
/**
	线程池状态变为SHUTDOWN
	- 不会接收新的任务
	- 但已提交的任务会执行完
	- -此方法不会阻塞调用线程的执行
*/
void shutdown();
```

```java
public void shutdown(){
  final ReentrantLock mainLock = this.mainLock;
  mainlock.lock();
  try{
    checkShutdownAccess();
    //修改线程池状态
    advanceRunState(SHUTDOWN);
    //仅会打断空闲线程
    interruptIdleWorkers();
    onShutdown();//扩展点 ScheduledThreadPoolExecutor
  } finally{
    mainLock.unlock();
  }
  //尝试终结（没有运行的线程可以立刻终结，如果还有裕兴的线程也不会等待）
  tryTerminate();
}
```

###### shutdownNow

```java
/**
	线程池状态变为STOP
	- 不会接收新任务
	- 会将队列中的任务返回
	- 并用interrupt的方式中断正在执行的任务
*/
List<Runnable> shutdownNow();
```

```java
public List<Runnable> shutdownNow(){
  List<Runnable> tasks;
  final ReentrantLock mainLock = this.mainLock;
  mainLock.lock();
  try{
    checkShutdownAccess();
    //修改线程池状态
    advanceRunState(STOP);
    //打断所有线程
    interruptWorkers();
    //获取队列中剩余任务
    tasks = drainQueue();
  } finally{
    mainLock.unlock();
  }
  //尝试终结
  tryTerminate();
}
```

###### 其他方法

```java
//不再RUNNING状态的线程池，此方法就是返回true
boolean isShutdown();

//线程池状态是否是TERMINATED
boolean isTerminated();

//调用shutdown后，由于调用线程并不会等待所有任务运行结束，因此如果它想在线程池TERMINATED后做一些事情，可以利用此方法等待
boolean awaitTermination(long timeout,TimeUnit unit) throws InterruptedException;
```

### 异步模式之工作线程

让有限的工作线程（Worker Thread）来轮流异步处理无限多的任务。也可以将其归类为分工模式，它的典型实现就是线程池，也体现了经典设计模式中的享元模式。

例如，海底捞的服务员（线程），轮流处理每位客人的点餐（任务），如果为每位客人都配一名转述的服务员，那么成本就太高了（对比另一种多线程设计模式：Thread-Per-Message）

注意，不同任务类型应该使用不同的线程池，这样能够避免饥饿，并能提升效率

例如，如果一个餐馆的工人既要中招呼客人（任务类型A），又要到后厨做菜（任务类型B）显然效率不咋地，分成服务员（线程池A）与厨师（线程池B）更为合理，当然你能想到更细致的分工

#### 饥饿

固定大小线程池会有饥饿先向

- 两个工人是同一个线程池中的两个线程
- 他们要做的事情是：为客人点餐和到后厨做菜，这时两个阶段的工作
  - 客人点餐：必须先点完餐，等菜做好，上菜，在此期间处理点餐的工人必须等待
  - 后厨做菜，没啥好说的，做就是了
- 比如工人A处理了点餐任务，接下来它要等着工人B把菜做好，然后上菜，他俩也配合的蛮好的
- 但现在同时来了两个客人，这个时候工人A和工人B都去处理点餐了，这时就没有人做饭了，直接死等
- 参照代码juc.TestStarvation

### 创建多少线程比较合适

- 过小导致程序不能充分利用系统资源、容易导致饥饿
- 过大导致更多的线程上下文切换，占用更多内存

#### CPU密集型计算

通常采用CPU核数+1能够实现最优的CPU利用率，+1是保证线程由于页缺失故障（操作系统）或其他原因导致暂停时，额外的这个线程就能顶上去，保证CPU时钟周期不被浪费

#### IO密集型计算

CPU不总是处于繁忙状态，例如，当你执行业务计算式，这时候会使用CPU资源，但当你执行IO操作时、远程RPC调用时，包括进行数据库操作时，这时候CPU就闲下来了，你可以利用多线程提高它的利用率。

经验公式如下

线程数=核数 x 期望CPU利用率 x 总时间(CPU计算时间+等待时间) / CPU计算时间

例如4核CPU计算时间是50%，其他等待时间是50%，期望CPU被100%利用，套用公式

4 x 100%（利用率） x 100%（总时间） / 50% (CPU计算时间) = 8

例如4核CPU计算时间是10%，其他等待时间是90%，期望CPU被100%利用，套用公式

4 x 100%（利用率） x 100%（总时间） / 10% (CPU计算时间) = 40

#### 任务调度线程池

在[任务调度线程池]功能加入之前，可以使用java.util.Timer来实现定时功能，Timer的优点在于简单易用，但由于所有任务都是由同一个线程来调度，因此所有任务都是串行执行的，同一时间只能有一个任务在执行，前一个任务的延迟或者异常都将会影响到之后的任务。

```java
public static void main(String[] args){
  Timer timer = new Timer();
  TimerTask task1 = new TimerTask() {
    @Override
    public void run() {
      System.out.println("task 1");
    }
  };

  TimerTask task2 = new TimerTask() {
    @Override
    public void run() {
      System.out.println("task 2");
    }
  };

  //使用timer添加两个任务，希望他们都在1s后执行
  //但是由于timer内只有一个线程来顺序执行队列中的任务，因此[任务1]的延迟，影响了[任务二]的执行
  timer.schedule(task1, 1000);
  timer.schedule(task2, 1000);
}
```

参照代码juc.TestThreadSchedule

##### 创建一个固定时间执行的线程

比如让线程每周四18:00:00 定时执行任务

参考代码juc.TestThreadFixedSchedule

### Tomcat线程池

Tomcat在哪里用到了线程池？

#### Connector

======================connector(NIO EndPoint)========================

LimitLatch -----> Acceptor ------> SocketChannel1,2,3... ----有读----> Poller --->  socketProcessor ----> Executor[worker1,worker2....]



- LimitLatch用来限流，可以控制最大连接个数，类似于J.U.C中的Semaphore
- Acceptor只负责[接收新的socket连接]
- Poller只负责监听socket channel是否有[可读I/O事件]
- 一旦可读，封装一个任务对象（socketProcessor），提交给Executor线程池处理
- Executor线程池中的工作线程最终负责[处理请求]

Tomcat线程池扩展了ThreadPoolExecutor，行为稍有不同

- 如果总线程数打到maximunPoolSize
  - 这时不会立刻抛出RejectedExecutionException异常
  - 而是再次尝试将任务放进队列，如果还是失败，才抛出RejectedExecutionException异常

#### Connector配置

| 配置项              | 默认值 | 说明                                 |
| ------------------- | ------ | ------------------------------------ |
| acceptorThreadCount | 1      | acceptor线程数量                     |
| pollerThreadCount   | 1      | poller线程数量                       |
| minSpareThreads     | 10     | 核心线程数，即corePoolSize           |
| maxThreads          | 200    | 最大线程数，即maximumPoolSize        |
| executor            | -      | Executor名称，用来引用下面的Executor |

#### Executor线程配置

| 配置项                  | 默认值            | 说明                                    |
| ----------------------- | ----------------- | --------------------------------------- |
| threadPriority          | 5                 | 线程优先级                              |
| daemon                  | true              | 是否守护线程                            |
| minSpareThreads         | 25                | 核心线程数，即corePoolSize              |
| maxThreads              | 200               | 最大线程数，即maximunPoolSize           |
| maxIdleTime             | 60000             | 线程生存时间，单位是毫秒，默认值即1分钟 |
| maxQueueSize            | Integer.MAX_VALUE | 队列长度                                |
| prestartminSpareThreads | false             | 核心线程是否在服务器启动时启动          |

#### 线程池工作流程

添加新任务 ，那么先判断条件1：提交任务小于核心线程数，条件1成立则加入队列；条件1不成立，则判断条件2：提交任务小于最大线程数，条件2成立则创建救急线程；条件2不成立则加入队列

### Fork/Join	

#### 概念

Fork/Join是JDK1.7加入的新的线程池实现，它体现的是一种分治思想，适用于能够进行任务拆分的CPU密集型运算

所谓的任务拆分，是将一个大型任务拆分为算法上相同的小任务，直至不能拆分可以直接求解。跟递归相关的一些计算，如归并排序、斐波那契数列都可以用分治思想进行求解

Fork/Join在分治的基础上加入了多线程，可以把每个任务的分解和合并交给不同的线程来完成，进一步提升了运算效率

Fork/Join默认会创建与CPU核心数带下相同的线程池

#### 使用

提交给Fork/Join线程池的任务需要继承RecursiveTask（有返回值）或RecursiveAction（没有返回值），例如下面定义了一个对1~n之间的整数求和的任务

代码参照juc.TestForkJoin

**最重要的就是任务分解**

### JUC工具包

#### AQS

全称是AbstractQueuedSynchronizer，是阻塞式锁和相关的同步器工具的框架

特点：

- 用state属性来表示资源的状态（分独占模式和共享模式），子类需要定义如何维护这个状态，控制如何或缺锁和释放锁
  - getState - 获取state状态
  - setState - 设置state状态
  - compareAndSetState - cas机制设置state状态
  - 独占模式表示只有一个线程能够访问资源，而共享模式可以允许多个线程访问资源
- 提供了基于FIFO的等待队列，类似于Monitor的EntryList（公平模式）
- 条件变量来实现等待、唤醒机制，支持多个条件变量，类似于Monitor的WaitSet

子类主要实现这样一些方法（默认抛出UnsupportedOperationException）

- tryAcquire
- tryRelease
- tryAcquireShared
- tryRelesaeShared
- isHeldExclusively

获取锁的方式

```java
//如果获取锁失败
if(!tryAcquire(arg)){
  //入队，可以选择阻塞当前线程 park & unpark
}
```

释放锁的方式

```java
//如果释放锁成功
if(tryRelease(arg)){
  //让阻塞线程恢复运行
}
```

参照代码juc.TestAqs，其中MyLock的实现和ReentrantLock相似

#### ReentrantLock

##### 加锁解锁流程

先从构造器开始，默认为非公平模式的实现

````java
public ReentrantLock(){
  sync = new NonfairSync();
}
````

NonfairSync继承自AQS

###### 没有竞争的情况下

1. state=1
2. head （Node队列头）
3. tail （Node队列尾）
4. exclusiveOwnerThread ---> Thread - 0

```java
final void lock() {
  if (compareAndSetState(0, 1))
    setExclusiveOwnerThread(Thread.currentThread());
  else
    acquire(1);
}
```

###### 第一个竞争出现的情况下

首先再使用tryAcquire(arg)尝试加锁，但是如果有了竞争了，那么肯定执行失败

```java
public final void acquire(int arg) {
  if (!tryAcquire(arg) &&
      acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
    selfInterrupt();
}
```

Thread - 1执行了

1. CAS尝试将state由0改为1，结果失败

2. 进入tryAcquire逻辑，这时state已经是1，结果仍然失败

3. 接下来进入addWaiter逻辑，构造Node队列

   - Node为waitStatus状态

   - Node的创建是懒惰的

   - 队列中第一个Node成为Dummy（哑元）或哨兵用来占位的，并不关联线程

     head ---> Node(dummy) --->Node(Thread - 1) ---> tail

4. 当前线程就进入了acquireQueued逻辑

   1. acquireQueued会在一个死循环中不断尝试获得锁，失败后进入park阻塞
   2. 如果自己紧邻着head(排在第二位)，那么会再次使用tryAcquire尝试获取锁，当然这时state仍为1，失败了
   3. 进入shouldParkAfterFailedAcquire逻辑，将前驱Node，即head的waitStatus改为-1（刚开始Node的Status都为0，Status为-1则表示这个Node有责任唤醒后继节点），这次返回false
   4. shouldParkAfterFailedAcquire执行完毕回到acquireQueued，再次tryAcquire尝试获取锁，当然这时state为1，还是失败
   5. 当再次进入shouldParkAfterFailedAcquire时，这时因为其前驱node的waitStatus已经是-1，这次返回true
   6. 进入parkAndCheckInterrupt，Thread - 1park...

那么再有多个线程经历上述过程，竞争失败那么就变成了

Head ----> Node(Dummy，status=-1) <----> Node(Thread - 1，status=-1) <----> Node(Thread - 2，status=-1)  <-----> Node(Thread - 3，status=0) <----> tail

Thread-0释放锁，进入tryRelease流程，如果成功

- 设置exclusiveOwnerThread为null
- state = 0

###### 恢复过程

- 当前队列不为null，并且head的waitStatus=-1，进入unparkSuccessor流程

- 找到队列中离head最近的一个Node（没取消的），unpark恢复其运行，本例子中即为Thread -1

- 回到Thread - 1的acquireQueued流程

如果加锁成功（没有竞争），会设置

- exclusiveOwnerThread为Thread-1，state=1
- head指向刚刚Thread-1所在的Node，该Node清空Thread
- 原本的head因为从链表断开，而可被垃圾回收

如果这时候有其他线程来竞争（非公平的体现），例如这时候来了个Thread-4

如果不巧又被Thread-4抢先了

- Thread-4被设置为exclusiveOwnerThread，state=1
- Thread-1再次进入accuireQueued流程，获取锁失败，重新进入park阻塞

#### 读写锁

##### 注意

- 读锁不支持条件变量

- 重入时升级不支持：即持有读锁的情况下去获取写锁，会导致获取写锁永久等待

#### StampedLock

该类自JDK8加入，是为了进一步优化读性能，他的特点是在使用读锁、写锁时都必须配合[戳]使用

加解读锁

```java
long stamp = lock.readLock();
lock.unlockRead(stamp);
```

加解写锁

```java
long stamp = lock.writeLock();
lock.unlockWrite(stamp);
```

乐观读，StampedLock支持tryOptimisticRead()方法（乐观读），读取完毕后需要做一次**戳校验**如果校验通过，表示这期间确实没有写操作，数据可以安全使用，如果校验没通过，需要重新获取读锁，保证数据安全。

```java
long stamp = lock.tryOptimisticRead();
//验戳
if(!lock.validate(stamp)){
  //锁升级
}
```

提供一个数据容器类内部分别使用读锁保护数据的read()方法，写锁保护数据的write()方法
