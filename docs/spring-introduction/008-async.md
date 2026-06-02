

# Threads and Multithreading in Java


## Youtube

- [Concept && Coding - by Shrayansh](https://www.youtube.com/@ConceptAndCodingByShrayansh)


- [Spring Boot | Exploring Asynchronous 🚀 Calls with @Async Annotation | JavaTechie](https://www.youtube.com/watch?v=R_gejlOXR7g)
- [Spring Boot Microservice | Multithreading Hands-On Example | Improve API Performance | Javatechie](https://www.youtube.com/watch?v=gMmN7wZZezI)
- [Spring Boot 3.2 With Virtual Threads Explained | Benchmarking Insights | JavaTechie](https://www.youtube.com/watch?v=9dUPPHREF7w)

- [Spring Boot - Multithreading | Process Millions of Records in Batches | Hands-on Example](https://www.youtube.com/watch?v=qaSBljS6SZk)



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

---

## 14. What is `@Async` and `@EnableAsync`?

### The Problem Without `@Async`

Consider a typical Spring Boot REST API that sends a welcome email after user registration:

```java
@Service
public class UserService {

    @Autowired
    private EmailService emailService;

    public User register(UserDto dto) {
        User user = saveUser(dto);         // 10ms — fast
        emailService.sendWelcomeEmail(user); // 3000ms — slow! blocks the thread
        return user;                        // user waits 3+ seconds
    }
}
```

```
HTTP Request ──► register() ──► saveUser (10ms) ──► sendEmail (3000ms) ──► Response
                 │                                                          │
                 └──────────────── 3010ms total ───────────────────────────┘
                                   (user waits!)
```

The caller is forced to wait while the email is being sent. The HTTP thread is **blocked** — doing nothing useful — for 3 seconds.

**Solution**: Run the email sending in a background thread using `@Async`.

---

### What is `@Async`?

`@Async` is a Spring annotation that marks a method to be executed **asynchronously** — in a separate background thread — instead of the caller's thread. The caller returns immediately without waiting for the method to complete.

```java
@Service
public class EmailService {

    @Async  // ← this method now runs on a background thread
    public void sendWelcomeEmail(User user) {
        // runs asynchronously — caller does not wait
    }
}
```

When you call an `@Async` method:
1. Spring intercepts the call via a **proxy**
2. Spring submits the method body as a task to a thread pool
3. The **caller thread returns immediately**
4. The method executes on a **background thread**

---

### What is `@EnableAsync`?

`@EnableAsync` is a configuration annotation that **activates Spring's asynchronous method execution capability**. Without it, `@Async` has absolutely no effect — methods execute synchronously as normal.

```java
@Configuration
@EnableAsync           // ← REQUIRED to activate @Async processing
public class AsyncConfig {
}
```

Or more commonly, place it on the main application class or any `@Configuration` class:

```java
@SpringBootApplication
@EnableAsync
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

---

### How `@Async` Works Internally

Spring uses **AOP (Aspect-Oriented Programming)** proxies to intercept `@Async` method calls.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      Spring @Async Internals                            │
│                                                                         │
│  Caller                Spring Proxy (AOP)           Actual Bean         │
│  ──────                ──────────────────           ───────────         │
│                                                                         │
│  userService           emailServiceProxy            emailService        │
│  .register()           (generated by Spring)        (real class)        │
│      │                        │                          │              │
│      │  emailService          │                          │              │
│      │  .sendEmail(user) ───► │                          │              │
│      │                        │  detect @Async           │              │
│      │                        │  annotation              │              │
│      │                        │  ↓                       │              │
│      │                        │  submit to thread pool   │              │
│      │  ◄────Return immediately│                          │              │
│      │  (non-blocking)        │                          │              │
│                               │                          │              │
│                               │  pool thread: ────────► sendEmail()     │
│                               │                (executes in background) │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key internals**:

| Component | Role |
|-----------|------|
| `AsyncAnnotationBeanPostProcessor` | Scans beans for `@Async` during startup and creates proxies |
| `AsyncExecutionInterceptor` | AOP advice that intercepts calls and submits to executor |
| `TaskExecutor` | The thread pool that actually executes the method |
| `SimpleAsyncUncaughtExceptionHandler` | Default handler for uncaught exceptions in `@Async` methods |

---

### Step-by-Step Setup

#### Step 1: Add `@EnableAsync` to a Configuration Class

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
}
```

#### Step 2: Configure a Custom Thread Pool (Recommended)

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // always-alive threads
        executor.setMaxPoolSize(10);           // max threads under load
        executor.setQueueCapacity(100);        // task queue before spawning max threads
        executor.setKeepAliveSeconds(60);      // idle thread lifetime
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

#### Step 3: Annotate Methods with `@Async`

```java
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Async("asyncTaskExecutor")  // references the bean name above
    public void sendWelcomeEmail(String email) {
        System.out.println("Sending email on: " + Thread.currentThread().getName());
        // simulate slow email sending
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        System.out.println("Email sent to: " + email);
    }
}
```

#### Step 4: Call the Method — It Returns Immediately

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserDto dto) {
        User user = userService.register(dto);
        return ResponseEntity.ok(user); // returns immediately!
    }
}

@Service
public class UserService {

    @Autowired private EmailService emailService;
    @Autowired private UserRepository userRepository;

    public User register(UserDto dto) {
        User user = userRepository.save(new User(dto));

        emailService.sendWelcomeEmail(user.getEmail()); // non-blocking call!

        return user; // returns in ~10ms, not ~3010ms
    }
}
```

```
With @Async:
  HTTP Request ──► register() ──► saveUser (10ms) ──► Response (10ms)
                                         │
                                         └──► [Background Thread] sendEmail (3000ms)
                                               (user doesn't wait for this)
```

---

### Return Types for `@Async` Methods

`@Async` methods can have one of two return types:

#### 1. `void` — Fire and Forget

```java
@Async
public void sendEmail(String to) {
    // runs in background, caller gets nothing back
    emailClient.send(to, "Subject", "Body");
}
```

Use when you don't need the result and don't care about completion.

#### 2. `CompletableFuture<T>` — Get the Result Later

```java
@Async
public CompletableFuture<String> sendEmailWithConfirmation(String to) {
    emailClient.send(to, "Subject", "Body");
    return CompletableFuture.completedFuture("Email sent to " + to);
}
```

Caller can optionally wait for the result:

```java
CompletableFuture<String> result = emailService.sendEmailWithConfirmation("user@test.com");
// ... do other work ...
String confirmation = result.get(); // block if needed
```

> **Note**: `Future<T>` also works but `CompletableFuture<T>` is preferred for its richer API.

---

### Using Multiple Thread Pools with `@Async`

You can define multiple executors for different task types:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Email-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Report-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Notify-");
        executor.initialize();
        return executor;
    }
}
```

```java
@Service
public class NotificationService {

    @Async("emailExecutor")
    public void sendEmail(String to) {
        System.out.println(Thread.currentThread().getName()); // Email-1
    }

    @Async("reportExecutor")
    public CompletableFuture<byte[]> generateReport(String id) {
        System.out.println(Thread.currentThread().getName()); // Report-1
        return CompletableFuture.completedFuture(new byte[0]);
    }

    @Async("notificationExecutor")
    public void pushNotification(String userId, String message) {
        System.out.println(Thread.currentThread().getName()); // Notify-1
    }
}
```

```
sendEmail()         ──► Email-1     (email pool)
generateReport()    ──► Report-1    (report pool, isolated resources)
pushNotification()  ──► Notify-1    (notification pool)
```

---

### Exception Handling in `@Async` Methods

#### For `void` Methods — `AsyncUncaughtExceptionHandler`

Exceptions in `void @Async` methods are silently swallowed by default. You must configure a custom handler:

```java
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(
                    Throwable ex, Method method, Object... params) {
                System.err.println("Async exception in method: " + method.getName());
                System.err.println("Exception message: " + ex.getMessage());
                System.err.println("Parameters: " + java.util.Arrays.toString(params));
                // → send alert, log to monitoring, write to dead-letter queue, etc.
            }
        };
    }
}
```

```java
@Service
public class EmailService {

    @Async
    public void sendEmail(String to) {
        // If this throws, AsyncUncaughtExceptionHandler catches it
        throw new RuntimeException("SMTP server down!");
        // Caller never sees this exception because return type is void
    }
}
```

#### For `CompletableFuture` Methods — Use `.exceptionally()`

```java
@Async
public CompletableFuture<String> sendEmailAsync(String to) {
    try {
        emailClient.send(to);
        return CompletableFuture.completedFuture("Sent to " + to);
    } catch (Exception e) {
        CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(e);
        return failed;
    }
}
```

Caller handles:

```java
emailService.sendEmailAsync("user@test.com")
    .exceptionally(ex -> {
        log.error("Email failed: {}", ex.getMessage());
        return "FAILED";
    })
    .thenAccept(result -> log.info("Result: {}", result));
```

---

### Common Pitfalls and How to Avoid Them

#### Pitfall 1: Calling `@Async` Method from the Same Class

The most common mistake. `@Async` works via Spring's proxy — calling a method **within the same class** bypasses the proxy entirely, so it runs synchronously.

```java
@Service
public class UserService {

    @Async  // ← THIS WILL NOT WORK!
    public void sendEmail(String to) { ... }

    public void register(UserDto dto) {
        this.sendEmail(dto.getEmail()); // ← calls real object, NOT the proxy
        // → runs SYNCHRONOUSLY, @Async is ignored!
    }
}
```

**Fix**: Move the `@Async` method to a **separate bean**:

```java
@Service
public class EmailService {

    @Async  // ← Works correctly when called from another bean
    public void sendEmail(String to) { ... }
}

@Service
public class UserService {

    @Autowired
    private EmailService emailService; // injected proxy

    public void register(UserDto dto) {
        emailService.sendEmail(dto.getEmail()); // ← goes through proxy → async!
    }
}
```

```
Same class call:  UserService.register() ──► this.sendEmail()
                                              (skips proxy — SYNCHRONOUS)

Cross-bean call:  UserService.register() ──► EmailService Proxy ──► background thread
                                              (goes through proxy — ASYNC)
```

#### Pitfall 2: Not Adding `@EnableAsync`

Without `@EnableAsync`, `@Async` is a no-op annotation. The method runs synchronously.

```java
// ❌ Missing @EnableAsync — @Async does NOTHING
@Configuration
public class AsyncConfig { }

// ✅ Correct
@Configuration
@EnableAsync
public class AsyncConfig { }
```

#### Pitfall 3: `@Async` on Private Methods

Spring's proxy cannot override private methods. `@Async` on private methods is silently ignored.

```java
// ❌ Will not work — private method
@Async
private void processInBackground() { }

// ✅ Must be public (or at minimum package-private with CGLIB)
@Async
public void processInBackground() { }
```

#### Pitfall 4: `@Async` on `@Transactional` Methods in the Same Bean

When both `@Async` and `@Transactional` are on the same method, the transaction context **does not propagate** to the new thread. Each async execution needs its own transaction.

```java
// ❌ Transaction from caller does NOT propagate
@Async
@Transactional
public void processAsync(Long id) {
    // runs in a new thread → gets its own new transaction
    // parent transaction is NOT available here
}
```

This is actually expected behavior — transactions are thread-local. Each `@Async` invocation gets a fresh transaction, which is usually what you want.

#### Pitfall 5: No Custom Executor → Using `SimpleAsyncTaskExecutor` Default

Without a configured executor, Spring uses `SimpleAsyncTaskExecutor` which **creates a new thread for every task** — no pooling, no back-pressure.

```java
// ❌ Default executor — new thread per call, no limit
@Async
public void process() { ... }
```

**Always configure a `ThreadPoolTaskExecutor`** for production.

---

### Real-Life Use Case: E-Commerce Order Processing

An e-commerce platform places an order. After saving the order to the database, several independent side effects must happen: send confirmation email, send SMS, update inventory, and generate invoice. None of these should block the customer's response.

```
Customer ──► POST /orders ──► saveOrder() ──► Response (fast!)
                                    │
                          ┌─────────┼──────────┬──────────────┐
                          ▼         ▼          ▼              ▼
                      [Email-1]  [SMS-1]  [Inventory-1]  [Invoice-1]
                      sendEmail  sendSms  updateInventory generateInvoice
                      (async)    (async)  (async)          (async)
```

#### Dependencies (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

#### Async Configuration

```java
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "orderExecutor")
    public Executor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Order-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return orderExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            System.err.printf("[ASYNC ERROR] Method: %s | Params: %s | Error: %s%n",
                method.getName(), Arrays.toString(params), ex.getMessage());
            // In production: send to monitoring (Datadog, Sentry, etc.)
        };
    }
}
```

#### Domain Model

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerEmail;
    private String customerPhone;
    private String productId;
    private int quantity;
    private double totalAmount;
    // getters, setters, constructors
}

public record OrderRequest(
    String customerEmail,
    String customerPhone,
    String productId,
    int quantity
) {}

public record OrderResponse(Long orderId, String status, String message) {}
```

#### Async Services

```java
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    @Async("orderExecutor")
    public void sendOrderConfirmationEmail(Order order) {
        System.out.println("[" + Thread.currentThread().getName() + "] "
            + "Sending confirmation email to: " + order.getCustomerEmail());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println("Email sent to: " + order.getCustomerEmail());
    }

    @Async("orderExecutor")
    public void sendSmsNotification(Order order) {
        System.out.println("[" + Thread.currentThread().getName() + "] "
            + "Sending SMS to: " + order.getCustomerPhone());
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        System.out.println("SMS sent to: " + order.getCustomerPhone());
    }
}

@Service
public class InventoryService {

    @Async("orderExecutor")
    public CompletableFuture<Boolean> updateInventory(String productId, int quantity) {
        System.out.println("[" + Thread.currentThread().getName() + "] "
            + "Updating inventory for product: " + productId);
        try { Thread.sleep(1500); } catch (InterruptedException e) {}
        System.out.println("Inventory updated: " + productId + " -" + quantity);
        return CompletableFuture.completedFuture(true);
    }
}

@Service
public class InvoiceService {

    @Async("orderExecutor")
    public CompletableFuture<String> generateInvoice(Order order) {
        System.out.println("[" + Thread.currentThread().getName() + "] "
            + "Generating invoice for order: " + order.getId());
        try { Thread.sleep(2500); } catch (InterruptedException e) {}
        String invoiceId = "INV-" + order.getId() + "-" + System.currentTimeMillis();
        System.out.println("Invoice generated: " + invoiceId);
        return CompletableFuture.completedFuture(invoiceId);
    }
}
```

#### Order Service — Orchestrating Async Tasks

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private InventoryService inventoryService;
    @Autowired private InvoiceService invoiceService;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Save the order (synchronous — must complete before returning)
        Order order = new Order();
        order.setCustomerEmail(request.customerEmail());
        order.setCustomerPhone(request.customerPhone());
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setTotalAmount(request.quantity() * 99.99);
        Order savedOrder = orderRepository.save(order);

        System.out.println("[" + Thread.currentThread().getName() + "] "
            + "Order saved with ID: " + savedOrder.getId());

        // Step 2: Fire all async side-effects — all run in parallel!
        notificationService.sendOrderConfirmationEmail(savedOrder);  // async, void
        notificationService.sendSmsNotification(savedOrder);          // async, void

        CompletableFuture<Boolean> inventoryFuture =
            inventoryService.updateInventory(savedOrder.getProductId(), savedOrder.getQuantity());

        CompletableFuture<String> invoiceFuture =
            invoiceService.generateInvoice(savedOrder);

        // Step 3: Optionally wait for critical async tasks
        // (inventory and invoice are important — we log if they fail)
        CompletableFuture.allOf(inventoryFuture, invoiceFuture)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    System.err.println("Critical async task failed: " + ex.getMessage());
                } else {
                    try {
                        System.out.println("Inventory updated: " + inventoryFuture.get());
                        System.out.println("Invoice ID: " + invoiceFuture.get());
                    } catch (Exception ignored) {}
                }
            });

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Order placed in " + elapsed + "ms (without @Async: ~7000ms)");

        // Returns in ~10ms — async tasks continue in background
        return new OrderResponse(
            savedOrder.getId(),
            "PLACED",
            "Order confirmed! Confirmation email and SMS will be sent shortly."
        );
    }
}
```

#### Controller

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }
}
```

#### Sample Output

```
POST /api/orders

