### History — Servlet, Servlet Container, and Pre-Spring Deployment

---

#### What is a Servlet?

A **Servlet** is a Java class that runs on a server and handles **HTTP requests and responses**. It is the foundation of all Java web applications that existed before frameworks like Spring MVC.

The word "Servlet" is a portmanteau of **"Server" + "Applet"** — it is a small server-side program that processes client requests.

Servlets are part of the **Jakarta EE** (formerly Java EE) specification, defined in the `jakarta.servlet` (or `javax.servlet` in older versions) package.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What a Servlet Does — Core Responsibility:                                      │
│                                                                                  │
│  1. Receive an HTTP Request from the client (browser, mobile app, etc.)          │
│  2. Process the request (query DB, run business logic, build response)           │
│  3. Send an HTTP Response back to the client (HTML, JSON, XML, etc.)             │
│                                                                                  │
│  HTTP Request (GET /hello)                                                       │
│  ─────────────────────────>                                                      │
│  Browser                          Server (Servlet)                               │
│  <─────────────────────────                                                      │
│  HTTP Response (200 OK, <html>...)                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Servlet Lifecycle — The Four Stages:**

Every servlet goes through exactly four stages managed by the servlet container:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Servlet Lifecycle:                                                              │
│                                                                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────────────┐  │
│  │   LOADING   │ →  │    INIT     │ →  │            SERVICE                  │  │
│  │             │    │             │    │  (repeated for every request)        │  │
│  │ Container   │    │ init()      │    │                                     │  │
│  │ loads the   │    │ called ONCE │    │  service(req, res)                  │  │
│  │ .class file │    │ at startup  │    │    → doGet(req, res)                │  │
│  │             │    │             │    │    → doPost(req, res)               │  │
│  │             │    │ Initialize  │    │    → doPut(req, res)                │  │
│  │             │    │ DB pools,   │    │    → doDelete(req, res)             │  │
│  │             │    │ config etc. │    │    → ...                            │  │
│  └─────────────┘    └─────────────┘    └──────────────┬──────────────────────┘  │
│                                                        │                         │
│                                                        ↓                         │
│                                        ┌───────────────────────────┐             │
│                                        │         DESTROY           │             │
│                                        │                           │             │
│                                        │ destroy() called ONCE     │             │
│                                        │ when container shuts down │             │
│                                        │ Release resources (DB,    │             │
│                                        │ files, connections, etc.) │             │
│                                        └───────────────────────────┘             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The core Servlet interface:**

```java
// jakarta.servlet.Servlet (the root interface)
public interface Servlet {
    void init(ServletConfig config) throws ServletException;
    void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException;
    void destroy();
    ServletConfig getServletConfig();
    String getServletInfo();
}
```

In practice, you never implement `Servlet` directly. The hierarchy is:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Servlet Class Hierarchy:                                                        │
│                                                                                  │
│  <<interface>>                                                                   │
│  Servlet                                                                         │
│    │                                                                             │
│    └── GenericServlet  (abstract — protocol-independent)                         │
│              │                                                                   │
│              └── HttpServlet  (abstract — HTTP-specific)                         │
│                        │                                                         │
│                        ├── doGet(HttpServletRequest, HttpServletResponse)        │
│                        ├── doPost(HttpServletRequest, HttpServletResponse)       │
│                        ├── doPut(HttpServletRequest, HttpServletResponse)        │
│                        ├── doDelete(HttpServletRequest, HttpServletResponse)     │
│                        └── doHead, doOptions, doTrace ...                        │
│                                  │                                               │
│                                  └── YOUR SERVLET                                │
│                                        HelloServlet extends HttpServlet          │
│                                        UserServlet extends HttpServlet           │
│                                        OrderServlet extends HttpServlet          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### What is a Servlet Container?

A **Servlet Container** (also called a **Web Container** or **Servlet Engine**) is the runtime environment that:

1. **Loads and manages** the lifecycle of servlets (`init`, `service`, `destroy`)
2. **Maps** incoming HTTP URLs to the correct servlet class
3. **Creates** `HttpServletRequest` and `HttpServletResponse` objects for each request
4. **Manages** threads — each request runs in its own thread from a thread pool
5. **Handles** network connections (TCP/IP, HTTP parsing)

The most widely used servlet container is **Apache Tomcat**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Servlet Container — Internal Architecture:                                      │
│                                                                                  │
│                        ┌────────────────────────────────────────────────────┐   │
│                        │            Apache Tomcat (Servlet Container)       │   │
│                        │                                                    │   │
│   Browser/Client       │  ┌──────────────┐     ┌──────────────────────────┐│   │
│                        │  │  Connector   │     │       Web Application    ││   │
│   GET /hello  ───────► │  │  (HTTP/1.1   │────►│                          ││   │
│                        │  │   port 8080) │     │  ┌────────────────────┐  ││   │
│                        │  └──────────────┘     │  │  Servlet Context   │  ││   │
│   ◄─────────  200 OK   │                       │  │                    │  ││   │
│   <html>Hello</html>   │  ┌──────────────┐     │  │  /hello ──────────►│  ││   │
│                        │  │  Thread Pool │     │  │  HelloServlet      │  ││   │
│                        │  │  (handles    │     │  │                    │  ││   │
│                        │  │  concurrent  │     │  │  /users ──────────►│  ││   │
│                        │  │  requests)   │     │  │  UserServlet       │  ││   │
│                        │  └──────────────┘     │  │                    │  ││   │
│                        │                       │  │  /orders ─────────►│  ││   │
│                        │                       │  │  OrderServlet      │  ││   │
│                        │                       │  └────────────────────┘  ││   │
│                        │                       └──────────────────────────┘│   │
│                        └────────────────────────────────────────────────────┘   │
│                                                                                  │
│  What Tomcat does automatically (so you don't have to):                          │
│    • Opens a socket on port 8080                                                 │
│    • Parses raw HTTP bytes into HttpServletRequest objects                        │
│    • Manages a thread pool for concurrent requests                               │
│    • Calls the right servlet based on the URL mapping                            │
│    • Serializes HttpServletResponse back to raw HTTP bytes on the wire           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Container vs Application Server:**

| | Servlet Container | Full Application Server |
|---|---|---|
| **Example** | Apache Tomcat, Jetty | JBoss/WildFly, WebLogic, WebSphere |
| **What it supports** | Servlets + JSP | Servlets + JSP + EJB + JMS + JTA + CDI |
| **Weight** | Lightweight | Heavyweight |
| **Use case** | Web apps (Spring Boot uses embedded Tomcat) | Full Jakarta EE enterprise apps |

---

#### Advantages and Disadvantages of Servlets

**Advantages:**

| Advantage | Explanation |
|---|---|
| **Platform independent** | Pure Java — write once, run on any OS |
| **Performance** | Servlet is loaded once, then handles many requests. Better than CGI (which spawned a new process per request). Thread-per-request model reuses the same JVM. |
| **Multithreaded** | Container handles concurrency via thread pools. One servlet instance serves many threads simultaneously. |
| **Full Java ecosystem** | Access to all Java libraries: JDBC, Collections, I/O, networking, etc. |
| **Session management** | Built-in `HttpSession` for stateful user sessions |
| **Secure** | Java's type safety + sandbox security model |
| **Mature specification** | Well-defined Jakarta EE spec, implementations available from many vendors |

**Disadvantages and Limitations:**

| Disadvantage | Explanation |
|---|---|
| **HTML inside Java** | Generating HTML responses requires writing `out.println("<html>...")` statements — messy, error-prone, not maintainable |
| **No separation of concerns** | Business logic, data access, and presentation (HTML) are all mixed inside one servlet class |
| **Boilerplate-heavy** | Every API endpoint = a new servlet class. Wiring, URL mapping, request parsing are all manual. |
| **URL mapping is inflexible** | Each servlet maps to fixed URL patterns. Dynamic path variables like `/users/{id}` require manual parsing of the URL string. |
| **No built-in dependency injection** | All object creation and wiring is manual (`new MyService()`) inside servlet methods |
| **Not RESTful by default** | No built-in support for JSON serialization/deserialization. You must manually parse request body and write JSON strings. |
| **Thread-safety burden** | Instance variables on a servlet are shared across threads — race conditions if not carefully designed |
| **Hard to test** | Servlets depend on `HttpServletRequest`/`HttpServletResponse` which are hard to mock in unit tests |

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  The Core Problem — HTML Embedded in Java:                                       │
│                                                                                  │
│  @Override                                                                       │
│  protected void doGet(HttpServletRequest req, HttpServletResponse res)           │
│          throws ServletException, IOException {                                  │
│                                                                                  │
│      String name = req.getParameter("name");   // get query param                │
│      String user = userDao.findByName(name);   // business logic                 │
│                                                                                  │
│      res.setContentType("text/html");                                            │
│      PrintWriter out = res.getWriter();                                          │
│                                                                                  │
│      // Presentation mixed with business logic — MESSY!                          │
│      out.println("<!DOCTYPE html>");                                             │
│      out.println("<html><head><title>User</title></head><body>");                │
│      out.println("  <h1>Hello, " + user + "</h1>");      // XSS risk!            │
│      out.println("  <p>Welcome to the system.</p>");                             │
│      out.println("</body></html>");                                              │
│  }                                                                               │
│                                                                                  │
│  Problems:                                                                       │
│    ✗ Business logic (findByName) and view (HTML) are in the same method          │
│    ✗ XSS vulnerability — user input printed directly without escaping            │
│    ✗ No IDE support for HTML inside Java strings                                 │
│    ✗ Designer cannot edit HTML without modifying Java code                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Writing APIs with Servlets — Code and Diagrams

##### Project Structure (pre-Spring)

```text
mywebapp/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   ├── HelloServlet.java
│                   ├── UserServlet.java
│                   └── dao/
│                       └── UserDao.java
└── src/
    └── main/
        └── webapp/
            ├── WEB-INF/
            │   └── web.xml              ← Deployment descriptor
            └── index.html
```

##### Method 1: `web.xml` — XML-Based Servlet Mapping

`web.xml` (the **Deployment Descriptor**) is an XML file inside `WEB-INF/` that tells the servlet container:
- Which servlet classes exist
- Which URL patterns they handle
- Initialization parameters, session config, filters, listeners, etc.

```xml
<!-- src/main/webapp/WEB-INF/web.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <!-- ──────────────────────────────────────────────────────
         STEP 1: Declare the servlet (give it a name + class)
    ─────────────────────────────────────────────────────── -->
    <servlet>
        <servlet-name>HelloServlet</servlet-name>
        <servlet-class>com.example.HelloServlet</servlet-class>
        <!-- Optional: load on startup (0 = first, higher = later) -->
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet>
        <servlet-name>UserServlet</servlet-name>
        <servlet-class>com.example.UserServlet</servlet-class>
        <!-- Init parameters available via getServletConfig().getInitParameter() -->
        <init-param>
            <param-name>dbUrl</param-name>
            <param-value>jdbc:mysql://localhost:3306/mydb</param-value>
        </init-param>
    </servlet>

    <!-- ──────────────────────────────────────────────────────
         STEP 2: Map servlet names to URL patterns
    ─────────────────────────────────────────────────────── -->
    <servlet-mapping>
        <servlet-name>HelloServlet</servlet-name>
        <url-pattern>/hello</url-pattern>
        <!-- Also supports wildcards: /hello/* or *.do -->
    </servlet-mapping>

    <servlet-mapping>
        <servlet-name>UserServlet</servlet-name>
        <url-pattern>/users</url-pattern>
    </servlet-mapping>

    <!-- ──────────────────────────────────────────────────────
         Optional: Context-wide init parameters (all servlets can read)
    ─────────────────────────────────────────────────────── -->
    <context-param>
        <param-name>appName</param-name>
        <param-value>MyWebApp</param-value>
    </context-param>

    <!-- Optional: Session timeout in minutes -->
    <session-config>
        <session-timeout>30</session-timeout>
    </session-config>

    <!-- Optional: Welcome file (served at /) -->
    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
    </welcome-file-list>

</web-app>
```

**How web.xml Servlet Mapping Works:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  web.xml URL Mapping — How Tomcat Resolves a Request:                            │
│                                                                                  │
│  Incoming request: GET /users?name=Alice                                         │
│                                                                                  │
│  Tomcat checks web.xml servlet-mapping entries in priority order:                │
│                                                                                  │
│  Priority 1: EXACT match                                                         │
│    /users → MATCH! → UserServlet                                                 │
│                                                                                  │
│  Priority 2: LONGEST PATH PREFIX match (with /*)                                 │
│    /api/*  would match /api/users, /api/orders                                   │
│                                                                                  │
│  Priority 3: EXTENSION match (e.g., *.do, *.action)                              │
│    *.do would match /login.do, /save.do                                          │
│                                                                                  │
│  Priority 4: DEFAULT servlet (/)                                                 │
│    Catches everything not matched above (serves static files)                    │
│                                                                                  │
│  URL Pattern Examples:                                                           │
│  ┌───────────────────────┬──────────────────────────────────────────────────┐    │
│  │ Pattern               │ Matches                                          │    │
│  ├───────────────────────┼──────────────────────────────────────────────────┤    │
│  │ /hello                │ Exactly /hello                                   │    │
│  │ /api/*                │ /api/users, /api/orders, /api/anything           │    │
│  │ *.do                  │ /login.do, /save.do, /anything.do                │    │
│  │ /                     │ Default — everything not matched by others       │    │
│  │ /*                    │ ALL requests (use with caution!)                 │    │
│  └───────────────────────┴──────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Method 2: `@WebServlet` Annotation (Servlet 3.0+, Java EE 6+)

Starting from Servlet 3.0 (2009), the `@WebServlet` annotation replaced the need to declare servlets in `web.xml`. The container scans for this annotation at startup.

```java
// HelloServlet.java
package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

// @WebServlet replaces the <servlet> + <servlet-mapping> blocks in web.xml
@WebServlet(
    name        = "HelloServlet",           // Optional: servlet name (default = class name)
    urlPatterns = {"/hello", "/hi"},        // URL patterns this servlet handles
    loadOnStartup = 1                       // Load eagerly at startup (optional)
)
public class HelloServlet extends HttpServlet {

    // init() — called ONCE when the servlet is first loaded
    @Override
    public void init() throws ServletException {
        // Initialize resources: DB connections, config, caches, etc.
        System.out.println("HelloServlet initialized");
    }

    // doGet() — handles HTTP GET requests
    // Called when browser hits GET /hello or GET /hi
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Read request data ─────────────────────────────────────────────────
        String name = request.getParameter("name");     // GET /hello?name=Alice
        String userAgent = request.getHeader("User-Agent");
        String sessionId = request.getSession().getId();

        // ── Business logic ────────────────────────────────────────────────────
        String greeting = (name != null && !name.isEmpty())
            ? "Hello, " + name + "!"
            : "Hello, World!";

        // ── Write response ────────────────────────────────────────────────────
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);  // 200

        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Hello</title></head>");
        out.println("<body>");
        out.println("  <h1>" + escapeHtml(greeting) + "</h1>");
        out.println("  <p>Session: " + sessionId + "</p>");
        out.println("</body></html>");
    }

    // doPost() — handles HTTP POST requests
    // Called when a form submits to POST /hello
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Read POST body parameters (from HTML form)
        String name = request.getParameter("name");
        String email = request.getParameter("email");

        // Store in session
        request.getSession().setAttribute("user", name);

        // Redirect after POST (Post/Redirect/Get pattern)
        response.sendRedirect(request.getContextPath() + "/hello?name=" + name);
    }

    // destroy() — called ONCE when container shuts down
    @Override
    public void destroy() {
        // Release resources: close DB connections, thread pools, etc.
        System.out.println("HelloServlet destroyed");
    }

    // Helper: prevent XSS by escaping HTML special chars
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
```

##### A Complete CRUD-style Servlet — `UserServlet`

```java
package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.dao.UserDao;
import com.example.model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

// Handles /users and /users/* requests
@WebServlet(urlPatterns = {"/users", "/users/*"})
public class UserServlet extends HttpServlet {

    // WARNING: instance variable — shared across ALL threads!
    // OK only if UserDao is thread-safe (stateless, uses connection pool)
    private UserDao userDao;

    @Override
    public void init() throws ServletException {
        // Read init param from web.xml (if declared there)
        String dbUrl = getServletConfig().getInitParameter("dbUrl");
        this.userDao = new UserDao(dbUrl);
    }

    // GET /users        → list all users
    // GET /users/42     → get user with id 42
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json;charset=UTF-8");
        PrintWriter out = res.getWriter();

        // Parse path: /users/42 → pathInfo = "/42"
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /users → return all users as JSON
            List<User> users = userDao.findAll();
            out.println(toJsonArray(users));

        } else {
            // GET /users/42 → return single user
            try {
                long id = Long.parseLong(pathInfo.substring(1)); // strip "/"
                User user = userDao.findById(id);
                if (user != null) {
                    out.println(toJson(user));
                } else {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);  // 404
                    out.println("{\"error\":\"User not found\"}");
                }
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);   // 400
                out.println("{\"error\":\"Invalid ID format\"}");
            }
        }
    }

    // POST /users → create a new user
    // Body: name=Alice&email=alice@example.com (form-encoded)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String name  = req.getParameter("name");
        String email = req.getParameter("email");

        if (name == null || email == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().println("{\"error\":\"name and email are required\"}");
            return;
        }

        User user = new User(name, email);
        userDao.save(user);

        res.setStatus(HttpServletResponse.SC_CREATED);   // 201
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().println(toJson(user));
    }

    // DELETE /users/42 → delete user with id 42
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();   // "/42"
        if (pathInfo == null || pathInfo.equals("/")) {
            res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        long id = Long.parseLong(pathInfo.substring(1));
        boolean deleted = userDao.deleteById(id);

        if (deleted) {
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);  // 204
        } else {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);   // 404
            res.getWriter().println("{\"error\":\"User not found\"}");
        }
    }

    // Manual JSON serialization — no Jackson available by default!
    private String toJson(User u) {
        return String.format("{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}",
            u.getId(), u.getName(), u.getEmail());
    }

    private String toJsonArray(List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            sb.append(toJson(users.get(i)));
            if (i < users.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
```

##### Request Flow Through the Servlet Container

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Request Flow: GET /users/42                                                     │
│                                                                                  │
│  ┌──────────┐   HTTP Request     ┌──────────────────────────────────────────┐    │
│  │ Browser  │ ────────────────►  │           Apache Tomcat                  │    │
│  │          │                   │                                           │    │
│  │          │                   │  1. Accept TCP connection on port 8080    │    │
│  │          │                   │                                           │    │
│  │          │                   │  2. Parse HTTP bytes into:                │    │
│  │          │                   │     HttpServletRequest {                  │    │
│  │          │                   │       method: "GET"                       │    │
│  │          │                   │       requestURI: "/myapp/users/42"       │    │
│  │          │                   │       headers: { Accept: application/json}│    │
│  │          │                   │       pathInfo: "/42"                     │    │
│  │          │                   │     }                                     │    │
│  │          │                   │                                           │    │
│  │          │                   │  3. Look up web.xml / @WebServlet:        │    │
│  │          │                   │     /users/* → UserServlet                │    │
│  │          │                   │                                           │    │
│  │          │                   │  4. Pick a thread from thread pool        │    │
│  │          │                   │                                           │    │
│  │          │                   │  5. Call UserServlet.service(req, res)    │    │
│  │          │                   │       → service() dispatches to doGet()   │    │
│  │          │                   │                                           │    │
│  │          │                   │  6. doGet() runs:                         │    │
│  │          │                   │     pathInfo = "/42"                      │    │
│  │          │                   │     id = 42                               │    │
│  │          │                   │     user = userDao.findById(42)           │    │
│  │          │                   │     out.println(toJson(user))             │    │
│  │          │                   │                                           │    │
│  │          │                   │  7. Flush HttpServletResponse →           │    │
│  │          │   HTTP Response       serialize to HTTP bytes → send          │    │
│  │          │ ◄────────────────     over TCP                                │    │
│  │          │ 200 OK                                                        │    │
│  │          │ Content-Type:     └──────────────────────────────────────────┘    │
│  │          │  application/json                                                 │
│  │          │ {"id":42,"name":"Alice","email":"alice@ex.com"}                   │
│  └──────────┘                                                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Comparison: `web.xml` vs `@WebServlet`

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  web.xml vs @WebServlet:                                                         │
│                                                                                  │
│  ┌───────────────────────────┬───────────────────────┬────────────────────────┐  │
│  │ Feature                   │ web.xml               │ @WebServlet            │  │
│  ├───────────────────────────┼───────────────────────┼────────────────────────┤  │
│  │ Available since           │ Servlet 1.0           │ Servlet 3.0 (2009)     │  │
│  │ Configuration location    │ External XML file     │ Directly on class      │  │
│  │ Requires restart to change│ YES (XML file change) │ YES (code change)      │  │
│  │ Override by external file │ YES                   │ web.xml can override   │  │
│  │ Good for                  │ Ops-time config       │ Dev simplicity         │  │
│  │ In Spring Boot            │ Not used (auto-config)│ Not used (Spring MVC)  │  │
│  └───────────────────────────┴───────────────────────┴────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### How We Deployed Applications Using Tomcat (Before Spring)

Before Spring Boot's embedded server, deploying a Java web application was a **multi-step manual process**.

##### Step 1 — Build a WAR File

You compiled your code and packaged it into a **WAR (Web Application Archive)** file.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  WAR File Structure:                                                             │
│                                                                                  │
│  mywebapp.war                                                                    │
│  ├── WEB-INF/                                                                    │
│  │   ├── web.xml                  ← Deployment descriptor                        │
│  │   ├── classes/                 ← Compiled .class files                        │
│  │   │   └── com/example/                                                       │
│  │   │       ├── HelloServlet.class                                              │
│  │   │       ├── UserServlet.class                                               │
│  │   │       └── dao/UserDao.class                                               │
│  │   └── lib/                     ← Third-party JAR dependencies                 │
│  │       ├── mysql-connector.jar                                                 │
│  │       └── jackson-databind.jar                                                │
│  ├── index.html                   ← Static resources                             │
│  ├── css/                                                                        │
│  └── js/                                                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```bash
# Build with Maven
mvn clean package
# Produces: target/mywebapp.war
```

`pom.xml` (Maven) — note the packaging type:

```xml
<project>
    <groupId>com.example</groupId>
    <artifactId>mywebapp</artifactId>
    <version>1.0</version>
    <packaging>war</packaging>   <!-- WAR not JAR -->

    <dependencies>
        <!-- Servlet API — provided by Tomcat at runtime, NOT bundled in WAR -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>   <!-- ← Tomcat provides this, exclude from WAR -->
        </dependency>

        <!-- JDBC driver — bundled in WAR/lib -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.0.33</version>
        </dependency>
    </dependencies>
</project>
```

##### Step 2 — Install Apache Tomcat Separately

```bash
# Download Tomcat from https://tomcat.apache.org/
# e.g., apache-tomcat-10.1.x

cd /opt
tar -xzf apache-tomcat-10.1.24.tar.gz
mv apache-tomcat-10.1.24 tomcat

# Directory structure:
# /opt/tomcat/
# ├── bin/              ← startup.sh, shutdown.sh, catalina.sh
# ├── conf/             ← server.xml, context.xml, web.xml (global)
# ├── lib/              ← Tomcat's own JARs (servlet-api.jar, jsp-api.jar, etc.)
# ├── logs/             ← catalina.out, access logs
# ├── webapps/          ← Deploy your WARs HERE
# │   ├── ROOT/         ← Default app at /
# │   ├── examples/     ← Sample apps
# │   └── manager/      ← Tomcat Manager web app
# └── work/             ← JSP compilation cache
```

##### Step 3 — Configure Tomcat

```xml
<!-- /opt/tomcat/conf/server.xml — Tomcat's main config -->
<Server port="8005" shutdown="SHUTDOWN">

    <Service name="Catalina">

        <!-- HTTP connector — listens on port 8080 -->
        <Connector port="8080"
                   protocol="HTTP/1.1"
                   connectionTimeout="20000"
                   redirectPort="8443"
                   maxThreads="200"          <!-- thread pool size -->
                   minSpareThreads="10" />

        <!-- HTTPS connector (if SSL configured) -->
        <!-- <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
                   SSLEnabled="true" keystoreFile="..." keystorePass="..." /> -->

        <Engine name="Catalina" defaultHost="localhost">
            <Host name="localhost" appBase="webapps"
                  unpackWARs="true"           <!-- auto-unpack WAR to directory -->
                  autoDeploy="true">          <!-- auto-deploy new WARs dropped in webapps/ -->

                <!-- Optional: set context path explicitly -->
                <!-- <Context path="/myapp" docBase="mywebapp" /> -->

            </Host>
        </Engine>
    </Service>
</Server>
```

##### Step 4 — Deploy the WAR

```bash
# Option 1: Copy WAR into webapps/ — Tomcat auto-deploys if autoDeploy=true
cp target/mywebapp.war /opt/tomcat/webapps/

# Tomcat detects it, unpacks it:
#   /opt/tomcat/webapps/mywebapp/   ← expanded WAR
#
# Application is now available at:
#   http://localhost:8080/mywebapp/hello
#   http://localhost:8080/mywebapp/users

# Option 2: Use Tomcat Manager Web UI (if configured)
# Navigate to http://localhost:8080/manager/html
# Upload WAR → deploy

# Option 3: Tomcat Manager REST API
curl -u admin:password \
     -T target/mywebapp.war \
     "http://localhost:8080/manager/text/deploy?path=/mywebapp&update=true"
```

##### Step 5 — Start and Stop Tomcat

```bash
# Start Tomcat
/opt/tomcat/bin/startup.sh

# View logs
tail -f /opt/tomcat/logs/catalina.out

# Stop Tomcat
/opt/tomcat/bin/shutdown.sh

# Or use catalina directly
/opt/tomcat/bin/catalina.sh run   # foreground
/opt/tomcat/bin/catalina.sh start # background
```

##### Complete Deployment Flow

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pre-Spring Deployment Flow:                                                     │
│                                                                                  │
│  Developer                         Build Server              Production Server   │
│  ──────────                        ────────────              ─────────────────   │
│                                                                                  │
│  1. Write Servlet code                                                           │
│     + web.xml                                                                    │
│         │                                                                        │
│         ▼                                                                        │
│  2. mvn clean package                                                            │
│     → mywebapp.war                                                               │
│         │                                                                        │
│         ▼                                                                        │
│  3. Upload WAR to server ─────────────────────────────────►                     │
│                                                              4. Install Tomcat   │
│                                                                 (one-time)       │
│                                                                    │             │
│                                                                    ▼             │
│                                                              5. Copy WAR to      │
│                                                                 webapps/         │
│                                                                    │             │
│                                                                    ▼             │
│                                                              6. ./startup.sh     │
│                                                                    │             │
│                                                                    ▼             │
│                                                              7. Tomcat:          │
│                                                                 - Unpack WAR     │
│                                                                 - Load web.xml   │
│                                                                 - Instantiate    │
│                                                                   Servlets       │
│                                                                 - Call init()    │
│                                                                    │             │
│                                                                    ▼             │
│                                                              8. App live at:     │
│                                                              :8080/mywebapp/     │
│                                                                                  │
│  To UPDATE:                                                                      │
│    Stop Tomcat → Replace WAR → Start Tomcat                                      │
│    (or use Tomcat Manager hot-deploy — but can cause memory leaks in old JVMs)   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### What Was Painful — The Problems That Spring Solved

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pre-Spring Pain Points → What Spring Solved:                                    │
│                                                                                  │
│  PROBLEM                          → SPRING SOLUTION                              │
│  ──────────────────────────────────────────────────────────────────────────────  │
│  One servlet per URL endpoint     → DispatcherServlet + @RequestMapping          │
│  (100 endpoints = 100 classes)      routes all requests from ONE servlet         │
│                                                                                  │
│  Manual object wiring             → Dependency Injection (@Autowired)             │
│  (new UserDao(), new UserService())  Spring creates and injects objects           │
│                                                                                  │
│  Manual JSON serialization        → @ResponseBody + Jackson auto-converts        │
│  (StringBuilder JSON strings)       Java objects to/from JSON                    │
│                                                                                  │
│  Manual request parsing           → @RequestParam, @PathVariable,                │
│  (req.getParameter("name"))         @RequestBody — Spring binds for you          │
│                                                                                  │
│  No transaction management        → @Transactional — Spring AOP wraps            │
│  (manual JDBC commit/rollback)      your method with transaction logic           │
│                                                                                  │
│  WAR + external Tomcat            → Spring Boot embedded Tomcat                  │
│  (install, configure, deploy)       java -jar myapp.jar → done                   │
│                                                                                  │
│  Hard to test                     → MockMvc, @WebMvcTest — test controllers      │
│  (mock HttpServletRequest)          without a real server                         │
│                                                                                  │
│  Manual web.xml or @WebServlet    → Auto-configuration + component scanning     │
│  registration                       No registration needed                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The single biggest change:** Spring's **DispatcherServlet** pattern — instead of mapping every URL to a different servlet class, you register ONE servlet that dispatches to `@Controller` methods:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Before Spring (100 endpoints = 100 servlets):                                   │
│                                                                                  │
│  GET /users     → UserServlet.doGet()                                            │
│  POST /users    → UserServlet.doPost()                                           │
│  GET /orders    → OrderServlet.doGet()                                           │
│  GET /products  → ProductServlet.doGet()                                         │
│  ...            → ...Servlet.doXxx()                                             │
│  100 endpoints = 100 Servlet classes + 100 web.xml entries                       │
│                                                                                  │
│  With Spring MVC (100 endpoints = 1 DispatcherServlet):                          │
│                                                                                  │
│  ALL requests → DispatcherServlet → HandlerMapping → @Controller method          │
│                                                                                  │
│  @RestController                                                                 │
│  public class UserController {                                                   │
│      @GetMapping("/users")                                                       │
│      public List<User> getAll() { return userService.findAll(); }                │
│                                                                                  │
│      @PostMapping("/users")                                                      │
│      public User create(@RequestBody User user) { return userService.save(user);}│
│  }                                                                               │
│                                                                                  │
│  @RestController                                                                 │
│  public class OrderController {                                                  │
│      @GetMapping("/orders/{id}")                                                 │
│      public Order getById(@PathVariable Long id) { return orderService.find(id);}│
│  }                                                                               │
│  // No web.xml. No servlet registration. Just annotate and run.                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```
