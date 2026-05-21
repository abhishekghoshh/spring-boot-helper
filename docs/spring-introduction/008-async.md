

# Threads and Multithreading in Java

---

## 1. What is a Thread and Multithreading in Java?

### What is a Thread?

A **thread** is the smallest unit of execution within a process. Every Java program has at least one thread — the **main thread** — which is created by the JVM when the program starts.

A thread has its own:

- **Program counter** — tracks which instruction is currently being executed
- **Stack** — stores local variables and method call information
- **Register set** — holds temporary data during execution

However, all threads within the same process **share**:

- **Heap memory** — objects and instance variables
- **Method area / Code segment** — compiled bytecode
- **File descriptors and I/O resources**

```
┌──────────────────────────────────────────────────────────┐
│                        PROCESS                           │
│                                                          │
│   ┌──────────────────────────────────────────────────┐   │
│   │              Shared Resources                    │   │
│   │     Heap Memory  |  Code Segment  |  Files       │   │
│   └──────────────────────────────────────────────────┘   │
│                                                          │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐            │
│   │ Thread 1 │   │ Thread 2 │   │ Thread 3 │            │
│   │──────────│   │──────────│   │──────────│            │
│   │ Stack    │   │ Stack    │   │ Stack    │            │
│   │ PC       │   │ PC       │   │ PC       │            │
│   │ Registers│   │ Registers│   │ Registers│            │
│   └──────────┘   └──────────┘   └──────────┘            │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### What is Multithreading?

**Multithreading** is a programming concept where multiple threads execute concurrently within a single process. Each thread runs independently but shares the same memory space.

```
Single-threaded execution:
  Task A ──────────────────► Task B ──────────────────► Task C
  (Total time = A + B + C)

Multithreaded execution:
  Thread-1: Task A ──────────────────►
  Thread-2: Task B ──────────────────►
  Thread-3: Task C ──────────────────►
  (Total time ≈ max(A, B, C))
```

### Process vs Thread

| Feature             | Process                                | Thread                                  |
|---------------------|----------------------------------------|-----------------------------------------|
| Memory              | Each process has its own memory space  | Threads share memory within a process   |
| Creation overhead   | Heavy — requires OS-level allocation   | Lightweight — shares parent process     |
| Communication       | IPC (pipes, sockets, shared memory)    | Direct access to shared variables       |
| Isolation           | Fully isolated                         | Not isolated — one thread crash can affect others |
| Context switching   | Expensive                              | Relatively cheaper                      |

### Concurrency vs Parallelism

```
Concurrency (single core — time slicing):
  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐
  │ A │ │ B │ │ A │ │ C │ │ B │ │ A │  ──► Time
  └───┘ └───┘ └───┘ └───┘ └───┘ └───┘

Parallelism (multiple cores — true simultaneous execution):
  Core 1: ┌───────────── A ─────────────┐
  Core 2: ┌───────────── B ─────────────┐
  Core 3: ┌───────────── C ─────────────┐
```

- **Concurrency**: Multiple tasks make progress by time-sharing a single CPU core. They are interleaved but not truly simultaneous.
- **Parallelism**: Multiple tasks truly execute at the same time on multiple CPU cores.

Java's multithreading supports **both** — the JVM and the OS scheduler decide whether threads run concurrently or in parallel depending on the available cores.

---

## 2. Advantages and Disadvantages of Multithreading

### Advantages

| Advantage                          | Description                                                                                                       |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| **Better CPU utilization**         | While one thread waits for I/O, another thread can use the CPU. No core sits idle.                                |
| **Improved responsiveness**        | In GUI applications, long tasks run on background threads, keeping the UI responsive.                             |
| **Resource sharing**               | Threads share the heap and code segment, making communication cheaper than inter-process communication.           |
| **Faster context switching**       | Thread context switches are cheaper than process context switches since threads share memory.                      |
| **Scalability**                    | Multithreaded applications can scale across multiple cores, achieving true parallelism.                           |
| **Simplified modeling**            | Some problems (producers-consumers, servers handling clients) are naturally modeled with multiple threads.         |
| **Reduced latency**               | Tasks like network calls, file I/O, and database queries can overlap, reducing total response time.               |

### Disadvantages

| Disadvantage                       | Description                                                                                                       |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| **Complexity**                     | Writing, debugging, and reasoning about multithreaded code is significantly harder than single-threaded code.     |
| **Race conditions**                | When multiple threads read/write shared data without synchronization, results become unpredictable.               |
| **Deadlocks**                      | Two or more threads can block forever, each waiting for a lock the other holds.                                   |
| **Starvation**                     | A low-priority thread may never get CPU time if high-priority threads keep running.                               |
| **Livelock**                       | Threads keep responding to each other but make no actual progress.                                                |
| **Memory overhead**                | Each thread requires its own stack memory (default ~512KB–1MB). Thousands of threads consume significant RAM.     |
| **Difficult debugging**            | Bugs may be non-deterministic — they appear only under specific timing conditions and are hard to reproduce.      |
| **Thread safety overhead**         | Synchronization primitives (`synchronized`, locks) add performance overhead and can reduce parallelism.           |

### Race Condition Example

```java
public class RaceConditionDemo {
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                counter++; // NOT atomic — read, increment, write
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Expected: 200000, Actual: unpredictable (e.g., 134829)
        System.out.println("Counter: " + counter);
    }
}
```

### Deadlock Example

```java
public class DeadlockDemo {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("Thread-1: Holding lockA, waiting for lockB...");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                synchronized (lockB) {
                    System.out.println("Thread-1: Acquired both locks");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("Thread-2: Holding lockB, waiting for lockA...");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                synchronized (lockA) {
                    System.out.println("Thread-2: Acquired both locks");
                }
            }
        });

        t1.start();
        t2.start();
        // Both threads block forever — DEADLOCK
    }
}
```

---

## 3. How to Create Threads in Java

Java provides **multiple ways** to create and run threads:

### Way 1: Extending the `Thread` class

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(Thread.currentThread().getName() + " - Count: " + i);
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
    }

    public static void main(String[] args) {
        MyThread t1 = new MyThread();
        MyThread t2 = new MyThread();
        t1.setName("Worker-1");
        t2.setName("Worker-2");
        t1.start(); // start() creates a new OS thread and calls run()
        t2.start();
        // NEVER call t1.run() directly — that runs on the main thread!
    }
}
```

> **Note**: Since Java doesn't support multiple inheritance, extending `Thread` prevents extending any other class.

### Way 2: Implementing the `Runnable` interface

```java
public class MyRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(Thread.currentThread().getName() + " - Count: " + i);
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(new MyRunnable(), "Worker-1");
        Thread t2 = new Thread(new MyRunnable(), "Worker-2");
        t1.start();
        t2.start();
    }
}
```

> **Preferred over extending Thread** because it allows you to extend another class and promotes composition over inheritance.

### Way 3: Using a Lambda Expression (Java 8+)

Since `Runnable` is a functional interface, you can use a lambda:

```java
public class LambdaThreadDemo {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println(Thread.currentThread().getName() + " - " + i);
                try { Thread.sleep(300); } catch (InterruptedException e) { break; }
            }
        }, "Lambda-Thread");

        t1.start();
    }
}
```

### Way 4: Implementing the `Callable` interface (returns a result)

Unlike `Runnable`, `Callable` can **return a value** and **throw checked exceptions**.

```java
import java.util.concurrent.*;

public class CallableDemo {
    public static void main(String[] args) throws Exception {
        Callable<Integer> task = () -> {
            int sum = 0;
            for (int i = 1; i <= 100; i++) sum += i;
            return sum;
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(task);

        System.out.println("Doing other work on main thread...");
        Integer result = future.get(); // blocks until result is ready
        System.out.println("Sum = " + result); // Sum = 5050

        executor.shutdown();
    }
}
```

### Way 5: Using `ExecutorService` (Thread Pool)

Creating threads manually is expensive. Thread pools **reuse threads** for multiple tasks.

```java
import java.util.concurrent.*;

public class ThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("Task " + taskId + " running on " 
                    + Thread.currentThread().getName());
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            });
        }

        pool.shutdown(); // no new tasks accepted; existing tasks complete
    }
}
```

```
Output (3 threads handle 6 tasks):
  Task 1 running on pool-1-thread-1
  Task 2 running on pool-1-thread-2
  Task 3 running on pool-1-thread-3
  Task 4 running on pool-1-thread-1   ← thread-1 reused
  Task 5 running on pool-1-thread-2   ← thread-2 reused
  Task 6 running on pool-1-thread-3   ← thread-3 reused
```

### Comparison Table

| Approach               | Returns value? | Throws checked exception? | Reuses threads? | Recommended? |
|------------------------|:--------------:|:-------------------------:|:---------------:|:------------:|
| `extends Thread`       | No             | No                        | No              | No           |
| `implements Runnable`  | No             | No                        | No              | Sometimes    |
| Lambda `Runnable`      | No             | No                        | No              | Sometimes    |
| `implements Callable`  | Yes            | Yes                       | With Executor   | Yes          |
| `ExecutorService`      | Yes (Future)   | Yes                       | Yes             | Yes          |

### `start()` vs `run()`

```
t.start():                          t.run():
  main ──► creates new OS thread     main ──► executes run() directly
           └──► run() executes              (NO new thread created!)
           on the new thread                on the MAIN thread
```

**Always call `start()`, never `run()` directly** — calling `run()` does not create a new thread.

---

## 4. Thread Lifecycle (States) — with Diagram and Code

### Thread States

A Java thread exists in one of the following states defined in `Thread.State`:

| State             | Description                                                                                             |
|-------------------|---------------------------------------------------------------------------------------------------------|
| **NEW**           | Thread object is created but `start()` has not been called yet.                                         |
| **RUNNABLE**      | Thread is ready to run or is currently running. The OS scheduler decides when it gets CPU time.          |
| **BLOCKED**       | Thread is waiting to acquire a monitor lock held by another thread (e.g., entering a `synchronized` block). |
| **WAITING**       | Thread is waiting indefinitely for another thread to perform a specific action (`wait()`, `join()`, `park()`). |
| **TIMED_WAITING** | Thread is waiting for a specified amount of time (`sleep()`, `wait(timeout)`, `join(timeout)`).         |
| **TERMINATED**    | Thread has finished execution — either `run()` completed or an uncaught exception occurred.             |

### Thread Lifecycle Diagram