[http-nio-8080-exec-1] Order saved with ID: 42
[http-nio-8080-exec-1] Order placed in 12ms           ← HTTP thread returns fast!

[Order-1] Sending confirmation email to: john@example.com
[Order-2] Sending SMS to: +1-555-0100
[Order-3] Updating inventory for product: PROD-001
[Order-4] Generating invoice for order: 42

SMS sent to: +1-555-0100                              ← 1000ms
Inventory updated: PROD-001 -2                        ← 1500ms
Email sent to: john@example.com                       ← 2000ms
Invoice generated: INV-42-1717200000000               ← 2500ms
```

```
Without @Async:
  HTTP thread: saveOrder(12ms) + email(2000ms) + sms(1000ms) + inventory(1500ms) + invoice(2500ms)
             = ~7012ms response time

With @Async:
  HTTP thread: saveOrder(12ms) → Response immediately
  Background: email + sms + inventory + invoice run in parallel
             = ~12ms response time  (585x faster!)
```

---

### `@Async` with Spring Security Context Propagation

By default, the `SecurityContext` (authenticated user) is **not** propagated to `@Async` threads because it is stored in `ThreadLocal`. To propagate it:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncSecurityConfig {

    @Bean(name = "securityAwareExecutor")
    public Executor securityAwareExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SecureAsync-");
        executor.initialize();
        // Wrap with security context delegation
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
```

```java
@Async("securityAwareExecutor")
public void auditAction(String action) {
    // SecurityContextHolder.getContext().getAuthentication() works here!
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("User " + auth.getName() + " performed: " + action);
}
```

---

### `@Async` with Request Scope Propagation (MDC / Logging Context)

Logging correlation IDs (e.g., MDC trace IDs) also live in `ThreadLocal` and won't automatically propagate to async threads. Use a decorator:

```java
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Executor;

@Bean(name = "mdcAwareExecutor")
public Executor mdcAwareExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("MdcAsync-");
    executor.setTaskDecorator(runnable -> {
        // Capture MDC from calling thread
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (mdcContext != null) MDC.setContextMap(mdcContext);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    });
    executor.initialize();
    return executor;
}
```

```java
@Async("mdcAwareExecutor")
public void processWithTracing(String data) {
    // MDC.get("traceId") returns the SAME traceId as the HTTP request thread!
    log.info("Processing {} with traceId: {}", data, MDC.get("traceId"));
}
```

---

### Why Should You Use `@Async`?

#### 1. Improved Response Times

Tasks that do not affect the response (emails, notifications, logging, analytics) should not block the HTTP thread. `@Async` moves them to the background, dramatically reducing latency.

#### 2. Better Resource Utilization

HTTP threads are expensive. Without `@Async`, a thread is blocked waiting for a slow operation (SMTP, third-party API). With `@Async`, the HTTP thread is freed immediately and can handle new requests.

#### 3. Parallel Execution of Independent Tasks

Multiple independent operations (send email + update cache + write audit log) can run in parallel instead of sequentially:

```
Sequential:  op1(1s) + op2(1s) + op3(1s) = 3s
Parallel:    max(op1, op2, op3) = 1s
```

#### 4. Resilience — Failures Don't Block the Main Flow

If an email fails to send, the order is already saved and the customer already got a response. With synchronous code, an SMTP timeout could break the entire order flow.

#### 5. Simple Integration with Spring's Ecosystem

`@Async` integrates cleanly with `@Transactional`, Spring Security, `CompletableFuture`, `@Scheduled`, and Spring's event system (`@EventListener`).

---

### Advantages and Disadvantages

#### Advantages

| Advantage | Detail |
|-----------|--------|
| **Non-blocking execution** | HTTP threads are freed immediately; response time drops dramatically |
| **Parallel processing** | Independent tasks run concurrently without manual thread management |
| **Simplicity** | Add one annotation — no boilerplate `new Thread()` or `ExecutorService` code |
| **Configurable** | Custom thread pools, queue sizes, rejection policies per use case |
| **Spring integration** | Works with `@Transactional`, Security, `CompletableFuture`, events |
| **Graceful shutdown** | `setWaitForTasksToCompleteOnShutdown(true)` drains the queue before JVM exit |
| **Testability** | Can be disabled in tests by removing `@EnableAsync` or using synchronous executor |
| **Back-pressure support** | `CallerRunsPolicy` slows the caller if the pool is saturated — prevents OOM |

#### Disadvantages

| Disadvantage | Detail |
|--------------|--------|
| **Proxy limitation** | Self-invocation (calling `@Async` from the same bean) silently bypasses async behavior |
| **Exception handling complexity** | `void` methods swallow exceptions — need custom `AsyncUncaughtExceptionHandler` |
| **Thread context loss** | `SecurityContext`, `MDC`, `RequestAttributes` (all `ThreadLocal`) don't propagate automatically |
| **Harder to debug** | Stack traces are fragmented across threads; tracing async flows requires correlation IDs |
| **No transaction propagation** | Parent transaction doesn't span to the async thread — each gets its own transaction |
| **Order not guaranteed** | Async tasks complete in unpredictable order |
| **Pool misconfiguration risk** | Default `SimpleAsyncTaskExecutor` creates unbounded threads — must configure custom pool |
| **Testing complexity** | Tests need `await()` utilities (e.g., Awaitility) or must use synchronous executors |

#### Summary Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│                     @Async — When to Use                           │
│                                                                    │
│  ✅ USE WHEN:                                                       │
│    • Sending emails / SMS / push notifications                     │
│    • Writing audit logs or analytics events                        │
│    • Generating reports or large documents (PDF, Excel)            │
│    • Calling slow third-party APIs that don't affect response      │
│    • Cache warming / invalidation                                  │
│    • Post-processing after saving (resize image, index document)   │
│    • Any fire-and-forget side effect                               │
│                                                                    │
│  ❌ AVOID WHEN:                                                     │
│    • The result is needed immediately in the same request          │
│    • Strong ordering guarantees are required                       │
│    • The operation is part of the same database transaction        │
│    • Very high throughput — consider message queues instead        │
│    • Operation must be retried on failure — use @Retryable + MQ   │
└────────────────────────────────────────────────────────────────────┘
```

---

### Testing `@Async` Methods

#### Option 1: Use Awaitility to Poll for Completion

```java
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    void sendEmail_shouldExecuteAsynchronously() {
        AtomicBoolean emailSent = new AtomicBoolean(false);

        emailService.sendWelcomeEmail("test@example.com");

        // Wait up to 5 seconds for the async task to complete
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> emailSent.get());
    }
}
```

#### Option 2: Use `SyncTaskExecutor` for Synchronous Testing

```java
@TestConfiguration
public class TestAsyncConfig {

    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor(); // runs @Async methods synchronously in tests
    }
}

@SpringBootTest
@Import(TestAsyncConfig.class)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    void placeOrder_shouldSendEmail() {
        orderService.placeOrder(new OrderRequest("a@b.com", "+1", "P1", 1));
        // With SyncTaskExecutor, all @Async methods have already completed here
        // Verify side effects directly
    }
}
```

#### Option 3: Return `CompletableFuture` and Call `.get()`

```java
@Test
void generateInvoice_shouldReturnInvoiceId() throws Exception {
    Order order = new Order();
    order.setId(42L);

    CompletableFuture<String> future = invoiceService.generateInvoice(order);
    String invoiceId = future.get(5, TimeUnit.SECONDS); // wait for result

    assertThat(invoiceId).startsWith("INV-42-");
}
```

---

### Quick Reference Cheat Sheet

```
@EnableAsync    — Activates @Async processing. Put on @Configuration class.
@Async          — Marks a method to run on a background thread.
@Async("name")  — Runs on a specific named executor bean.

Return types:
  void                    → fire-and-forget, exceptions go to AsyncUncaughtExceptionHandler
  CompletableFuture<T>    → caller can get result / chain / handle exceptions

Common pitfalls:
  ❌ Same-class self-invocation  → use a separate @Service bean
  ❌ Missing @EnableAsync        → @Async silently does nothing
  ❌ Private methods             → proxy cannot intercept, ignored silently
  ❌ Default executor            → SimpleAsyncTaskExecutor, no pooling!
  ❌ ThreadLocal propagation     → SecurityContext / MDC won't carry over

Always:
  ✅ Configure ThreadPoolTaskExecutor with bounded queue
  ✅ Set setWaitForTasksToCompleteOnShutdown(true)
  ✅ Register AsyncUncaughtExceptionHandler for void methods
  ✅ Use CallerRunsPolicy for natural back-pressure
```


---

## 15. How `@Async` Works Internally — Thread Creation Deep Dive

### The Full Execution Flow from Method Call to Background Thread

When you call an `@Async`-annotated method, Spring goes through a precise chain of components before a new thread starts executing your code. Here is the complete internal flow:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     @Async Full Internal Execution Flow                         │
│                                                                                 │
│  APPLICATION STARTUP                                                            │
│  ─────────────────                                                              │
│  @EnableAsync                                                                   │
│       │                                                                         │
│       ▼                                                                         │
│  AsyncAnnotationBeanPostProcessor                                               │
│       │  scans all beans for @Async methods                                     │
│       │  wraps each qualifying bean in a proxy                                  │
│       ▼                                                                         │
│  Spring Context: EmailService bean reference → EmailServiceProxy                │
│                                                                                 │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                 │
│  RUNTIME — when emailService.sendEmail("x@y.com") is called                    │
│  ──────────────────────────────────────────────────────────                     │
│                                                                                 │
│  1. Caller Thread (http-exec-1)                                                 │
│       │                                                                         │
│       │  emailService.sendEmail("x@y.com")                                      │
│       ▼                                                                         │
│  2. EmailServiceProxy (CGLIB / JDK Proxy)                                       │
│       │  intercepts the method call                                             │
│       │  delegates to AnnotationAsyncExecutionInterceptor                       │
│       ▼                                                                         │
│  3. AsyncExecutionInterceptor.invoke()                                          │
│       │  a) Determines which Executor to use                                    │
│       │     → checks @Async("beanName") → looks up bean                        │
│       │     → falls back to default executor if no name                         │
│       │  b) Wraps the method call in a Callable                                 │
│       │  c) Submits Callable to executor.submit(callable)                       │
│       │  d) Returns CompletableFuture (or null for void)                        │
│       ▼                                                                         │
│  4. ThreadPoolTaskExecutor / SimpleAsyncTaskExecutor                            │
│       │  queues or directly hands task to a worker thread                       │
│       ▼                                                                         │
│  5. Worker Thread (Async-1)                                                     │
│       │  picks up the Callable from the queue                                   │
│       │  calls the REAL EmailService.sendEmail("x@y.com")                       │
│       │  executes the method body                                               │
│       ▼                                                                         │
│  6. Caller Thread (http-exec-1)                                                 │
│       │  already returned at step 3 — continues its own work                   │
│       ▼ (non-blocking!)                                                         │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

### How the Proxy is Created at Startup

`@EnableAsync` imports `AsyncConfigurationSelector`, which registers `ProxyAsyncConfiguration`. That configuration registers `AsyncAnnotationBeanPostProcessor` into the Spring context.

```
@EnableAsync
    │
    ▼ imports
AsyncConfigurationSelector
    │
    ▼ selects based on AdviceMode (PROXY vs ASPECTJ)
ProxyAsyncConfiguration
    │
    ▼ registers
AsyncAnnotationBeanPostProcessor
    │  extends AbstractBeanFactoryAwareAdvisingPostProcessor
    │
    ▼ during bean creation (postProcessAfterInitialization)
    │  for each bean:
    │    scan for @Async on class or methods
    │    if found → wrap bean in a proxy
    │      CGLIB proxy  (for class-based injection)
    │      JDK proxy    (for interface-based injection)
    │
    ▼ proxy uses AsyncAnnotationAdvisor
         contains AnnotationAsyncExecutionInterceptor (the AOP advice)
