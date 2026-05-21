

## Youtube

- [Handle 1,000,000 Threads with Java and Spring Boot !!!](https://www.youtube.com/watch?v=yLEEyErPQ2g)




## Spring Boot with Virtual Threads

---

### 1. What is a Virtual Thread?

A **Virtual Thread** (introduced as a preview in Java 19, finalized in **Java 21** via [JEP 444](https://openjdk.org/jeps/444)) is a lightweight thread managed by the **JVM**, not by the operating system. Virtual threads are part of **Project Loom** — a fundamental change in how Java handles concurrency.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Traditional (Platform) Thread vs Virtual Thread:                                 │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │  PLATFORM THREAD (traditional):                                         │    │
│  │                                                                          │    │
│  │  Java Thread ←──── 1:1 mapping ────→ OS Thread (kernel thread)         │    │
│  │                                                                          │    │
│  │  • Created by the OS (expensive: ~1MB stack, kernel scheduling)        │    │
│  │  • Limited by OS resources (typically 2,000–10,000 threads max)        │    │
│  │  • Heavy context switching (kernel mode switch)                         │    │
│  │  • Each thread reserves a fixed stack size (default ~1MB)              │    │
│  │  • Managed by OS scheduler                                              │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │  VIRTUAL THREAD (Java 21+):                                             │    │
│  │                                                                          │    │
│  │  Virtual Thread ←── M:N mapping ──→ Platform Thread (carrier thread)   │    │
│  │                                                                          │    │
│  │  • Created by the JVM (cheap: ~few KB stack, grows on demand)          │    │
│  │  • Millions of virtual threads can exist simultaneously                │    │
│  │  • Lightweight context switching (user-mode, no kernel involved)       │    │
│  │  • Stack grows/shrinks dynamically (starts very small)                 │    │
│  │  • Managed by JVM scheduler (ForkJoinPool)                             │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  Key Insight:                                                                    │
│  A virtual thread is NOT tied to an OS thread for its entire lifetime.          │
│  When a virtual thread blocks (I/O, sleep, lock), the JVM UNMOUNTS it from     │
│  the carrier thread → the carrier is FREE to run another virtual thread.        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### How Virtual Threads Work Internally

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  ┌────────────────────── JVM ──────────────────────────┐                        │
│  │                                                      │                        │
│  │  Virtual Threads (millions possible):               │                        │
│  │  ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ...   │                        │
│  │  │V1│ │V2│ │V3│ │V4│ │V5│ │V6│ │V7│ │V8│        │                        │
│  │  └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘        │                        │
│  │    │     │     │                                     │                        │
│  │    │     │     │    ← JVM Scheduler decides         │                        │
│  │    │     │     │       which virtual thread          │                        │
│  │    ▼     ▼     ▼       runs on which carrier         │                        │
│  │  ┌────────────────────────────────────────────┐     │                        │
│  │  │  Carrier Threads (ForkJoinPool):           │     │                        │
│  │  │  ┌────┐  ┌────┐  ┌────┐  ┌────┐          │     │                        │
│  │  │  │ CT1│  │ CT2│  │ CT3│  │ CT4│          │     │                        │
│  │  │  └────┘  └────┘  └────┘  └────┘          │     │                        │
│  │  │  (# carriers = # CPU cores by default)    │     │                        │
│  │  └────────────────────────────────────────────┘     │                        │
│  │        │         │        │        │                 │                        │
│  └────────│─────────│────────│────────│─────────────────┘                        │
│           │         │        │        │                                          │
│           ▼         ▼        ▼        ▼                                          │
│  ┌────────────────────────────────────────────────────────────┐                 │
│  │  OS Threads (Platform Threads):                             │                 │
│  │  1:1 mapped to carrier threads                             │                 │
│  └────────────────────────────────────────────────────────────┘                 │
│                                                                                  │
│  The Flow:                                                                       │
│  1. V1 is MOUNTED on CT1 → V1 runs its code                                    │
│  2. V1 hits a blocking call (e.g., HTTP request, DB query)                      │
│  3. JVM UNMOUNTS V1 from CT1 (V1's state saved on heap)                        │
│  4. JVM MOUNTS V5 on CT1 → CT1 now runs V5's code                             │
│  5. V1's I/O completes → V1 is put back in the run queue                       │
│  6. When a carrier becomes free → V1 is MOUNTED again (maybe on CT3 now)       │
│                                                                                  │
│  Result: 4 carriers can handle thousands of virtual threads efficiently!        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Mount and Unmount — The Key Mechanism

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  Virtual Thread V1 executing code:                                               │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────┐         │
│  │ V1 running on Carrier Thread CT1:                                   │         │
│  │                                                                      │         │
│  │  processOrder() {                                                    │         │
│  │      Order order = parseRequest(request);     // ← CPU work (fast)  │         │
│  │      User user = userService.findById(id);    // ← BLOCKS (DB I/O) │         │
│  │      ─────────────── V1 UNMOUNTS HERE ─────────────────────         │         │
│  │      // V1's stack frame saved to heap                               │         │
│  │      // CT1 is now FREE to run another virtual thread               │         │
│  │  }                                                                   │         │
│  └────────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│  While V1 is waiting for DB response:                                            │
│  ┌────────────────────────────────────────────────────────────────────┐         │
│  │ V7 now running on Carrier Thread CT1:                               │         │
│  │                                                                      │         │
│  │  sendNotification() {                                                │         │
│  │      template = loadTemplate();               // ← CPU work         │         │
│  │      result = emailClient.send(email);        // ← BLOCKS (I/O)    │         │
│  │      ─────────────── V7 UNMOUNTS HERE ─────────────────────         │         │
│  │  }                                                                   │         │
│  └────────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│  V1's DB call returns → V1 goes back to run queue → mounted on any free carrier│
│                                                                                  │
│  KEY POINT:                                                                      │
│  Platform threads BLOCK the OS thread during I/O (thread sits idle, wasting it) │
│  Virtual threads YIELD the carrier during I/O (carrier serves other VTs)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Platform Thread vs Virtual Thread — Analogy

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Restaurant Analogy:                                                             │
│                                                                                  │
│  PLATFORM THREADS = One Waiter Per Table:                                       │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  • 10 tables → you need 10 waiters                                     │   │
│  │  • Each waiter stands at their table, even when the customer is        │   │
│  │    just reading the menu (waiter is IDLE but OCCUPIED)                  │   │
│  │  • Want 1000 tables? You need 1000 waiters (expensive!)               │   │
│  │  • Each waiter costs salary + uniform + space (= memory + OS thread)  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  VIRTUAL THREADS = Waiters Shared Across Tables:                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  • 1000 tables → only 4 waiters (= 4 carrier threads = 4 CPU cores)  │   │
│  │  • Waiter takes order at Table 1, then MOVES to Table 2               │   │
│  │  • While Table 1 waits for food (I/O), the waiter serves others       │   │
│  │  • When Table 1's food is ready, ANY free waiter delivers it          │   │
│  │  • 4 waiters can efficiently serve 1000 tables!                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  This is why virtual threads shine for I/O-bound workloads:                     │
│  More time waiting (I/O) = more time the carrier can serve other VTs           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2. Advantages and Disadvantages of Virtual Threads vs Platform Threads

#### Advantages of Virtual Threads

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advantages of Virtual Threads:                                                  │
│                                                                                  │
│  1. MASSIVE SCALABILITY — Millions of Concurrent Threads                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Platform threads: limited to ~2,000–10,000 per JVM (OS limit)         │   │
│  │  Virtual threads: millions are feasible                                 │   │
│  │                                                                          │   │
│  │  A typical web server handles 200 concurrent requests with 200          │   │
│  │  platform threads. With virtual threads, the SAME server handles        │   │
│  │  100,000+ concurrent requests without running out of threads.          │   │
│  │                                                                          │   │
│  │  Real impact: no more "thread pool exhaustion" errors under load.      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  2. EXTREMELY LIGHTWEIGHT — Low Memory Footprint                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Platform thread: ~1 MB stack (fixed, reserved upfront)                │   │
│  │  Virtual thread:  ~few KB (dynamic, grows only as needed)              │   │
│  │                                                                          │   │
│  │  10,000 platform threads: 10,000 × 1 MB = ~10 GB memory (just stacks!)│   │
│  │  10,000 virtual threads:  10,000 × ~5 KB = ~50 MB memory              │   │
│  │                                                                          │   │
│  │  100,000 virtual threads: ~500 MB — still very manageable.            │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  3. CHEAP CREATION AND DESTRUCTION                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Platform thread creation: ~1 ms (OS kernel call, allocate stack)      │   │
│  │  Virtual thread creation:  ~1 µs (1000x faster, JVM heap only)        │   │
│  │                                                                          │   │
│  │  No need for thread pools! Create a new virtual thread per task.       │   │
│  │  Traditional "thread-per-request" model becomes viable again.          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  4. SIMPLE PROGRAMMING MODEL — No Reactive/Async Complexity                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Before virtual threads, high concurrency required:                     │   │
│  │  → Reactive programming (WebFlux, Mono, Flux) — steep learning curve  │   │
│  │  → CompletableFuture chains — callback hell                            │   │
│  │  → Event loops (Netty) — hard to debug                                 │   │
│  │                                                                          │   │
│  │  With virtual threads:                                                   │   │
│  │  → Write BLOCKING code (simple, readable)                              │   │
│  │  → The JVM handles the "non-blocking" part transparently               │   │
│  │  → Same throughput as reactive, with imperative simplicity             │   │
│  │                                                                          │   │
│  │  // Reactive (hard to read/debug):                                      │   │
│  │  return userRepo.findById(id)                                           │   │
│  │      .flatMap(user -> orderRepo.findByUser(user))                      │   │
│  │      .flatMap(order -> paymentService.charge(order))                   │   │
│  │      .onErrorResume(e -> Mono.error(new PaymentException(e)));         │   │
│  │                                                                          │   │
│  │  // Virtual threads (simple blocking code — same performance):         │   │
│  │  User user = userRepo.findById(id);                                    │   │
│  │  Order order = orderRepo.findByUser(user);                             │   │
│  │  Payment payment = paymentService.charge(order);                       │   │
│  │  return payment;                                                        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  5. BETTER OBSERVABILITY AND DEBUGGING                                          │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual threads have:                                                   │   │
│  │  → Full stack traces (unlike reactive streams)                         │   │
│  │  → Thread names (easy to identify in logs)                             │   │
│  │  → Standard debugging support (breakpoints, step-through)              │   │
│  │  → Standard exception propagation (try/catch works normally)           │   │
│  │  → Thread dumps via jstack/jcmd (shows all virtual threads)            │   │
│  │                                                                          │   │
│  │  Reactive streams: stack traces are often meaningless, debugging is    │   │
│  │  extremely difficult, exceptions lose context across operators.         │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  6. EFFICIENT I/O UTILIZATION — No Wasted OS Threads                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  When a platform thread blocks on I/O, the OS thread sits IDLE.        │   │
│  │  When a virtual thread blocks on I/O, the carrier thread is FREED.     │   │
│  │                                                                          │   │
│  │  In I/O-heavy apps (REST APIs, DB queries, HTTP calls):               │   │
│  │  → Platform threads: 200 threads × 80% blocked = 160 wasted threads  │   │
│  │  → Virtual threads: carriers always busy, VTs parked cheaply on heap  │   │
│  │                                                                          │   │
│  │  Result: same hardware handles 10-100x more concurrent requests.       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  7. BACKWARD COMPATIBLE                                                          │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual threads implement java.lang.Thread — same API!                │   │
│  │  → Thread.sleep() works (and properly yields the carrier)             │   │
│  │  → synchronized blocks work (with some caveats — see disadvantages)   │   │
│  │  → ThreadLocal works (but has overhead — see disadvantages)            │   │
│  │  → Existing libraries work without modification (mostly)               │   │
│  │  → No new programming model to learn                                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Disadvantages and Limitations of Virtual Threads

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Disadvantages and Limitations of Virtual Threads:                                │
│                                                                                  │
│  1. NOT BENEFICIAL FOR CPU-BOUND WORKLOADS                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual threads shine when threads BLOCK (wait for I/O).              │   │
│  │  If your workload is CPU-intensive (calculations, data processing):   │   │
│  │                                                                          │   │
│  │  → Virtual threads offer NO advantage over platform threads            │   │
│  │  → You still can only run N tasks in parallel (N = CPU cores)         │   │
│  │  → The carrier threads are always busy → no unmounting happens        │   │
│  │  → Adding more virtual threads just adds scheduling overhead           │   │
│  │                                                                          │   │
│  │  CPU-bound examples: video encoding, encryption, ML inference,         │   │
│  │  mathematical computation, image processing, compression.              │   │
│  │                                                                          │   │
│  │  ✓ Virtual threads for: REST APIs, DB queries, HTTP clients, file I/O │   │
│  │  ✗ Platform threads for: pure computation, number crunching           │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  2. PINNING — synchronized BLOCKS THE CARRIER THREAD                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  When a virtual thread enters a `synchronized` block or method, it    │   │
│  │  gets PINNED to its carrier thread — it CANNOT be unmounted.           │   │
│  │                                                                          │   │
│  │  synchronized (lock) {                                                  │   │
│  │      // Virtual thread is PINNED to carrier                            │   │
│  │      database.query(...);  // ← carrier is BLOCKED (just like a       │   │
│  │      // platform thread!) — other VTs can't use this carrier          │   │
│  │  }                                                                      │   │
│  │                                                                          │   │
│  │  Why this is bad:                                                        │   │
│  │  → If many VTs are pinned → carriers are exhausted → deadlock-like    │   │
│  │  → Throughput degrades to platform-thread levels                       │   │
│  │                                                                          │   │
│  │  Solution: Replace `synchronized` with ReentrantLock:                  │   │
│  │  private final ReentrantLock lock = new ReentrantLock();               │   │
│  │  lock.lock();                                                           │   │
│  │  try {                                                                  │   │
│  │      database.query(...);  // ← VT CAN be unmounted here             │   │
│  │  } finally {                                                            │   │
│  │      lock.unlock();                                                     │   │
│  │  }                                                                      │   │
│  │                                                                          │   │
│  │  Detect pinning: -Djdk.tracePinnedThreads=full (JVM flag)             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  3. THREAD-LOCAL OVERHEAD                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ThreadLocal WORKS with virtual threads — but each virtual thread     │   │
│  │  gets its OWN copy.                                                     │   │
│  │                                                                          │   │
│  │  With 1,000,000 virtual threads, each with a ThreadLocal holding      │   │
│  │  a 1 KB object → 1 GB of memory just for ThreadLocals!               │   │
│  │                                                                          │   │
│  │  Platform threads: 200 threads × 1 KB = 200 KB (fine)                 │   │
│  │  Virtual threads: 1,000,000 VTs × 1 KB = 1 GB (problem!)             │   │
│  │                                                                          │   │
│  │  Solution: Use ScopedValue (Java 21 preview, Java 25 finalized):      │   │
│  │  → Immutable, inherited efficiently, no per-VT copy overhead          │   │
│  │  → ScopedValue.where(KEY, value).run(() -> { ... });                  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  4. THIRD-PARTY LIBRARY COMPATIBILITY                                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Libraries that use `synchronized` internally cause pinning:           │   │
│  │  → JDBC drivers (some versions)                                        │   │
│  │  → Older versions of connection pools (HikariCP before 5.1)           │   │
│  │  → File I/O operations (some JDK internals use synchronized)          │   │
│  │  → Native code (JNI) pins the carrier                                 │   │
│  │                                                                          │   │
│  │  You may need to:                                                        │   │
│  │  → Update libraries to versions that support virtual threads           │   │
│  │  → Check library compatibility before adopting                         │   │
│  │  → Monitor for pinning in production (-Djdk.tracePinnedThreads)       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  5. NO THREAD POOLING — DON'T POOL VIRTUAL THREADS                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Thread pools exist because platform threads are expensive to create.  │   │
│  │  Virtual threads are CHEAP to create — pooling them is an ANTI-PATTERN.│   │
│  │                                                                          │   │
│  │  ✗ DON'T: Executors.newFixedThreadPool(100, virtualThreadFactory)     │   │
│  │  ✓ DO:    Executors.newVirtualThreadPerTaskExecutor()                  │   │
│  │                                                                          │   │
│  │  Creating a fixed pool of virtual threads defeats their purpose.       │   │
│  │  The point is: create one per task, let them be garbage collected.     │   │
│  │                                                                          │   │
│  │  If you need to LIMIT concurrency (e.g., max 50 DB connections):      │   │
│  │  → Use a Semaphore, not a thread pool                                  │   │
│  │  → The bottleneck is the RESOURCE, not the thread count                │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  6. NOT PREEMPTIVE FOR CPU-BOUND WORK                                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual threads yield (unmount) only at well-defined points:          │   │
│  │  → Blocking I/O calls                                                  │   │
│  │  → Thread.sleep()                                                       │   │
│  │  → Lock acquisition (ReentrantLock)                                    │   │
│  │  → Object.wait()                                                        │   │
│  │                                                                          │   │
│  │  A virtual thread doing a long CPU loop will NOT yield:               │   │
│  │  while (true) { compute(); }  // ← monopolizes the carrier!          │   │
│  │                                                                          │   │
│  │  This can starve other virtual threads waiting to be scheduled.        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  7. REQUIRES JAVA 21+                                                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual threads are finalized in Java 21 (September 2023).            │   │
│  │  → Projects on Java 8, 11, or 17 cannot use them                      │   │
│  │  → Spring Boot 3.2+ is needed for full virtual thread support          │   │
│  │  → Organizations on older JDKs must upgrade first                      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Side-by-Side Comparison Table

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Platform Thread vs Virtual Thread — Complete Comparison:                         │
│                                                                                  │
│  ┌──────────────────────────┬─────────────────────────┬──────────────────────┐  │
│  │ Aspect                   │ Platform Thread          │ Virtual Thread       │  │
│  ├──────────────────────────┼─────────────────────────┼──────────────────────┤  │
│  │ Managed by               │ OS kernel               │ JVM (user-space)    │  │
│  │ Stack size               │ ~1 MB (fixed)           │ ~few KB (dynamic)   │  │
│  │ Creation cost            │ ~1 ms (kernel call)     │ ~1 µs (JVM only)   │  │
│  │ Max per JVM              │ ~2,000–10,000           │ Millions            │  │
│  │ Memory per 10K threads   │ ~10 GB                  │ ~50 MB              │  │
│  │ Blocking I/O impact      │ OS thread blocked       │ Carrier freed       │  │
│  │ Context switch           │ Expensive (kernel)      │ Cheap (user-space)  │  │
│  │ Best for                 │ CPU-bound work          │ I/O-bound work      │  │
│  │ Pooling                  │ Required (expensive)    │ Anti-pattern (cheap)│  │
│  │ Scheduling               │ OS scheduler            │ JVM ForkJoinPool    │  │
│  │ ThreadLocal              │ Efficient (few threads) │ Costly (many VTs)   │  │
│  │ synchronized             │ No issue                │ Causes pinning      │  │
│  │ Debugging                │ Full support            │ Full support        │  │
│  │ Stack traces             │ Normal                  │ Normal              │  │
│  │ Available since          │ Java 1.0                │ Java 21             │  │
│  │ Spring Boot support      │ All versions            │ 3.2+                │  │
│  └──────────────────────────┴─────────────────────────┴──────────────────────┘  │
│                                                                                  │
│  When to Use Virtual Threads:                                                    │
│  ✓ High-concurrency REST APIs (hundreds/thousands of simultaneous requests)    │
│  ✓ Microservices making many outbound HTTP calls                               │
│  ✓ Database-heavy applications (many concurrent queries)                        │
│  ✓ Applications calling multiple external services per request                  │
│  ✓ Chat applications, WebSocket servers, long-polling                          │
│                                                                                  │
│  When to KEEP Platform Threads:                                                  │
│  ✗ Pure CPU-bound computation (math, encryption, compression)                  │
│  ✗ Low-concurrency apps (< 100 simultaneous requests)                          │
│  ✗ Apps using synchronized extensively (need refactoring first)                │
│  ✗ Apps on Java < 21                                                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 3. How to Create Virtual Threads

#### 3.1 Basic Virtual Thread Creation

```java
// ── Method 1: Thread.startVirtualThread() — simplest way ─────────────────────────
public class VirtualThreadBasics {

    public static void main(String[] args) throws InterruptedException {

        // Create and start a virtual thread immediately
        Thread vThread = Thread.startVirtualThread(() -> {
            System.out.println("Hello from virtual thread: " + Thread.currentThread());
            System.out.println("Is virtual: " + Thread.currentThread().isVirtual());  // true
        });

        vThread.join();   // wait for it to finish
    }
}
// Output:
// Hello from virtual thread: VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1
// Is virtual: true


// ── Method 2: Thread.ofVirtual() builder — more control ─────────────────────────
public class VirtualThreadBuilder {

    public static void main(String[] args) throws InterruptedException {

        // Builder pattern — set name, daemon status, etc.
        Thread vThread = Thread.ofVirtual()
            .name("my-virtual-thread")         // custom name
            .unstarted(() -> {                  // create but don't start yet
                System.out.println("Running: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(Duration.ofSeconds(1));   // yields carrier (not blocking!)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Done!");
            });

        vThread.start();    // start manually
        vThread.join();     // wait for completion
    }
}


// ── Method 3: Named virtual thread factory — for multiple threads ────────────────
public class VirtualThreadFactory {

    public static void main(String[] args) throws InterruptedException {

        // Factory that creates named virtual threads
        ThreadFactory factory = Thread.ofVirtual()
            .name("worker-", 0)    // prefix + counter: worker-0, worker-1, worker-2...
            .factory();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            Thread t = factory.newThread(() -> {
                System.out.println(Thread.currentThread().getName() + " processing task " + taskId);
            });
            t.start();
            threads.add(t);
        }

        // Wait for all to complete
        for (Thread t : threads) {
            t.join();
        }
    }
}
// Output:
// worker-0 processing task 0
// worker-1 processing task 1
// worker-2 processing task 2
// ... (order may vary)
```

#### 3.2 Virtual Thread Executor — `newVirtualThreadPerTaskExecutor()`

This is the **most common and recommended** way to use virtual threads in applications. It creates a new virtual thread for every submitted task — no pooling, no reuse.

```java
// ── The recommended ExecutorService for virtual threads ──────────────────────────
import java.util.concurrent.*;

public class VirtualThreadExecutorExample {

    public static void main(String[] args) throws Exception {

        // Create an executor that spawns a NEW virtual thread per task
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit 10,000 tasks — each gets its own virtual thread!
            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < 10_000; i++) {
                final int taskId = i;
                Future<String> future = executor.submit(() -> {
                    // Simulate I/O (HTTP call, DB query, etc.)
                    Thread.sleep(Duration.ofSeconds(1));
                    return "Result from task " + taskId +
                           " on " + Thread.currentThread().getName();
                });
                futures.add(future);
            }

            // All 10,000 tasks run CONCURRENTLY (not sequentially!)
            // With platform threads, you'd need 10,000 OS threads (impossible)
            // With virtual threads, this is trivial

            for (Future<String> future : futures) {
                System.out.println(future.get());  // blocks until this task completes
            }
        }
        // try-with-resources: executor.close() waits for all tasks and shuts down
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Executors.newVirtualThreadPerTaskExecutor() internals:                           │
│                                                                                  │
│  executor.submit(task1) → creates VirtualThread-1 → runs task1                  │
│  executor.submit(task2) → creates VirtualThread-2 → runs task2                  │
│  executor.submit(task3) → creates VirtualThread-3 → runs task3                  │
│  ...                                                                             │
│  executor.submit(task10000) → creates VirtualThread-10000 → runs task10000      │
│                                                                                  │
│  All 10,000 virtual threads are scheduled onto ~4-8 carrier threads             │
│  (= number of CPU cores). The JVM automatically handles mounting/unmounting.    │
│                                                                                  │
│  ⚠ NO POOLING: Each submit() creates a NEW virtual thread (this is fine!)      │
│  ⚠ NO REUSE: Virtual threads are garbage collected after task completion        │
│  ⚠ NO LIMIT: unlimited concurrency (use Semaphore to limit if needed)          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 3.3 Custom Thread Factory for Virtual Threads

```java
// ── Custom ThreadFactory — useful for Spring Boot integration ────────────────────
public class CustomVirtualThreadFactory {

    public static void main(String[] args) {

        // Create a named virtual thread factory
        ThreadFactory virtualFactory = Thread.ofVirtual()
            .name("http-handler-", 0)      // http-handler-0, http-handler-1, ...
            .factory();

        // Use with ExecutorService
        ExecutorService executor = Executors.newThreadPerTaskExecutor(virtualFactory);

        // Use with ScheduledExecutorService (NOTE: no built-in virtual scheduled executor)
        // For scheduled tasks, use platform threads (see below)

        executor.submit(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            // Output: Thread: http-handler-0
        });

        executor.close();
    }
}
```

#### 3.4 Thread Pool Executor with Virtual Threads — Why You Shouldn't (and What to Do Instead)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ⚠ IMPORTANT: Don't Pool Virtual Threads!                                       │
│                                                                                  │
│  Thread pools exist because platform threads are EXPENSIVE:                     │
│  → Creating 200 OS threads takes time and memory                               │
│  → Better to create them once and reuse (pooling)                               │
│                                                                                  │
│  Virtual threads are CHEAP:                                                      │
│  → Creating millions takes negligible time and memory                           │
│  → Pooling adds overhead WITHOUT benefit                                        │
│  → Just create a new one per task!                                              │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ WRONG — Pooling virtual threads (anti-pattern):                     │   │
│  │  ExecutorService pool = Executors.newFixedThreadPool(100,              │   │
│  │      Thread.ofVirtual().factory());                                     │   │
│  │  // Limits to 100 concurrent VTs — defeats the purpose!               │   │
│  │                                                                          │   │
│  │  ✓ CORRECT — One virtual thread per task:                              │   │
│  │  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();│   │
│  │  // Unlimited VTs, JVM handles everything                              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  If you need to LIMIT concurrency (e.g., DB connection pool has 50 max):       │
│  → Use a Semaphore to limit concurrent access to the RESOURCE                  │
│  → NOT a thread pool to limit thread count                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Limiting concurrency with Semaphore (correct pattern) ────────────────────────
public class ConcurrencyLimitExample {

    // Limit: max 50 concurrent DB operations (connection pool size = 50)
    private static final Semaphore DB_SEMAPHORE = new Semaphore(50);

    public static void main(String[] args) {

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit 10,000 tasks — all get their own virtual thread
            for (int i = 0; i < 10_000; i++) {
                executor.submit(() -> {
                    try {
                        DB_SEMAPHORE.acquire();     // wait for a "permit" (max 50 at a time)
                        try {
                            queryDatabase();         // only 50 VTs here at any time
                        } finally {
                            DB_SEMAPHORE.release();  // release permit for next VT
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        // 10,000 virtual threads created, but only 50 access DB concurrently
        // The other 9,950 are PARKED (not consuming carrier threads!)
    }

    private static void queryDatabase() {
        // Simulate DB query
        try { Thread.sleep(Duration.ofMillis(100)); } catch (InterruptedException e) {}
    }
}
```

#### 3.5 Using Virtual Threads with `StructuredTaskScope` (Java 21 Preview)

```java
// ── StructuredTaskScope — structured concurrency with virtual threads ────────────
// Preview in Java 21, meant to replace raw Future handling

import java.util.concurrent.StructuredTaskScope;

public class StructuredConcurrencyExample {

    record UserProfile(User user, List<Order> orders, List<Address> addresses) {}

    // Fetch user profile by making 3 parallel calls
    public UserProfile fetchUserProfile(Long userId) throws Exception {

        // StructuredTaskScope creates virtual threads for each subtask
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Fork 3 concurrent tasks — each runs in its own virtual thread
            StructuredTaskScope.Subtask<User> userTask =
                scope.fork(() -> userService.findById(userId));

            StructuredTaskScope.Subtask<List<Order>> ordersTask =
                scope.fork(() -> orderService.findByUserId(userId));

            StructuredTaskScope.Subtask<List<Address>> addressesTask =
                scope.fork(() -> addressService.findByUserId(userId));

            // Wait for ALL to complete (or first failure)
            scope.join();            // blocks until all subtasks finish
            scope.throwIfFailed();   // propagates the first exception

            // All completed successfully — collect results
            return new UserProfile(
                userTask.get(),
                ordersTask.get(),
                addressesTask.get()
            );
        }
        // If any subtask fails → others are cancelled automatically
        // All virtual threads are cleaned up (structured = no leaks)
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  StructuredTaskScope — How It Works:                                             │
│                                                                                  │
│  try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {                │
│      ┌────────────────────────────────────────────────────────────────┐         │
│      │  scope.fork(task1) → VirtualThread-1: fetch user from DB      │         │
│      │  scope.fork(task2) → VirtualThread-2: fetch orders from DB    │         │
│      │  scope.fork(task3) → VirtualThread-3: fetch addresses from DB │         │
│      │                                                                │         │
│      │  All 3 run in PARALLEL (each in its own virtual thread)       │         │
│      └────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│      scope.join();    ← waits for ALL 3 to complete                             │
│                                                                                  │
│      If task2 FAILS:                                                             │
│      → scope.throwIfFailed() propagates the error                               │
│      → VirtualThread-1 and VirtualThread-3 are CANCELLED                        │
│      → No orphan threads left running                                            │
│  }                                                                               │
│                                                                                  │
│  ShutdownOnFailure: cancel siblings on first failure (fail fast)                │
│  ShutdownOnSuccess: cancel siblings on first success (first wins)               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 3.6 Spring Boot Integration — Enabling Virtual Threads

```java
// ══════════════════════════════════════════════════════════════════════════════════
// METHOD 1: application.properties (Spring Boot 3.2+, Java 21+)
// ══════════════════════════════════════════════════════════════════════════════════

// application.properties:
// spring.threads.virtual.enabled=true
//
// That's it! Spring Boot will:
// → Use virtual threads for handling HTTP requests (Tomcat/Jetty/Undertow)
// → Use virtual threads for @Async methods
// → Use virtual threads for Spring MVC request handling


// ══════════════════════════════════════════════════════════════════════════════════
// METHOD 2: Programmatic configuration (more control)
// ══════════════════════════════════════════════════════════════════════════════════

@Configuration
public class VirtualThreadConfig {

    // Custom AsyncTaskExecutor using virtual threads
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    // Configure Tomcat to use virtual threads for request handling
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}


// ══════════════════════════════════════════════════════════════════════════════════
// METHOD 3: @Async with virtual threads
// ══════════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

@Service
public class EmailService {

    @Async("virtualThreadExecutor")
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String body) {
        // This runs in a virtual thread!
        emailClient.send(to, subject, body);   // blocking I/O — but that's fine!
        return CompletableFuture.completedFuture(null);
    }
}


// ══════════════════════════════════════════════════════════════════════════════════
// METHOD 4: Virtual threads in a REST controller (manual usage)
// ══════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final OrderService orderService;

    // With spring.threads.virtual.enabled=true, this handler already runs
    // on a virtual thread. But for parallel sub-tasks within a request:

    @GetMapping("/{id}/dashboard")
    public DashboardResponse getDashboard(@PathVariable Long id) throws Exception {

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var userTask = scope.fork(() -> userService.findById(id));
            var ordersTask = scope.fork(() -> orderService.getRecentOrders(id));
            var statsTask = scope.fork(() -> userService.getStats(id));

            scope.join();
            scope.throwIfFailed();

            return new DashboardResponse(
                userTask.get(),
                ordersTask.get(),
                statsTask.get()
            );
        }
        // 3 DB calls in parallel, each in its own virtual thread
        // Total latency = max(user, orders, stats) instead of sum
    }
}
```

#### 3.7 Complete Working Example — Comparison: Platform vs Virtual Threads

```java
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class PlatformVsVirtualComparison {

    public static void main(String[] args) throws Exception {

        int taskCount = 10_000;

        // ── Platform Threads (fixed pool of 200) ─────────────────────────────────
        System.out.println("=== Platform Threads (200 pool) ===");
        Instant start = Instant.now();

        try (ExecutorService platformExecutor = Executors.newFixedThreadPool(200)) {
            List<Future<?>> futures = IntStream.range(0, taskCount)
                .mapToObj(i -> platformExecutor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(1));  // simulate I/O
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .toList();

            for (Future<?> f : futures) f.get();
        }

        Duration platformDuration = Duration.between(start, Instant.now());
        System.out.println("Platform threads: " + platformDuration.toMillis() + " ms");
        // With 200 threads and 10,000 tasks sleeping 1s each:
        // 10,000 / 200 = 50 batches × 1s = ~50 seconds


        // ── Virtual Threads (one per task) ───────────────────────────────────────
        System.out.println("\n=== Virtual Threads (one per task) ===");
        start = Instant.now();

        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = IntStream.range(0, taskCount)
                .mapToObj(i -> virtualExecutor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(1));  // simulate I/O
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .toList();

            for (Future<?> f : futures) f.get();
        }

        Duration virtualDuration = Duration.between(start, Instant.now());
        System.out.println("Virtual threads: " + virtualDuration.toMillis() + " ms");
        // With 10,000 virtual threads ALL sleeping concurrently:
        // ALL 10,000 sleep at the same time → total time ≈ 1 second!
    }
}

