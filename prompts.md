

Add all the theories for the following question in detail and wherever possible add diagram and add code

Explain all the annotations in detail, its properties used in spring boot with different code example and diagram if needed

Explain in detail, its properties used in spring boot with different code example and diagram if needed




1. Spring Config Management

- What is profiling meaning in spring boot? 
- what is application-{profile}.properties or application-{profile}.yaml?
- How and Why should we use it? 
- Explain with code example how to set the profile in spring boot in `spring.profiles.active` or using `-Dspring-boot.run.profiles` or using `pom.xml and mvn spring-boot:run -Pdev`? Which one has more priority?
- How the values are set in the application if we have the same key(`migration.enabled`) specified in different places like @Value, default profile and dev/prod profile
- Advantage disadvantage and limitation of this
- Can we use multiple profiles in comma separated way in `spring.profiles.active`? which values will be picked ultimately then?




2. Async

- What is @Async and @EnableAsync Annotation? 
- What is usage of it? 
- How to use it with real life use case and code example
- Why should we use it?
- What is the advatage and disadvantage?


- How it works internally and how it creates a new thread, explain with diagram
- Spring boot uses by default `SimpleAsyncTaskExecutor`, what is the use of `AsyncExecutionInterceptor` class
- Why it is not recomended to use the spring provided default `ThreadPoolTaskExecutor`? what are the disadvatges of it?
   - Under utilization of threads
   - High latency
   - Thread Exhausation
   - High Memory usage



- How can we supply our own custom spring threadpool task executor?
- What if we create our own custom java threadpool executor? will it get picked up while using with async? If not how can we use our own custom java threadpool executor with `@Async`
- How can we use AsyncConfigurer class to create custom threadpool executor and use it with @Async



- What are the condition of @Async annotation to work properly and what is the root cause?
   - @Async annotation Different the different class, calling method in the same class which has @Async annotation will not work, Explain the role of AOP and proxy class and interception
   - Method annotated with Async should be public
- How async and transaction management works together and what are the challenges? Explain with code example and diagram
- If we call an async method from a method annotated with @Transactional, will the transactional context will be transfer to the async method? What will be the issue?
- What will happen if we use the @Transactional and @Async on the same method? What will be the issue?
- What happend if we call a method annotated with @Transactional from the method annotated with Async


- How return any object from the method annotated with Async using Future, AsyncResult and CompletableFuture
- How to handle exception in a method annotated with Async? why we get the exception when we invoke completableFuture.get
- How to handle exception in a method annotated with Async with no return or void method? How can we use AsyncUncaughtExceptionHandler and AsyncConfigurer here?