```
                           ┌─────────────┐
                           │     NEW      │
                           │  (created)   │
                           └──────┬───────┘
                                  │ start()
                                  ▼
                     ┌────────────────────────┐
                     │       RUNNABLE          │
                ┌───►│  (ready / running)      │◄────────────────┐
                │    └───┬──────┬─────────┬───┘                  │
                │        │      │         │                      │
                │        │      │         │                      │
     ┌──────────┘        │      │         │          ┌───────────┘
     │                   │      │         │          │
     │   synchronized    │      │         │ wait()   │ notify() /
     │   lock acquired   │      │         │ join()   │ notifyAll() /
     │                   │      │         │ park()   │ join completes
     │                   │      │         │          │
     │                   │      │         ▼          │
     │                   │      │    ┌────────────┐  │
     │                   │      │    │  WAITING   │──┘
     │                   │      │    │(indefinite)│
     │                   │      │    └────────────┘
     │                   │      │
     │   waiting for     │      │  sleep(ms)
     │   monitor lock    │      │  wait(ms)
     │                   │      │  join(ms)
     │                   │      │
     │                   ▼      ▼
     │          ┌──────────┐  ┌───────────────┐
     │          │ BLOCKED  │  │ TIMED_WAITING │──── timeout expires /
     │          │(for lock)│  │  (has timeout) │     notify()
     │          └────┬─────┘  └───────────────┘         │
     │               │                                   │
     └───────────────┘                                   │
                ▲                                        │
                └────────────────────────────────────────┘
                                  │
                     run() completes / exception
                                  │
                                  ▼
                        ┌──────────────────┐
                        │   TERMINATED     │
                        │  (dead thread)   │
                        └──────────────────┘
```

### Thread Lifecycle Code Example

```java
public class ThreadLifecycleDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {

        // ---- NEW state ----
        Thread thread = new Thread(() -> {
            try {
                // ---- TIMED_WAITING state (during sleep) ----
                Thread.sleep(1000);

                synchronized (lock) {
                    // ---- WAITING state (during wait) ----
                    lock.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "DemoThread");

        System.out.println("After creation: " + thread.getState());       // NEW

        // ---- RUNNABLE state ----
        thread.start();
        System.out.println("After start(): " + thread.getState());        // RUNNABLE

        Thread.sleep(200); // let the thread enter sleep()
        System.out.println("During sleep(): " + thread.getState());       // TIMED_WAITING

        Thread.sleep(1500); // let sleep() finish and wait() begin
        System.out.println("During wait(): " + thread.getState());        // WAITING

        // Notify the thread to wake up
        synchronized (lock) {
            lock.notify();
        }

        Thread.sleep(100); // let it finish
        System.out.println("After completion: " + thread.getState());     // TERMINATED
    }
}
```

**Output:**

```
After creation: NEW
After start(): RUNNABLE
During sleep(): TIMED_WAITING
During wait(): WAITING
After completion: TERMINATED
```

### Demonstrating BLOCKED State

```java
public class BlockedStateDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(5000); // holds the lock for 5 seconds
                } catch (InterruptedException e) {}
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (lock) { // will block because t1 holds the lock
                System.out.println("Thread-2 acquired the lock");
            }
        }, "Thread-2");

        t1.start();
        Thread.sleep(100); // ensure t1 acquires lock first

        t2.start();
        Thread.sleep(100); // ensure t2 attempts to acquire lock

        System.out.println("Thread-2 state: " + t2.getState()); // BLOCKED
    }
}
```

### State Transition Summary

```
NEW ──start()──► RUNNABLE ──run() ends──► TERMINATED

RUNNABLE ──synchronized (lock busy)──────► BLOCKED
BLOCKED  ──lock acquired────────────────► RUNNABLE

RUNNABLE ──wait() / join() / park()──────► WAITING
WAITING  ──notify() / join done──────────► RUNNABLE

RUNNABLE ──sleep(ms) / wait(ms)──────────► TIMED_WAITING
TIMED_WAITING ──timeout / notify()───────► RUNNABLE
```

---

## 5. Inter-Thread Communication

Inter-thread communication (ITC) allows threads to **coordinate** with each other. Instead of busy-waiting (repeatedly checking a condition), threads can **signal** each other when something important happens.

### The Core Methods: `wait()`, `notify()`, and `notifyAll()`

These methods are defined in `java.lang.Object` (not in `Thread`) because they operate on the **object's monitor lock**.

| Method          | Description                                                                                                   |
|-----------------|---------------------------------------------------------------------------------------------------------------|
| `wait()`        | Releases the lock and puts the current thread into **WAITING** state until another thread calls `notify()`/`notifyAll()` on the same object. |
| `wait(long ms)` | Same as `wait()` but returns automatically after the timeout.                                                 |
| `notify()`      | Wakes up **one** arbitrary thread waiting on this object's monitor.                                           |
| `notifyAll()`   | Wakes up **all** threads waiting on this object's monitor.                                                    |

> **Critical Rule**: These methods must be called **inside a `synchronized` block/method** on the same object. Otherwise, Java throws `IllegalMonitorStateException`.

### How `wait()` and `notify()` Work Internally

```
Thread-A                          Thread-B
─────────                         ─────────
synchronized(lock) {
    // checks condition
    // condition is false
    lock.wait();
    // ──► releases lock            synchronized(lock) {
    // ──► enters WAITING state          // modifies shared data
    //                                   // condition becomes true
    //     (sleeping...)                  lock.notify();
    //                               }  // ──► releases lock
    // ◄── re-acquires lock
    // ◄── resumes execution
    // checks condition again
}
```

### Classic Example: Producer-Consumer Problem

```java
import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumerDemo {

    private static final int CAPACITY = 5;
    private static final Queue<Integer> buffer = new LinkedList<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            int value = 0;
            while (true) {
                synchronized (lock) {
                    // Wait if buffer is full
                    while (buffer.size() == CAPACITY) {
                        try {
                            System.out.println("Buffer full. Producer waiting...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    buffer.add(++value);
                    System.out.println("Produced: " + value 
                        + " | Buffer size: " + buffer.size());
                    lock.notifyAll(); // wake up consumer
                }
                try { Thread.sleep(200); } catch (InterruptedException e) { return; }
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    // Wait if buffer is empty
                    while (buffer.isEmpty()) {
                        try {
                            System.out.println("Buffer empty. Consumer waiting...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    int value = buffer.poll();
                    System.out.println("Consumed: " + value 
                        + " | Buffer size: " + buffer.size());
                    lock.notifyAll(); // wake up producer
                }
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            }
        }, "Consumer");

        producer.start();
        consumer.start();
    }
}
```

```
Output (sample):
  Buffer empty. Consumer waiting...
  Produced: 1 | Buffer size: 1
  Consumed: 1 | Buffer size: 0
  Produced: 2 | Buffer size: 1
  Produced: 3 | Buffer size: 2
  Consumed: 2 | Buffer size: 1
  ...
```

```
┌──────────────────────────────────────────────────────────┐
│                   Shared Buffer (Queue)                  │
│                   Capacity = 5                           │
│  ┌───┬───┬───┬───┬───┐                                  │
│  │   │   │   │   │   │                                   │
│  └───┴───┴───┴───┴───┘                                  │
│    ▲                 │                                   │
│    │ produce         │ consume                           │
│    │                 ▼                                   │
│  ┌──────────┐    ┌──────────┐                            │
│  │ Producer │    │ Consumer │                            │
│  │ Thread   │    │ Thread   │                            │
│  └──────────┘    └──────────┘                            │
│                                                          │
│  If buffer FULL  → Producer calls wait()                 │
│  If buffer EMPTY → Consumer calls wait()                 │
│  After produce   → notifyAll() wakes Consumer            │
│  After consume   → notifyAll() wakes Producer            │
└──────────────────────────────────────────────────────────┘
```

### Why Use `while` Instead of `if` for `wait()`?

```java
// WRONG — can cause spurious wakeup bugs
synchronized (lock) {
    if (buffer.isEmpty()) {
        lock.wait();
    }
    // May execute even if buffer is STILL empty (spurious wakeup)
    buffer.poll();
}

// CORRECT — always re-check condition after waking up
synchronized (lock) {
    while (buffer.isEmpty()) {
        lock.wait();
    }
    // Guaranteed: buffer is NOT empty here
    buffer.poll();
}
```

**Reason**: A thread can wake up without being notified (called a **spurious wakeup**). Using `while` ensures the condition is always re-checked.

### Modern Alternative: `BlockingQueue`

Java's `java.util.concurrent` package provides `BlockingQueue`, which handles all the `wait()`/`notify()` logic internally:

```java
import java.util.concurrent.*;

public class BlockingQueueDemo {

    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        // Producer
        Thread producer = new Thread(() -> {
            int value = 0;
            try {
                while (true) {
                    queue.put(++value); // blocks if queue is full
                    System.out.println("Produced: " + value);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    int value = queue.take(); // blocks if queue is empty
                    System.out.println("Consumed: " + value);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
    }
}
```

> `BlockingQueue` is **thread-safe** and eliminates the need for manual synchronization.

### Other Inter-Thread Communication Mechanisms

| Mechanism              | Description                                                                                         |
|------------------------|-----------------------------------------------------------------------------------------------------|
| `wait()` / `notify()`  | Low-level, built into every Java object. Requires `synchronized`.                                  |
| `BlockingQueue`         | High-level queue with built-in blocking. Best for producer-consumer patterns.                      |
| `CountDownLatch`        | One or more threads wait until a set of operations in other threads complete.                       |
| `CyclicBarrier`         | Threads wait at a barrier point until all threads arrive, then all proceed.                        |
| `Semaphore`             | Controls access to a shared resource by maintaining a set of permits.                              |
| `Exchanger`             | Two threads exchange objects at a synchronization point.                                           |
| `Lock` + `Condition`    | More flexible alternative to `synchronized` + `wait()`/`notify()` with multiple wait-sets.        |
| `CompletableFuture`     | Compose async tasks with callbacks, chaining, and combining (Java 8+).                            |

### `CountDownLatch` Example

```java
import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {
            final int serviceId = i;
            new Thread(() -> {
                try {
                    Thread.sleep(serviceId * 1000L);
                    System.out.println("Service-" + serviceId + " is ready");
                    latch.countDown(); // decrement count
                } catch (InterruptedException e) {}
            }).start();
        }

        System.out.println("Waiting for all services to start...");
        latch.await(); // blocks until count reaches 0
        System.out.println("All services are ready! Starting application.");
    }
}
```

```
Output:
  Waiting for all services to start...
  Service-1 is ready
  Service-2 is ready
  Service-3 is ready
  All services are ready! Starting application.
```

### `Lock` and `Condition` Example