```

```java
// What Spring does internally (conceptual equivalent):
EmailService realBean = new EmailService();

// Spring wraps it:
EmailService proxy = ProxyFactory.createCglibProxy(realBean,
    new AsyncAnnotationAdvisor(executor, exceptionHandler));

// The context contains the proxy, not the real bean
context.registerBean("emailService", proxy);
```

---

### `AsyncExecutionInterceptor` — The Core of `@Async`

`AsyncExecutionInterceptor` (and its subclass `AnnotationAsyncExecutionInterceptor`) is the **AOP MethodInterceptor** that is the actual engine behind `@Async`. It implements `MethodInterceptor` from Spring AOP and gets invoked on every `@Async` method call.

#### Class Hierarchy

```
MethodInterceptor (Spring AOP interface)
    │
    ▼
AsyncExecutionAspectSupport            ← base class, holds executor resolution logic
    │
    ▼
AsyncExecutionInterceptor              ← implements MethodInterceptor
    │
    ▼
AnnotationAsyncExecutionInterceptor    ← reads @Async("qualifier") value
```

#### What `AsyncExecutionInterceptor.invoke()` Does — Source-Level View

```java
// Simplified representation of AsyncExecutionInterceptor.invoke()
@Override
public Object invoke(final MethodInvocation invocation) throws Throwable {

    // Step 1: Find the target class and method
    Class<?> targetClass = invocation.getThis().getClass();
    Method specificMethod = AopUtils.getMostSpecificMethod(
        invocation.getMethod(), targetClass);

    // Step 2: Determine which executor to use
    //   - reads @Async("qualifierName") → finds bean by name
    //   - if no qualifier → uses default executor
    //   - if no default → falls back to SimpleAsyncTaskExecutor
    AsyncTaskExecutor executor = determineAsyncExecutor(specificMethod);

    // Step 3: Wrap the real method call in a Callable
    Callable<Object> task = () -> {
        try {
            Object result = invocation.proceed();  // calls the real method
            if (result instanceof Future) {
                return ((Future<?>) result).get(); // unwrap if needed
            }
        } catch (ExecutionException ex) {
            handleError(ex.getCause(), specificMethod, invocation.getArguments());
        } catch (Throwable ex) {
            handleError(ex, specificMethod, invocation.getArguments());
        }
        return null;
    };

    // Step 4: Submit to the executor — this is where the new thread starts!
    return doSubmit(task, executor, invocation.getMethod().getReturnType());
}

// Step 5: doSubmit — submits and wraps return type
protected Object doSubmit(Callable<Object> task,
                          AsyncTaskExecutor executor,
                          Class<?> returnType) {

    if (CompletableFuture.class.isAssignableFrom(returnType)) {
        // Returns a CompletableFuture backed by the executor
        return CompletableFuture.supplyAsync(() -> {
            try { return task.call(); }
            catch (Exception ex) { throw new CompletionException(ex); }
        }, executor);
    }
    else if (ListenableFuture.class.isAssignableFrom(returnType)) {
        return executor.submitListenable(task);
    }
    else if (Future.class.isAssignableFrom(returnType)) {
        return executor.submit(task);
    }
    else {
        // void return type — fire and forget
        executor.submit(task);
        return null;
    }
}
```

#### Executor Resolution Logic

```
determineAsyncExecutor(method)
    │
    ├─ 1. Check @Async("qualifierName") on the method
    │       → look up executor bean by name/qualifier in ApplicationContext
    │
    ├─ 2. Check @Async("qualifierName") on the class
    │       → same lookup
    │
    ├─ 3. No qualifier → use default executor
    │       → AsyncConfigurer.getAsyncExecutor() if configured
    │
    └─ 4. No AsyncConfigurer → fall back to SimpleAsyncTaskExecutor
              (creates a NEW thread for EVERY task — no pooling!)
```

---

### How a New Thread Is Created — Step by Step

Tracing exactly what happens inside `ThreadPoolTaskExecutor` when a task is submitted:

```
executor.submit(callable)
    │
    ▼
ThreadPoolTaskExecutor.submit()
    │  delegates to internal ThreadPoolExecutor
    ▼
ThreadPoolExecutor.execute(command)
    │
    ├─ Is pool size < corePoolSize?
    │     YES → addWorker(command, true)
    │             → new Worker(command) is created
    │             → Worker wraps a new Thread:
    │                 Thread t = threadFactory.newThread(worker)
    │                 t.start()   ← OS creates a native thread here!
    │
    ├─ Is queue not full?
    │     YES → workQueue.offer(command)  ← task sits in queue
    │           → an existing idle thread picks it up via take()
    │
    ├─ Is pool size < maximumPoolSize?
    │     YES → addWorker(command, false)
    │             → new Worker + new Thread created (non-core)
    │
    └─ Queue full AND at max threads → RejectedExecutionHandler.rejectedExecution()
```

#### Thread Lifecycle Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│              ThreadPoolExecutor — Worker Thread Lifecycle             │
│                                                                      │
│  Task submitted                                                       │
│       │                                                              │
│       ▼                                                              │
│  ┌─────────────────────────┐                                         │
│  │     Task Queue          │  ◄── tasks waiting here                 │
│  │  [task1][task2][task3]  │                                         │
│  └──────────┬──────────────┘                                         │
│             │ take()                                                  │
│             ▼                                                         │
│  ┌──────────────────────────────────────────────────────┐            │
│  │                Worker Thread (Async-1)               │            │
│  │                                                      │            │
│  │  Thread.State: RUNNABLE                              │            │
│  │       │                                              │            │
│  │       ▼                                              │            │
│  │  runWorker() loop:                                   │            │
│  │    while (task != null || (task = getTask()) != null)│            │
│  │      task.run()        ← executes your @Async method │            │
│  │      task = null       ← task complete               │            │
│  │      [loop — pick next task]                         │            │
│  │                                                      │            │
│  │  If queue empty AND idle > keepAliveTime:            │            │
│  │    Thread exits → removed from pool                  │            │
│  └──────────────────────────────────────────────────────┘            │
│                                                                      │
│  Thread States during @Async execution:                              │
│  NEW → RUNNABLE (executing task) → WAITING (queue empty, blocking)   │
│      → RUNNABLE (new task) → TERMINATED (keepAlive expired)          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

#### JVM ↔ OS Thread Mapping

```
JVM (Java Level)                    OS (Native Level)
────────────────                    ──────────────────

ThreadPoolExecutor
  Worker-1 (Java Thread)   ────►   OS Native Thread (pthread)
  Worker-2 (Java Thread)   ────►   OS Native Thread (pthread)
  Worker-3 (Java Thread)   ────►   OS Native Thread (pthread)

Each Java thread (platform thread) = 1 OS native thread
Stack size per thread: ~512KB – 1MB (configurable with -Xss)

OS Scheduler decides:
  → which native thread runs on which CPU core
  → context-switching overhead when threads > CPU cores
```

---

### What is `SimpleAsyncTaskExecutor`? (Spring Boot's Default)

When no executor is configured, Spring's `@Async` falls back to `SimpleAsyncTaskExecutor`. Understanding it is critical because it is the **most dangerous default** in Spring async programming.

#### How `SimpleAsyncTaskExecutor` Creates Threads

```java
// Simplified source of SimpleAsyncTaskExecutor
public class SimpleAsyncTaskExecutor implements AsyncTaskExecutor {

    private int concurrencyLimit = UNBOUNDED_CONCURRENCY; // -1 = no limit!

    @Override
    public void execute(Runnable task) {
        // No pool. No queue. Always creates a new thread.
        Thread thread = createThread(task);
        thread.start();
        // After task finishes, thread is DISCARDED — never reused!
    }

    protected Thread createThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(getThreadNamePrefix() + threadCount.incrementAndGet());
        thread.setDaemon(isDaemon());
        thread.setPriority(getThreadPriority());
        return thread;
    }
}
```

```
Request 1  ──► @Async call ──► new Thread("task-1") started ──► executes ──► DESTROYED
Request 2  ──► @Async call ──► new Thread("task-2") started ──► executes ──► DESTROYED
Request 3  ──► @Async call ──► new Thread("task-3") started ──► executes ──► DESTROYED
...
Request N  ──► @Async call ──► new Thread("task-N") started ──► executes ──► DESTROYED

No thread is ever reused.
No limit on concurrent threads (unless setConcurrencyLimit() is set).
```

---

## 16. Why You Should NOT Use the Default `SimpleAsyncTaskExecutor`

The default `SimpleAsyncTaskExecutor` has four fundamental production problems. Each is explored below with diagrams and metrics.

---

### Problem 1: Thread Exhaustion

**What happens**: Every `@Async` call creates a brand-new OS thread. Under concurrent load, the number of threads grows unboundedly until the JVM or OS crashes.

```
Low traffic:
  10 requests/sec × @Async call = 10 new threads/sec
  Each task takes 2s → 20 threads alive at any moment
  (manageable)

High traffic:
  1000 requests/sec × @Async call = 1000 new threads/sec
  Each task takes 2s → 2000 threads alive simultaneously
  (dangerous — approaching OS limits)

Spike traffic:
  5000 requests/sec × @Async call = 5000 new threads/sec
  OS thread limit (~32768 on Linux) exceeded
  → JVM throws: java.lang.OutOfMemoryError: unable to create native thread
  → APPLICATION CRASHES
```

```
┌───────────────────────────────────────────────────────────────────┐
│              Thread Count Over Time (SimpleAsyncTaskExecutor)     │
│                                                                   │
│  Threads                                                          │
│    ▲                                                              │
│    │                                       ╔══════╗              │
│    │                              ╔═════════╝      ║ CRASH        │
│    │                    ╔══════════╝               ║              │
│    │           ╔════════╝                          ║              │
│  32K─ ─ ─ ─ ─ ╫─ ─ ─ ─ ─ ─ ─ ─ ─ ─ OS thread limit ─ ─ ─ ─   │
│    │           ║                                                  │
│    │  ╔════════╝                                                  │
│    │  ║                                                           │
│    └──╫───────────────────────────────────────────► time         │
│       │     │        │         │        │                         │
│       normal load  moderate   high    spike                      │
└───────────────────────────────────────────────────────────────────┘
```

**With `ThreadPoolTaskExecutor`**:
```
Max threads = corePoolSize + queue capacity (bounded)
Thread count stays flat at configured maximum regardless of traffic.
Excess tasks → queue → wait → or rejected with CallerRunsPolicy (backpressure)
```

---

### Problem 2: High Memory Usage

**What happens**: Each thread consumes stack memory. By default, each JVM platform thread has a **512KB–1MB stack** (configurable via `-Xss`). With `SimpleAsyncTaskExecutor`, threads multiply with traffic.

```
Memory per thread: ~1MB (stack) + thread metadata overhead

  100 threads  =  ~100 MB
  500 threads  =  ~500 MB
 1000 threads  =  ~  1 GB
 2000 threads  =  ~  2 GB   ← common under moderate load with slow tasks
 5000 threads  =  ~  5 GB   ← JVM likely crashes with OutOfMemoryError
```

```
┌──────────────────────────────────────────────────────────────────┐
│           Memory Comparison: Default vs Custom Executor          │
│                                                                  │
│  SimpleAsyncTaskExecutor (unbounded new threads):                │
│  ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐       │
│  │T-1 │T-2 │T-3 │T-4 │T-5 │T-6 │T-7 │T-8 │T-9 │T-10│ .. │      │
│  │1MB │1MB │1MB │1MB │1MB │1MB │1MB │1MB │1MB │1MB │ .. │      │
│  └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘       │
│         grows proportionally with every request!                 │
│                                                                  │
│  ThreadPoolTaskExecutor (fixed corePoolSize=10, max=20):         │
│  ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐            │
│  │T-1 │T-2 │T-3 │T-4 │T-5 │T-6 │T-7 │T-8 │T-9 │T-10│           │
│  │1MB │1MB │1MB │1MB │1MB │1MB │1MB │1MB │1MB │1MB │           │
│  └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘            │
│         flat ceiling — memory stays bounded regardless of load!  │
└──────────────────────────────────────────────────────────────────┘
```

---

### Problem 3: High Latency Due to Thread Creation Overhead

**What happens**: Creating a new OS thread is expensive. The JVM must ask the OS to allocate native memory for the thread stack, register the thread with the OS scheduler, and perform context setup. This overhead adds measurable latency to every `@Async` call.

```
Thread creation cost (approximate, depends on OS + JVM):
  Thread instantiation + OS allocation:  ~50µs – 200µs
  Stack memory allocation:               ~512KB – 1MB per thread
  OS scheduler registration:             overhead proportional to total thread count

With SimpleAsyncTaskExecutor:
  Every @Async call pays this cost EVERY TIME — no reuse.

With ThreadPoolTaskExecutor:
  Threads are created ONCE at startup (corePoolSize warm-up).
  Subsequent @Async calls just submit to the queue:
  → task.offer(queue)  ≈  nanoseconds
  → idle thread.take() ≈  nanoseconds
  → zero thread creation overhead!
```

```
┌──────────────────────────────────────────────────────────────────┐
│              @Async Call Latency Comparison (per call)           │
│                                                                  │
│  SimpleAsyncTaskExecutor:                                        │
│                                                                  │
│  call ──► [allocate stack 512KB] ──► [create OS thread] ──►     │
│           [OS scheduler register] ──► [context setup] ──►       │
│           [execute task] ──► [thread destroyed]                  │
│  Overhead per call: ~100–500µs  (just for thread setup!)         │
│                                                                  │
│  ThreadPoolTaskExecutor (thread already alive):                  │
│                                                                  │
│  call ──► [queue.offer(task)] ──► [idle thread wakes] ──►        │
│           [execute task]                                         │
│  Overhead per call: ~1–10µs    (queue operation only)            │
│                                                                  │
│  Latency ratio: 50x – 500x slower with SimpleAsyncTaskExecutor  │
└──────────────────────────────────────────────────────────────────┘
```

**Cumulative effect at scale**:

```
1000 @Async calls/second × 200µs thread creation overhead each
= 200ms of pure thread-creation overhead per second
= 200ms/s wasted on thread management instead of doing actual work