// ── Expected Output ──────────────────────────────────────────────────────────────
// === Platform Threads (200 pool) ===
// Platform threads: ~50000 ms (50 seconds)
//
// === Virtual Threads (one per task) ===
// Virtual threads: ~1050 ms (about 1 second)
//
// Virtual threads: 50x FASTER for I/O-bound workloads!
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance Comparison — 10,000 tasks (each sleeping 1 second):                 │
│                                                                                  │
│  Platform Threads (pool=200):                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Batch 1: tasks 0-199 → sleep 1s                                       │   │
│  │  Batch 2: tasks 200-399 → sleep 1s                                     │   │
│  │  Batch 3: tasks 400-599 → sleep 1s                                     │   │
│  │  ...                                                                     │   │
│  │  Batch 50: tasks 9800-9999 → sleep 1s                                  │   │
│  │  Total: 50 batches × 1 second = ~50 seconds                            │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Virtual Threads (one per task):                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ALL 10,000 tasks start immediately, each in its own virtual thread    │   │
│  │  All 10,000 sleep concurrently (VTs yield their carriers during sleep) │   │
│  │  Total: ~1 second (all sleeping at the same time!)                     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌────────────┬──────────────────┬──────────────────┬─────────────────────┐    │
│  │ Metric     │ Platform (200)   │ Virtual (10K)    │ Improvement         │    │
│  ├────────────┼──────────────────┼──────────────────┼─────────────────────┤    │
│  │ Time       │ ~50 seconds      │ ~1 second        │ 50x faster          │    │
│  │ Threads    │ 200 OS threads   │ ~8 carriers      │ 25x less resources  │    │
│  │ Memory     │ ~200 MB stacks   │ ~50 MB           │ 4x less memory      │    │
│  │ OS threads │ 200              │ ~8 (carriers)    │ 25x fewer           │    │
│  └────────────┴──────────────────┴──────────────────┴─────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 3.8 Summary — Creating Virtual Threads (Quick Reference)