```java
import java.util.concurrent.locks.*;
import java.util.LinkedList;
import java.util.Queue;

public class LockConditionDemo {

    private static final int CAPACITY = 5;
    private static final Queue<Integer> buffer = new LinkedList<>();
    private static final Lock lock = new ReentrantLock();
    private static final Condition notFull = lock.newCondition();
    private static final Condition notEmpty = lock.newCondition();

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            int value = 0;
            while (true) {
                lock.lock();
                try {
                    while (buffer.size() == CAPACITY) {
                        notFull.await(); // wait until not full
                    }
                    buffer.add(++value);
                    System.out.println("Produced: " + value);
                    notEmpty.signal(); // signal consumer
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }
                try { Thread.sleep(200); } catch (InterruptedException e) { return; }
            }
        });

        Thread consumer = new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (buffer.isEmpty()) {
                        notEmpty.await(); // wait until not empty
                    }
                    int val = buffer.poll();
                    System.out.println("Consumed: " + val);
                    notFull.signal(); // signal producer
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            }
        });

        producer.start();
        consumer.start();
    }
}
```

> **Advantage over `wait()`/`notify()`**: `Condition` allows **multiple wait-sets** on the same lock (e.g., separate conditions for "not full" and "not empty").

---

## 6. What is a Thread Pool? How to Use It?

### The Problem: Creating Threads Manually

Every time you create a new thread with `new Thread(...)`, the JVM asks the OS to allocate a native thread. This involves:

- Allocating stack memory (~512KB–1MB per thread)
- Registering the thread with the OS scheduler
- Context-switching overhead when many threads compete for CPU

If your application handles 10,000 requests and creates a new thread for each one, you'll exhaust memory and the OS thread limit.

```
Without Thread Pool (1 thread per task):
  Request 1  ──► new Thread() ──► run ──► GC'd
  Request 2  ──► new Thread() ──► run ──► GC'd
  Request 3  ──► new Thread() ──► run ──► GC'd
  ...
  Request 10000 ──► new Thread() ──► OutOfMemoryError!

With Thread Pool (fixed number of reusable threads):
  Request 1  ──► pool assigns Thread-1 ──► run ──► Thread-1 returned to pool
  Request 2  ──► pool assigns Thread-2 ──► run ──► Thread-2 returned to pool
  Request 3  ──► pool assigns Thread-3 ──► run ──► Thread-3 returned to pool
  Request 4  ──► waits in queue ──► Thread-1 free ──► assigns Thread-1
  ...
```

### What is a Thread Pool?

A **thread pool** is a collection of pre-created, reusable worker threads managed by the JVM. Instead of creating and destroying threads for every task, tasks are submitted to the pool, which assigns an available thread. When the thread finishes, it returns to the pool and picks up the next task.

### Thread Pool Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         Thread Pool                              │
│                                                                  │
│   ┌──────────────────────────────────┐                           │
│   │         Task Queue (BlockingQueue)│                          │
│   │  ┌──────┬──────┬──────┬──────┐   │                          │
│   │  │Task 5│Task 4│Task 3│Task 2│   │  ◄──── submit(task)      │
│   │  └──────┴──────┴──────┴──────┘   │                          │
│   └──────────────┬───────────────────┘                           │
│                  │ take()                                        │
│                  ▼                                               │
│   ┌────────────────────────────────────────┐                     │
│   │          Worker Threads                │                     │
│   │  ┌──────────┐ ┌──────────┐ ┌──────────┐                     │
│   │  │ Thread-1 │ │ Thread-2 │ │ Thread-3 │                     │
│   │  │ (busy)   │ │ (idle)   │ │ (busy)   │                     │
│   │  └──────────┘ └──────────┘ └──────────┘                     │
│   └────────────────────────────────────────┘                     │
│                                                                  │
│   When a thread finishes its task:                               │
│     1. Returns result (if Callable/Future)                       │
│     2. Goes back to pick next task from queue                    │
│     3. If queue is empty, thread waits (idle)                    │
└──────────────────────────────────────────────────────────────────┘
```

### Creating Thread Pools with `Executors` Factory

Java provides the `Executors` utility class with factory methods to create common pool types:

#### 1. `newFixedThreadPool(int nThreads)`

Creates a pool with a **fixed number** of threads. Extra tasks wait in a queue.

```java
import java.util.concurrent.*;

public class FixedThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 7; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("Task-" + taskId + " started on " 
                    + Thread.currentThread().getName());
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                System.out.println("Task-" + taskId + " completed");
            });
        }

        pool.shutdown(); // graceful shutdown — completes queued tasks
        // pool.shutdownNow(); // forceful — interrupts running tasks
    }
}
```

```
Output:
  Task-1 started on pool-1-thread-1
  Task-2 started on pool-1-thread-2
  Task-3 started on pool-1-thread-3
  (Tasks 4-7 wait in queue...)
  Task-1 completed
  Task-4 started on pool-1-thread-1   ← thread-1 reused
  Task-2 completed
  Task-5 started on pool-1-thread-2   ← thread-2 reused
  ...
```

**Best for**: Known, bounded workloads (e.g., processing a batch of files with a fixed number of workers).

#### 2. `newCachedThreadPool()`

Creates threads on demand. Idle threads are kept alive for 60 seconds, then terminated.

```java
import java.util.concurrent.*;

public class CachedThreadPoolDemo {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("Task-" + taskId + " on " 
                    + Thread.currentThread().getName());
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            });
        }

        pool.shutdown();
    }
}
```

**Best for**: Many short-lived tasks. **Danger**: Can create unbounded threads if tasks are slow.

#### 3. `newSingleThreadExecutor()`

A pool with exactly **one** thread — tasks execute sequentially in FIFO order.

```java
import java.util.concurrent.*;

public class SingleThreadExecutorDemo {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newSingleThreadExecutor();

        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("Task-" + taskId + " on " 
                    + Thread.currentThread().getName());
            });
        }

        pool.shutdown();
    }
}
```

```
Output (always sequential):
  Task-1 on pool-1-thread-1
  Task-2 on pool-1-thread-1
  Task-3 on pool-1-thread-1
  Task-4 on pool-1-thread-1
```

**Best for**: Tasks that must execute one at a time (e.g., writing to a log file).

#### 4. `newScheduledThreadPool(int corePoolSize)`

Supports **delayed** and **periodic** task execution.

```java
import java.util.concurrent.*;

public class ScheduledPoolDemo {
    public static void main(String[] args) {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

        // Run once after 3 seconds
        pool.schedule(() -> {
            System.out.println("Delayed task executed after 3 seconds");
        }, 3, TimeUnit.SECONDS);

        // Run every 2 seconds, starting after 1 second
        pool.scheduleAtFixedRate(() -> {
            System.out.println("Periodic task at " + System.currentTimeMillis());
        }, 1, 2, TimeUnit.SECONDS);

        // Run with 2-second delay between end of one execution and start of next
        pool.scheduleWithFixedDelay(() -> {
            System.out.println("Fixed-delay task at " + System.currentTimeMillis());
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }, 1, 2, TimeUnit.SECONDS);
    }
}
```

**Best for**: Cron-like tasks, heartbeats, periodic cleanup, retry logic.

### Comparison of Thread Pool Types

| Pool Type                | Core Threads | Max Threads     | Queue Type           | Idle Timeout | Use Case                     |
|--------------------------|:------------:|:---------------:|----------------------|:------------:|------------------------------|
| `newFixedThreadPool(n)`  | n            | n               | `LinkedBlockingQueue` (unbounded) | Never        | Known bounded workloads      |
| `newCachedThreadPool()`  | 0            | Integer.MAX_VALUE | `SynchronousQueue`   | 60 seconds   | Many short-lived tasks       |
| `newSingleThreadExecutor()` | 1         | 1               | `LinkedBlockingQueue` (unbounded) | Never        | Sequential task execution    |
| `newScheduledThreadPool(n)` | n          | Integer.MAX_VALUE | `DelayedWorkQueue`  | Never        | Delayed/periodic tasks       |

### Proper Shutdown

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

// Submit tasks...

pool.shutdown(); // Step 1: Stop accepting new tasks

try {
    // Step 2: Wait for existing tasks to finish (up to 60 seconds)
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Step 3: Force shutdown if still running
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Pool did not terminate");
        }
    }
} catch (InterruptedException e) {
    pool.shutdownNow();
    Thread.currentThread().interrupt();
}
```

---

## 7. What is `ThreadPoolExecutor`? How to Use It?

### The Problem with `Executors` Factory Methods

The `Executors` factory methods are convenient but hide important details and can be **dangerous in production**:

| Factory Method              | Hidden Danger                                                                                      |
|-----------------------------|-----------------------------------------------------------------------------------------------------|
| `newFixedThreadPool(n)`     | Uses an **unbounded** `LinkedBlockingQueue` — can accumulate millions of tasks and cause `OutOfMemoryError` |
| `newCachedThreadPool()`     | Max threads = `Integer.MAX_VALUE` — can create millions of threads and crash the JVM               |
| `newSingleThreadExecutor()` | Uses an **unbounded** queue — same OOM risk as fixed pool                                          |

> This is why **Alibaba Java Coding Guidelines** and many production codebases recommend using `ThreadPoolExecutor` directly for full control.

### What is `ThreadPoolExecutor`?

`ThreadPoolExecutor` is the **actual implementation class** behind all `Executors` factory methods. It gives you full control over every aspect of the thread pool.

### Class Hierarchy

```
                 Executor (interface)
                    │
                    ▼
              ExecutorService (interface)
                    │
                    ▼
           AbstractExecutorService (abstract)
                    │
                    ▼
            ThreadPoolExecutor (concrete)
                    │
                    ▼
          ScheduledThreadPoolExecutor (extends ThreadPoolExecutor)
```

### Constructor Parameters

```java
public ThreadPoolExecutor(
    int corePoolSize,        // Minimum threads kept alive (even if idle)
    int maximumPoolSize,     // Maximum threads allowed
    long keepAliveTime,      // Idle time before extra threads are terminated
    TimeUnit unit,           // Time unit for keepAliveTime
    BlockingQueue<Runnable> workQueue,  // Queue for waiting tasks
    ThreadFactory threadFactory,        // How to create threads
    RejectedExecutionHandler handler    // What to do when pool is saturated
)
```

### How Tasks Flow Through ThreadPoolExecutor

```
                    submit(task)
                        │
                        ▼
              ┌─────────────────────┐
              │ corePoolSize full?  │
              └────┬───────────┬────┘
                   │ No        │ Yes
                   ▼           ▼
          ┌──────────────┐  ┌─────────────────┐
          │ Create new   │  │ workQueue full?  │
          │ core thread  │  └────┬────────┬────┘
          │ and run task │       │ No     │ Yes
          └──────────────┘       ▼        ▼
                        ┌───────────┐  ┌──────────────────────┐
                        │ Add task  │  │ maximumPoolSize full? │
                        │ to queue  │  └────┬────────────┬─────┘
                        └───────────┘       │ No         │ Yes
                                            ▼            ▼
                                  ┌──────────────┐  ┌──────────────────┐
                                  │ Create new   │  │ RejectionHandler │
                                  │ extra thread │  │ (AbortPolicy,    │
                                  │ and run task │  │  CallerRunsPolicy│
                                  └──────────────┘  │  DiscardPolicy,  │
                                                    │  DiscardOldest)  │
                                                    └──────────────────┘
```