ThreadPoolTaskExecutor:
1000 @Async calls/second × 5µs queue overhead each
= 5ms of overhead per second
→ 40x less overhead
```

---

### Problem 4: Under-Utilization of Threads (No Reuse)

**What happens**: With `SimpleAsyncTaskExecutor`, a thread is created, runs one task, then is destroyed. The JVM's effort to create the thread is entirely wasted after one use. Idle time between task submission and task start is also higher because a cold thread must be created each time.

```
SimpleAsyncTaskExecutor — Thread lifecycle:
  ┌────────────────────────────────────────────────────────────────┐
  │  Request 1                                                     │
  │    new Thread() ──► [setup overhead] ──► run task ──► destroy  │
  │                                                                │
  │  Request 2                                                     │
  │    new Thread() ──► [setup overhead] ──► run task ──► destroy  │
  │                                                                │
  │  Each thread does ONE unit of work, then dies.                 │
  │  Thread utilization = task_duration / (task_duration + setup)  │
  │  E.g. task=100ms, setup=200µs → 99.8% "used"                  │
  │  BUT: no reuse means GC pressure from constant                 │
  │       Thread object creation and destruction                   │
  └────────────────────────────────────────────────────────────────┘

ThreadPoolTaskExecutor — Thread lifecycle:
  ┌────────────────────────────────────────────────────────────────┐
  │  Thread-1 created once:                                        │
  │    [setup] ──► task1 ──► task2 ──► task3 ──► task4 ──► ...    │
  │                │         │         │         │                 │
  │                ├─ idle ──┤         ├─ idle ──┤                 │
  │                                                                │
  │  Same thread handles thousands of tasks over its lifetime.     │
  │  Zero setup overhead after the first creation.                 │
  └────────────────────────────────────────────────────────────────┘
```

---

### Side-by-Side Comparison: `SimpleAsyncTaskExecutor` vs `ThreadPoolTaskExecutor`

| Characteristic | `SimpleAsyncTaskExecutor` (Default) | `ThreadPoolTaskExecutor` (Recommended) |
|----------------|-------------------------------------|----------------------------------------|
| **Thread creation** | New thread per `@Async` call | Pre-created, reused threads |
| **Thread limit** | Unbounded (no max by default) | Bounded by `maxPoolSize` |
| **Memory footprint** | Grows with request volume | Fixed ceiling |
| **Task submission latency** | ~100–500µs (thread creation cost) | ~1–10µs (queue enqueue) |
| **Thread reuse** | None — create, use once, discard | Yes — threads serve thousands of tasks |
| **Queue** | None | `ArrayBlockingQueue` (bounded) |
| **Backpressure** | None — creates threads until OOM | `CallerRunsPolicy` / `AbortPolicy` |
| **Production-safe** | No | Yes |
| **Thread Exhaustion risk** | High (crash under spike traffic) | None (bounded pool + queue) |
| **GC pressure** | High (constant Thread alloc/dealloc) | Low (threads live for the app lifetime) |
| **Graceful shutdown** | No | Yes (`setWaitForTasksToCompleteOnShutdown`) |

---

### Why Spring Chose `SimpleAsyncTaskExecutor` as the Default

Spring does not know your application's requirements — CPU count, task duration, expected throughput — at framework level. `SimpleAsyncTaskExecutor` is:

- **Zero-configuration** — works without any setup for demos and simple cases
- **Safe for correctness** — tasks always get a thread and execute
- **Explicit opt-in for pooling** — forces you to consciously configure a pool

But this choice is **explicitly called out in the Spring documentation** as unsuitable for production:

> *"By default, Spring will be searching for an associated thread pool definition: either a unique TaskExecutor bean in the context, or an Executor bean named "taskExecutor" otherwise. If neither of the two is resolvable, a SimpleAsyncTaskExecutor will be used to process async method invocations."*
> — Spring Framework Reference Documentation

---

### The Correct Production Configuration

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean("asyncExecutor")
    public Executor getAsyncExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // For I/O-bound async tasks (email, HTTP calls, DB writes):
        executor.setCorePoolSize(cpuCores * 2);   // always-alive threads
        executor.setMaxPoolSize(cpuCores * 4);    // burst capacity
        executor.setQueueCapacity(500);           // bounded queue — prevents OOM

        executor.setKeepAliveSeconds(60);         // idle non-core thread timeout
        executor.setThreadNamePrefix("App-Async-");

        // Backpressure: if pool + queue are full, caller runs the task itself
        // This slows the producer instead of dropping tasks or crashing
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Drain in-flight tasks before JVM shuts down
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
```

```
Runtime.getRuntime().availableProcessors() = 8 (example)

corePoolSize  = 16  (8 × 2)   — always warm, zero creation overhead
maxPoolSize   = 32  (8 × 4)   — burst headroom for traffic spikes
queueCapacity = 500           — up to 500 tasks can wait without new threads

Memory ceiling:
  32 threads × 1MB stack = 32MB  (fixed regardless of traffic)

vs SimpleAsyncTaskExecutor at 1000 concurrent tasks:
  1000 threads × 1MB = 1GB  (and growing!)
```

---

### Quick Decision Guide: Which Executor to Use

```
Starting a new Spring Boot app?
  │
  ├── Dev/testing only?
  │     └── SimpleAsyncTaskExecutor is fine (zero config)
  │
  └── Production?
        │
        ├── CPU-bound tasks (data processing, computation)?
        │     └── ThreadPoolTaskExecutor
        │           corePoolSize = CPU cores
        │           maxPoolSize  = CPU cores (no benefit adding more)
        │           queue        = ArrayBlockingQueue(200)
        │
        ├── I/O-bound tasks (email, REST calls, DB)?
        │     └── ThreadPoolTaskExecutor
        │           corePoolSize = CPU cores × 2
        │           maxPoolSize  = CPU cores × 4
        │           queue        = ArrayBlockingQueue(500)
        │
        └── Java 21+ (Virtual Threads)?
              └── SimpleAsyncTaskExecutor with setVirtualThreads(true)
                    — virtual threads are cheap to create, no pooling needed!
                    executor.setVirtualThreads(true);
```

#### Java 21+ Virtual Threads — The Exception to the Rule

```java
@Bean
public Executor asyncExecutor() {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
    executor.setVirtualThreads(true);   // backed by JVM virtual threads
    executor.setThreadNamePrefix("VThread-Async-");
    return executor;
    // Virtual threads are extremely cheap (~few KB each vs ~1MB for platform threads)
    // Creating millions is safe — JVM manages them on a small carrier thread pool
    // For Java 21+, this is actually the RECOMMENDED approach for I/O-bound work
}
```


---

## 17. Custom Executor Strategies for `@Async`

There are three distinct approaches to providing a custom executor for `@Async`. Each is covered in full detail below.

```
┌──────────────────────────────────────────────────────────────────────┐
│             Three Ways to Supply a Custom Executor to @Async         │
│                                                                      │
│  Approach 1: Custom Spring ThreadPoolTaskExecutor                    │
│    → Register as a @Bean, reference by name in @Async("beanName")   │
│    → OR register as the singleton "taskExecutor" bean (auto-picked)  │
│                                                                      │
│  Approach 2: Plain Java ThreadPoolExecutor                           │
│    → NOT auto-picked by Spring — must be wrapped or adapted          │
│    → Wrap with TaskExecutorAdapter → use as a @Bean                  │
│    → Then reference with @Async("beanName")                          │
│                                                                      │
│  Approach 3: AsyncConfigurer                                         │
│    → Implement AsyncConfigurer → override getAsyncExecutor()         │
│    → Becomes the global default executor for all @Async calls        │
│    → Also configure global exception handler here                    │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Approach 1 — Custom Spring `ThreadPoolTaskExecutor`

`ThreadPoolTaskExecutor` is Spring's own wrapper around Java's `ThreadPoolExecutor`. It is the **recommended production choice** because it:

- Integrates with Spring's lifecycle (`InitializingBean`, `DisposableBean`)
- Supports Spring's `TaskDecorator` for context propagation (MDC, security)
- Provides `setWaitForTasksToCompleteOnShutdown()` for graceful drain
- Exposes Spring-friendly setter API

#### Option A — Named Bean, Referenced Explicitly by `@Async`

Register the executor as a named bean, then pass that name to `@Async`:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailExecutor")           // ← bean name used in @Async
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("Email-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    @Bean(name = "reportExecutor")          // ← second named pool
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Report-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.AbortPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

Use in services:

```java
@Service
public class EmailService {

    @Async("emailExecutor")                 // ← matches bean name
    public void sendEmail(String to) {
        System.out.println("Running on: " + Thread.currentThread().getName());
        // Thread name: Email-1, Email-2, ...
    }
}

@Service
public class ReportService {

    @Async("reportExecutor")               // ← different pool
    public CompletableFuture<byte[]> generatePdf(String reportId) {
        System.out.println("Running on: " + Thread.currentThread().getName());
        // Thread name: Report-1, Report-2, ...
        return CompletableFuture.completedFuture(new byte[0]);
    }
}
```

```
EmailService.sendEmail()     → Email-1  (email pool, 3–6 threads)
ReportService.generatePdf() → Report-1 (report pool, 5–10 threads)

Pools are completely isolated — a report backlog does NOT starve emails.
```

#### Option B — Bean Named `"taskExecutor"` (Spring's Auto-Discovery)

Spring auto-discovers an executor bean named exactly `"taskExecutor"` and uses it as the default for bare `@Async` (no qualifier). You get the custom pool without specifying the name everywhere:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")            // ← Spring auto-discovers this name
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("Task-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

```java
@Service
public class NotificationService {

    @Async                                  // ← no name — auto-resolves to "taskExecutor"
    public void notify(String userId) {
        System.out.println("Running on: " + Thread.currentThread().getName());
        // Thread name: Task-1, Task-2, ...
    }
}
```

**Spring's resolution order for bare `@Async`:**

```
@Async (no qualifier)
    │
    ├─ 1. Look for bean named "taskExecutor" in context  ◄── Option B
    │       if found → use it
    │
    ├─ 2. Look for a unique TaskExecutor/Executor bean
    │       if exactly one exists → use it
    │
    ├─ 3. AsyncConfigurer.getAsyncExecutor() if configured ◄── Approach 3
    │
    └─ 4. Fall back to SimpleAsyncTaskExecutor            ← dangerous default
```

#### Adding a `TaskDecorator` for Context Propagation

`TaskDecorator` lets you wrap each submitted `Runnable` — use it to copy `ThreadLocal` values (MDC, request attributes, security context) from the submitting thread to the worker thread:

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(300);
    executor.setThreadNamePrefix("Task-");

    executor.setTaskDecorator(runnable -> {
        // Capture context from the submitting thread (HTTP thread)
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        RequestAttributes requestAttributes =
            RequestContextHolder.getRequestAttributes();

        return () -> {
            try {
                // Restore captured context on the worker thread
                if (mdcContext != null) MDC.setContextMap(mdcContext);
                if (requestAttributes != null)
                    RequestContextHolder.setRequestAttributes(requestAttributes);

                runnable.run();
            } finally {
                // Always clean up thread-local state
                MDC.clear();
                RequestContextHolder.resetRequestAttributes();
            }
        };
    });

    executor.initialize();
    return executor;
}
```

```
HTTP Thread (exec-1):              Worker Thread (Task-1):
  MDC: {traceId: "abc-123"}    →    MDC: {traceId: "abc-123"}  ← copied!
  RequestAttributes: present    →    RequestAttributes: present  ← copied!

Without TaskDecorator:
  MDC: {traceId: "abc-123"}
  Worker Thread → MDC: {}       ← empty! trace context is lost
```

---

### Approach 2 — Plain Java `ThreadPoolExecutor` with `@Async`

#### Will a Raw Java `ThreadPoolExecutor` Be Auto-Picked by `@Async`?

**No.** Spring's `@Async` resolution looks for beans of type `TaskExecutor` or `Executor` (Spring's `org.springframework.core.task.Executor`) — but only in specific ways. A raw `java.util.concurrent.ThreadPoolExecutor` bean **will be picked up only if it is the unique `Executor` bean or named `"taskExecutor"`**, because `ThreadPoolExecutor` implements `java.util.concurrent.Executor` which Spring can adapt.

However, if you register it as a generic bean without giving it the right name or interface, Spring may not pick it up — and you lose features like `TaskDecorator` and lifecycle management.

**The correct way** is to wrap it in a `TaskExecutorAdapter`:

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   java.util.concurrent.ThreadPoolExecutor                        │
│            │                                                     │
│            │  wrapped by                                         │
│            ▼                                                     │
│   TaskExecutorAdapter                                            │
│   (implements AsyncTaskExecutor → TaskExecutor → Executor)       │
│            │                                                     │
│            │  registered as @Bean                                │
│            ▼                                                     │
│   Spring Context — usable with @Async("beanName")                │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

#### Example: Wrapping a Java `ThreadPoolExecutor` with `TaskExecutorAdapter`

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableAsync
public class JavaExecutorConfig {

    @Bean(name = "customJavaExecutor")
    public TaskExecutorAdapter customJavaExecutor() {

        // Step 1: Build a custom ThreadFactory with meaningful thread names
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r,
                    "JavaPool-Worker-" + counter.getAndIncrement());
                t.setDaemon(false);            // non-daemon — JVM waits for it
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };

        // Step 2: Build a fully configured Java ThreadPoolExecutor
        ThreadPoolExecutor javaExecutor = new ThreadPoolExecutor(
            4,                                  // corePoolSize
            8,                                  // maximumPoolSize
            60L, TimeUnit.SECONDS,              // keepAliveTime for non-core threads
            new ArrayBlockingQueue<>(100),      // bounded queue — prevents OOM
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()  // back-pressure policy
        );

        // Optional: allow core threads to timeout too (useful for low-traffic apps)
        javaExecutor.allowCoreThreadTimeOut(true);

        // Step 3: Wrap in TaskExecutorAdapter to make it Spring-compatible
        return new TaskExecutorAdapter(javaExecutor);
    }
}
```

Use it just like a Spring executor:

```java
@Service
public class DataService {