```java
// ══════════════════════════════════════════════════════════════════════════════════
//  QUICK REFERENCE — All Ways to Create Virtual Threads:
// ══════════════════════════════════════════════════════════════════════════════════

// 1. Start immediately (fire and forget)
Thread.startVirtualThread(() -> doWork());

// 2. Builder — unstarted (start later)
Thread vt = Thread.ofVirtual().name("my-vt").unstarted(() -> doWork());
vt.start();

// 3. Factory — create many named threads
ThreadFactory factory = Thread.ofVirtual().name("worker-", 0).factory();
Thread t = factory.newThread(() -> doWork());
t.start();

// 4. ExecutorService — RECOMMENDED for most use cases
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> doWork());
    Future<String> result = executor.submit(() -> computeResult());
}

// 5. Custom executor with named factory
ThreadFactory namedFactory = Thread.ofVirtual().name("api-handler-", 0).factory();
ExecutorService executor = Executors.newThreadPerTaskExecutor(namedFactory);

// 6. Spring Boot — just set the property!
// application.properties: spring.threads.virtual.enabled=true

// 7. Spring Boot — programmatic
@Bean
public AsyncTaskExecutor taskExecutor() {
    return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Decision Guide — Which Approach to Use:                                         │
│                                                                                  │
│  ┌─────────────────────────────────────────┬─────────────────────────────────┐  │
│  │ Scenario                                │ Approach                        │  │
│  ├─────────────────────────────────────────┼─────────────────────────────────┤  │
│  │ Spring Boot web app (auto handling)    │ spring.threads.virtual.enabled  │  │
│  │ Manual parallel I/O tasks               │ newVirtualThreadPerTaskExecutor │  │
│  │ Parallel subtasks within a request      │ StructuredTaskScope             │  │
│  │ Spring @Async methods                   │ Custom Executor bean            │  │
│  │ Quick one-off background task           │ Thread.startVirtualThread()     │  │
│  │ Named threads for logging/debugging    │ Thread.ofVirtual().name(...)    │  │
│  │ Limit concurrency to a resource         │ Executor + Semaphore            │  │
│  │ CPU-bound computation                   │ DON'T use virtual threads      │  │
│  └─────────────────────────────────────────┴─────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 4. Virtual Threads in Spring Boot Tomcat Configuration

When you enable virtual threads in Spring Boot, Tomcat's **request-handling thread pool** is replaced with a virtual-thread-per-task executor. Instead of a fixed pool of 200 platform threads, every incoming HTTP request gets its own virtual thread.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How Tomcat Normally Works (Platform Threads):                                   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Tomcat Thread Pool (default: 200 platform threads)                     │   │
│  │                                                                          │   │
│  │  Request 1 ──→ [Platform Thread-1]  → Controller → Service → DB       │   │
│  │  Request 2 ──→ [Platform Thread-2]  → Controller → Service → DB       │   │
│  │  ...                                                                     │   │
│  │  Request 200 → [Platform Thread-200] → Controller → Service → DB      │   │
│  │  Request 201 → WAITING (all 200 threads busy) ← BLOCKED!             │   │
│  │                                                                          │   │
│  │  Problem: If all 200 threads are blocked on I/O (DB, HTTP calls),     │   │
│  │  the server CANNOT accept new requests until a thread is freed.        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  How Tomcat Works with Virtual Threads:                                          │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual Thread Per Task Executor (unlimited virtual threads)           │   │
│  │                                                                          │   │
│  │  Request 1 ──→ [VirtualThread-1]     → Controller → Service → DB     │   │
│  │  Request 2 ──→ [VirtualThread-2]     → Controller → Service → DB     │   │
│  │  ...                                                                     │   │
│  │  Request 10000 → [VirtualThread-10000] → Controller → Service → DB   │   │
│  │                                                                          │   │
│  │  All 10,000 requests handled CONCURRENTLY!                             │   │
│  │  When VT blocks on DB → it UNMOUNTS → carrier handles another VT     │   │
│  │  Only ~8 carrier threads (= CPU cores) do the real OS-level work      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 4.1 Method 1: One Property — The Simplest Way (Spring Boot 3.2+)

```yaml
# application.properties
spring.threads.virtual.enabled=true