### Full Example with Custom ThreadPoolExecutor

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExecutorDemo {

    public static void main(String[] args) {
        // Custom ThreadFactory — gives meaningful names to threads
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "my-worker-" + counter.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };

        // Create ThreadPoolExecutor with full control
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                              // corePoolSize
            5,                              // maximumPoolSize
            30, TimeUnit.SECONDS,           // keepAliveTime for idle extra threads
            new ArrayBlockingQueue<>(10),   // bounded queue — capacity 10
            namedThreadFactory,             // custom thread factory
            new ThreadPoolExecutor.CallerRunsPolicy()  // rejection policy
        );

        // Submit 20 tasks
        for (int i = 1; i <= 20; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task-" + taskId + " running on " 
                    + Thread.currentThread().getName()
                    + " | Pool size: " + executor.getPoolSize()
                    + " | Queue size: " + executor.getQueue().size());
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            });
        }

        executor.shutdown();
    }
}
```

### Rejection Policies (RejectedExecutionHandler)

When both the queue and the max pool are full, the rejection policy decides what happens:

| Policy                    | Behavior                                                                                                   |
|---------------------------|------------------------------------------------------------------------------------------------------------|
| `AbortPolicy` (default)  | Throws `RejectedExecutionException`. Task is lost.                                                         |
| `CallerRunsPolicy`        | Runs the task on the **caller's thread** (e.g., main thread). Provides natural back-pressure.              |
| `DiscardPolicy`           | Silently discards the task. No exception, no logging. Dangerous!                                           |
| `DiscardOldestPolicy`     | Discards the **oldest** task in the queue and retries submitting the new task.                              |
| Custom handler            | Implement `RejectedExecutionHandler` for custom logic (e.g., log, persist to DB, send to dead-letter queue). |

```java
// Custom rejection handler
RejectedExecutionHandler customHandler = (runnable, poolExecutor) -> {
    System.err.println("Task rejected! Queue size: " 
        + poolExecutor.getQueue().size()
        + ", Active threads: " + poolExecutor.getActiveCount());
    // Could persist to a database, send to a message queue, etc.
};

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 4, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(5),
    Executors.defaultThreadFactory(),
    customHandler
);
```

### BlockingQueue Choices

| Queue Type                | Behavior                                                                          | When to Use                     |
|---------------------------|-----------------------------------------------------------------------------------|---------------------------------|
| `ArrayBlockingQueue(n)`   | Bounded, FIFO. Blocks when full.                                                  | Production — prevents OOM       |
| `LinkedBlockingQueue()`   | Unbounded (default) or bounded. FIFO.                                             | Be careful with unbounded!      |
| `SynchronousQueue`        | Zero capacity. Each put() must wait for a take(). Hands off task directly.         | For cached-style pools          |
| `PriorityBlockingQueue`   | Unbounded, tasks ordered by priority (Comparable/Comparator).                     | Priority-based task scheduling  |

### Monitoring ThreadPoolExecutor

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 5, 30, 
    TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

// Submit tasks...

// Monitoring methods
System.out.println("Core pool size    : " + executor.getCorePoolSize());
System.out.println("Current pool size : " + executor.getPoolSize());
System.out.println("Active threads    : " + executor.getActiveCount());
System.out.println("Max pool size     : " + executor.getMaximumPoolSize());
System.out.println("Task count        : " + executor.getTaskCount());
System.out.println("Completed tasks   : " + executor.getCompletedTaskCount());
System.out.println("Queue size        : " + executor.getQueue().size());
System.out.println("Is shutdown       : " + executor.isShutdown());
System.out.println("Is terminated     : " + executor.isTerminated());
```

### How `Executors` Factories Map to `ThreadPoolExecutor`

```java
// Executors.newFixedThreadPool(5) is equivalent to:
new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>());  // ⚠ UNBOUNDED queue

// Executors.newCachedThreadPool() is equivalent to:
new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
    new SynchronousQueue<>());     // ⚠ UNBOUNDED threads

// Executors.newSingleThreadExecutor() is equivalent to:
new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>());  // ⚠ UNBOUNDED queue
```

### Production-Ready Configuration Guidelines

```java
// Rule of thumb for pool sizing:
// CPU-bound tasks:  corePoolSize = number of CPU cores
// I/O-bound tasks:  corePoolSize = CPU cores * 2 (or higher)

int cpuCores = Runtime.getRuntime().availableProcessors();

// For I/O-heavy workload (REST calls, DB queries):
ThreadPoolExecutor ioPool = new ThreadPoolExecutor(
    cpuCores * 2,                       // core threads
    cpuCores * 4,                       // max threads
    60, TimeUnit.SECONDS,               // idle timeout
    new ArrayBlockingQueue<>(500),      // bounded queue
    new ThreadPoolExecutor.CallerRunsPolicy()  // back-pressure
);

// For CPU-heavy workload (computation, data processing):
ThreadPoolExecutor cpuPool = new ThreadPoolExecutor(
    cpuCores,                           // core = CPU cores
    cpuCores,                           // max = same (no benefit adding more)
    0, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(200),
    new ThreadPoolExecutor.AbortPolicy()
);
```

---

## 8. What is `AsyncTaskExecutor` and `TaskExecutorAdapter`? How to Use Them?

These are part of **Spring Framework's** abstraction over Java's `ExecutorService`. Spring provides its own `TaskExecutor` hierarchy to decouple your application from specific Java concurrency implementations.

### Spring's Task Executor Hierarchy

```
                    Executor (java.util.concurrent)
                        │
                        ▼
                  TaskExecutor (Spring interface)
                   │           │
                   ▼           ▼
       AsyncTaskExecutor    SchedulingTaskExecutor
               │
               ▼
      AsyncListenableTaskExecutor
               │
               ▼
  ┌────────────┼──────────────────────────────┐
  │            │                              │
  ▼            ▼                              ▼
ThreadPool  SimpleAsync                 ConcurrentTask
TaskExecutor TaskExecutor               Executor
  (Spring)    (Spring)                   (Spring 6+)
```

### What is `TaskExecutor`?

`TaskExecutor` is Spring's core abstraction for executing tasks asynchronously. It's a simple interface:

```java
// org.springframework.core.task.TaskExecutor
@FunctionalInterface
public interface TaskExecutor extends Executor {
    void execute(Runnable task);
}
```

Spring uses this interface everywhere — `@Async`, `@Scheduled`, Spring MVC async handling, Spring WebFlux, etc.

### What is `AsyncTaskExecutor`?

`AsyncTaskExecutor` extends `TaskExecutor` and adds the ability to **submit tasks that return a `Future`**:

```java
// org.springframework.core.task.AsyncTaskExecutor
public interface AsyncTaskExecutor extends TaskExecutor {

    // Submit a Runnable, get a Future to track completion
    Future<?> submit(Runnable task);

    // Submit a Callable, get a Future with the result
    <T> Future<T> submit(Callable<T> task);
}
```

This is the interface Spring's `@Async` uses under the hood.

### What is `TaskExecutorAdapter`?

`TaskExecutorAdapter` wraps a standard Java `java.util.concurrent.Executor` (or `ExecutorService`) and adapts it to Spring's `AsyncTaskExecutor` interface. It's a **bridge** between Java's concurrency API and Spring's task abstraction.

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   Java's ExecutorService                                    │
│   (e.g., ThreadPoolExecutor)                                │
│       │                                                     │
│       │  wraps                                              │
│       ▼                                                     │
│   TaskExecutorAdapter                                       │
│   (implements AsyncTaskExecutor)                            │
│       │                                                     │
│       │  can be used as                                     │
│       ▼                                                     │
│   Spring's TaskExecutor / AsyncTaskExecutor                 │
│   (used by @Async, Spring MVC, etc.)                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Spring's Built-in TaskExecutor Implementations

| Implementation                   | Description                                                                                        |
|----------------------------------|----------------------------------------------------------------------------------------------------|
| `ThreadPoolTaskExecutor`         | The **most commonly used** Spring executor. Wraps `ThreadPoolExecutor` with Spring-friendly config. |
| `SimpleAsyncTaskExecutor`        | Creates a **new thread for every task** — no pooling! Only for simple/testing scenarios.            |
| `ConcurrentTaskExecutor`         | Adapts a Java `Executor`/`ExecutorService` to Spring's `TaskExecutor`.                             |
| `TaskExecutorAdapter`            | Similar to `ConcurrentTaskExecutor` — wraps a Java `Executor` as Spring `AsyncTaskExecutor`.       |

### Example 1: `ThreadPoolTaskExecutor` (Recommended for Production)

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);            // core threads
        executor.setMaxPoolSize(10);            // max threads
        executor.setQueueCapacity(25);          // queue size
        executor.setKeepAliveSeconds(60);       // idle thread timeout
        executor.setThreadNamePrefix("Async-"); // thread names: Async-1, Async-2...
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);  // graceful shutdown
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

Using it with `@Async`:

```java
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Async("taskExecutor")  // references the bean name
    public CompletableFuture<String> sendEmail(String to) {
        System.out.println("Sending email on: " + Thread.currentThread().getName());
        // Simulate email sending
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        return CompletableFuture.completedFuture("Email sent to " + to);
    }
}
```

Calling the async method:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-email")
    public CompletableFuture<String> sendEmail(@RequestParam String to) {
        // Returns immediately — email sends on background thread
        return emailService.sendEmail(to);
    }
}
```

```
Request: POST /api/send-email?to=user@example.com