    @Async("customJavaExecutor")
    public CompletableFuture<String> processData(String input) {
        System.out.println("Running on: " + Thread.currentThread().getName());
        // Thread name: JavaPool-Worker-1, JavaPool-Worker-2, ...
        return CompletableFuture.completedFuture("Processed: " + input);
    }
}
```

#### Alternative: Wrapping with `ConcurrentTaskExecutor`

`ConcurrentTaskExecutor` is Spring's other adapter — functionally similar to `TaskExecutorAdapter` but slightly more integrated with Spring's scheduling infrastructure:

```java
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

@Bean(name = "concurrentJavaExecutor")
public ConcurrentTaskExecutor concurrentJavaExecutor() {
    ThreadPoolExecutor javaExecutor = new ThreadPoolExecutor(
        4, 8,
        60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100),
        Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    return new ConcurrentTaskExecutor(javaExecutor);
}
```

#### `TaskExecutorAdapter` vs `ConcurrentTaskExecutor`

| Feature | `TaskExecutorAdapter` | `ConcurrentTaskExecutor` |
|---------|----------------------|--------------------------|
| Wraps `java.util.concurrent.Executor` | Yes | Yes |
| Returns `Future` from `submit()` | Yes | Yes |
| Spring lifecycle management | No (you manage shutdown) | Partial |
| Scheduling support (`@Scheduled`) | No | Yes |
| Most common use case | Bridging Java → Spring for `@Async` | Bridging for both `@Async` + scheduling |

#### Why Not Just Use `ExecutorService` Directly as a `@Bean`?

```java
// ❌ This works but is NOT recommended
@Bean(name = "taskExecutor")
public ExecutorService taskExecutor() {
    return new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100));
}
```

Problems with this approach:
- No `TaskDecorator` support — cannot propagate MDC / security context
- No `setWaitForTasksToCompleteOnShutdown` — tasks may be killed on shutdown
- Spring's `@Async` must cast it, which may fail depending on the Spring version
- No thread name prefix control

**Always wrap Java executors** via `TaskExecutorAdapter` or `ConcurrentTaskExecutor`.

---

### Approach 3 — `AsyncConfigurer` for Global Default Executor

`AsyncConfigurer` is an interface that lets you provide **one centralized, global configuration** for all `@Async` calls that have no explicit qualifier. It is the cleanest way to:

1. Set the **application-wide default executor** for bare `@Async` methods
2. Set the **application-wide exception handler** for `void @Async` methods

```java
// org.springframework.scheduling.annotation.AsyncConfigurer
public interface AsyncConfigurer {
    @Nullable
    default Executor getAsyncExecutor() { return null; }

    @Nullable
    default AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() { return null; }
}
```

#### Full `AsyncConfigurer` Implementation with Spring `ThreadPoolTaskExecutor`

```java
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class GlobalAsyncConfig implements AsyncConfigurer {

    /**
     * This executor becomes the DEFAULT for all @Async methods
     * that do not specify a qualifier name.
     *
     * Also exposed as a bean so other components can inject it directly.
     */
    @Override
    @Bean("globalAsyncExecutor")
    public Executor getAsyncExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cpuCores * 2);      // I/O-bound baseline
        executor.setMaxPoolSize(cpuCores * 4);       // burst capacity
        executor.setQueueCapacity(500);              // bounded — no OOM
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Global-Async-");

        // Back-pressure: slow the caller, don't drop tasks
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Graceful shutdown: drain in-flight tasks before JVM exits
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Global handler for exceptions thrown inside void @Async methods.
     * Without this, exceptions are silently swallowed.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    // --- Inner exception handler class ---
    static class CustomAsyncExceptionHandler
            implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(
                Throwable ex, Method method, Object... params) {

            System.err.printf(
                "[ASYNC ERROR] Class: %s | Method: %s | Params: %s%n",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params)
            );
            System.err.printf("[ASYNC ERROR] Exception: %s: %s%n",
                ex.getClass().getSimpleName(), ex.getMessage());

            // Production: alert monitoring (Datadog, PagerDuty, Sentry, etc.)
            // alertingService.sendAlert("Async failure: " + method.getName(), ex);
        }
    }
}
```

#### Using the Global Default

```java
@Service
public class EmailService {

    @Async                   // no qualifier → uses GlobalAsyncConfig.getAsyncExecutor()
    public void sendEmail(String to) {
        // Runs on Global-Async-1, Global-Async-2, ...
        System.out.println("Thread: " + Thread.currentThread().getName());
        throw new RuntimeException("SMTP timeout"); // caught by CustomAsyncExceptionHandler
    }
}

@Service
public class SmsService {

    @Async                   // also uses the global default
    public void sendSms(String phone) {
        System.out.println("Thread: " + Thread.currentThread().getName());
    }
}
```

#### `AsyncConfigurer` + Named Beans for Mixed Strategy

The most flexible production setup: a global default via `AsyncConfigurer` plus additional named pools for specialized workloads:

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // ── Global default (for plain @Async) ───────────────────────────────────
    @Override
    @Bean("taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Default-Async-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    // ── Specialized pool for email ───────────────────────────────────────────
    @Bean("emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Email-");
        executor.initialize();
        return executor;
    }

    // ── Specialized pool for heavy reports (isolated from email) ─────────────
    @Bean("reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("Report-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.AbortPolicy()
        );
        executor.initialize();
        return executor;
    }

    // ── Global exception handler ─────────────────────────────────────────────
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            System.err.printf("[ASYNC ERROR] %s.%s(%s): %s%n",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params),
                ex.getMessage());
    }
}
```

Services pick the right pool per task type:

```java
@Service
public class UserService {

    @Async                           // → Default-Async-* pool (general tasks)
    public void updateLastLogin(Long userId) { ... }
}

@Service
public class EmailService {

    @Async("emailExecutor")          // → Email-* pool (light, fast tasks)
    public void sendWelcomeEmail(String to) { ... }
}

@Service
public class ReportService {

    @Async("reportExecutor")         // → Report-* pool (heavy, slow tasks, isolated)
    public CompletableFuture<byte[]> generateMonthlyReport(int month) { ... }
}
```

```
Request → UserService.updateLastLogin()    → Default-Async-1   (general pool)
Request → EmailService.sendWelcomeEmail()  → Email-1           (email pool)
Request → ReportService.generateReport()   → Report-1          (report pool)

Report pool saturation does NOT affect email pool.
Email pool saturation does NOT affect general pool.
Isolation is complete.
```

---

### How Spring Resolves the Executor — Complete Decision Tree

```
@Async("qualifier")   ─────────────────────────────────────────────────┐
                                                                        │
  ┌─────────────────────────────────────────────────────────────────── │
  │  Has qualifier string?                                              │
  │       YES                                                           │
  │       └─ Look up bean by name/qualifier in ApplicationContext       │
  │            Found? → use it                                          │
  │            Not found? → throw NoSuchBeanDefinitionException         │
  └─────────────────────────────────────────────────────────────────────
                        │
  @Async (no qualifier) │
                        ▼
  ┌─────────────────────────────────────────────────────────────────────
  │  AsyncConfigurer.getAsyncExecutor() configured?                     │
  │       YES → use that executor as the global default                 │
  └─────────────────────────────────────────────────────────────────────
                        │
                        ▼
  ┌─────────────────────────────────────────────────────────────────────
  │  Is there a bean named "taskExecutor" in the context?               │
  │       YES → use it                                                  │
  └─────────────────────────────────────────────────────────────────────
                        │
                        ▼
  ┌─────────────────────────────────────────────────────────────────────
  │  Is there exactly ONE TaskExecutor/Executor bean in the context?    │
  │       YES → use it                                                  │
  └─────────────────────────────────────────────────────────────────────
                        │
                        ▼
  ┌─────────────────────────────────────────────────────────────────────
  │  FALLBACK: SimpleAsyncTaskExecutor                                  │
  │    ← new thread per task, no pooling, no back-pressure!             │
  └─────────────────────────────────────────────────────────────────────
```

---

### Complete Comparison of All Three Approaches