# OR in application.yml:
spring:
  threads:
    virtual:
      enabled: true
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What spring.threads.virtual.enabled=true does:                                  │
│                                                                                  │
│  1. Tomcat's request executor → replaced with VirtualThreadPerTaskExecutor      │
│  2. @Async default executor → uses virtual threads                              │
│  3. Spring MVC async support → uses virtual threads                             │
│  4. ApplicationTaskExecutor bean → TaskExecutorAdapter with virtual threads     │
│                                                                                  │
│  Requirements:                                                                   │
│  → Java 21+ (virtual threads are finalized in Java 21)                         │
│  → Spring Boot 3.2+ (first version with built-in virtual thread support)       │
│  → Spring Framework 6.1+                                                        │
│                                                                                  │
│  That's it. No other code changes needed. Your existing @RestController,        │
│  @Service, @Repository code works exactly as before — but now each request     │
│  runs on a virtual thread instead of a pooled platform thread.                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 4.2 Method 2: Programmatic Tomcat Configuration (Full Control)

```java
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class TomcatVirtualThreadConfig {

    /**
     * Replaces Tomcat's default thread pool with a virtual-thread-per-task executor.
     * Every incoming HTTP request gets its own virtual thread.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What happens inside Tomcat:                                                     │
│                                                                                  │
│  BEFORE (default Tomcat config):                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ProtocolHandler                                                         │   │
│  │    └── Executor: ThreadPoolExecutor                                     │   │
│  │          ├── corePoolSize: 10                                            │   │
│  │          ├── maxPoolSize: 200                                            │   │
│  │          ├── keepAliveTime: 60s                                          │   │
│  │          └── workQueue: TaskQueue (unbounded)                            │   │
│  │                                                                          │   │
│  │  → Max 200 concurrent requests. Request 201 waits in the queue.        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  AFTER (virtual thread executor):                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ProtocolHandler                                                         │   │
│  │    └── Executor: VirtualThreadPerTaskExecutor                           │   │
│  │          ├── No pool size limit (unlimited virtual threads)             │   │
│  │          ├── No queue needed (every task gets its own VT immediately)   │   │
│  │          └── Carrier pool: ForkJoinPool (# CPUs threads)                │   │
│  │                                                                          │   │
│  │  → Unlimited concurrent requests. Every request gets a VT instantly.   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 4.3 Method 3: Tomcat + Jetty + Undertow Configuration

```java
// ── For TOMCAT (default embedded server) ─────────────────────────────────────────
@Configuration
@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
public class TomcatVirtualThreadConfig {

    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}


// ── For JETTY ────────────────────────────────────────────────────────────────────
@Configuration
@ConditionalOnClass(name = "org.eclipse.jetty.server.Server")
public class JettyVirtualThreadConfig {