Main thread (http-nio-8080-exec-1) returns immediately.
Background thread (Async-1) sends the email.
```

### How `ThreadPoolTaskExecutor` Wraps `ThreadPoolExecutor`

```
┌────────────────────────────────────────────────────────────┐
│        ThreadPoolTaskExecutor (Spring)                     │
│                                                            │
│   Spring-friendly setters:                                 │
│     setCorePoolSize(5)                                     │
│     setMaxPoolSize(10)                                     │
│     setQueueCapacity(25)                                   │
│     setThreadNamePrefix("Async-")                          │
│     setWaitForTasksToCompleteOnShutdown(true)               │
│                                                            │
│   ┌────────────────────────────────────────────────────┐   │
│   │        ThreadPoolExecutor (Java)                   │   │
│   │                                                    │   │
│   │   core=5, max=10, queue=ArrayBlockingQueue(25)     │   │
│   │   keepAlive=60s, handler=CallerRunsPolicy          │   │
│   └────────────────────────────────────────────────────┘   │
│                                                            │
│   Lifecycle: implements InitializingBean, DisposableBean   │
│   → auto-initializes on Spring startup                     │
│   → auto-shuts-down on Spring shutdown                     │
└────────────────────────────────────────────────────────────┘
```

### Example 2: `TaskExecutorAdapter` — Wrapping a Java Executor

Use `TaskExecutorAdapter` when you already have a Java `ExecutorService` and want to use it in Spring's ecosystem.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorAdapterConfig {

    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        // Create a standard Java ThreadPoolExecutor
        ThreadPoolExecutor javaExecutor = new ThreadPoolExecutor(
            4,                                  // core pool size
            8,                                  // max pool size
            60, TimeUnit.SECONDS,               // keep alive
            new ArrayBlockingQueue<>(100),       // bounded queue
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "custom-pool-" + count.getAndIncrement());
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Wrap it in TaskExecutorAdapter for Spring compatibility
        return new TaskExecutorAdapter(javaExecutor);
    }
}
```

Using the adapted executor:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

@Service
public class ReportService {

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    public Future<String> generateReportAsync(String reportId) {
        return asyncTaskExecutor.submit(() -> {
            System.out.println("Generating report " + reportId 
                + " on " + Thread.currentThread().getName());
            Thread.sleep(3000); // simulate work
            return "Report-" + reportId + " completed";
        });
    }

    public void fireAndForget(String message) {
        asyncTaskExecutor.execute(() -> {
            System.out.println("Processing: " + message 
                + " on " + Thread.currentThread().getName());
        });
    }
}
```

### Example 3: `ConcurrentTaskExecutor` — Alternative Adapter

`ConcurrentTaskExecutor` is similar to `TaskExecutorAdapter` but is more commonly used in Spring:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import java.util.concurrent.*;

@Configuration
public class ConcurrentExecutorConfig {

    @Bean
    public AsyncTaskExecutor concurrentTaskExecutor() {
        ExecutorService javaExecutor = Executors.newFixedThreadPool(4);
        return new ConcurrentTaskExecutor(javaExecutor);
    }
}
```

### Example 4: `SimpleAsyncTaskExecutor` (No Pooling!)

Creates a **new thread per task**. No reuse, no pooling. Use only for testing or very low-volume scenarios.

```java
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleExecutorConfig {

    @Bean
    public AsyncTaskExecutor simpleExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("Simple-");
        executor.setConcurrencyLimit(10); // max 10 concurrent threads
        // executor.setVirtualThreads(true); // Java 21+ virtual threads!
        return executor;
    }
}
```

> **Warning**: Without `setConcurrencyLimit()`, it creates unlimited threads.

### Example 5: Using `@Async` with Multiple Executors

```java
@Configuration
@EnableAsync
public class MultiExecutorConfig {

    @Bean(name = "emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Email-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "reportExecutor")
    public ThreadPoolTaskExecutor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Report-");
        executor.initialize();
        return executor;
    }
}
```

```java
@Service
public class NotificationService {

    @Async("emailExecutor")   // uses email pool
    public void sendEmail(String to) {
        System.out.println("Email on " + Thread.currentThread().getName());
    }

    @Async("reportExecutor")  // uses report pool
    public CompletableFuture<byte[]> generatePdf(String data) {
        System.out.println("PDF on " + Thread.currentThread().getName());
        return CompletableFuture.completedFuture(new byte[0]);
    }
}
```

```
sendEmail("user@test.com")    → runs on Email-1
generatePdf("sales-q1")      → runs on Report-1
```

### Comparison Table: Spring Task Executors

| Class                          | Pools threads? | Returns Future? | Spring managed? | Use Case                                      |
|--------------------------------|:--------------:|:---------------:|:---------------:|------------------------------------------------|
| `ThreadPoolTaskExecutor`       | Yes            | Yes             | Yes (lifecycle) | **Production default** — use this              |
| `SimpleAsyncTaskExecutor`      | No             | Yes             | Yes             | Testing, virtual threads (Java 21+)            |
| `ConcurrentTaskExecutor`       | Depends        | Yes             | Partial         | Wrapping existing Java Executor                |
| `TaskExecutorAdapter`          | Depends        | Yes             | No              | Bridging Java Executor → Spring AsyncTaskExecutor |

### When to Use What

```
Need async in Spring Boot?
  │
  ├── Use @Async + ThreadPoolTaskExecutor (95% of cases)
  │
  ├── Already have a Java ExecutorService?
  │     └── Wrap with TaskExecutorAdapter or ConcurrentTaskExecutor
  │
  ├── Need virtual threads (Java 21+)?
  │     └── SimpleAsyncTaskExecutor with setVirtualThreads(true)
  │
  └── Need fine-grained control?
        └── Create ThreadPoolExecutor manually + wrap with adapter
```

---

## 9. What is `Runnable` and `Callable`?

### `Runnable` — A Task That Returns Nothing

`Runnable` is a functional interface introduced in **Java 1.0**. It represents a task that can be executed by a thread but **cannot return a result** and **cannot throw checked exceptions**.

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

#### Using `Runnable`

```java
// 1. Traditional way — implementing the interface
public class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Running on: " + Thread.currentThread().getName());
    }
}

// 2. Anonymous class
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Anonymous task running");
    }
};

// 3. Lambda expression (Java 8+)
Runnable lambdaTask = () -> System.out.println("Lambda task running");

// Execute with Thread
new Thread(lambdaTask, "Worker-1").start();

// Execute with ExecutorService
ExecutorService pool = Executors.newFixedThreadPool(2);
pool.execute(lambdaTask);     // fire-and-forget
Future<?> future = pool.submit(lambdaTask);  // returns Future (result is null)
pool.shutdown();
```

### `Callable` — A Task That Returns a Result

`Callable` was introduced in **Java 5** as a more powerful alternative to `Runnable`. It **can return a value** and **can throw checked exceptions**.

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

#### Using `Callable`

```java
import java.util.concurrent.*;

public class CallableExample {
    public static void main(String[] args) throws Exception {
        // Callable that computes factorial
        Callable<Long> factorialTask = () -> {
            long result = 1;
            for (int i = 1; i <= 20; i++) result *= i;
            return result;
        };

        // Callable that can throw checked exceptions
        Callable<String> riskyTask = () -> {
            if (Math.random() > 0.5) {
                throw new Exception("Something went wrong!");
            }
            return "Success";
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Long> factorialFuture = pool.submit(factorialTask);
        Future<String> riskyFuture = pool.submit(riskyTask);

        System.out.println("Factorial: " + factorialFuture.get());
        
        try {
            System.out.println("Risky: " + riskyFuture.get());
        } catch (ExecutionException e) {
            System.out.println("Task failed: " + e.getCause().getMessage());
        }

        pool.shutdown();
    }
}
```

### `Runnable` vs `Callable` Comparison

```
┌──────────────────────────────────────────────────────────────┐
│                    Runnable                                   │
│                                                              │
│   void run()                                                 │
│   ├── No return value                                        │
│   ├── Cannot throw checked exceptions                        │
│   ├── Works with Thread, ExecutorService.execute()           │
│   └── Since Java 1.0                                         │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│                    Callable<V>                                │
│                                                              │
│   V call() throws Exception                                  │
│   ├── Returns a value of type V                              │
│   ├── Can throw checked exceptions                           │
│   ├── Works with ExecutorService.submit() only               │
│   └── Since Java 5                                           │
└──────────────────────────────────────────────────────────────┘
```

| Feature                  | `Runnable`                        | `Callable<V>`                     |
|--------------------------|-----------------------------------|-----------------------------------|
| Method                   | `void run()`                      | `V call() throws Exception`      |
| Return value             | None                              | Yes — generic type `V`           |
| Checked exceptions       | Cannot throw                      | Can throw                        |
| Used with `Thread`       | Yes                               | No (needs `FutureTask` wrapper)  |
| Used with `ExecutorService` | Yes (`execute()` and `submit()`) | Yes (`submit()` only)          |
| Returns `Future`         | `submit()` returns `Future<?>` (null result) | `submit()` returns `Future<V>` |
| Introduced in            | Java 1.0                         | Java 5                           |

### Converting Between `Runnable` and `Callable`

```java
import java.util.concurrent.*;

// Runnable → Callable (wraps runnable, returns null)
Runnable runnable = () -> System.out.println("running");
Callable<Object> callableFromRunnable = Executors.callable(runnable);

// Runnable → Callable with a fixed result
Callable<String> callableWithResult = Executors.callable(runnable, "DONE");

// Using Callable with Thread (via FutureTask)
Callable<Integer> task = () -> 42;
FutureTask<Integer> futureTask = new FutureTask<>(task);
new Thread(futureTask).start();
System.out.println(futureTask.get()); // 42
```

---

## 10. What is `Future`? How to Use It?

### What is `Future`?

`Future<V>` is an interface (since Java 5) that represents the **result of an asynchronous computation**. When you submit a task to an `ExecutorService`, you get back a `Future` that acts as a placeholder for the result that will be available later.

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws InterruptedException, 
                                              ExecutionException, 
                                              TimeoutException;
}
```

### How `Future` Works

```
Main Thread                           Worker Thread
───────────                           ─────────────
Future<String> f = pool.submit(task)
  │                                   task starts executing...
  │  (continues doing other work)     │
  │                                   │ computing...
  │                                   │
  ▼                                   │
String result = f.get()               │
  │                                   │
  │  ◄──── BLOCKS ────►              │
  │  (main thread waits)              │
  │                                   ▼
  │                                   task completes → result ready
  │  ◄─────────────────────────────── returns result
  │
  ▼
  Uses result
```

### `Future` Methods Explained

| Method                    | Description                                                                                                |
|---------------------------|------------------------------------------------------------------------------------------------------------|
| `get()`                   | **Blocks** until the result is available, then returns it. Throws `ExecutionException` if task threw.      |
| `get(timeout, unit)`      | Blocks for at most the specified time. Throws `TimeoutException` if result not ready.                      |
| `isDone()`                | Returns `true` if the task completed (normally, exception, or cancelled). Non-blocking.                    |
| `isCancelled()`           | Returns `true` if the task was cancelled before completion.                                                |
| `cancel(mayInterrupt)`    | Attempts to cancel. If `true`, interrupts the running thread. Returns `false` if already done/cancelled.   |

### Complete `Future` Example

```java
import java.util.concurrent.*;