| | Approach 1A (Named Bean) | Approach 1B (`"taskExecutor"` bean) | Approach 2 (Java Executor + Adapter) | Approach 3 (`AsyncConfigurer`) |
|---|---|---|---|---|
| **How selected** | `@Async("beanName")` | `@Async` (auto by name) | `@Async("beanName")` | `@Async` (global default) |
| **Spring lifecycle** | Yes | Yes | Partial (via adapter) | Yes |
| **TaskDecorator support** | Yes | Yes | No (adapter doesn't wrap) | Yes |
| **Multiple pools** | Yes (many beans) | One pool only | Yes (many adapters) | Mix of both |
| **Exception handler** | No (per method) | No | No | Yes (global) |
| **Best for** | Per-service isolated pools | Single default pool | Reusing existing Java executor | App-wide default + exception handling |

---

### Quick Setup Guide: Which Approach to Choose

```
New Spring Boot app from scratch?
  └── Approach 3 (AsyncConfigurer) for the global default
       + Approach 1A (named beans) for specialized pools
       This gives you centralized exception handling AND per-use-case isolation.

Already have a Java ThreadPoolExecutor (from a library or legacy code)?
  └── Approach 2: wrap it with TaskExecutorAdapter, register as @Bean
       Use @Async("beanName") to reference it.

Simple app, one pool for everything?
  └── Approach 1B: register one @Bean named "taskExecutor"
       All plain @Async calls will auto-resolve to it.
       Zero-boilerplate for callers.
```

---

## 18. Conditions for `@Async` to Work — Root Causes and AOP Internals

### Why `@Async` Has Strict Conditions

`@Async` is implemented using **Spring AOP (Aspect-Oriented Programming)**. It does NOT modify your class's bytecode. Instead, Spring wraps your bean object in a **proxy object** at startup. Every call that goes through the proxy is intercepted; every call that bypasses the proxy runs normally — synchronously.

Understanding this one fact explains **all** conditions for `@Async` to work.

```
┌────────────────────────────────────────────────────────────────────────┐
│                  How AOP Proxy Works for @Async                        │
│                                                                        │
│   Spring Application Context                                           │
│                                                                        │
│   ┌─────────────────────────────────────────────────────────────┐     │
│   │                                                             │     │
│   │   emailService bean ──────────────────────────────────────  │     │
│   │              │                                             │     │
│   │              │  Spring creates a PROXY                     │     │
│   │              ▼                                             │     │
│   │   ┌─────────────────────────────────────┐                  │     │
│   │   │    EmailServiceProxy (CGLIB)         │                  │     │
│   │   │                                     │                  │     │
│   │   │  sendEmail(to) {                    │                  │     │
│   │   │    // intercept!                    │                  │     │
│   │   │    executor.submit(() -> {          │                  │     │
│   │   │      realBean.sendEmail(to);        │                  │     │
│   │   │    });                              │                  │     │
│   │   │    return;  // immediately!         │                  │     │
│   │   │  }                                  │                  │     │
│   │   └─────────────────────────────────────┘                  │     │
│   │              │                                             │     │
│   │              │  context stores the PROXY, not the real bean│     │
│   │                                                             │     │
│   └─────────────────────────────────────────────────────────────┘     │
│                                                                        │
│   When caller does:  emailService.sendEmail("x@y.com")                │
│     → hits the PROXY → intercepted → background thread                │
│                                                                        │
│   When EmailService does:  this.sendEmail("x@y.com")                  │
│     → hits the REAL object → NOT intercepted → synchronous!           │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

### Condition 1: `@Async` Must Be Called from a Different Spring Bean

**Root cause**: The proxy only intercepts calls **from outside the bean**. When a method inside the same class calls another method in the same class, it uses `this` — a direct reference to the real object, bypassing the proxy entirely.

#### What Goes Wrong with Same-Class Calls

```java
@Service
public class OrderService {

    @Async                            // ← @Async declared here
    public void sendConfirmation(Long orderId) {
        System.out.println("Thread: " + Thread.currentThread().getName());
        // Expected: runs on a background thread
        // Actual:   runs on the SAME thread as the caller!
    }

    @Transactional
    public void placeOrder(OrderRequest request) {
        Order saved = orderRepository.save(new Order(request));

        this.sendConfirmation(saved.getId()); // ← THIS IS THE PROBLEM!
        // 'this' = the real OrderService object, NOT the proxy
        // The call never reaches the proxy → @Async is ignored
    }
}
```

#### Tracing the Byte-Level Call Path

```
Caller (HTTP thread):
  orderService.placeOrder(request)
       │
       ▼ (goes through proxy — @Transactional IS applied)
  OrderServiceProxy.placeOrder(request)
       │
       ▼ opens transaction, calls real method
  OrderService (real object).placeOrder(request)
       │
       │  this.sendConfirmation(id)   ← 'this' = real OrderService object
       │                                 skips the proxy!
       ▼
  OrderService (real object).sendConfirmation(id)
       │
       ▼ executes on the HTTP thread — @Async completely ignored!
```

#### The Proxy Intercept Path (Correct Cross-Bean Call)

```
Caller:
  orderService.placeOrder(request)   ← through proxy (works: @Transactional)
       │
       ▼
  notificationService.sendConfirmation(id)  ← through a DIFFERENT proxy (works: @Async)
       │
       ▼
  Background thread executes sendConfirmation()
```

#### Fix: Move `@Async` Methods to a Separate Bean

```java
@Service
public class OrderService {

    @Autowired
    private NotificationService notificationService; // ← injected PROXY

    @Transactional
    public void placeOrder(OrderRequest request) {
        Order saved = orderRepository.save(new Order(request));

        notificationService.sendConfirmation(saved.getId()); // ← goes through proxy ✅
    }
}

@Service
public class NotificationService {

    @Async                           // ← works: called through proxy from another bean
    public void sendConfirmation(Long orderId) {
        System.out.println("Thread: " + Thread.currentThread().getName());
        // Runs on a background thread as expected!
    }
}
```

#### Alternative Fix: Self-Injection (Not Recommended)

If refactoring to a separate class is impractical, you can inject a reference to the bean's own proxy:

```java
@Service
public class OrderService implements ApplicationContextAware {

    @Autowired
    private ApplicationContext applicationContext;

    @Async
    public void sendConfirmation(Long orderId) { ... }

    public void placeOrder(OrderRequest request) {
        Order saved = orderRepository.save(new Order(request));

        // Get the proxy of this bean from the context
        OrderService proxy = applicationContext.getBean(OrderService.class);
        proxy.sendConfirmation(saved.getId()); // ← goes through proxy ✅
    }
}
```

> This works but is a code smell. The correct solution is always a separate bean.

---

### Condition 2: The `@Async` Method Must Be `public`

**Root cause**: Spring's default proxy mechanism uses **CGLIB** (or JDK dynamic proxies for interfaces). Both can only override `public` (and `protected` for CGLIB) methods. Private methods are inaccessible to the subclass that CGLIB generates; they are final to the proxy.

#### CGLIB Proxy Mechanics

```
Your class:                    CGLIB-generated proxy (runtime):
┌────────────────────┐         ┌────────────────────────────────┐
│ EmailService       │         │ EmailService$$SpringCGLIB$$0   │
│                    │         │   extends EmailService         │
│ + sendEmail()      │ ──────► │ + sendEmail() {                │
│   (public)         │  can    │     // intercept → async!      │
│                    │ override│   }                            │
│ - processEmail()   │         │                                │
│   (private)        │ ✗ cannot│ - processEmail()               │
│                    │ override│   // CANNOT override private   │
└────────────────────┘         └────────────────────────────────┘
```

```java
@Service
public class EmailService {

    // ✅ Works — CGLIB can override and intercept
    @Async
    public void sendEmail(String to) { ... }

    // ✅ Works — CGLIB can override protected methods
    @Async
    protected void processEmail(String to) { ... }

    // ❌ Silent failure — CGLIB cannot override, @Async ignored
    @Async
    private void queueEmail(String to) { ... }

    // ❌ Silent failure — final methods cannot be overridden
    @Async
    public final void auditEmail(String to) { ... }
}
```

**Why it fails silently**: Spring's `AsyncAnnotationBeanPostProcessor` logs a warning but does not throw an exception. The method simply runs synchronously.

---

### Condition 3: `@EnableAsync` Must Be Present

Without `@EnableAsync`, no `AsyncAnnotationBeanPostProcessor` is registered, no proxies are created, and `@Async` is just an unused marker annotation. All calls execute synchronously.

```java
// ❌ Broken — proxy mechanism never activated
@Configuration
public class AppConfig { }

// ✅ Correct
@Configuration
@EnableAsync
public class AppConfig { }

// ✅ Also correct — on the main class
@SpringBootApplication
@EnableAsync
public class MyApp { public static void main(String[] args) { ... } }
```

---

### Condition 4: Only One `@EnableAsync` Should Exist

If `@EnableAsync` is declared on multiple configuration classes, Spring creates multiple `AsyncAnnotationBeanPostProcessor` instances. This is usually harmless but can cause unexpected behavior with executor resolution. Keep it in one place.

---

### Complete Conditions Summary

```
┌──────────────────────────────────────────────────────────────────────┐
│             @Async Conditions — All or Nothing                        │
│                                                                      │
│  ✅ MUST HAVE:                                                        │
│     1. @EnableAsync on a @Configuration class                        │
│     2. @Async on a public method (protected also works with CGLIB)   │
│     3. Call must come from a DIFFERENT Spring bean (via proxy)        │
│     4. The class must be a Spring-managed bean (@Service, @Component) │
│                                                                      │
│  ❌ WILL NOT WORK:                                                    │
│     • @Async on private methods (silently ignored)                   │
│     • @Async on final methods (CGLIB cannot override)                │
│     • this.asyncMethod() — self-invocation bypasses proxy            │
│     • new MyService().asyncMethod() — not a Spring proxy             │
│     • @Async on static methods (no instance to proxy)                │
│     • Missing @EnableAsync — annotation is no-op                     │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 19. `@Async` and `@Transactional` — Interactions and Challenges

This is one of the most nuanced and frequently misunderstood areas of Spring. The core reason for all the complexity is a single fact:

> **Spring's transaction context is stored in `ThreadLocal`. When `@Async` spawns a new thread, `ThreadLocal` is NOT copied. Every `@Async` thread has its own isolated transaction context.**

```
┌───────────────────────────────────────────────────────────────────────┐
│                  ThreadLocal — The Root of All Issues                  │
│                                                                       │
│  HTTP Thread (exec-1):                                                │
│    ThreadLocal {                                                      │
│      TransactionSynchronizationManager.currentTransactionStatus       │
│        → Connection: conn-42, isolation: READ_COMMITTED               │
│        → TX active: true, read-only: false                            │
│    }                                                                  │
│                                                                       │
│  Async Thread (Async-1):  ← spawned by @Async                        │
│    ThreadLocal {                                                       │
│      TransactionSynchronizationManager.currentTransactionStatus       │
│        → (empty — new thread starts with NO transaction context)      │
│    }                                                                  │
│                                                                       │
│  These two ThreadLocals are COMPLETELY SEPARATE.                      │
│  No data flows from HTTP thread's TL to Async thread's TL.           │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

### Scenario 1: Calling `@Async` from a `@Transactional` Method

**Question**: Does the transaction from the calling method propagate to the async method?

**Answer: NO.** The `@Async` method runs on a new thread with no transaction context.

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationService notificationService;

    @Transactional
    public void placeOrder(OrderRequest request) {
        // TX is ACTIVE here — HTTP thread's ThreadLocal holds the connection

        Order order = orderRepository.save(new Order(request));
        // order is saved but NOT yet committed (tx still open)

        notificationService.sendConfirmation(order.getId());
        // ↑ This call passes order.getId() to a NEW THREAD
        // The new thread has NO access to the open transaction
        // The order may NOT be visible in the DB yet (not committed!)

        // Transaction commits HERE — only after this method returns
    }
}

@Service
public class NotificationService {

    @Autowired private OrderRepository orderRepository;

    @Async
    public void sendConfirmation(Long orderId) {
        // Running on a NEW thread — NO transaction context from caller!
        // BIG PROBLEM: the order may not be committed yet!

        Order order = orderRepository.findById(orderId).orElseThrow();
        // ↑ This may throw NoSuchElementException or return stale data
        // because the caller's transaction hasn't committed yet!

        System.out.println("Sending confirmation for: " + order.getId());
    }
}
```

```
Timeline Diagram:

HTTP Thread (exec-1):                 Async Thread (Async-1):
─────────────────────                 ────────────────────────
TX BEGIN
  └─ save order (id=42)
  └─ call notificationService.send()
     │                                ← spawned → sendConfirmation(42)
     │                                   findById(42) ← PROBLEM!
     │                                   Row not visible: TX not committed
     │                                   → throws or returns empty
  └─ [main method returns]
TX COMMIT ← order 42 becomes visible
                                       (async thread may have already failed!)
```

#### The Classic Race Condition

```
Best case:  Async thread starts AFTER the TX commits → works fine
Worst case: Async thread reads before TX commits → NoSuchElementException

This is non-deterministic — depends on thread scheduling!
Works in dev (low load), fails in prod (high concurrency).
```

#### Fix: Use `@TransactionalEventListener` Instead of Direct `@Async` Call

The correct pattern is to publish an event AFTER the transaction commits:

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void placeOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));

        // Publish event — it will fire AFTER the transaction commits
        eventPublisher.publishEvent(new OrderPlacedEvent(order.getId()));

        // Transaction commits here — order is now visible in DB
    }
}

// Event class
public record OrderPlacedEvent(Long orderId) {}

// Listener fires AFTER TX commit
@Service
public class NotificationService {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // Guaranteed: called ONLY after the transaction has committed
        // The order IS visible in the DB at this point!
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        System.out.println("Order visible: " + order.getId());
        sendConfirmation(order);
    }
}
```

```
Timeline with @TransactionalEventListener:

HTTP Thread (exec-1):
  TX BEGIN
    save order (id=42)
    publish OrderPlacedEvent
                             ← event queued, not yet fired!
  TX COMMIT ← order committed, visible to all
    → AFTER_COMMIT fires → submits to @Async executor

Async Thread (Async-1):
    handleOrderPlaced(42)
    findById(42) → ✅ FOUND! TX is committed, row is visible.
```

---

### Scenario 2: `@Transactional` and `@Async` on the Same Method

**Question**: If you put both annotations on the same method, what happens?

**Answer**: Both annotations work, but they apply to different things — and the behaviour is often misunderstood.

```java
@Service
public class ReportService {

    @Async          // ← submits to thread pool, caller returns immediately
    @Transactional  // ← wraps the ASYNC execution in a NEW transaction
    public CompletableFuture<String> generateReport(Long reportId) {
        // This runs on a background thread (Async-1)
        // A BRAND NEW transaction is started on Async-1's ThreadLocal
        // The CALLER's transaction (if any) is NOT available here

        Report report = reportRepository.findById(reportId).orElseThrow();
        report.setStatus("GENERATING");
        reportRepository.save(report);  // inside Async-1's own TX

        String result = compute(report);

        report.setStatus("DONE");
        reportRepository.save(report);  // same TX

        return CompletableFuture.completedFuture(result);
        // TX commits here — on Async-1, not on the caller's thread
    }
}
```

```
┌────────────────────────────────────────────────────────────────────┐
│       @Async + @Transactional on the Same Method                   │
│                                                                    │
│  HTTP Thread (exec-1):                                             │
│    call generateReport(1)                                          │
│         │                                                          │
│         ▼ (goes through proxy)                                     │
│    Step 1 (@Async proxy): submit task to executor, return CF       │
│         │                                                          │
│         ▼ RETURNS to caller immediately                            │
│    HTTP thread is FREE                                             │
│                                                                    │
│  Async Thread (Async-1):                                           │
│    Step 2 (@Transactional proxy): BEGIN new TX on Async-1          │
│         │                                                          │
│         ▼                                                          │
│    Real method body executes (with fresh TX)                       │
│         │                                                          │
│         ▼                                                          │
│    Step 3: TX COMMITS on Async-1                                   │
│         │                                                          │
│         ▼ (any rollback also happens on Async-1)                   │
└────────────────────────────────────────────────────────────────────┘
```

#### Proxy Order Matters

Both `@Async` and `@Transactional` use AOP proxies. The order in which they are applied determines which proxy wraps which:

```
Default proxy order (Spring Boot):
  Outer proxy: @Async (AsyncAnnotationBeanPostProcessor, order = Ordered.LOWEST_PRECEDENCE - 2)
  Inner proxy: @Transactional (TransactionInterceptor)

Call flow:
  caller → [Async proxy: submit to executor, return] → [TX proxy: BEGIN TX] → real method → [TX: COMMIT]

The @Async proxy fires first (submits the task), then inside the background thread the @Transactional proxy fires (opens a new TX).
```

#### Key Points About `@Async + @Transactional` on Same Method

| Aspect | Behaviour |
|--------|-----------|
| Caller's transaction | NOT propagated — async thread gets its own |
| Transaction isolation | Fresh TX, default propagation (`REQUIRED`) |
| TX commits | On the async thread when the method returns |
| Rollback | Only on the async thread's TX — caller's TX is unaffected |
| LazyLoadingException | Possible if accessing lazy collections after the TX scope |
| Return type | Must be `CompletableFuture<T>` to get the result; `void` is fire-and-forget |

---

### Scenario 3: Calling `@Transactional` from an `@Async` Method

**Question**: What happens when an `@Async` method calls another method annotated with `@Transactional`?

**Answer**: This works correctly and is the **recommended pattern**. The `@Async` method runs on a background thread. When it calls a `@Transactional` method on a different bean, a new transaction is started on that background thread.

```java
@Service
public class ReportProcessor {

    @Autowired private ReportWriter reportWriter;  // different bean

    @Async
    public void processReport(Long reportId) {
        // Running on background thread Async-1
        // No transaction active here (unless we start one)

        List<String> data = fetchExternalData(reportId); // no TX needed

        reportWriter.saveReport(reportId, data); // ← calls @Transactional on different bean ✅
    }
}

@Service
public class ReportWriter {

    @Autowired private ReportRepository reportRepository;

    @Transactional
    public void saveReport(Long reportId, List<String> data) {
        // New TX started on the ASYNC THREAD (Async-1)
        // This TX is ISOLATED — rolls back only if this method throws

        Report report = new Report(reportId, data);
        reportRepository.save(report);

        // TX commits here — data is persisted
    }
}
```

```
Timeline:

Async Thread (Async-1):
  processReport(1)               ← @Async, no TX
      │
      ├─ fetchExternalData()     ← no TX, just HTTP call
      │
      └─ reportWriter.saveReport() ─────────────────────────────────┐
           │                                                         │
           ▼ (through @Transactional proxy)                          │
           TX BEGIN on Async-1                                       │
             save Report to DB                                       │
           TX COMMIT on Async-1 ─────────────────────────────────────┘
```

#### Propagation Behaviour Table

When an `@Async` method calls `@Transactional` methods, propagation rules apply just as in normal synchronous code, but relative to the **async thread's** transaction context:

| Propagation | Behaviour from `@Async` thread |
|-------------|-------------------------------|
| `REQUIRED` (default) | No active TX on async thread → starts a NEW TX |
| `REQUIRES_NEW` | Always starts a new TX (suspends current if one exists) |
| `MANDATORY` | **Throws** `IllegalTransactionStateException` — no active TX on async thread |
| `SUPPORTS` | No TX on async thread → runs without TX |
| `NOT_SUPPORTED` | Suspends any active TX and runs without TX |
| `NEVER` | No TX active on async thread → runs normally |

```java
// ❌ This will throw IllegalTransactionStateException
@Service
public class StrictService {

    @Transactional(propagation = Propagation.MANDATORY)
    public void doWork() {
        // Expects an ACTIVE transaction to already exist
        // But @Async threads start with NO transaction → THROWS!
    }
}

@Service
public class AsyncCaller {

    @Autowired private StrictService strictService;