    @Bean
    public JettyServerCustomizer jettyVirtualThreadCustomizer() {
        return server -> {
            // Jetty 12+ supports virtual threads via QueuedThreadPool
            QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
            threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}


// ── For UNDERTOW ─────────────────────────────────────────────────────────────────
// Undertow uses XNIO — virtual thread support requires Undertow 2.3+ / Wildfly
// Spring Boot 3.2+ with spring.threads.virtual.enabled=true handles this automatically
```

#### 4.4 Verifying Virtual Threads are Active

```java
@RestController
@RequestMapping("/api/debug")
public class ThreadDebugController {

    @GetMapping("/thread-info")
    public Map<String, Object> getThreadInfo() {
        Thread current = Thread.currentThread();
        return Map.of(
            "threadName", current.getName(),
            "isVirtual", current.isVirtual(),        // ← should be TRUE
            "threadClass", current.getClass().getName(),
            "threadId", current.threadId()
        );
    }
}

// Response when virtual threads are enabled:
// {
//   "threadName": "tomcat-handler-1",
//   "isVirtual": true,                     ← Confirms virtual threads!
//   "threadClass": "java.lang.VirtualThread",
//   "threadId": 42
// }

// Response WITHOUT virtual threads (default):
// {
//   "threadName": "http-nio-8080-exec-1",
//   "isVirtual": false,                    ← Platform thread
//   "threadClass": "java.lang.Thread",
//   "threadId": 35
// }
```

---

### 5. Using Virtual Threads with Spring `@Async`

`@Async` makes a method execute on a **separate thread** (non-blocking for the caller). By default, Spring uses a platform thread pool. With virtual threads, each `@Async` call gets its own lightweight virtual thread.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Async — Before vs After Virtual Threads:                                       │
│                                                                                  │
│  BEFORE (platform threads):                                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Async → picks thread from ThreadPoolTaskExecutor (pool: 8-20)        │   │
│  │  → If pool is full, tasks QUEUE up (wait for a free thread)            │   │
│  │  → If queue is full, tasks are REJECTED (RejectedExecutionException)   │   │
│  │  → Must carefully tune corePoolSize, maxPoolSize, queueCapacity        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  AFTER (virtual threads):                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Async → creates a NEW virtual thread instantly (no pool, no queue)   │   │
│  │  → No pool exhaustion — ever                                            │   │
│  │  → No RejectedExecutionException — ever                                │   │
│  │  → No tuning needed                                                     │   │
│  │  → 10,000 concurrent @Async calls? No problem.                         │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 5.1 Method 1: Global Virtual Thread Executor for All `@Async` Methods

```java
@Configuration
@EnableAsync
public class AsyncVirtualThreadConfig {

    /**
     * Replace the default applicationTaskExecutor with virtual threads.
     * ALL @Async methods will use virtual threads automatically.
     */
    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

// Now every @Async method runs on a virtual thread:
@Service
public class NotificationService {

    @Async   // runs on virtual thread (no executor name needed — uses default)
    public void sendPushNotification(Long userId, String message) {
        // This is I/O-bound (HTTP call to push notification service)
        pushNotificationClient.send(userId, message);   // blocking, but fine!
        log.info("Push sent on thread: {} (virtual={})",
            Thread.currentThread().getName(),
            Thread.currentThread().isVirtual());   // true!
    }

    @Async   // also runs on virtual thread
    public CompletableFuture<EmailResult> sendEmail(String to, String subject, String body) {
        EmailResult result = smtpClient.send(to, subject, body);  // blocking I/O
        return CompletableFuture.completedFuture(result);
    }
}
```

#### 5.2 Method 2: Named Executor — Mix Platform and Virtual Threads

```java
@Configuration
@EnableAsync
public class MixedAsyncConfig {

    /**
     * Virtual thread executor — for I/O-bound async tasks
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Platform thread executor — for CPU-bound async tasks
     */
    @Bean(name = "cpuBoundExecutor")
    public Executor cpuBoundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cpu-worker-");
        executor.initialize();
        return executor;
    }
}

@Service
public class ReportService {

    // I/O-bound: fetches data from multiple external APIs
    @Async("virtualThreadExecutor")
    public CompletableFuture<ReportData> fetchExternalData(Long reportId) {
        // Makes 5 HTTP calls sequentially — each blocks, but VT yields carrier
        UserData user = externalApiClient.fetchUser(reportId);
        List<Transaction> txns = externalApiClient.fetchTransactions(reportId);
        AccountInfo account = externalApiClient.fetchAccount(reportId);
        // ... blocking I/O calls — PERFECT for virtual threads
        return CompletableFuture.completedFuture(new ReportData(user, txns, account));
    }

    // CPU-bound: heavy computation (encryption, PDF generation)
    @Async("cpuBoundExecutor")
    public CompletableFuture<byte[]> generatePdfReport(ReportData data) {
        // Heavy CPU work — NO I/O blocking
        // Virtual threads would NOT help here — use platform threads
        byte[] pdf = pdfEngine.render(data);   // CPU-intensive
        return CompletableFuture.completedFuture(pdf);
    }
}
```

#### 5.3 Method 3: Using `TaskExecutorAdapter` (Spring's Wrapper)

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        // Spring's adapter wraps any Executor into an AsyncTaskExecutor
        // Provides better integration with Spring's async infrastructure
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

#### 5.4 Complete Example: `@Async` with Virtual Threads in a Real Application

```java
// ── Configuration ────────────────────────────────────────────────────────────────
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}


// ── Service using @Async ─────────────────────────────────────────────────────────
@Service
@Slf4j
public class OrderProcessingService {

    private final PaymentGateway paymentGateway;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // Caller does NOT wait for this — it runs on a virtual thread in background
    @Async
    public void processOrderAsync(Order order) {
        log.info("Processing order {} on thread: {} (virtual={})",
            order.getId(),
            Thread.currentThread().getName(),
            Thread.currentThread().isVirtual());   // true

        // All these are I/O-bound calls — virtual threads handle them efficiently
        paymentGateway.charge(order.getPaymentDetails());
        inventoryService.reserve(order.getItems());
        notificationService.sendOrderConfirmation(order.getCustomerEmail());
        auditService.logOrderProcessed(order);
    }

    // Caller gets a Future — can wait for result later
    @Async
    public CompletableFuture<OrderStatus> checkOrderStatusAsync(Long orderId) {
        // HTTP call to external fulfillment service — I/O bound
        OrderStatus status = fulfillmentClient.getStatus(orderId);
        return CompletableFuture.completedFuture(status);
    }
}


// ── Controller calling @Async methods ────────────────────────────────────────────
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProcessingService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Order order = orderMapper.toOrder(request);
        orderRepository.save(order);

        // Fire-and-forget: runs on a virtual thread in background
        orderService.processOrderAsync(order);