public class FutureDemo {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        // Submit multiple tasks
        Future<Double> priceFuture = pool.submit(() -> {
            Thread.sleep(2000); // simulate API call
            return 99.95;
        });

        Future<Integer> stockFuture = pool.submit(() -> {
            Thread.sleep(1500); // simulate DB query
            return 42;
        });

        Future<String> descFuture = pool.submit(() -> {
            Thread.sleep(1000); // simulate service call
            return "Premium Widget";
        });

        System.out.println("All tasks submitted. Main thread continues...");

        try {
            // Check if done without blocking
            System.out.println("Price done? " + priceFuture.isDone());  // likely false

            // Get with timeout
            String description = descFuture.get(3, TimeUnit.SECONDS);
            System.out.println("Description: " + description);

            // Blocking get
            Double price = priceFuture.get();
            Integer stock = stockFuture.get();

            System.out.println("Price: $" + price);
            System.out.println("Stock: " + stock + " units");

        } catch (TimeoutException e) {
            System.out.println("Task timed out!");
        } catch (ExecutionException e) {
            System.out.println("Task failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pool.shutdown();
    }
}
```

### Cancelling a `Future`

```java
import java.util.concurrent.*;

public class FutureCancelDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<?> longTask = pool.submit(() -> {
            try {
                for (int i = 1; i <= 100; i++) {
                    System.out.println("Step " + i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                System.out.println("Task was interrupted!");
            }
        });

        Thread.sleep(2000); // let it run for 2 seconds

        // Cancel with interruption
        boolean cancelled = longTask.cancel(true);
        System.out.println("Cancelled: " + cancelled);       // true
        System.out.println("Is cancelled: " + longTask.isCancelled()); // true
        System.out.println("Is done: " + longTask.isDone()); // true

        pool.shutdown();
    }
}
```

### `invokeAll()` and `invokeAny()` with `Future`

```java
import java.util.concurrent.*;
import java.util.*;

public class InvokeDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = List.of(
            () -> { Thread.sleep(2000); return "Result from Task-1"; },
            () -> { Thread.sleep(1000); return "Result from Task-2"; },
            () -> { Thread.sleep(3000); return "Result from Task-3"; }
        );

        // invokeAll — waits for ALL tasks to complete, returns list of Futures
        List<Future<String>> futures = pool.invokeAll(tasks);
        for (Future<String> f : futures) {
            System.out.println(f.get()); // all results available
        }

        // invokeAny — returns result of the FASTEST task, cancels the rest
        String fastest = pool.invokeAny(tasks);
        System.out.println("Fastest: " + fastest); // "Result from Task-2"

        pool.shutdown();
    }
}
```

### Limitations of `Future`

```
┌──────────────────────────────────────────────────────────────┐
│              Problems with Future                            │
│                                                              │
│  1. BLOCKING get()                                           │
│     └─ f.get() blocks the calling thread                     │
│     └─ Defeats the purpose of async if you block immediately │
│                                                              │
│  2. NO callbacks                                             │
│     └─ Cannot say "when done, do this"                       │
│     └─ Must poll isDone() or block on get()                  │
│                                                              │
│  3. CANNOT chain/compose                                     │
│     └─ Cannot: future1.thenApply(result -> transform(result))│
│     └─ Cannot combine two futures                            │
│                                                              │
│  4. NO exception handling pipeline                           │
│     └─ Must try-catch around get()                           │
│     └─ Cannot: future.exceptionally(ex -> fallback)          │
│                                                              │
│  5. CANNOT manually complete                                 │
│     └─ Cannot set result from outside                        │
│     └─ Must wait for the submitted task to finish            │
└──────────────────────────────────────────────────────────────┘
```

---

## 11. What is `CompletableFuture`? How to Use It?

### What is `CompletableFuture`?

`CompletableFuture<T>` (Java 8+) is a powerful enhancement over `Future` that supports:

- **Non-blocking callbacks** — attach actions to run when the future completes
- **Chaining/composition** — chain multiple async operations together
- **Combining** — merge results from multiple futures
- **Exception handling** — handle errors in the async pipeline
- **Manual completion** — complete a future from outside

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    // Implements both Future (for get/cancel) and CompletionStage (for composition)
}
```

### Class Hierarchy

```
         Future<V> (interface)          CompletionStage<T> (interface)
              │                                   │
              └──────────────┬────────────────────┘
                             │
                             ▼
                   CompletableFuture<T>
```

### Basic Usage

```java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureBasics {
    public static void main(String[] args) throws Exception {

        // 1. Run async task with no return value
        CompletableFuture<Void> voidFuture = CompletableFuture.runAsync(() -> {
            System.out.println("Running on: " + Thread.currentThread().getName());
        });

        // 2. Run async task that returns a value
        CompletableFuture<String> supplyFuture = CompletableFuture.supplyAsync(() -> {
            return "Hello from " + Thread.currentThread().getName();
        });

        // 3. Non-blocking callback
        supplyFuture.thenAccept(result -> {
            System.out.println("Got: " + result);
        });

        // 4. Chaining — transform the result
        CompletableFuture<Integer> lengthFuture = supplyFuture
            .thenApply(String::length);
        
        System.out.println("Length: " + lengthFuture.get()); // non-blocking pipeline, blocking only at the end

        // 5. Manually complete a future
        CompletableFuture<String> manual = new CompletableFuture<>();
        manual.complete("Manually set value");
        System.out.println(manual.get()); // "Manually set value"
    }
}
```

---

## 12. How is `CompletableFuture` Better Than `Future`?

### Side-by-Side Comparison

| Feature                          | `Future`                                      | `CompletableFuture`                                          |
|----------------------------------|-----------------------------------------------|--------------------------------------------------------------|
| **Get result**                   | `get()` — blocks the calling thread           | `get()` + non-blocking `thenApply()`, `thenAccept()`         |
| **Callbacks**                    | Not supported                                 | `thenApply()`, `thenAccept()`, `thenRun()`                   |
| **Chaining**                     | Not supported                                 | `thenApply().thenApply().thenAccept()`                       |
| **Combining futures**            | Not supported                                 | `thenCombine()`, `allOf()`, `anyOf()`                        |
| **Exception handling**           | Only via `try-catch` around `get()`           | `exceptionally()`, `handle()`, `whenComplete()`              |
| **Manual completion**            | Not supported                                 | `complete()`, `completeExceptionally()`                      |
| **Async execution control**      | Requires `ExecutorService`                    | Built-in `runAsync()`, `supplyAsync()` with optional executor|
| **Composition**                  | Not supported                                 | `thenCompose()` for flat-mapping futures                     |
| **Timeout (Java 9+)**           | Only `get(timeout, unit)`                     | `orTimeout()`, `completeOnTimeout()`                         |

### Problem 1: Blocking `get()` — Solved

```java
// FUTURE — must block to get result
Future<String> future = executor.submit(() -> fetchData());
String data = future.get(); // BLOCKS HERE — thread is wasted
process(data);

// COMPLETABLE FUTURE — non-blocking callback
CompletableFuture.supplyAsync(() -> fetchData())
    .thenAccept(data -> process(data)); // runs WHEN ready, no blocking
```

### Problem 2: No Chaining — Solved

```java
// FUTURE — manual, ugly chaining
Future<String> f1 = executor.submit(() -> fetchUser());
String user = f1.get(); // block
Future<String> f2 = executor.submit(() -> fetchOrders(user));
String orders = f2.get(); // block again
Future<String> f3 = executor.submit(() -> formatReport(orders));
String report = f3.get(); // block yet again

// COMPLETABLE FUTURE — elegant pipeline
CompletableFuture.supplyAsync(() -> fetchUser())
    .thenApply(user -> fetchOrders(user))
    .thenApply(orders -> formatReport(orders))
    .thenAccept(report -> sendEmail(report));
// No blocking! Entire pipeline is async.
```

### Problem 3: No Exception Handling — Solved

```java
// FUTURE — try-catch around get()
try {
    String result = future.get();
} catch (ExecutionException e) {
    // handle error
}

// COMPLETABLE FUTURE — declarative error handling
CompletableFuture.supplyAsync(() -> riskyOperation())
    .thenApply(result -> transform(result))
    .exceptionally(ex -> {
        System.out.println("Failed: " + ex.getMessage());
        return "fallback value";
    })
    .thenAccept(System.out::println);
```

### Problem 4: Cannot Combine Futures — Solved

```java
// FUTURE — manual combination
Future<Double> priceFuture = executor.submit(() -> getPrice());
Future<Integer> stockFuture = executor.submit(() -> getStock());
Double price = priceFuture.get();  // block
Integer stock = stockFuture.get(); // block
String summary = "Price: " + price + ", Stock: " + stock;

// COMPLETABLE FUTURE — combine two results
CompletableFuture<Double> priceCF = CompletableFuture.supplyAsync(() -> getPrice());
CompletableFuture<Integer> stockCF = CompletableFuture.supplyAsync(() -> getStock());

priceCF.thenCombine(stockCF, (price, stock) -> 
    "Price: " + price + ", Stock: " + stock
).thenAccept(System.out::println);
// Both run in parallel, combined when both done. No blocking!
```

### Visual Comparison

```
Future:
  submit ──► [waiting...] ──► get() BLOCKS ──► use result
  submit ──► [waiting...] ──► get() BLOCKS ──► use result
  (sequential blocking — threads wasted)

CompletableFuture:
  supplyAsync ──► thenApply ──► thenCombine ──► thenAccept
  supplyAsync ──────────────────────┘
  (non-blocking pipeline — threads freed immediately)
```

---

## 13. All `CompletableFuture` Methods with Examples

### Method Categories Overview

```
┌──────────────────────────────────────────────────────────────┐
│              CompletableFuture Method Categories             │
│                                                              │
│  1. CREATION       — runAsync, supplyAsync, completedFuture  │
│  2. TRANSFORM      — thenApply, thenApplyAsync               │
│  3. CONSUME        — thenAccept, thenAcceptAsync             │
│  4. RUN AFTER      — thenRun, thenRunAsync                   │
│  5. COMBINE TWO    — thenCombine, thenCombineAsync           │
│  6. COMPOSE/CHAIN  — thenCompose, thenComposeAsync           │
│  7. ACCEPT BOTH    — thenAcceptBoth, thenAcceptBothAsync     │
│  8. RUN AFTER BOTH — runAfterBoth, runAfterBothAsync         │
│  9. EITHER/FASTEST — applyToEither, acceptEither, runAfterEither │
│ 10. ALL / ANY      — allOf, anyOf                            │
│ 11. EXCEPTION      — exceptionally, handle, whenComplete     │
│ 12. COMPLETION     — complete, completeExceptionally         │
│ 13. TIMEOUT (9+)   — orTimeout, completeOnTimeout            │
└──────────────────────────────────────────────────────────────┘
```