    @Async
    public void run() {
        strictService.doWork(); // ← IllegalTransactionStateException!
        // No TX on this async thread, but MANDATORY requires one
    }
}
```

---

### Complete Decision Guide: `@Async` + Transactions

```
Need to do DB work in an @Async method?
  │
  ├── Simple DB write/read that doesn't depend on caller's TX?
  │     └── @Async method calls @Transactional on a different bean ✅
  │           Each DB operation gets its own clean transaction.
  │
  ├── Need atomic DB operations in the async task itself?
  │     └── Put @Transactional ON the @Async method
  │           → async thread gets its own transaction
  │           → caller's TX is NOT involved
  │
  ├── Need to guarantee async method runs AFTER caller's TX commits?
  │     └── Use @TransactionalEventListener(AFTER_COMMIT) + @Async ✅
  │           Best pattern for fire-and-forget post-TX work
  │
  ├── Need to share data between caller's TX and async method?
  │     └── NOT possible with @Async — different threads, different TXs
  │           Consider CompletableFuture within same TX instead,
  │           or pass committed data as method arguments.
  │
  └── Calling @Transactional(propagation=MANDATORY) from @Async?
        └── ❌ Will throw — async thread has no active TX
              Fix: change propagation to REQUIRED or REQUIRES_NEW
```

---

### Real-World Example: Order Processing with Correct TX Handling

```java
// ─── Event ────────────────────────────────────────────────────────
public record OrderPlacedEvent(Long orderId, String customerEmail) {}

// ─── OrderService: transactional, publishes event after save ──────
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ApplicationEventPublisher events;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        // TX begins
        Order order = orderRepository.save(new Order(request));

        // Event queued — fires AFTER this TX commits
        events.publishEvent(
            new OrderPlacedEvent(order.getId(), request.customerEmail())
        );

        return order;
        // TX commits here → order row is now visible to all threads
    }
}

// ─── NotificationService: async, runs after TX commit ─────────────
@Service
public class NotificationService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private EmailClient emailClient;
    @Autowired private InventoryService inventoryService;

    @Async("orderExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        // ✅ Guaranteed: caller's TX is committed
        // ✅ Runs on a background thread (no blocking the HTTP thread)

        // This @Async method starts with NO active transaction
        // Each sub-call manages its own TX

        sendEmail(event.customerEmail());              // no TX needed
        inventoryService.deductStock(event.orderId()); // @Transactional — new TX
        generateInvoice(event.orderId());              // @Transactional — new TX
    }

    private void sendEmail(String to) {
        emailClient.send(to, "Order Confirmed", "...");
    }

    private void generateInvoice(Long orderId) {
        // Delegates to @Transactional bean
    }
}

// ─── InventoryService: own transaction, called from async ─────────
@Service
public class InventoryService {

    @Autowired private InventoryRepository inventoryRepository;

    @Transactional                    // fresh TX on the async thread
    public void deductStock(Long orderId) {
        // order IS committed → findById always works here
        // this TX is independent — its own commit and rollback
        Inventory inv = inventoryRepository.findByOrderId(orderId);
        inv.deduct(1);
        inventoryRepository.save(inv);
    }
}
```

```
┌─────────────────────────────────────────────────────────────────────┐
│               Full Flow with Correct TX Handling                    │
│                                                                     │
│  HTTP Thread (exec-1):                                              │
│    @Transactional placeOrder()                                      │
│    ├─ save Order (id=42) — not yet visible                          │
│    ├─ publish OrderPlacedEvent (queued, not yet fired)              │
│    └─ TX COMMIT → order visible in DB                               │
│         └─ AFTER_COMMIT fires                                       │
│              └─ submit to orderExecutor                             │
│                                                                     │
│  Async Thread (Order-1):                                            │
│    @Async onOrderPlaced(42)  — no active TX                         │
│    ├─ sendEmail()            — no TX                                │
│    ├─ inventoryService.deductStock(42)                              │
│    │    ├─ TX BEGIN on Order-1                                      │
│    │    ├─ deduct stock                                             │
│    │    └─ TX COMMIT on Order-1                                     │
│    └─ generateInvoice(42)                                           │
│         ├─ TX BEGIN on Order-1                                      │
│         ├─ create invoice                                           │
│         └─ TX COMMIT on Order-1                                     │
│                                                                     │
│  Result:                                                            │
│  ✅ HTTP responds in ~10ms (not 3000ms)                             │
│  ✅ No race condition — event fires after commit                    │
│  ✅ Each DB operation has its own clean TX                          │
│  ✅ Failures in async don't affect the HTTP response                │
└─────────────────────────────────────────────────────────────────────┘
```

---

### `@Async` + `@Transactional` Summary Table

| Scenario | What Happens | Recommended? |
|----------|-------------|--------------|
| `@Async` calls `@Transactional` (different bean) | New TX on async thread | ✅ Yes |
| `@Transactional` calls `@Async` (different bean) | Async gets NO TX from caller — race condition risk | ⚠️ Use `@TransactionalEventListener` |
| `@Async` + `@Transactional` on SAME method | Async submits task; within task, own TX is created | ✅ OK, understood |
| `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | Async fires only after caller's TX commits | ✅ Best pattern |
| Calling `MANDATORY` propagation from `@Async` | Throws `IllegalTransactionStateException` | ❌ Never do this |
| Lazy collection access in `@Async` after TX | `LazyInitializationException` | ❌ Load eagerly or in TX |

---

## 20. Returning Values from `@Async` Methods

An `@Async` method runs on a background thread. The calling thread returns immediately. So how does the caller get the result back? The return type of the method determines the answer.

Spring supports three return type families for `@Async` methods:

```
┌──────────────────────────────────────────────────────────────────────┐
│           Valid Return Types for @Async Methods                      │
│                                                                      │
│  void / Void                                                         │
│    → fire-and-forget: caller gets nothing back                       │
│    → exceptions go to AsyncUncaughtExceptionHandler                  │
│                                                                      │
│  Future<T>                (java.util.concurrent)                     │
│    → legacy; blocking .get(); wraps result in AsyncResult<T>         │
│                                                                      │
│  ListenableFuture<T>      (Spring — deprecated since 6.0)            │
│    → non-blocking callbacks; superseded by CompletableFuture         │
│                                                                      │
│  CompletableFuture<T>     (java.util.concurrent — recommended)       │
│    → fully non-blocking; composable pipeline; modern standard        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Return Type 1: `void` — Fire and Forget

The simplest form. The caller submits the task and moves on. There is no handle to observe the result or catch exceptions via normal try/catch.

```java
@Service
public class AuditService {

    @Async
    public void logEvent(String userId, String action) {
        // Runs on a background thread — caller has no handle to this
        System.out.println("[" + Thread.currentThread().getName() + "] Logging: " + action);
        auditRepository.save(new AuditEntry(userId, action, Instant.now()));
    }
}

// Caller:
@Service
public class UserService {

    @Autowired private AuditService auditService;

    public void deleteAccount(String userId) {
        userRepository.delete(userId);
        auditService.logEvent(userId, "DELETE_ACCOUNT"); // returns immediately — void
        // No way to know when logEvent finishes or if it threw an exception
    }
}
```

---

### Return Type 2: `Future<T>` with `AsyncResult<T>`

`java.util.concurrent.Future<T>` is the legacy way to get a result back. Spring provides `AsyncResult<T>` as a convenient wrapper to return from `@Async` methods.

```java
import org.springframework.scheduling.annotation.AsyncResult;
import java.util.concurrent.Future;

@Service
public class PricingService {

    @Async
    public Future<Double> getPrice(String productId) {
        // Runs on background thread
        double price = externalPricingApi.fetch(productId); // slow call
        return new AsyncResult<>(price); // wrap result in AsyncResult
    }
}
```

#### How `AsyncResult` Works Internally

`AsyncResult<T>` is a simple Spring class that implements both `Future<T>` and `ListenableFuture<T>`. It holds the result or exception at construction time — it is already-resolved by the time it is returned.

```java
// Simplified internals of AsyncResult<V>
public class AsyncResult<V> implements Future<V> {
    private final V value;
    private final Throwable executionException;

    public AsyncResult(V value) {
        this.value = value;
        this.executionException = null;
    }

    public static <V> AsyncResult<V> forExecutionException(Throwable ex) {
        return new AsyncResult<>(null, ex);
    }

    @Override
    public V get() throws ExecutionException {
        if (executionException != null)
            throw new ExecutionException(executionException);
        return value;
    }

    @Override
    public boolean isDone() { return true; } // always done when Spring returns it
}
```

```
Timeline with Future<T>:

Background thread (Async-1):
  executes getPrice()
  creates AsyncResult<>(42.99)   ← already holds the value

Spring intercept layer:
  wraps the AsyncResult in a CompletableFuture internally
  returns CompletableFuture to the caller

Caller thread:
  Future<Double> priceFuture = pricingService.getPrice("P1"); // returns instantly (the CF)
  // ... do other work ...
  double price = priceFuture.get(); // BLOCKS until background thread finishes
```

#### Caller Usage: Blocking `.get()`

```java
@Service
public class CartService {

    @Autowired private PricingService pricingService;

    public CartSummary buildCart(List<String> productIds) throws Exception {
        // Submit all price lookups in parallel
        List<Future<Double>> futures = productIds.stream()
            .map(pricingService::getPrice)
            .toList();

        // Now block and collect results
        List<Double> prices = new ArrayList<>();
        for (Future<Double> f : futures) {
            prices.add(f.get()); // ← BLOCKS until each completes
        }

        return new CartSummary(productIds, prices);
    }
}
```

```
Timeline (parallel price lookups):

Caller thread:
  submit getPrice("P1") → Async-1 starts
  submit getPrice("P2") → Async-2 starts
  submit getPrice("P3") → Async-3 starts
                          (all three run in parallel on the thread pool)
  f1.get() → waits for Async-1
  f2.get() → waits for Async-2  (may already be done)
  f3.get() → waits for Async-3  (may already be done)

Total time ≈ max(P1 time, P2 time, P3 time) — NOT sum!
```

#### `Future` Limitations

| Limitation | Impact |
|-----------|--------|
| `.get()` is always blocking | Caller thread is suspended until result is ready |
| No non-blocking callback | Cannot chain work without blocking |
| No exception transparency | Exceptions are wrapped in `ExecutionException` |
| No combinators | Cannot do `anyOf`, `allOf`, `thenApply` |

---

### Return Type 3: `CompletableFuture<T>` (Recommended)

`CompletableFuture<T>` is the modern, fully-featured, non-blocking way to return values from `@Async` methods. It supports:

- Non-blocking callbacks (`thenApply`, `thenAccept`, `thenCompose`)
- Combinators (`allOf`, `anyOf`)
- Exception handling in the pipeline (`exceptionally`, `handle`)
- Manual completion (`complete`, `completeExceptionally`)

```java
import java.util.concurrent.CompletableFuture;

@Service
public class UserProfileService {

    @Async("profileExecutor")
    public CompletableFuture<UserProfile> fetchProfile(Long userId) {
        // Runs on background thread
        UserProfile profile = profileRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        return CompletableFuture.completedFuture(profile);
        // Returns a COMPLETED CompletableFuture wrapping the result
    }

    @Async("profileExecutor")
    public CompletableFuture<List<Order>> fetchOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return CompletableFuture.completedFuture(orders);
    }
}
```

#### Non-Blocking Pipeline with `thenApply` / `thenCombine`

```java
@Service
public class DashboardService {

    @Autowired private UserProfileService userProfileService;

    public CompletableFuture<Dashboard> buildDashboard(Long userId) {
        // Both calls fire simultaneously — different background threads
        CompletableFuture<UserProfile> profileFuture =
            userProfileService.fetchProfile(userId);

        CompletableFuture<List<Order>> ordersFuture =
            userProfileService.fetchOrders(userId);

        // Combine when BOTH are done — non-blocking!
        return profileFuture.thenCombine(ordersFuture,
            (profile, orders) -> new Dashboard(profile, orders)
        );
    }
}
```

```
Timeline (non-blocking combination):

Caller thread:
  fire fetchProfile(1)  → Async-1 starts
  fire fetchOrders(1)   → Async-2 starts
  thenCombine(...)      ← registers callback — caller thread is FREE

Async-1: fetches profile → completes profileFuture
Async-2: fetches orders  → completes ordersFuture

When BOTH complete (whichever is last):
  → thenCombine callback runs: builds Dashboard
  → resultFuture is completed
```

#### `CompletableFuture.completedFuture()` vs `CompletableFuture.supplyAsync()`

```java
// ✅ Correct — use completedFuture() inside an @Async method
@Async
public CompletableFuture<String> getData() {
    String result = slowQuery(); // runs on @Async thread pool
    return CompletableFuture.completedFuture(result);
    // The @Async proxy wraps this in the thread submission
}

// ❌ Wrong — supplyAsync() spawns its OWN thread pool internally
@Async
public CompletableFuture<String> getData() {
    return CompletableFuture.supplyAsync(() -> slowQuery());
    // Now TWO thread switches happen: @Async pool → ForkJoinPool.commonPool()
    // The @Async executor is bypassed for the actual work!
}
```

```
@Async + completedFuture():
  HTTP thread → [@Async proxy] → [Async pool thread] → slowQuery() → return

@Async + supplyAsync():
  HTTP thread → [@Async proxy] → [Async pool thread] (does nothing) →
  ForkJoinPool.commonPool() thread → slowQuery() → return
  (wasted thread switch!)
