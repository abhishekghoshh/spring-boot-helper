

Add all the theories for the following question in detail and wherever possible add diagram and add code

Explain all the annotations in detail, its properties used in spring boot with different code example and diagram if needed

Explain in detail, its properties used in spring boot with different code example and diagram if needed



About Custom Annotation for Java 

- [Spring boot: Custom Interceptors | How to Intercept Incoming HTTP Request and Custom Annotations](https://www.youtube.com/watch?v=hkuq4fv43eU)


---

## Async

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



---

## Intercepter and Filter?

- What is Interceptor?
  - It intercepts the requests before it reaches to the controller
- Why and how it is used? Explain with real life use case used in Industry with code
  - Implement `HandlerInterceptor` and `WebMvcConfigurer`
  - registry add interceptor
  - add path pattern, exclude path pattern
- Advatage and disadvatge and limitation of Interceptor
- In which level/layer Interceptor works? Explain with diagram and internal code of the DispatcherServlet
- How to add and use multiple interceptors?



- What is Filter?
  - It intercepts the request before it reaches to the servelet
  - Before hitting to the particular servlet there are a filter chain
- Why and how it is used? Explain with real life use case used in Industry with code
  - Implement Filter
  - Create a bean of FilterRegistrationBean with url pattern, order
- Advatage and disadvatge and limitation of Filter
- In which level/layer Filter works? Explain with diagram and internal code
- How to add and use multiple Filters?

- Explain difference between Filter and Interceptor? When we to use what?


- What is Annotations? Explain in detail different properties(target, retention, etc) and custom attributes to pass information(like name, key etc) of an annotation?
- How to create custom Annotation to intercept a method or class using AOP around

---

## Spring hateos

- What is HATEOS in RestFull API's? Explain with diagram
- Why we should it?
  - Loose Coupling between backend and frontend regarding the resource and further actions
  - API Discovery
  - Server sent a next set of APIs/actions in the response itself. which client can take, so that client have less business logic around API's
- Explain the advatge and disadvatage and limitation of it?
  - disadvatage:
    - increase complexity on the server side
    - latency impact
    - Increase response Payload size
- What is ideal way to implement the HATEOS
- How to implement this using Spring Boot? Explain with real life use case used in Industry with code
  - Using WebMvcLinkBuilder
  - Using Link.of

---

## Exception Handling

### Response Codes

- Response generaly contains 3 parts (status code, header, body) We can manually build this using ResponseEntity
- Explain each response code and their Reason, Mosltly used in which http method, context in more detailed manner and real world use case
  - All 1xx Response Codes (Informational)
    - 100 Continue
      - Before sending the request client check with the server if it can handle the request and ready
      - Like client is sending a dummy request when valid header and payload and `Expect:100-continue` is present in head, then server will validate the header and without processing anything it will return
  - All 2xx Response Codes (Success)
    - 201 Created
    - 202 Accepted
    - 204 No Content
    - 206 Partial Content
  - All 3xx Response Codes (Redirection)
    - 301 Permenantly Moved (Redirected url could have a different http method)
    - 308 Permenant Redirect (Same as 301 but do not allow http method to change while redirecting)
    - 304 Not Modified
      - Client makes a GET call, Server return with last modified time in header
      - client caches the response
      - client make a get call pass the last modified time in `if-Modified-since` header
      - Server checks the particular resource last update time with what client provided, if resource is not updated , server simply returns 304(NOT_MODIFIED)
      - If Modified, server process the request as usual and return
      - Try to avoid using it with PATCH for example: you trying to update a name of the user but let's say name is already in DB, so no update is required, in that case we should not return 304 NOT_MODIFIED, instead we should return 204 or 200
  - All 4xx Response Codes (Validation Error)
    - 400 Bad Request
      - Client not sending the proper request
    - 401 Unauthorized
      - API require authentication, but client is not sending anything
    - 403 Forbidden
      - DELETE /user/{userId} is only accesible by the admin, other users should get 403 if they want to hit the API
    - 404 Resource Not found
      - API does not exists
      - GET /user/{userId} the userId does not exists anymore
    - 405 Method Not Allowed
      - Hitting GET api for /submit, when the API is only associated with POST
    - 422 Un Processable Entity
      - In the Business layer it is mentioned that some country is not allowed
    - 429 Too many requests
      - Rate Limitting 
    - 409 Conflict
      - Like we are hitting DELETE /user/{userId}, but there is already a request processing for /user/{userId}
  - All 5xx Response Codes (Server Error)
    - 500 Internal server error
      - Generic error when no more specific error is suitable 
    - 501 Not implemented
      - Like API is in development not completed
    - 502 Bad Gateway
      - Server is acting as proxy and while calling upstream got invalid response
      - Application is deployed behind nginx, and nginx is not able to reach/call the application
- Only 5xx response errors are solvable by the server development team


### Exception handling framework

- Classes Involved in handling an Exception, Explain in detail with diagram and code
  - `HandlerExceptionResolver` Interface
    - `HandlerExceptionResolverComposite`
    - `DefaultErrorAttributes`
    - `AbstractHandlerExceptionResolver` Abstract
      - `AbstractHandlerMethodExceptionResolver` Abstract
        - `ExceptionHandlerExceptionResolver`
      - `ResponseStatusExceptionResolver`
      - `DefaultHandlerExceptionResolver`

- Explain in detail with code how the exception is generated and handled from Dispatcher Servelet and with a controller(Manually throwing an exception)
  - Dispatcher Servelet pass the request to `HandlerExceptionResolverComposite` class 
  - then `HandlerExceptionResolverComposite` invoke this classes in sequence from left to right in this classes `ExceptionHandlerExceptionResolver` -> `ResponseStatusExceptionResolver` ->  `DefaultHandlerExceptionResolver`
  - If it is able to handle the exception then it will go to the `DefaultErrorAttributes` and return error response else goes to the next exception resolver
  - If we directly throw any exception like Runtime or CustomException(HttpStatus,Message), spring will by default through 500 Internal server error, it will not pickup the status or body from the exception, As Spring internally using `DefaultErrorAttributes` creates the ResponseEntity(500 Internal server error) and returns. No Spring classes can handle the custom exception class
  - This happens because we are letting spring to handle the exception, we are not handling it from our side. We can directly handle it in the controller and return a response entity when we got any exception
  

- Explain in detail, What is handled by `ExceptionHandlerExceptionResolver`, 
  - @ExceptionHandler
    - This is a controller level exception handling, it is added @ExceptionHandler in the same controller
    - It can handle multiple type of exception and also we can aggregate like `@ExceptionHandler({CustomException.class,IllegalArgumentException.class})`
    - We can take `HttpServeletResponse` in the method argument and do like this `response.sendError(status,message)`
    - It can take `HttpServeletRequest`, `HttpServeletResponse`, and `Exception` in the method argument optionally
  - @ControllerAdvice
    - With `@ControllerAdvice` and `@ExceptionHandler` we can achieve global exception handling
    - However the first priority is always given to the `@ExceptionHandler(CustomException.class)` declared in the same controller. If it doesn't find there then it will check `@ExceptionHandler(CustomException.class)` declared in the `@ControllerAdvice`
    - Let's we have a `CustomException` extending `RuntimeException` and in `@ControllerAdvice` we have 2 `@ExceptionHandler` one for `CustomException` and another for `RuntimeException`, then the method with `CustomException` will get priority as it has the exact matching


- Explain in detail, What is handled by `ResponseStatusExceptionResolver`, 
  - It handles uncaught exception annotated with `@ResponseStatus` Annotation
  - Let's say my CustomException is annotated with `@ResponseStatus` Annotation, and if this is thrown then `ResponseStatusExceptionResolver` will catch it and set the status and the exception message or the reason added in the annotation in the response




---


## Spring Config Management

- What is profiling meaning in spring boot? 
- what is application-{profile}.properties or application-{profile}.yaml?
- How and Why should we use it? 
- Explain with code example how to set the profile in spring boot in `spring.profiles.active` or using `-Dspring-boot.run.profiles` or using `pom.xml and mvn spring-boot:run -Pdev`? Which one has more priority?
- How the values are set in the application if we have the same key(`migration.enabled`) specified in different places like @Value, default profile and dev/prod profile
- Advantage disadvantage and limitation of this
- Can we use multiple profiles in comma separated way in `spring.profiles.active`? which values will be picked ultimately then?



## Spring Boot config cloud