### Async Variant Pattern

Most methods come in **three overloads**:

| Variant                  | Execution Thread                                                           |
|--------------------------|----------------------------------------------------------------------------|
| `thenApply(fn)`          | Runs on the **same thread** that completed the previous stage (or caller)  |
| `thenApplyAsync(fn)`     | Runs on a thread from the **ForkJoinPool.commonPool()**                    |
| `thenApplyAsync(fn, executor)` | Runs on a thread from the **specified executor**                    |

```java
// Same thread (may be main or ForkJoinPool thread)
cf.thenApply(x -> transform(x));

// ForkJoinPool.commonPool()
cf.thenApplyAsync(x -> transform(x));

// Custom executor
cf.thenApplyAsync(x -> transform(x), myExecutor);
```

---

### Category 1: Creation Methods

#### `runAsync` — Run a task with no return value

```java
// Signature: static CompletableFuture<Void> runAsync(Runnable runnable)
// Signature: static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor)

CompletableFuture<Void> cf1 = CompletableFuture.runAsync(() -> {
    System.out.println("Task on: " + Thread.currentThread().getName());
    // runs on ForkJoinPool.commonPool by default
});

// With custom executor
ExecutorService myPool = Executors.newFixedThreadPool(2);
CompletableFuture<Void> cf2 = CompletableFuture.runAsync(() -> {
    System.out.println("Task on: " + Thread.currentThread().getName());
}, myPool);
```

#### `supplyAsync` — Run a task that returns a value

```java
// Signature: static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
// Signature: static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor)

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    return "Hello from " + Thread.currentThread().getName();
});

System.out.println(cf.get()); // "Hello from ForkJoinPool.commonPool-worker-1"
```

#### `completedFuture` — Create an already-completed future

```java
// Signature: static <U> CompletableFuture<U> completedFuture(U value)

CompletableFuture<String> cf = CompletableFuture.completedFuture("cached-value");
System.out.println(cf.get()); // "cached-value" — returns immediately
System.out.println(cf.isDone()); // true
```

#### `failedFuture` (Java 9+) — Create an already-failed future

```java
// Signature: static <U> CompletableFuture<U> failedFuture(Throwable ex)

CompletableFuture<String> cf = CompletableFuture.failedFuture(
    new RuntimeException("Oops")
);
cf.exceptionally(ex -> "Fallback: " + ex.getMessage())
  .thenAccept(System.out::println); // "Fallback: Oops"
```

---

### Category 2: Transform — `thenApply`

Transforms the result. Like `map()` on a Stream.

```java
// Signature: <U> CompletableFuture<U> thenApply(Function<T, U> fn)
// Signature: <U> CompletableFuture<U> thenApplyAsync(Function<T, U> fn)
// Signature: <U> CompletableFuture<U> thenApplyAsync(Function<T, U> fn, Executor executor)

CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> "  John Doe  ");

CompletableFuture<String> trimmed = nameFuture.thenApply(String::trim);
CompletableFuture<String> upper = trimmed.thenApply(String::toUpperCase);
CompletableFuture<Integer> length = upper.thenApply(String::length);

System.out.println(length.get()); // 8

// Chained
int len = CompletableFuture.supplyAsync(() -> "  John Doe  ")
    .thenApply(String::trim)
    .thenApply(String::toUpperCase)
    .thenApply(String::length)
    .get();
System.out.println(len); // 8
```

```
"  John Doe  " ──trim──► "John Doe" ──toUpperCase──► "JOHN DOE" ──length──► 8
```

---

### Category 3: Consume — `thenAccept`

Consumes the result (no return value). Like `forEach()`.

```java
// Signature: CompletableFuture<Void> thenAccept(Consumer<T> action)
// Signature: CompletableFuture<Void> thenAcceptAsync(Consumer<T> action)
// Signature: CompletableFuture<Void> thenAcceptAsync(Consumer<T> action, Executor executor)

CompletableFuture.supplyAsync(() -> "Hello, World!")
    .thenAccept(result -> System.out.println("Received: " + result));
// Output: Received: Hello, World!
```

---

### Category 4: Run After — `thenRun`

Runs an action after the future completes. Does NOT receive the result.

```java
// Signature: CompletableFuture<Void> thenRun(Runnable action)
// Signature: CompletableFuture<Void> thenRunAsync(Runnable action)
// Signature: CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor)

CompletableFuture.supplyAsync(() -> {
    System.out.println("Computing...");
    return 42;
}).thenRun(() -> {
    System.out.println("Computation finished! (but I don't know the result)");
});
```

---

### Category 5: Combine Two Futures — `thenCombine`

Combines results of **two independent** futures.

```java
// Signature: <U,V> CompletableFuture<V> thenCombine(CompletionStage<U> other, BiFunction<T,U,V> fn)
// Signature: <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<U> other, BiFunction<T,U,V> fn)
// Signature: <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<U> other, BiFunction<T,U,V> fn, Executor executor)

CompletableFuture<Double> priceCF = CompletableFuture.supplyAsync(() -> {
    sleep(2000);
    return 29.99;
});

CompletableFuture<Double> discountCF = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return 0.15; // 15% discount
});

CompletableFuture<String> finalPrice = priceCF.thenCombine(discountCF, 
    (price, discount) -> {
        double discounted = price * (1 - discount);
        return String.format("Final price: $%.2f", discounted);
    });

System.out.println(finalPrice.get()); // "Final price: $25.49"
```

```
priceCF:    ──────[computing price]──────► 29.99 ─┐
                                                    ├──► combine ──► "Final price: $25.49"
discountCF: ──[computing discount]──► 0.15 ────────┘
(both run in parallel!)
```

---

### Category 6: Compose/Chain — `thenCompose`

Chains **dependent** futures. Like `flatMap()` on a Stream. Use when the next step itself returns a `CompletableFuture`.

```java
// Signature: <U> CompletableFuture<U> thenCompose(Function<T, CompletionStage<U>> fn)
// Signature: <U> CompletableFuture<U> thenComposeAsync(Function<T, CompletionStage<U>> fn)
// Signature: <U> CompletableFuture<U> thenComposeAsync(Function<T, CompletionStage<U>> fn, Executor executor)

CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> "user-123");

// thenApply would give CompletableFuture<CompletableFuture<String>> — NESTED!
// thenCompose flattens it to CompletableFuture<String>

CompletableFuture<String> ordersFuture = userFuture.thenCompose(userId -> 
    CompletableFuture.supplyAsync(() -> "Orders for " + userId)
);

System.out.println(ordersFuture.get()); // "Orders for user-123"
```

#### `thenApply` vs `thenCompose`

```java
// thenApply — Function returns a plain value
// T → U
CompletableFuture<String> cf1 = supplyAsync(() -> "hello")
    .thenApply(s -> s.toUpperCase());  // String → String

// thenCompose — Function returns a CompletableFuture
// T → CompletableFuture<U>
CompletableFuture<String> cf2 = supplyAsync(() -> "user-1")
    .thenCompose(id -> fetchOrdersAsync(id));  // String → CompletableFuture<String>
```

```
thenApply:   CF<A> ──fn(A→B)──────────► CF<B>
thenCompose: CF<A> ──fn(A→CF<B>)───────► CF<B>  (flattened)
             CF<A> ──fn via thenApply──► CF<CF<B>>  (nested — wrong!)
```

---

### Category 7: Accept Both — `thenAcceptBoth`

Consumes results of two futures (no return value).

```java
// Signature: CompletableFuture<Void> thenAcceptBoth(CompletionStage<U> other, BiConsumer<T,U> action)
// Signature: CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<U> other, BiConsumer<T,U> action)
// Signature: CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<U> other, BiConsumer<T,U> action, Executor e)

CompletableFuture<String> greetCF = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> nameCF = CompletableFuture.supplyAsync(() -> "Alice");

greetCF.thenAcceptBoth(nameCF, (greet, name) -> {
    System.out.println(greet + ", " + name + "!"); // "Hello, Alice!"
});
```

---

### Category 8: Run After Both — `runAfterBoth`

Runs an action after **both** futures complete. Does not receive results.

```java
// Signature: CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action)
// Signature: CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action)
// Signature: CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor e)

CompletableFuture<Void> taskA = CompletableFuture.runAsync(() -> sleep(2000));
CompletableFuture<Void> taskB = CompletableFuture.runAsync(() -> sleep(1000));

taskA.runAfterBoth(taskB, () -> {
    System.out.println("Both tasks completed!");
});
```

---

### Category 9: Either/Fastest — `applyToEither`, `acceptEither`, `runAfterEither`

Uses the result of whichever future completes **first**.

```java
// applyToEither — transform the faster result
// Signature: <U> CompletableFuture<U> applyToEither(CompletionStage<T> other, Function<T,U> fn)
// Signature: <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<T> other, Function<T,U> fn)
// Signature: <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<T> other, Function<T,U> fn, Executor e)

CompletableFuture<String> server1 = CompletableFuture.supplyAsync(() -> {
    sleep(3000);
    return "Response from Server-1";
});
CompletableFuture<String> server2 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Response from Server-2";
});

// Use whichever responds first
CompletableFuture<String> fastest = server1.applyToEither(server2, 
    response -> "Winner: " + response);
System.out.println(fastest.get()); // "Winner: Response from Server-2"
```

```java
// acceptEither — consume the faster result
// Signature: CompletableFuture<Void> acceptEither(CompletionStage<T> other, Consumer<T> action)
// Signature: CompletableFuture<Void> acceptEitherAsync(CompletionStage<T> other, Consumer<T> action)
// Signature: CompletableFuture<Void> acceptEitherAsync(CompletionStage<T> other, Consumer<T> action, Executor e)

server1.acceptEither(server2, response -> {
    System.out.println("First response: " + response);
});
```

```java
// runAfterEither — run action when EITHER completes (no result)
// Signature: CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action)
// Signature: CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action)
// Signature: CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor e)

server1.runAfterEither(server2, () -> {
    System.out.println("At least one server responded!");
});
```

---

### Category 10: All / Any — `allOf`, `anyOf`

#### `allOf` — Wait for ALL futures to complete

```java
// Signature: static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs)

CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
    sleep(2000); return "User data";
});
CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
    sleep(1000); return "Order data";
});
CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> {
    sleep(1500); return "Payment data";
});

CompletableFuture<Void> allDone = CompletableFuture.allOf(cf1, cf2, cf3);

// Wait for all, then collect results
allDone.thenRun(() -> {
    try {
        String user = cf1.get();    // already complete — no blocking
        String orders = cf2.get();
        String payment = cf3.get();
        System.out.println(user + " | " + orders + " | " + payment);
    } catch (Exception e) { e.printStackTrace(); }
});
```