```

---

### Complete Comparison: `Future` vs `AsyncResult` vs `CompletableFuture`

| Feature | `Future<T>` + `AsyncResult<T>` | `CompletableFuture<T>` |
|---------|-------------------------------|------------------------|
| Java version | Java 5+ | Java 8+ |
| Blocking `.get()` | Required to get result | Optional (can use callbacks) |
| Non-blocking callbacks | No | Yes (`thenApply`, `thenAccept`, etc.) |
| Combinators | No | Yes (`allOf`, `anyOf`, `thenCombine`) |
| Exception handling in pipeline | No | Yes (`exceptionally`, `handle`) |
| Manual completion | No | Yes (`.complete()`, `.completeExceptionally()`) |
| Timeout support | `.get(timeout, unit)` | `.orTimeout()`, `.completeOnTimeout()` |
| Spring integration | `AsyncResult` helper needed | Native — return directly |
| Recommended | Legacy only | ✅ Modern standard |

---

## 21. Exception Handling in `@Async` Methods

### Why Normal `try/catch` at the Call Site Does NOT Work

When you call an `@Async` method, the **calling thread returns immediately** — before the background thread has even started executing. Any exception thrown inside the `@Async` method happens on the background thread, long after the call site has exited. There is no stack to propagate the exception back to.

```
Caller thread:                     Background thread (Async-1):
  try {
    asyncService.doWork();  ──────► submit task to executor
  } catch (Exception e) {           (caller's try block has ALREADY exited)
    // ← NEVER reached!             doWork() starts here
  }                                  throws RuntimeException("fail")
                                     → nobody catches it on caller's side!
```

Exception handling strategy depends entirely on the **return type** of the `@Async` method.

---

### Case 1: `CompletableFuture<T>` Return Type — Exception in `.get()`

When an `@Async` method returns `CompletableFuture<T>` and throws an exception, the exception is **stored inside the `CompletableFuture`**. It is NOT thrown immediately. The exception surfaces only when the caller calls `.get()` or `.join()`.

```
┌──────────────────────────────────────────────────────────────────────┐
│          Exception Flow with CompletableFuture                       │
│                                                                      │
│  Background thread (Async-1):                                        │
│    @Async method body executes                                       │
│         │                                                            │
│         ├─ Normal completion:                                        │
│         │    return CompletableFuture.completedFuture(result)        │
│         │    → CF stores the result → .get() returns the value       │
│         │                                                            │
│         └─ Exception thrown:                                         │
│              RuntimeException("SMTP failed")                         │
│              ↓                                                        │
│    Spring's AsyncExecutionInterceptor catches it                     │
│    calls: future.completeExceptionally(exception)                    │
│              ↓                                                        │
│    CF stores the exception internally                                │
│              ↓                                                        │
│  Caller calls future.get()                                           │
│    → CompletableFuture detects stored exception                      │
│    → wraps it in ExecutionException                                  │
│    → throws ExecutionException at the .get() call site               │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

#### Why `ExecutionException` Wraps the Original

`CompletableFuture.get()` is specified by the `Future<T>` interface. The interface contract requires that any exception from the computation is wrapped in `java.util.concurrent.ExecutionException`. This wrapping is done by the JDK — Spring has no control over it.

```java
// JDK source (simplified):
public T get() throws InterruptedException, ExecutionException {
    Object result = waitForResult(); // blocks
    if (result instanceof AltResult ar) {
        if (ar.ex != null)
            throw new ExecutionException(ar.ex); // ← always wraps!
    }
    return (T) result;
}
```

```java
@Service
public class EmailService {

    @Async
    public CompletableFuture<String> sendEmail(String to) {
        if (to == null) {
            throw new IllegalArgumentException("Recipient is null");
            // ↑ This exception is caught by Spring's AsyncExecutionInterceptor
            //   and stored in the CompletableFuture via completeExceptionally()
        }
        mailSender.send(to, "Subject", "Body");
        return CompletableFuture.completedFuture("sent");
    }
}

// Caller:
@Service
public class OrderService {

    @Autowired private EmailService emailService;

    public void notifyCustomer(String email) {
        CompletableFuture<String> future = emailService.sendEmail(email);

        try {
            String result = future.get(); // blocks — may throw
            System.out.println("Email result: " + result);

        } catch (ExecutionException e) {
            // ← This IS reached — exception surfaces here at .get()
            Throwable cause = e.getCause(); // unwrap to get original exception
            System.err.println("Email failed: " + cause.getMessage());
            // cause = IllegalArgumentException("Recipient is null")

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for email", e);
        }
    }
}
```

#### Non-Blocking Exception Handling with `exceptionally` / `handle`

Instead of blocking with `.get()` and catching `ExecutionException`, use the non-blocking pipeline:

```java
public void notifyCustomer(String email) {
    emailService.sendEmail(email)
        .exceptionally(ex -> {
            // ex = the ORIGINAL exception (NOT wrapped in ExecutionException!)
            // Spring unwraps it before passing to exceptionally()
            System.err.println("Email failed: " + ex.getMessage());
            return "failed"; // fallback value
        })
        .thenAccept(result ->
            System.out.println("Outcome: " + result) // "sent" or "failed"
        );
    // Caller thread returns immediately — completely non-blocking!
}
```

```java
// handle() — runs whether success or failure, gives access to both result and exception
emailService.sendEmail(email)
    .handle((result, ex) -> {
        if (ex != null) {
            log.error("Email failed for {}: {}", email, ex.getMessage());
            return "error";
        }
        return result; // "sent"
    })
    .thenAccept(outcome -> metrics.record(outcome));
```

```
Key difference between .get() and exceptionally():

.get()           → blocks the caller thread; exception wrapped in ExecutionException
.exceptionally() → non-blocking callback; exception is the raw original Throwable
.handle()        → non-blocking; runs always (success or failure)
.whenComplete()  → non-blocking; runs always but does NOT transform the result
```

#### Timeout Handling

```java
try {
    String result = future.get(5, TimeUnit.SECONDS); // timeout
} catch (TimeoutException e) {
    future.cancel(true);
    throw new ServiceException("Email service timed out");
} catch (ExecutionException e) {
    throw new ServiceException("Email failed: " + e.getCause().getMessage());
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}

// Or with Java 9+ non-blocking timeout:
CompletableFuture<String> withTimeout = emailService.sendEmail(email)
    .orTimeout(5, TimeUnit.SECONDS)        // completes exceptionally with TimeoutException
    .exceptionally(ex -> "timeout-fallback");
```

---

### Case 2: `Future<T>` Return Type — Exception in `.get()`

`Future<T>` works the same way as `CompletableFuture<T>` for exception propagation: the exception is stored in the `AsyncResult` and wrapped in `ExecutionException` when `.get()` is called.

```java
@Service
public class PricingService {

    @Async
    public Future<Double> getPrice(String productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID required");
        }
        return new AsyncResult<>(externalApi.fetchPrice(productId));
    }
}

// Caller:
Future<Double> priceFuture = pricingService.getPrice(null);
try {
    double price = priceFuture.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause(); // IllegalArgumentException
    System.err.println("Pricing failed: " + cause.getMessage());
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

---

### Case 3: `void` Return Type — Exceptions Are Lost by Default!

When an `@Async` method returns `void`, there is no `CompletableFuture` to hold the exception. The exception is thrown on the background thread with **no caller to receive it**. By default, Spring logs it and **silently swallows it**.

```
┌──────────────────────────────────────────────────────────────────────┐
│          Exception Flow for void @Async (no handler configured)      │
│                                                                      │
│  Background thread (Async-1):                                        │
│    @Async void method throws RuntimeException("DB unavailable")      │
│         │                                                            │
│         ▼                                                            │
│  Spring's AsyncExecutionInterceptor catches it                       │
│         │                                                            │
│         ▼                                                            │
│  Looks for AsyncUncaughtExceptionHandler                             │
│    → None configured? → SimpleAsyncUncaughtExceptionHandler (default)│
│         │                                                            │
│         ▼                                                            │
│  SimpleAsyncUncaughtExceptionHandler.handleUncaughtException()       │
│    → just calls: logger.error("Unexpected exception occurred ...", ex)│
│    → exception is LOGGED and DROPPED                                 │
│    → application continues running as if nothing happened            │
│                                                                      │
│  Caller thread:                                                       │
│    → already returned, has no idea this failed                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

```java
@Service
public class AuditService {

    @Async
    public void logEvent(String userId, String action) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
            // ← thrown on background thread — silently logged if no handler configured!
        }
        auditRepository.save(new AuditEntry(userId, action));
    }
}

// Caller:
auditService.logEvent(null, "LOGIN");
// No exception visible to caller — just a log line in the background
```

---

### `AsyncUncaughtExceptionHandler` — Custom Handler for `void` Methods

To intercept exceptions from `void @Async` methods, implement `AsyncUncaughtExceptionHandler`:

```java
// org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
@FunctionalInterface
public interface AsyncUncaughtExceptionHandler {
    void handleUncaughtException(Throwable ex, Method method, Object... params);
    //                           ^ the exception   ^ which method   ^ method args
}
```

#### Writing a Production-Grade Handler

```java
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ApplicationAsyncExceptionHandler
        implements AsyncUncaughtExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(ApplicationAsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable ex,
                                        Method method,
                                        Object... params) {
        // 1. Structured logging with full context
        log.error("[ASYNC ERROR] {}.{}({}) threw: {} — {}",
            method.getDeclaringClass().getSimpleName(),
            method.getName(),
            Arrays.toString(params),
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            ex // stack trace as last arg for SLF4J
        );

        // 2. Send to monitoring / alerting
        // alertingService.sendAlert(ex, method, params);

        // 3. Write to a dead-letter store for retry
        // deadLetterQueue.publish(new AsyncFailure(method.getName(), params, ex));

        // 4. Increment failure metric
        // metricsService.increment("async.failures", "method", method.getName());
    }
}
```

---

### Registering `AsyncUncaughtExceptionHandler` via `AsyncConfigurer`

`AsyncConfigurer` is the standard way to register both a custom executor and a custom exception handler in one place:

```java
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // ── Default executor for all bare @Async calls ───────────────────
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("App-Async-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    // ── Global handler for exceptions from void @Async methods ───────
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ApplicationAsyncExceptionHandler();
        // ← every unhandled exception from a void @Async method ends up here
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────┐
│       AsyncConfigurer — Single Source of Truth for @Async Config     │
│                                                                      │
│   AsyncConfig implements AsyncConfigurer                             │
│         │                                                            │
│         ├── getAsyncExecutor()                                       │
│         │     └─ ThreadPoolTaskExecutor (8–16 threads, queue 300)    │
│         │     └─ Used as default for all bare @Async calls           │
│         │                                                            │
│         └── getAsyncUncaughtExceptionHandler()                       │
│               └─ ApplicationAsyncExceptionHandler                    │
│               └─ Called whenever a void @Async method throws         │
│                                                                      │
│   Named executor beans (@Bean) can still be added alongside          │
│   for specialized pools (@Async("emailExecutor") etc.)               │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### `AsyncUncaughtExceptionHandler` — Important Limitations

| Limitation | Detail |
|-----------|--------|
| Only fires for `void` methods | `Future<T>` / `CompletableFuture<T>` methods store the exception in the future — handler is NOT called |
| Cannot retry | The method has already returned; you can only observe and log |
| Cannot access return value | By definition — `void` has no return |
| Runs on the background thread | Not the caller's thread |
| Exception does NOT propagate | After the handler runs, the thread continues normally |

```java
// Exception handler IS called:
@Async
public void sendEmail(String to) {       // void return
    throw new SmtpException("timeout"); // → goes to handler
}

// Exception handler is NOT called:
@Async
public CompletableFuture<String> sendEmail(String to) {  // CF return
    throw new SmtpException("timeout"); // → stored in CF, handler skipped
}                                        // caller must handle via .get() / exceptionally()
```

---

### Combining Both Strategies: Full Production Pattern

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("[ASYNC UNCAUGHT] {}.{}({}): {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params),
                ex.getMessage(), ex);
            // send to alerting, dead-letter, metrics...
        };
    }
}

// ── Service with both patterns ──────────────────────────────────────
@Service
public class NotificationService {

    // void @Async — exception goes to AsyncUncaughtExceptionHandler
    @Async
    public void sendPushNotification(Long userId, String message) {
        pushClient.send(userId, message); // if this throws → handler
    }

    // CompletableFuture @Async — exception stored in CF, caller handles
    @Async
    public CompletableFuture<String> sendEmail(String to, String subject) {
        try {
            mailSender.send(to, subject);
            return CompletableFuture.completedFuture("ok");
        } catch (MailException e) {
            // Option A: let Spring catch it (stored in CF via completeExceptionally)
            throw e;

            // Option B: complete exceptionally explicitly (same outcome)
            // CompletableFuture<String> cf = new CompletableFuture<>();
            // cf.completeExceptionally(e);
            // return cf;
        }
    }
}

// ── Caller ──────────────────────────────────────────────────────────
@Service
public class OrderService {

    @Autowired private NotificationService notificationService;

    public void processOrder(Order order) {
        // Fire-and-forget — exceptions handled globally
        notificationService.sendPushNotification(order.getUserId(), "Order placed!");

        // Non-blocking result with error handling
        notificationService.sendEmail(order.getEmail(), "Order Confirmation")
            .exceptionally(ex -> {
                log.warn("Email failed for order {}: {}", order.getId(), ex.getMessage());
                return "failed";
            })
            .thenAccept(result -> log.info("Email outcome: {}", result));
    }
}
```

---

### Exception Handling Decision Guide

```
@Async method throws an exception — what handles it?
  │
  ├── Return type = void
  │     └── AsyncUncaughtExceptionHandler.handleUncaughtException()
  │           → configure via AsyncConfigurer.getAsyncUncaughtExceptionHandler()
  │           → default: SimpleAsyncUncaughtExceptionHandler (just logs)
  │           → implement your own for alerting, dead-lettering, metrics
  │
  └── Return type = CompletableFuture<T> or Future<T>
        └── Exception stored inside the Future
              │
              ├── Caller calls .get() → throws ExecutionException(cause)
              │     catch ExecutionException e → e.getCause() = original
              │
              ├── Caller uses .exceptionally(ex -> ...) → non-blocking
              │     ex = original exception (NOT wrapped)
              │
              └── Caller ignores the Future → exception silently lost!
                    → add .exceptionally(ex -> { log.error(...); return null; })
                       even if you don't use the result!
```

---

### Complete Summary: Return Types and Exception Handling

| Return Type | How to Return Value | Exception Surface | Handler Mechanism |
|------------|--------------------|--------------------|-------------------|
| `void` | N/A | Nowhere by default | `AsyncUncaughtExceptionHandler` |
| `Future<T>` | `return new AsyncResult<>(value)` | `ExecutionException` on `.get()` | `try/catch` at call site |
| `CompletableFuture<T>` | `return CompletableFuture.completedFuture(value)` | `ExecutionException` on `.get()`, or raw via `.exceptionally()` | `.exceptionally()`, `.handle()`, or `try/catch` with `.get()` |