        // Return immediately — don't wait for processing
        return ResponseEntity.accepted()
            .body(new OrderResponse(order.getId(), "PROCESSING"));
    }

    @GetMapping("/{id}/status")
    public CompletableFuture<OrderStatus> getStatus(@PathVariable Long id) {
        // Returns a Future — Spring MVC will wait for it to complete
        // The async work runs on a virtual thread
        return orderService.checkOrderStatusAsync(id);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Async with Virtual Threads — What Happens:                                     │
│                                                                                  │
│  Controller (Request VT)          Async Service (New VT)                        │
│  ┌───────────────────────┐        ┌──────────────────────────────┐             │
│  │ POST /api/orders      │        │ processOrderAsync(order)     │             │
│  │                       │        │                              │             │
│  │ 1. save order to DB   │        │ 1. charge payment (I/O)     │             │
│  │ 2. call @Async method─┼───────→│ 2. reserve inventory (I/O)  │             │
│  │ 3. return 202 ACCEPTED│        │ 3. send email (I/O)         │             │
│  │    immediately        │        │ 4. audit log (I/O)          │             │
│  └───────────────────────┘        └──────────────────────────────┘             │
│       ↑ returns instantly              ↑ runs independently on its own VT      │
│                                                                                  │
│  Both the request handler AND the @Async method run on virtual threads.         │
│  Neither blocks an OS thread during I/O waits.                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 6. Advantages and Disadvantages of Virtual Threads in Spring Boot

#### Advantages Specific to Spring Boot

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advantages of Virtual Threads in Spring Boot:                                   │
│                                                                                  │
│  1. DRAMATICALLY HIGHER THROUGHPUT FOR I/O-HEAVY APPS                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Typical Spring Boot REST API:                                          │   │
│  │  → Receives HTTP request                                                │   │
│  │  → Queries database (blocks ~5-50ms)                                   │   │
│  │  → Calls external service (blocks ~50-500ms)                           │   │
│  │  → Returns response                                                     │   │
│  │                                                                          │   │
│  │  With platform threads (200 pool):                                      │   │
│  │  → Max 200 concurrent requests being processed                         │   │
│  │  → If each takes 100ms → max ~2,000 requests/second                   │   │
│  │                                                                          │   │
│  │  With virtual threads:                                                   │   │
│  │  → 10,000+ concurrent requests being processed                         │   │
│  │  → Same 100ms latency per request → 10,000+ requests/second           │   │
│  │  → 5x throughput improvement WITHOUT any code changes!                 │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  2. ZERO CODE CHANGES — ONE PROPERTY                                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  spring.threads.virtual.enabled=true                                    │   │
│  │                                                                          │   │
│  │  That's the ENTIRE migration. No refactoring needed:                   │   │
│  │  → Same @RestController, @Service, @Repository code                    │   │
│  │  → Same blocking JDBC/JPA code                                          │   │
│  │  → Same RestTemplate / WebClient blocking calls                        │   │
│  │  → Same @Transactional handling                                         │   │
│  │  → Same @Async processing                                               │   │
│  │                                                                          │   │
│  │  Compare to migrating to WebFlux (reactive):                            │   │
│  │  → Rewrite ALL controllers to return Mono/Flux                         │   │
│  │  → Replace JDBC with R2DBC                                              │   │
│  │  → Replace RestTemplate with WebClient                                  │   │
│  │  → Retrain entire team on reactive paradigm                            │   │
│  │  → 3-6 months of refactoring vs 1 line property change!               │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  3. NO MORE THREAD POOL TUNING                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Before virtual threads — you had to carefully tune:                    │   │
│  │  → server.tomcat.threads.max=200 (how many concurrent requests?)       │   │
│  │  → server.tomcat.threads.min-spare=10                                  │   │
│  │  → spring.task.execution.pool.core-size=8                              │   │
│  │  → spring.task.execution.pool.max-size=20                              │   │
│  │  → spring.task.execution.pool.queue-capacity=100                       │   │
│  │                                                                          │   │
│  │  Wrong values → thread starvation OR wasted resources                  │   │
│  │                                                                          │   │
│  │  With virtual threads: NO TUNING NEEDED. The JVM handles it.           │   │
│  │  Every request gets a thread instantly. No pool. No queue.             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  4. ELIMINATES THE NEED FOR WEBFLUX IN MOST CASES                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  WebFlux was created to solve: "200 platform threads aren't enough."   │   │
│  │  Virtual threads solve the SAME problem — without reactive complexity.  │   │
│  │                                                                          │   │
│  │  Spring MVC + Virtual Threads = WebFlux throughput + imperative code   │   │
│  │                                                                          │   │
│  │  You NO LONGER need to choose between:                                  │   │
│  │  "Simple code (MVC)" vs "High throughput (WebFlux)"                    │   │
│  │  You get BOTH with virtual threads.                                     │   │
│  │                                                                          │   │
│  │  WebFlux still valid for: streaming, SSE, true backpressure needs.    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  5. IMPROVED COLD START AND SCALING IN KUBERNETES                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Virtual threads start almost instantly → no warm-up period.           │   │
│  │  New pods can handle full traffic immediately (no thread pool ramp-up).│   │
│  │  Fewer pods needed: 1 pod can handle 10x more concurrent requests.    │   │
│  │  Result: lower infrastructure costs + faster autoscaling.              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  6. BETTER @ASYNC EXPERIENCE                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  No more RejectedExecutionException when pool is full.                 │   │
│  │  No more careful pool sizing for async operations.                     │   │
│  │  Fire 10,000 @Async tasks simultaneously → all execute immediately.   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  7. WORKS WITH EXISTING SPRING ECOSYSTEM                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  → Spring Data JPA (Hibernate) ✓                                       │   │
│  │  → Spring Security ✓                                                    │   │
│  │  → Spring Transaction (@Transactional) ✓                               │   │
│  │  → Spring Cache (@Cacheable) ✓                                          │   │
│  │  → Spring Actuator ✓                                                    │   │
│  │  → Spring AOP ✓                                                         │   │
│  │  → RestTemplate, WebClient, FeignClient ✓                              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Disadvantages Specific to Spring Boot

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Disadvantages of Virtual Threads in Spring Boot:                                │
│                                                                                  │
│  1. CONNECTION POOL BECOMES THE BOTTLENECK                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Without virtual threads: 200 threads → 200 max concurrent DB queries  │   │
│  │  With virtual threads: 10,000 VTs → all 10,000 want a DB connection!  │   │
│  │                                                                          │   │
│  │  But HikariCP default: maximum-pool-size=10 (only 10 connections!)    │   │
│  │                                                                          │   │
│  │  Result: 10,000 virtual threads contending for 10 DB connections      │   │
│  │  → Massive contention on connection pool                              │   │
│  │  → Connection wait timeout errors                                     │   │
│  │  → Potential deadlocks if connection wait exceeds timeout              │   │
│  │                                                                          │   │
│  │  Mitigation:                                                             │   │
│  │  → Increase connection pool: spring.datasource.hikari.maximum-pool-   │   │
│  │    size=50 (but limited by DB max connections)                         │   │
│  │  → Use Semaphore to limit concurrent DB access                         │   │
│  │  → Monitor connection wait times via metrics                           │   │
│  │  → The database is the REAL bottleneck, not threads                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  2. OBSERVABILITY CHALLENGES — MILLIONS OF THREADS IN DUMPS                     │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Thread dumps become massive with virtual threads:                     │   │
│  │  → jstack output: millions of virtual threads listed                   │   │
│  │  → Hard to find the problematic thread in the noise                   │   │
│  │  → Monitoring tools may not yet support VT-aware views                │   │
│  │                                                                          │   │
│  │  Mitigation:                                                             │   │
│  │  → Use jcmd Thread.dump_to_file for structured JSON output            │   │
│  │  → Filter by thread state (BLOCKED, WAITING)                           │   │
│  │  → Upgrade monitoring tools (VisualVM, IntelliJ profiler)             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  3. SPRING SECURITY ThreadLocal CONCERNS                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Spring Security stores SecurityContext in ThreadLocal.                 │   │
│  │  With platform threads + thread pool, context is reused (cleared after │   │
│  │  request). With virtual threads (new VT per request), each VT gets    │   │
│  │  its own ThreadLocal — memory overhead with millions of VTs.          │   │
│  │                                                                          │   │
│  │  Spring Security 6.x handles this correctly (cleans up ThreadLocal    │   │
│  │  after request), but it's still more allocations than with pooling.   │   │
│  │                                                                          │   │
│  │  Spring Framework is migrating to ScopedValues (future versions).     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  4. NOT ALL SPRING BOOT STARTERS ARE VT-OPTIMIZED                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Some Spring Boot starters use synchronized internally:               │   │
│  │  → spring-boot-starter-data-redis (Lettuce: some sync blocks)        │   │
│  │  → JDBC drivers (older versions with synchronized I/O)                │   │
│  │  → Certain logging frameworks (logback synchronized appenders)        │   │
│  │                                                                          │   │
│  │  These cause PINNING — virtual threads stuck on carrier.              │   │
│  │  Spring Boot 3.3+ and 3.4+ addressed many of these issues.           │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  5. SCHEDULED TASKS DON'T BENEFIT                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Scheduled tasks run on a ScheduledExecutorService.                   │   │
│  │  There's no built-in "virtual thread scheduled executor" in Java 21.  │   │
│  │  @Scheduled methods still use platform threads (by default).          │   │
│  │                                                                          │   │
│  │  If your scheduled task does I/O, you can:                             │   │
│  │  → Make it a thin wrapper that submits work to a VT executor          │   │
│  │  → Use @Async on a method called FROM the scheduled method            │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  6. POTENTIAL RESOURCE EXHAUSTION (different kind)                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Without thread pool limits, a traffic spike creates unlimited VTs:    │   │
│  │  → Each VT eventually calls DB, external API, etc.                    │   │
│  │  → Downstream services get overwhelmed (no backpressure)              │   │
│  │  → Out-of-memory if millions of VTs hold data on heap                 │   │
│  │                                                                          │   │
│  │  Platform thread pools provided IMPLICIT rate limiting:               │   │
│  │  → Only 200 requests processed at a time → natural backpressure      │   │
│  │                                                                          │   │
│  │  With VTs, you MUST add explicit limits:                               │   │
│  │  → Rate limiting (Resilience4j, bucket4j)                              │   │
│  │  → Semaphore on resource access                                        │   │
│  │  → Circuit breakers on external calls                                  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 7. Issues Faced When Using Virtual Threads in Spring Boot

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Real-World Issues When Adopting Virtual Threads in Spring Boot:                  │
│                                                                                  │
│  ┌────┬──────────────────────────────────┬────────────────────────────────────┐ │
│  │ #  │ Issue                             │ Impact                            │ │
│  ├────┼──────────────────────────────────┼────────────────────────────────────┤ │
│  │ 1  │ synchronized pinning in libs     │ Carriers blocked → low throughput │ │
│  │ 2  │ HikariCP connection pool pinning │ DB operations pin carriers        │ │
│  │ 3  │ JDBC driver synchronized I/O     │ Every DB call pins the VT        │ │
│  │ 4  │ Logback synchronized appender    │ Logging pins carrier thread       │ │
│  │ 5  │ ThreadLocal memory explosion     │ Millions of VTs × ThreadLocal    │ │
│  │ 6  │ Connection pool exhaustion       │ Too many VTs, too few connections │ │
│  │ 7  │ Spring Security context overhead │ SecurityContext per VT           │ │
│  │ 8  │ Jackson ObjectMapper sync blocks │ JSON serialization pinning       │ │
│  │ 9  │ File I/O internals (JDK)         │ Some file ops use synchronized   │ │
│  │ 10 │ Native code (JNI) pinning        │ Native libs always pin carrier   │ │
│  └────┴──────────────────────────────────┴────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 7.1 Issue 1: HikariCP and JDBC Driver Pinning

```java
// ── THE PROBLEM ──────────────────────────────────────────────────────────────────
// HikariCP (default connection pool in Spring Boot) internally uses synchronized:

// Inside HikariCP (simplified):
public Connection getConnection() {
    synchronized (this) {                    // ← PINS the virtual thread!
        Connection conn = pool.borrowObject();
        return conn;
    }
}

// Every time your code does a DB query:
@Transactional
public User findById(Long id) {
    return userRepository.findById(id);      // → getConnection() → synchronized
    // Virtual thread is PINNED for the ENTIRE duration of:
    // 1. Getting connection from pool (synchronized)
    // 2. Executing the query (some JDBC drivers also use synchronized)
    // 3. Returning connection to pool (synchronized)
}

// If you have 8 carriers and 8 VTs are all pinned on DB calls:
// → ALL carriers are blocked → NO other VTs can run → DEADLOCK-like behavior!
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  The Pinning Problem — Visual:                                                   │
│                                                                                  │
│  Carriers:  [CT1]  [CT2]  [CT3]  [CT4]  (4 carriers on 4-core machine)        │
│                                                                                  │
│  Normal operation (no pinning):                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  CT1: V1(run)→V1(I/O,unmount)→V5(run)→V5(I/O,unmount)→V9(run)...     │   │
│  │  CT2: V2(run)→V2(I/O,unmount)→V6(run)→V6(I/O,unmount)→V10(run)...    │   │
│  │  CT3: V3(run)→V3(I/O,unmount)→V7(run)→...                             │   │
│  │  CT4: V4(run)→V4(I/O,unmount)→V8(run)→...                             │   │
│  │  → All carriers constantly busy serving different VTs                  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  With pinning (synchronized in HikariCP/JDBC):                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  CT1: V1(run)→V1(sync DB call... STUCK... waiting...)  ← PINNED!     │   │
│  │  CT2: V2(run)→V2(sync DB call... STUCK... waiting...)  ← PINNED!     │   │
│  │  CT3: V3(run)→V3(sync DB call... STUCK... waiting...)  ← PINNED!     │   │
│  │  CT4: V4(run)→V4(sync DB call... STUCK... waiting...)  ← PINNED!     │   │
│  │                                                                          │   │
│  │  ALL carriers are OCCUPIED (pinned). 9,996 other VTs are WAITING.     │   │
│  │  No carrier available to run them. System is effectively FROZEN.       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── THE SOLUTION (Java 21) ───────────────────────────────────────────────────────

// Solution 1: Use updated library versions that replaced synchronized with ReentrantLock
// HikariCP 5.1+ — replaced synchronized with ReentrantLock
// PostgreSQL JDBC 42.7+ — virtual thread friendly
// MySQL Connector/J 8.2+ — reduced pinning

// pom.xml — use VT-friendly versions:
// <dependency>
//     <groupId>com.zaxxer</groupId>
//     <artifactId>HikariCP</artifactId>
//     <version>5.1.0</version>  <!-- VT-friendly -->
// </dependency>
// <dependency>
//     <groupId>org.postgresql</groupId>
//     <artifactId>postgresql</artifactId>
//     <version>42.7.0</version>  <!-- VT-friendly -->
// </dependency>

// Solution 2: Detect pinning — add JVM flag:
// java -Djdk.tracePinnedThreads=full -jar myapp.jar
// This logs a warning whenever a virtual thread is pinned:
// WARNING: Thread[#42,VirtualThread-42] pinned while holding monitor
//     at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:128)
```

#### 7.2 Issue 2: Logback Synchronized Appender

```java
// ── THE PROBLEM ──────────────────────────────────────────────────────────────────
// Logback's default ConsoleAppender uses synchronized internally:

// Inside Logback (simplified):
public class OutputStreamAppender {
    synchronized void writeBytes(byte[] byteArray) {   // ← PINS!
        outputStream.write(byteArray);
    }
}

// Every time you log something:
log.info("Processing request for user {}", userId);
// → Logback appender → synchronized write → PINS the virtual thread

// With 10,000 concurrent requests all logging → massive pinning!

// ── THE SOLUTION ─────────────────────────────────────────────────────────────────
// Use Logback 1.4.12+ (shipped with Spring Boot 3.2+) which uses ReentrantLock
// OR use async appender:

// logback-spring.xml:
// <configuration>
//     <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
//         <queueSize>10000</queueSize>
//         <discardingThreshold>0</discardingThreshold>
//         <appender-ref ref="CONSOLE" />
//     </appender>
//
//     <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
//         <encoder>
//             <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
//         </encoder>
//     </appender>
//
//     <root level="INFO">
//         <appender-ref ref="ASYNC" />  <!-- Use async appender to avoid pinning -->
//     </root>
// </configuration>
```

#### 7.3 Issue 3: ThreadLocal Memory with Spring Security

```java
// ── THE PROBLEM ──────────────────────────────────────────────────────────────────
// Spring Security stores authentication in ThreadLocal:

public class SecurityContextHolder {
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    public static SecurityContext getContext() {
        return contextHolder.get();   // Each VT has its own copy
    }
}

// With platform threads (200 threads):
// → 200 ThreadLocal slots = ~200 × 1 KB = 200 KB (negligible)

// With virtual threads (100,000 VTs during a spike):
// → 100,000 ThreadLocal slots = ~100,000 × 1 KB = 100 MB
// → Plus: MDC (logging), transaction context, locale, etc.
// → Total ThreadLocal overhead can be significant

// ── THE MITIGATION ───────────────────────────────────────────────────────────────
// Spring Security 6.x properly cleans up ThreadLocal after each request.
// The concern is peak memory usage during traffic spikes, not leaks.

// Future solution: ScopedValue (Java 25+) — inherited by child threads efficiently,
// no per-thread storage overhead.
```

#### 7.4 Issue 4: Connection Pool Exhaustion

```java
// ── THE PROBLEM ──────────────────────────────────────────────────────────────────
// Default HikariCP: maximum-pool-size = 10
// Virtual threads: potentially 10,000+ concurrent requests

@Service
public class UserService {

    @Transactional
    public User findById(Long id) {
        // 10,000 virtual threads ALL call this simultaneously
        // All 10,000 want a database connection
        // HikariCP has only 10 connections
        // 9,990 VTs wait for a connection → connection wait timeout!
        return userRepository.findById(id).orElseThrow();
    }
}

// Error: HikariPool-1 - Connection is not available, request timed out after 30000ms

// ── THE SOLUTION ─────────────────────────────────────────────────────────────────
// application.properties:
// spring.datasource.hikari.maximum-pool-size=50    (increase, but limited by DB)
// spring.datasource.hikari.connection-timeout=60000 (increase timeout)

// Better: Use Semaphore to limit concurrent DB access:
@Service
public class UserService {

    private static final Semaphore DB_LIMITER = new Semaphore(50);

    public User findById(Long id) {
        try {
            DB_LIMITER.acquire();
            try {
                return userRepository.findById(id).orElseThrow();
            } finally {
                DB_LIMITER.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for DB access", e);
        }
    }
}
```

---

### 8. The Locking Issue in Java 21 and How Java 24 Resolved It

This is the **most significant issue** that affected virtual threads in production and the biggest improvement in Java 24 for virtual thread users.

#### 8.1 The Problem: `synchronized` Causes Pinning (Java 21-23)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  THE CORE PROBLEM — Java 21, 22, 23:                                             │
│                                                                                  │
│  When a virtual thread enters a `synchronized` block or method, it gets         │
│  PINNED to its carrier thread. While pinned, the virtual thread CANNOT be       │
│  unmounted — the carrier is stuck serving ONLY this one virtual thread.          │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  synchronized (lock) {                                                   │   │
│  │      // ← Virtual thread is PINNED to carrier here                     │   │
│  │      Thread.sleep(1000);      // Carrier is BLOCKED (not freed!)       │   │
│  │      someIoOperation();       // Carrier is BLOCKED (not freed!)       │   │
│  │      databaseCall();          // Carrier is BLOCKED (not freed!)       │   │
│  │  }                                                                       │   │
│  │  // ← Pinning ends here                                                │   │
│  │                                                                          │   │
│  │  vs. with ReentrantLock:                                                │   │
│  │  lock.lock();                                                            │   │
│  │  try {                                                                   │   │
│  │      Thread.sleep(1000);      // Carrier is FREED (VT unmounts!)      │   │
│  │      someIoOperation();       // Carrier is FREED (VT unmounts!)      │   │
│  │      databaseCall();          // Carrier is FREED (VT unmounts!)      │   │
│  │  } finally {                                                             │   │
│  │      lock.unlock();                                                      │   │
│  │  }                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  WHY this happens:                                                               │
│  `synchronized` is implemented using object monitors (monitorenter/monitorexit  │
│  bytecodes). In Java 21-23, the JVM's monitor implementation does NOT support   │
│  unmounting a virtual thread while it holds a monitor.                           │
│                                                                                  │
│  ReentrantLock uses java.util.concurrent.locks.AbstractQueuedSynchronizer (AQS) │
│  which properly parks/unparks threads — supporting VT unmounting.               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.2 Why This Was a Massive Problem in Spring Boot

```java
// ── Spring Boot's ecosystem is FULL of synchronized code ─────────────────────────

// 1. JDBC Drivers (MySQL Connector/J before 8.2):
public class ConnectionImpl {
    public synchronized void execSQL(String sql, ...) {   // ← PINS!
        // Every SQL query pins the virtual thread
        sendPacket(queryPacket);
        readResults();
    }
}

// 2. HikariCP (before 5.1):
public class ConcurrentBag {
    public synchronized IConcurrentBagEntry borrow(long timeout) {   // ← PINS!
        // Getting a connection pins the virtual thread
    }
}

// 3. Hibernate/JPA SessionFactory:
public class SessionFactoryImpl {
    public synchronized Session openSession() {   // ← PINS!
        // Opening a Hibernate session pins
    }
}

// 4. Logback logging:
public class OutputStreamAppender {
    public synchronized void doAppend(E event) {   // ← PINS!
        // Every log.info() call pins
    }
}

// 5. Java's own URL class:
public class URL {
    synchronized InputStream openStream() {   // ← PINS!
        // HTTP connections pin the VT
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Impact on a Typical Spring Boot Application:                                    │
│                                                                                  │
│  A single REST endpoint:                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                          │   │
│  │  @GetMapping("/users/{id}")                                             │   │
│  │  public User getUser(@PathVariable Long id) {                           │   │
│  │      log.info("Fetching user {}", id);        // ← Logback PINS       │   │
│  │      User user = userRepository.findById(id); // ← HikariCP PINS      │   │
│  │                                                // ← JDBC driver PINS   │   │
│  │      log.info("Found user: {}", user.getName());  // ← Logback PINS   │   │
│  │      return user;                                                       │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  Pinning count per request: 4+ times!                                  │   │
│  │  On a 4-core machine (4 carriers), only ~4 requests can actually       │   │
│  │  make progress at any time. Virtual threads provide NO benefit!        │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  This is why many teams reported: "We enabled virtual threads in Spring Boot    │
│  but saw NO improvement (or even WORSE performance)!"                           │
│                                                                                  │
│  Root cause: synchronized blocks everywhere in the dependency stack.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.3 The Workaround in Java 21-23: Replace `synchronized` with `ReentrantLock`

```java
// ── The workaround that library authors had to do ────────────────────────────────

// BEFORE (pins virtual threads):
public class ConnectionPool {

    private final Object lock = new Object();

    public Connection getConnection() {
        synchronized (lock) {                    // ← PINS!
            return pool.borrowObject();
        }
    }

    public void releaseConnection(Connection conn) {
        synchronized (lock) {                    // ← PINS!
            pool.returnObject(conn);
        }
    }
}


// AFTER (virtual-thread-friendly):
public class ConnectionPool {

    private final ReentrantLock lock = new ReentrantLock();

    public Connection getConnection() {
        lock.lock();                              // ← Does NOT pin!
        try {
            return pool.borrowObject();
        } finally {
            lock.unlock();
        }
    }

    public void releaseConnection(Connection conn) {
        lock.lock();                              // ← Does NOT pin!
        try {
            pool.returnObject(conn);
        } finally {
            lock.unlock();
        }
    }
}

// HikariCP 5.1, Logback 1.4.12, PostgreSQL JDBC 42.7, MySQL 8.2
// all did this migration — replaced synchronized with ReentrantLock.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Library Migration Status (as of Spring Boot 3.2/3.3):                           │
│                                                                                  │
│  ┌────────────────────────────────────┬────────────────────┬─────────────────┐  │
│  │ Library                            │ VT-Friendly Since  │ Change Made     │  │
│  ├────────────────────────────────────┼────────────────────┼─────────────────┤  │
│  │ HikariCP                           │ 5.1.0              │ ReentrantLock   │  │
│  │ PostgreSQL JDBC                    │ 42.7.0             │ ReentrantLock   │  │
│  │ MySQL Connector/J                  │ 8.2.0              │ ReentrantLock   │  │
│  │ Logback                            │ 1.4.12             │ ReentrantLock   │  │
│  │ Spring Framework                   │ 6.1.0              │ ReentrantLock   │  │
│  │ Tomcat                             │ 10.1.16            │ ReentrantLock   │  │
│  │ Jackson (ObjectMapper)             │ 2.16.0             │ ReentrantLock   │  │
│  │ Apache HttpClient 5               │ 5.3                │ ReentrantLock   │  │
│  └────────────────────────────────────┴────────────────────┴─────────────────┘  │
│                                                                                  │
│  Problem: YOUR code and THIRD-PARTY code might still use synchronized.          │
│  Every synchronized block is a potential pinning site.                           │
│  There's no compile-time warning — you only find it at runtime.                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.4 The Fix: Java 24 — JEP 491: Synchronized Pinning Eliminated

**[JEP 491](https://openjdk.org/jeps/491)** (delivered in Java 24, March 2025) fundamentally changes how `synchronized` interacts with virtual threads. **Virtual threads are no longer pinned when entering `synchronized` blocks.**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JEP 491: Synchronize Virtual Threads without Pinning (Java 24)                  │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  BEFORE (Java 21, 22, 23):                                              │   │
│  │                                                                          │   │
│  │  synchronized (lock) {                                                   │   │
│  │      Thread.sleep(1000);      // Carrier is BLOCKED (pinned!)          │   │
│  │      databaseCall();          // Carrier is BLOCKED (pinned!)          │   │
│  │  }                                                                       │   │
│  │  // Virtual thread CANNOT be unmounted while holding a monitor         │   │
│  │  // Carrier thread is WASTED — sits idle during I/O                    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  AFTER (Java 24+):                                                      │   │
│  │                                                                          │   │
│  │  synchronized (lock) {                                                   │   │
│  │      Thread.sleep(1000);      // Carrier is FREED! VT unmounts!       │   │
│  │      databaseCall();          // Carrier is FREED! VT unmounts!       │   │
│  │  }                                                                       │   │
│  │  // Virtual thread CAN be unmounted even while holding a monitor!     │   │
│  │  // Carrier thread is FREE to run other virtual threads               │   │
│  │  // The monitor ownership is preserved in the VT's state on the heap  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  IMPACT:                                                                         │
│  → ALL existing synchronized code now works perfectly with virtual threads     │
│  → No need to rewrite synchronized → ReentrantLock (though still valid)       │
│  → Third-party libraries with synchronized → automatically VT-friendly        │
│  → Legacy Java code → automatically benefits                                   │
│  → Spring Boot apps → full VT benefit WITHOUT library upgrades                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.5 How Java 24 Fixed It — Technical Details

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Changed Internally in Java 24:                                             │
│                                                                                  │
│  Java 21-23 (Object Monitors — old implementation):                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  monitorenter (bytecode):                                                │   │
│  │  1. VT acquires the object monitor on the carrier's OS thread           │   │
│  │  2. Monitor is tied to the OS thread (kernel-level lock)                │   │
│  │  3. VT cannot be unmounted — OS thread holds the lock                  │   │
│  │  4. If VT blocks inside → carrier is stuck → PINNED                   │   │
│  │                                                                          │   │
│  │  The monitor was associated with the CARRIER (platform thread),         │   │
│  │  not with the virtual thread. Unmounting the VT would "lose" the lock. │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Java 24+ (Object Monitors — new implementation):                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  monitorenter (bytecode — reimplemented):                                │   │
│  │  1. VT acquires the object monitor, ownership stored on HEAP            │   │
│  │  2. Monitor is associated with the VIRTUAL THREAD (not the carrier)    │   │
│  │  3. If VT blocks inside synchronized:                                   │   │
│  │     a. VT's state (including monitor ownership) saved to heap          │   │
│  │     b. VT is unmounted from carrier                                    │   │
│  │     c. Carrier is FREE to run other VTs                                │   │
│  │  4. When VT is remounted (on any carrier):                             │   │
│  │     a. Monitor ownership is restored from heap                         │   │
│  │     b. VT continues holding the lock — just on a different carrier    │   │
│  │                                                                          │   │
│  │  The monitor is now a property of the VIRTUAL THREAD's continuation,   │   │
│  │  not of the underlying OS thread.                                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Remaining limitation (even in Java 24):                                         │
│  → JNI (native code) still pins. If you call native methods inside             │
│    synchronized, the VT is still pinned. This is unavoidable — native          │
│    code doesn't participate in JVM's continuation mechanism.                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.6 Before and After — Performance Impact

```java
// ── Demonstration: synchronized + I/O ────────────────────────────────────────────
public class PinningDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws Exception {

        int taskCount = 10_000;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            Instant start = Instant.now();

            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    synchronized (LOCK) {                // ← The contended lock
                        try {
                            Thread.sleep(Duration.ofMillis(10));  // simulate I/O inside sync
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }));
            }

            for (Future<?> f : futures) f.get();

            Duration elapsed = Duration.between(start, Instant.now());
            System.out.println("Time: " + elapsed.toMillis() + " ms");
        }
    }
}

// ── Results: ─────────────────────────────────────────────────────────────────────
//
// Java 21 (pinning):
// → 10,000 tasks × 10ms = 100 seconds
// → Only 1 VT can hold LOCK at a time (serial)
// → While sleeping inside synchronized, carrier is PINNED
// → Other VTs waiting for LOCK are ALSO pinned on carriers
// → Throughput: terrible (worse than platform threads!)
//
// Java 24 (no pinning):
// → 10,000 tasks × 10ms = 100 seconds (lock still serializes work)
// → BUT: while one VT sleeps inside synchronized, it UNMOUNTS
// → Carrier is FREE to run other VTs doing different work
// → VTs waiting for LOCK are parked on heap (not consuming carriers)
// → Other concurrent work (different locks or no locks) runs fine
//
// The real improvement is MIXED workloads:
// When SOME VTs are in synchronized blocks and OTHERS are doing different work,
// Java 24 prevents the synchronized VTs from monopolizing all carriers.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Impact of Java 24 JEP 491 on Spring Boot:                                       │
│                                                                                  │
│  ┌─────────────────────────────────────┬──────────────────────────────────────┐ │
│  │ Scenario                            │ Java 21 → Java 24 Improvement      │ │
│  ├─────────────────────────────────────┼──────────────────────────────────────┤ │
│  │ App with old JDBC driver            │ Severe pinning → No pinning        │ │
│  │ App with old HikariCP               │ Connection pool pinning → Fixed    │ │
│  │ App with synchronized service code  │ Carrier exhaustion → Normal        │ │
│  │ App using legacy libraries          │ Must upgrade all libs → Works      │ │
│  │ App with Logback synchronized       │ Logging pins carriers → Fixed      │ │
│  │ Spring Security ThreadLocal + sync  │ Auth check pins → No pinning      │ │
│  │ Jackson synchronized serialization  │ JSON response pins → Fixed        │ │
│  ├─────────────────────────────────────┼──────────────────────────────────────┤ │
│  │ OVERALL                             │ "Fragile opt-in" → "Just works"   │ │
│  └─────────────────────────────────────┴──────────────────────────────────────┘ │
│                                                                                  │
│  Java 21-23:                                                                     │
│  "Virtual threads work ONLY IF all your dependencies are VT-friendly."          │
│  → Must audit ALL libraries for synchronized                                   │
│  → Must upgrade HikariCP, JDBC drivers, Logback, etc.                          │
│  → ONE synchronized block in any library can degrade performance               │
│  → Fragile, audit-heavy adoption process                                       │
│                                                                                  │
│  Java 24+:                                                                       │
│  "Virtual threads JUST WORK with any Java code (except native/JNI)."           │
│  → No need to audit for synchronized                                           │
│  → Old libraries work fine                                                      │
│  → No ReentrantLock migration needed                                           │
│  → Simple, confident adoption                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.7 Summary Timeline

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Virtual Threads + synchronized — Timeline:                                      │
│                                                                                  │
│  Java 19-20 (2022):                                                              │
│  └── Virtual threads in PREVIEW. synchronized pinning documented as            │
│      "known limitation." Community starts testing.                              │
│                                                                                  │
│  Java 21 (Sept 2023):                                                            │
│  └── Virtual threads FINALIZED (JEP 444). synchronized still causes pinning.  │
│      Workaround: replace synchronized with ReentrantLock.                      │
│      Libraries start migrating (HikariCP, JDBC drivers, Logback).             │
│                                                                                  │
│  Spring Boot 3.2 (Nov 2023):                                                    │
│  └── First Spring Boot version with spring.threads.virtual.enabled=true.       │
│      Teams adopt but face pinning issues with older dependencies.              │
│                                                                                  │
│  Java 22 (Mar 2024):                                                             │
│  └── No change to pinning. JEP 491 proposed for future release.               │
│                                                                                  │
│  Java 23 (Sept 2024):                                                            │
│  └── No change to pinning. JEP 491 development continues.                     │
│                                                                                  │
│  Java 24 (Mar 2025) — THE FIX:                                                  │
│  └── JEP 491 delivered! synchronized NO LONGER pins virtual threads.           │
│      Object monitors reimplemented to support VT unmounting.                   │
│      The biggest barrier to virtual thread adoption is REMOVED.                │
│                                                                                  │
│  Summary:                                                                        │
│  → Java 21-23: Virtual threads are great BUT synchronized = pinning hazard     │
│  → Java 24+: Virtual threads are great AND synchronized is fine too            │
│  → For Spring Boot on Java 24+: virtual threads "just work" everywhere         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 8.8 Recommendation for Spring Boot Projects

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Recommendations:                                                                │
│                                                                                  │
│  If on Java 21-23:                                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  1. Enable: spring.threads.virtual.enabled=true                         │   │
│  │  2. Upgrade: HikariCP 5.1+, JDBC driver latest, Logback 1.4.12+       │   │
│  │  3. Detect: java -Djdk.tracePinnedThreads=full -jar app.jar            │   │
│  │  4. Fix: Replace YOUR synchronized with ReentrantLock (where pinning   │   │
│  │     is detected)                                                        │   │
│  │  5. Test: Load test and check for carrier exhaustion                    │   │
│  │  6. Monitor: Watch for unexpected latency spikes under load             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  If on Java 24+:                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  1. Enable: spring.threads.virtual.enabled=true                         │   │
│  │  2. Done! (No pinning concerns for synchronized)                       │   │
│  │  3. Still avoid: native (JNI) code inside synchronized (still pins)   │   │
│  │  4. Still consider: connection pool sizing (the REAL bottleneck)       │   │
│  │  5. Still consider: Semaphore for resource limiting                    │   │
│  │  6. Keep ReentrantLock in YOUR code anyway (good practice, portable)  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Spring Boot version compatibility:                                              │
│  ┌─────────────────────────┬──────────────────────────────────────────────────┐ │
│  │ Spring Boot Version     │ Virtual Thread Support                           │ │
│  ├─────────────────────────┼──────────────────────────────────────────────────┤ │
│  │ 3.0, 3.1               │ No built-in support (manual config possible)    │ │
│  │ 3.2                     │ First-class support (spring.threads.virtual)    │ │
│  │ 3.3                     │ Improved (more VT-aware auto-config)            │ │
│  │ 3.4+                    │ Full ecosystem compatibility, Java 24 support  │ │
│  └─────────────────────────┴──────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```