**Practical pattern — collect all results into a list:**

```java
List<CompletableFuture<String>> futures = List.of(cf1, cf2, cf3);

CompletableFuture<List<String>> allResults = CompletableFuture
    .allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream()
        .map(CompletableFuture::join)  // join() is like get() but throws unchecked
        .toList()
    );

System.out.println(allResults.get()); // [User data, Order data, Payment data]
```

```
cf1: ──────[user data]──────────► done ──┐
cf2: ──[order data]──► done ─────────────┤ allOf waits for ALL
cf3: ────[payment data]────► done ───────┘
                                          └──► thenApply: collect results
```

#### `anyOf` — Return whichever future completes FIRST

```java
// Signature: static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs)

CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
    sleep(500); return "Fast service";
});
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    sleep(3000); return "Slow service";
});

CompletableFuture<Object> first = CompletableFuture.anyOf(fast, slow);
System.out.println(first.get()); // "Fast service"
```

---

### Category 11: Exception Handling — `exceptionally`, `handle`, `whenComplete`

#### `exceptionally` — Recover from errors

```java
// Signature: CompletableFuture<T> exceptionally(Function<Throwable, T> fn)
// Signature: CompletableFuture<T> exceptionallyAsync(Function<Throwable, T> fn)        // Java 12+
// Signature: CompletableFuture<T> exceptionallyAsync(Function<Throwable, T> fn, Executor e) // Java 12+

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    if (true) throw new RuntimeException("Service unavailable");
    return "data";
}).exceptionally(ex -> {
    System.out.println("Error: " + ex.getMessage());
    return "fallback data";
});

System.out.println(cf.get()); // "fallback data"
```

```
supplyAsync ──► exception! ──► exceptionally ──► "fallback data"
supplyAsync ──► "data"     ──► (skipped)     ──► "data"
```

#### `handle` — Handle both result and exception

```java
// Signature: <U> CompletableFuture<U> handle(BiFunction<T, Throwable, U> fn)
// Signature: <U> CompletableFuture<U> handleAsync(BiFunction<T, Throwable, U> fn)
// Signature: <U> CompletableFuture<U> handleAsync(BiFunction<T, Throwable, U> fn, Executor e)

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) throw new RuntimeException("Boom!");
    return "Success";
}).handle((result, ex) -> {
    if (ex != null) {
        return "Recovered from: " + ex.getMessage();
    }
    return "Got: " + result;
});

System.out.println(cf.get());
// Either "Got: Success" or "Recovered from: Boom!"
```

> **`handle` vs `exceptionally`**: `handle` is called for **both** success and failure. `exceptionally` is called **only** on failure.

#### `whenComplete` — Side-effect after completion (no transformation)

```java
// Signature: CompletableFuture<T> whenComplete(BiConsumer<T, Throwable> action)
// Signature: CompletableFuture<T> whenCompleteAsync(BiConsumer<T, Throwable> action)
// Signature: CompletableFuture<T> whenCompleteAsync(BiConsumer<T, Throwable> action, Executor e)

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "data")
    .whenComplete((result, ex) -> {
        if (ex != null) {
            System.out.println("FAILED: " + ex.getMessage());
        } else {
            System.out.println("COMPLETED with: " + result);
        }
    });
// The original result/exception passes through unchanged
System.out.println(cf.get()); // "data"
```

#### `exceptionallyCompose` (Java 12+) — Recover with another `CompletableFuture`

```java
// Signature: CompletableFuture<T> exceptionallyCompose(Function<Throwable, CompletionStage<T>> fn)

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Primary failed");
    // return "primary result";
}).exceptionallyCompose(ex -> 
    CompletableFuture.supplyAsync(() -> "Backup result")
);

System.out.println(cf.get()); // "Backup result"
```

---

### Category 12: Manual Completion

#### `complete` — Set result manually

```java
CompletableFuture<String> cf = new CompletableFuture<>();

// Some other thread or callback completes it
new Thread(() -> {
    sleep(1000);
    cf.complete("Result from callback");
}).start();

System.out.println(cf.get()); // "Result from callback" — blocks until complete
```

#### `completeExceptionally` — Set exception manually

```java
CompletableFuture<String> cf = new CompletableFuture<>();

cf.completeExceptionally(new RuntimeException("Something failed"));

cf.exceptionally(ex -> "Handled: " + ex.getMessage())
  .thenAccept(System.out::println); // "Handled: Something failed"
```

#### `obtrudeValue` / `obtrudeException` — Force override the result

```java
CompletableFuture<String> cf = CompletableFuture.completedFuture("original");
cf.obtrudeValue("overridden"); // forces a new result even if already complete
System.out.println(cf.get()); // "overridden"
```

> Use with caution — this overrides an already-completed future.

---

### Category 13: Timeout (Java 9+)

#### `orTimeout` — Fail with `TimeoutException` after specified time

```java
// Signature: CompletableFuture<T> orTimeout(long timeout, TimeUnit unit)

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    sleep(5000); // takes 5 seconds
    return "data";
}).orTimeout(2, TimeUnit.SECONDS); // timeout after 2 seconds

try {
    cf.get();
} catch (ExecutionException e) {
    System.out.println(e.getCause()); // java.util.concurrent.TimeoutException
}
```

#### `completeOnTimeout` — Use default value if timeout

```java
// Signature: CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit)

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    sleep(5000);
    return "real data";
}).completeOnTimeout("default data", 2, TimeUnit.SECONDS);

System.out.println(cf.get()); // "default data" (completed with default after 2s)
```

---

### Complete Method Reference Table

| Method | Input | Output | Description |
|--------|-------|--------|-------------|
| **Creation** | | | |
| `runAsync(Runnable)` | `Runnable` | `CF<Void>` | Run task, no result |
| `supplyAsync(Supplier<U>)` | `Supplier<U>` | `CF<U>` | Run task, return result |
| `completedFuture(U)` | value | `CF<U>` | Already-completed future |
| `failedFuture(Throwable)` | exception | `CF<U>` | Already-failed future (Java 9+) |
| **Transform** | | | |
| `thenApply(Function<T,U>)` | `T → U` | `CF<U>` | Transform result (map) |
| `thenCompose(Function<T,CF<U>>)` | `T → CF<U>` | `CF<U>` | Chain futures (flatMap) |
| **Consume** | | | |
| `thenAccept(Consumer<T>)` | `T → void` | `CF<Void>` | Consume result |
| `thenRun(Runnable)` | `() → void` | `CF<Void>` | Run action, ignore result |
| **Combine Two** | | | |
| `thenCombine(CF<U>, BiFunction<T,U,V>)` | `(T, U) → V` | `CF<V>` | Combine two results |
| `thenAcceptBoth(CF<U>, BiConsumer<T,U>)` | `(T, U) → void` | `CF<Void>` | Consume two results |
| `runAfterBoth(CF<?>, Runnable)` | `() → void` | `CF<Void>` | Run after both done |
| **Either/Fastest** | | | |
| `applyToEither(CF<T>, Function<T,U>)` | `T → U` | `CF<U>` | Transform faster result |
| `acceptEither(CF<T>, Consumer<T>)` | `T → void` | `CF<Void>` | Consume faster result |
| `runAfterEither(CF<?>, Runnable)` | `() → void` | `CF<Void>` | Run when either done |
| **All / Any** | | | |
| `allOf(CF<?>...)` | n futures | `CF<Void>` | Complete when ALL done |
| `anyOf(CF<?>...)` | n futures | `CF<Object>` | Complete when ANY done |
| **Exception Handling** | | | |
| `exceptionally(Function<Throwable,T>)` | `ex → T` | `CF<T>` | Recover from error |
| `handle(BiFunction<T,Throwable,U>)` | `(T, ex) → U` | `CF<U>` | Handle result or error |
| `whenComplete(BiConsumer<T,Throwable>)` | `(T, ex) → void` | `CF<T>` | Side-effect, pass through |
| `exceptionallyCompose(...)` | `ex → CF<T>` | `CF<T>` | Recover with async (Java 12+) |
| **Manual Completion** | | | |
| `complete(T)` | value | `boolean` | Set result |
| `completeExceptionally(Throwable)` | exception | `boolean` | Set exception |
| **Timeout (Java 9+)** | | | |
| `orTimeout(long, TimeUnit)` | timeout | `CF<T>` | Fail on timeout |
| `completeOnTimeout(T, long, TimeUnit)` | default + timeout | `CF<T>` | Default on timeout |

---

### Real-World Example: Parallel API Aggregation

```java
import java.util.concurrent.*;

public class ApiAggregationDemo {

    // Simulated API calls
    static CompletableFuture<String> fetchUser(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return "{\"name\": \"John\", \"id\": \"" + userId + "\"}";
        });
    }

    static CompletableFuture<String> fetchOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            return "[{\"orderId\": 101}, {\"orderId\": 102}]";
        });
    }

    static CompletableFuture<String> fetchRecommendations(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "[\"item-A\", \"item-B\", \"item-C\"]";
        });
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        String userId = "user-42";

        CompletableFuture<String> userCF = fetchUser(userId);
        CompletableFuture<String> ordersCF = fetchOrders(userId);
        CompletableFuture<String> recsCF = fetchRecommendations(userId)
            .exceptionally(ex -> "[]"); // fallback if recommendations fail

        // Wait for all three, then combine
        CompletableFuture<String> aggregated = CompletableFuture
            .allOf(userCF, ordersCF, recsCF)
            .thenApply(v -> {
                String user = userCF.join();
                String orders = ordersCF.join();
                String recs = recsCF.join();
                return String.format(
                    "{\"user\": %s, \"orders\": %s, \"recommendations\": %s}",
                    user, orders, recs
                );
            });

        String result = aggregated.get();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println(result);
        System.out.println("Completed in " + elapsed + "ms");
        // ~2000ms (parallel) instead of ~4500ms (sequential)
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

```
Sequential:  fetchUser(1.5s) + fetchOrders(2s) + fetchRecs(1s) = 4.5s
Parallel:    max(1.5s, 2s, 1s) = ~2s  ← 2.25x faster

┌─── fetchUser ──────────────────► 1.5s ──┐
├─── fetchOrders ────────────────────────► │ 2s ──┐
├─── fetchRecommendations ──► 1s ─────────┤       │
│                                         │       │
└─────────── allOf waits ─────────────────┴───────┘
                                                   └──► thenApply: aggregate JSON
```

### Helper: `sleep()` Utility Used in Examples

```java
static void sleep(long ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```




