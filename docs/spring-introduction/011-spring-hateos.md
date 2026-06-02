# Spring HATEOAS

## 1. What is HATEOAS?

**HATEOAS** — Hypermedia As The Engine Of Application State — is one of the six constraints that define a fully RESTful API (Roy Fielding's PhD thesis, 2000). The core idea is simple:

> Every API response includes not only the requested data, but also a set of **links** describing what the client can do **next** from the current state.

The client drives navigation by following the links the server provides. It never hard-codes URLs or constructs them by convention — it only knows one entry-point URL (the API root) and discovers everything else at runtime.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        REST Maturity Model (Richardson)                      │
│                                                                              │
│  Level 0 — One URL, one HTTP method (SOAP over HTTP)                         │
│    POST /api  {"action": "getOrder", "id": 123}                              │
│                                                                              │
│  Level 1 — Resources as individual URLs                                      │
│    GET /orders/123                                                           │
│                                                                              │
│  Level 2 — HTTP verbs used correctly (GET, POST, PUT, DELETE)                │
│    DELETE /orders/123                                                        │
│                                                                              │
│  Level 3 — HATEOAS ← the fully RESTful level                                │
│    GET /orders/123                                                           │
│    Response includes:                                                        │
│      { "id": 123, "status": "PENDING",                                       │
│        "_links": {                                                           │
│          "self":   { "href": "/orders/123" },                                │
│          "cancel": { "href": "/orders/123/cancel", "method": "POST" },       │
│          "payment":{ "href": "/orders/123/payment" }                         │
│        }                                                                     │
│      }                                                                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

### State Machine Diagram — HATEOAS in Action

The links change depending on the **current state** of the resource. The server decides which actions are valid right now and embeds only those links.

```
                              ┌──────────┐
                              │  Order   │
                         ┌───►│ PENDING  │◄───────────────────────────┐
                         │    └────┬─────┘                             │
                         │         │  links: [self, cancel, payment]   │
                         │         │                                   │
                   POST /orders    │ POST /orders/123/payment          │
                         │         ▼                                   │
                         │    ┌──────────┐                             │
                         │    │  Order   │                             │
                         │    │  PAID    │                             │
                         │    └────┬─────┘                             │
                         │         │  links: [self, ship, refund]      │
                         │         │                                   │
                         │         │ POST /orders/123/ship             │
                         │         ▼                                   │
                         │    ┌──────────┐                             │
                         │    │  Order   │                             │
                         │    │ SHIPPED  │                             │
                         │    └────┬─────┘                             │
                         │         │  links: [self, track, return]     │
                         │         │                                   │
                         │         │ POST /orders/123/deliver          │
                         │         ▼                                   │
                         │    ┌──────────┐                             │
                         │    │  Order   │                             │
                         │    │DELIVERED │                             │
                         │    └──────────┘                             │
                         │         links: [self, return, review]       │
                         │                                             │
                         └─────────────────────────────────────────────┘

Key insight: The CLIENT does not decide what actions are possible.
             The SERVER embeds exactly the links valid for the current state.
             A CANCELLED order has no "ship" link — client cannot even try.
```

### Request / Response Example

**Without HATEOAS** (Level 2 REST):
```json
GET /orders/123
{
  "id": 123,
  "status": "PENDING",
  "total": 99.99
}
```
The client must know (hardcoded) that it can call `POST /orders/123/cancel` or `POST /orders/123/payment`.

**With HATEOAS** (Level 3 REST):
```json
GET /orders/123
{
  "id": 123,
  "status": "PENDING",
  "total": 99.99,
  "_links": {
    "self": {
      "href": "http://api.example.com/orders/123"
    },
    "cancel": {
      "href": "http://api.example.com/orders/123/cancel",
      "type": "application/json"
    },
    "payment": {
      "href": "http://api.example.com/orders/123/payment"
    },
    "customer": {
      "href": "http://api.example.com/customers/456"
    }
  }
}
```

The client walks the API like a website — following links rather than constructing URLs.

---

## 2. Why Use HATEOAS?

### Loose Coupling Between Client and Server

Without HATEOAS, every client (mobile app, frontend, partner integrations) must be programmed with the URL structure of the API. When the server changes `/orders/{id}/cancel` to `/orders/{id}/cancellation`, every client breaks and must be updated in lockstep.

With HATEOAS, clients only hard-code the **entry-point** URL (e.g., `https://api.example.com/`). All downstream URLs are discovered at runtime from link relations. The server can refactor its URL structure freely — clients adapt automatically.

```
Without HATEOAS:
  Client knows:  POST /orders/{id}/cancel
  Server renames to: POST /orders/{id}/cancellation
  Result: ALL clients break simultaneously

With HATEOAS:
  Client knows:  the rel="cancel" link from the order response
  Server changes the href of "cancel" to /orders/{id}/cancellation
  Result: clients follow the new href automatically — zero breakage
```

### API Discovery

A HATEOAS API is self-describing. A new developer or client application can start at the root and discover the entire API graph by following links — no separate documentation needed to understand resource relationships.

```
GET /
{
  "_links": {
    "orders":    { "href": "/orders" },
    "customers": { "href": "/customers" },
    "products":  { "href": "/products" },
    "me":        { "href": "/users/me" }
  }
}

→ Follow "orders":
GET /orders
{
  "_embedded": { "orders": [...] },
  "_links": {
    "self":   { "href": "/orders" },
    "create": { "href": "/orders", "type": "POST" },
    "search": { "href": "/orders/search{?status,from,to}" }
  }
}
```

### Server Controls What Actions Are Available

The server embeds only the links that are valid given the current state and the current user's permissions. A read-only user sees no "cancel" or "delete" link. A cancelled order has no "ship" link. The client has no business logic to compute what is allowed — it simply checks whether a link with a given `rel` is present.

```
Admin user's GET /orders/123:               Read-only user's GET /orders/123:
  "_links": {                                 "_links": {
    "self":   { "href": "/orders/123" },        "self":   { "href": "/orders/123" }
    "cancel": { "href": "..." },              }
    "delete": { "href": "..." }             (no cancel, no delete)
  }
```

---

## 3. Advantages, Disadvantages, and Limitations

### Advantages

| Advantage | Detail |
|-----------|--------|
| **Loose coupling** | Clients depend on link relations (`rel`), not URLs — server can refactor freely |
| **API discoverability** | Clients navigate the API graph without prior knowledge |
| **State-driven actions** | Server controls what is possible; client needs no business logic for this |
| **Evolvability** | Add new link relations without breaking existing clients |
| **Self-documenting** | The response itself describes further actions |
| **Reduced client complexity** | No URL construction, no state-machine logic on the client side |

### Disadvantages

| Disadvantage | Detail |
|-------------|--------|
| **Increased server complexity** | Every endpoint must compute which links are valid for the current state and user |
| **Latency impact** | Link generation — especially with `WebMvcLinkBuilder` — involves reflection and URL resolution per request |
| **Increased response payload size** | Each response carries extra `_links` JSON that may be unused by the client |
| **Client must understand `_links`** | Legacy or third-party clients that don't parse hypermedia still need hardcoded URLs |
| **Harder to cache** | State-dependent links mean the same resource URL can return different links for different users |
| **Tooling support** | Swagger/OpenAPI tooling generates less useful docs for HATEOAS APIs |

### Limitations

```
HATEOAS does NOT solve:
  ✗ Authentication / Authorization — still need JWT, OAuth2, etc.
  ✗ Versioning — URL or header-based versioning still needed if contracts change
  ✗ Performance — it adds payload and CPU overhead for link generation
  ✗ Mandatory adoption — clients that ignore _links get no benefit

HATEOAS is MOST valuable when:
  ✓ Multiple independent clients consume the same API
  ✓ API evolves frequently (URL restructuring)
  ✓ The resource has complex state transitions (order flow, workflow engines)
  ✓ You control both server and client code

HATEOAS is LEAST valuable when:
  ✗ Simple CRUD APIs with stable URLs
  ✗ Internal microservice-to-microservice calls (use service contracts instead)
  ✗ Very high-throughput APIs where payload size matters
```

---

## 4. The Ideal Way to Implement HATEOAS

### Design Principles

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    HATEOAS Implementation Checklist                      │
│                                                                          │
│  1. Use standard link relation types (IANA rel registry):                │
│       "self"  — canonical URL of this resource                           │
│       "next"  — next page in a collection                                │
│       "prev"  — previous page                                            │
│       "first" — first page                                               │
│       "last"  — last page                                                │
│       Use custom rels for domain-specific actions:                       │
│       "cancel", "ship", "refund", "approve"                              │
│                                                                          │
│  2. Only embed links that are valid for the current state AND user       │
│       Do NOT include "cancel" if the order is DELIVERED                  │
│       Do NOT include "delete" if the user lacks ADMIN role               │
│                                                                          │
│  3. Use Representation Model wrappers (EntityModel, CollectionModel)     │
│       Never add _links manually to your domain objects                   │
│                                                                          │
│  4. Keep link generation in a dedicated Assembler class                  │
│       Separate concern: controller assembles data, assembler builds links │
│                                                                          │
│  5. Use HAL (Hypertext Application Language) as the media type           │
│       application/hal+json  → standard _links structure                  │
│       application/hal+xml   → XML variant                                │
│                                                                          │
│  6. Provide an API root ("/") that links to all top-level resources      │
│                                                                          │
│  7. Use HAL-FORMS or Siren for write operations (optional — advanced)    │
│       These include the HTTP method, expected body schema in the link    │
└──────────────────────────────────────────────────────────────────────────┘
```

### HAL — Hypertext Application Language

Spring HATEOAS produces **HAL** format by default. HAL defines two reserved properties:

```json
{
  "id": 123,
  "status": "PENDING",

  "_links": {
    "self":    { "href": "http://api.example.com/orders/123" },
    "cancel":  { "href": "http://api.example.com/orders/123/cancel" }
  },

  "_embedded": {
    "orderItems": [
      { "productId": 1, "qty": 2,
        "_links": { "self": { "href": "/products/1" } }
      }
    ]
  }
}
```

```
_links    → map of rel → Link object   (single resource links)
_embedded → map of rel → array         (nested resource collections)
```

---

## 5. Implementing HATEOAS with Spring Boot

### Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

Spring Boot auto-configures:
- HAL serialization for all `EntityModel` / `CollectionModel` responses
- `WebMvcLinkBuilder` support
- `application/hal+json` as the default media type

### Core Classes

```
EntityModel<T>           — wraps a single resource T + its links
CollectionModel<T>       — wraps a collection of resources + collection-level links
PagedModel<T>            — CollectionModel + pagination metadata (page, size, totalElements)
Link                     — a single link (href + rel + optional type/title)
IanaLinkRelations        — constants for standard IANA rels (SELF, NEXT, PREV, FIRST, LAST)
WebMvcLinkBuilder        — builds links by pointing to controller methods (type-safe)
RepresentationModelAssembler<T,D> — converts domain entity T to EntityModel<D>
```

---

## 6. Method 1 — `WebMvcLinkBuilder` (Type-Safe, Recommended)

`WebMvcLinkBuilder` builds links by calling the **actual controller method** in a "mock" way — so the link's href is always exactly what the controller is mapped to. No string URL construction.

### Real-Life Use Case: E-Commerce Order API

```java
// ─── Domain model ────────────────────────────────────────────────────
public record Order(
    Long   id,
    String status,   // PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    double total,
    Long   customerId
) {}

// ─── Controller ──────────────────────────────────────────────────────
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public EntityModel<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        return toModel(order);
    }

    @GetMapping
    public CollectionModel<EntityModel<Order>> getAllOrders() {
        List<EntityModel<Order>> orders = orderService.findAll()
            .stream()
            .map(this::toModel)
            .toList();

        return CollectionModel.of(orders,
            linkTo(methodOn(OrderController.class).getAllOrders()).withSelfRel()
        );
    }

    @PostMapping("/{id}/cancel")
    public EntityModel<Order> cancelOrder(@PathVariable Long id) {
        Order cancelled = orderService.cancel(id);
        return toModel(cancelled);
    }

    @PostMapping("/{id}/payment")
    public EntityModel<Order> payOrder(@PathVariable Long id,
                                       @RequestBody PaymentRequest payment) {
        Order paid = orderService.pay(id, payment);
        return toModel(paid);
    }

    @PostMapping("/{id}/ship")
    public EntityModel<Order> shipOrder(@PathVariable Long id) {
        Order shipped = orderService.ship(id);
        return toModel(shipped);
    }

    // ── Link builder — state-driven link generation ───────────────────
    private EntityModel<Order> toModel(Order order) {

        // "self" link is always present
        EntityModel<Order> model = EntityModel.of(order,
            linkTo(methodOn(OrderController.class)
                .getOrder(order.id()))
                .withSelfRel()
        );

        // Links depend on the CURRENT STATE of the order
        switch (order.status()) {
            case "PENDING" -> {
                model.add(
                    linkTo(methodOn(OrderController.class)
                        .cancelOrder(order.id()))
                        .withRel("cancel"),

                    linkTo(methodOn(OrderController.class)
                        .payOrder(order.id(), null))
                        .withRel("payment")
                );
            }
            case "PAID" -> {
                model.add(
                    linkTo(methodOn(OrderController.class)
                        .shipOrder(order.id()))
                        .withRel("ship")
                );
            }
            case "SHIPPED" -> {
                // no further state transitions exposed — read-only
            }
        }

        // "customer" link — cross-resource navigation
        model.add(
            linkTo(methodOn(CustomerController.class)
                .getCustomer(order.customerId()))
                .withRel("customer")
        );

        return model;
    }
}
```

### JSON Response — PENDING Order

```json
GET /orders/123

{
  "id": 123,
  "status": "PENDING",
  "total": 149.99,
  "customerId": 456,
  "_links": {
    "self": {
      "href": "http://localhost:8080/orders/123"
    },
    "cancel": {
      "href": "http://localhost:8080/orders/123/cancel"
    },
    "payment": {
      "href": "http://localhost:8080/orders/123/payment"
    },
    "customer": {
      "href": "http://localhost:8080/customers/456"
    }
  }
}
```

### JSON Response — PAID Order (cancel link is gone, ship link appears)

```json
GET /orders/123

{
  "id": 123,
  "status": "PAID",
  "total": 149.99,
  "customerId": 456,
  "_links": {
    "self":     { "href": "http://localhost:8080/orders/123" },
    "ship":     { "href": "http://localhost:8080/orders/123/ship" },
    "customer": { "href": "http://localhost:8080/customers/456" }
  }
}
```

### Using a `RepresentationModelAssembler` — Clean Separation of Concerns

Rather than building links inside the controller, extract to a dedicated **assembler** class:

```java
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class OrderModelAssembler
        extends RepresentationModelAssemblerSupport<Order, EntityModel<Order>> {

    public OrderModelAssembler() {
        super(OrderController.class, (Class<EntityModel<Order>>) (Class<?>) EntityModel.class);
    }

    @Override
    public EntityModel<Order> toModel(Order order) {
        EntityModel<Order> model = EntityModel.of(order,
            linkTo(methodOn(OrderController.class)
                .getOrder(order.id()))
                .withSelfRel()
        );

        if ("PENDING".equals(order.status())) {
            model.add(
                linkTo(methodOn(OrderController.class).cancelOrder(order.id())).withRel("cancel"),
                linkTo(methodOn(OrderController.class).payOrder(order.id(), null)).withRel("payment")
            );
        }

        if ("PAID".equals(order.status())) {
            model.add(
                linkTo(methodOn(OrderController.class).shipOrder(order.id())).withRel("ship")
            );
        }

        model.add(
            linkTo(methodOn(CustomerController.class)
                .getCustomer(order.customerId()))
                .withRel("customer")
        );

        return model;
    }
}

// ── Slim controller — delegates all link building to assembler ────────
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderModelAssembler assembler;

    public OrderController(OrderService orderService, OrderModelAssembler assembler) {
        this.orderService = orderService;
        this.assembler    = assembler;
    }

    @GetMapping("/{id}")
    public EntityModel<Order> getOrder(@PathVariable Long id) {
        return assembler.toModel(orderService.findById(id));
    }

    @GetMapping
    public CollectionModel<EntityModel<Order>> getAllOrders() {
        List<EntityModel<Order>> list = orderService.findAll()
            .stream().map(assembler::toModel).toList();

        return CollectionModel.of(list,
            linkTo(methodOn(OrderController.class).getAllOrders()).withSelfRel()
        );
    }

    // ... action endpoints (cancel, pay, ship)
}
```

### `WebMvcLinkBuilder` — Key Methods

```java
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// Point to a controller CLASS
linkTo(OrderController.class)
    .withSelfRel()
// → href: http://host/orders

// Point to a specific METHOD (type-safe — method call is a mock, never executed)
linkTo(methodOn(OrderController.class).getOrder(123L))
    .withSelfRel()
// → href: http://host/orders/123

linkTo(methodOn(OrderController.class).getOrder(123L))
    .withRel("cancel")
// → href: http://host/orders/123  with rel "cancel"
//   (use the actual cancel method for correct URL):
linkTo(methodOn(OrderController.class).cancelOrder(123L))
    .withRel("cancel")
// → href: http://host/orders/123/cancel

// slash() — append a path segment
linkTo(OrderController.class).slash(id).slash("history")
// → href: http://host/orders/123/history

// afford() — add HAL-FORMS affordances (describe POST body schema — advanced)
linkTo(methodOn(OrderController.class).getOrder(123L))
    .withSelfRel()
    .andAffordance(afford(methodOn(OrderController.class)
        .cancelOrder(123L)));
```

---

## 7. Method 2 — `Link.of()` (Manual Link Construction)

`Link.of()` builds a link directly from a URI string — useful when the target is an external URL, a templated URI, or a resource not in this Spring application.

```java
import org.springframework.hateoas.Link;
import org.springframework.hateoas.UriTemplate;

// ── Simple absolute URL ──────────────────────────────────────────────
Link selfLink = Link.of("http://api.example.com/orders/123")
    .withSelfRel();

Link cancelLink = Link.of("http://api.example.com/orders/123/cancel")
    .withRel("cancel");

// ── With explicit rel ────────────────────────────────────────────────
Link paymentLink = Link.of("http://api.example.com/orders/123/payment", "payment");

// ── URI Template (RFC 6570) — for collection search links ────────────
Link searchLink = Link.of(
    UriTemplate.of("http://api.example.com/orders{?status,from,to,page,size}"),
    "search"
);
// Client expands: /orders?status=PENDING&page=0&size=20

// ── External link — cross-system navigation ──────────────────────────
Link trackingLink = Link.of(
    "https://tracking.fedex.com/track/{trackingNumber}",
    "tracking"
);

// ── Building EntityModel with Link.of() ─────────────────────────────
@GetMapping("/{id}")
public EntityModel<Order> getOrder(@PathVariable Long id,
                                   HttpServletRequest httpRequest) {
    Order order = orderService.findById(id);

    String baseUrl = ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .build()
        .toUriString();

    return EntityModel.of(order,
        Link.of(baseUrl + "/orders/" + id).withSelfRel(),
        Link.of(baseUrl + "/orders/" + id + "/cancel").withRel("cancel"),
        Link.of(baseUrl + "/customers/" + order.customerId()).withRel("customer"),
        Link.of("https://tracking.example.com/track/" + order.trackingId(), "tracking")
    );
}
```

### `WebMvcLinkBuilder` vs `Link.of()` — When to Use Which

```
┌──────────────────────────────────────────────────────────────────────────┐
│             WebMvcLinkBuilder  vs  Link.of()                             │
│                                                                          │
│  WebMvcLinkBuilder:                                                      │
│    ✓ Links to your OWN Spring MVC controllers                            │
│    ✓ Type-safe — compiler catches wrong method references                │
│    ✓ Automatically uses correct host/port/context-path                   │
│    ✓ Respects @RequestMapping and @PathVariable bindings                 │
│    → Use for: all internal resource links                                │
│                                                                          │
│  Link.of():                                                              │
│    ✓ External URLs (third-party APIs, CDN, tracking services)            │
│    ✓ RFC 6570 URI Templates (search links with multiple params)          │
│    ✓ URLs constructed from runtime values (tracking IDs from DB, etc.)   │
│    ✓ When you don't have a controller method to point to                 │
│    → Use for: external links, templated links, non-MVC resources         │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Paginated Collections — `PagedModel`

For paginated responses, Spring HATEOAS provides `PagedModel` which adds `_links` for `first`, `prev`, `self`, `next`, `last`:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderModelAssembler assembler;

    public OrderController(OrderRepository orderRepository,
                           OrderModelAssembler assembler) {
        this.orderRepository = orderRepository;
        this.assembler       = assembler;
    }

    @GetMapping
    public PagedModel<EntityModel<Order>> getOrders(
            Pageable pageable,
            PagedResourcesAssembler<Order> pagedAssembler) {

        Page<Order> page = orderRepository.findAll(pageable);

        return pagedAssembler.toModel(page, assembler);
        // ↑ automatically generates _links.first, .prev, .self, .next, .last
    }
}
```

```json
GET /orders?page=1&size=2

{
  "_embedded": {
    "orders": [
      { "id": 3, "status": "PAID", "_links": { "self": { "href": "/orders/3" } } },
      { "id": 4, "status": "PENDING", "_links": { "self": { "href": "/orders/4" } } }
    ]
  },
  "_links": {
    "first": { "href": "http://localhost:8080/orders?page=0&size=2" },
    "prev":  { "href": "http://localhost:8080/orders?page=0&size=2" },
    "self":  { "href": "http://localhost:8080/orders?page=1&size=2" },
    "next":  { "href": "http://localhost:8080/orders?page=2&size=2" },
    "last":  { "href": "http://localhost:8080/orders?page=4&size=2" }
  },
  "page": {
    "size": 2,
    "totalElements": 10,
    "totalPages": 5,
    "number": 1
  }
}
```

---

## 9. API Root — The Entry Point

A proper HATEOAS API exposes a root endpoint that links to all top-level resources. Clients start here and navigate — they never construct URLs.

```java
@RestController
@RequestMapping("/")
public class RootController {

    @GetMapping
    public RepresentationModel<?> root() {
        RepresentationModel<?> root = new RepresentationModel<>();

        root.add(linkTo(methodOn(OrderController.class).getAllOrders()).withRel("orders"));
        root.add(linkTo(methodOn(CustomerController.class).getAllCustomers()).withRel("customers"));
        root.add(linkTo(methodOn(ProductController.class).getAllProducts()).withRel("products"));
        root.add(Link.of("http://api.example.com/docs", "documentation"));

        return root;
    }
}
```

```json
GET /

{
  "_links": {
    "orders":        { "href": "http://localhost:8080/orders" },
    "customers":     { "href": "http://localhost:8080/customers" },
    "products":      { "href": "http://localhost:8080/products" },
    "documentation": { "href": "http://api.example.com/docs" }
  }
}
```

---

## 10. Summary — HATEOAS Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Spring HATEOAS — Full Picture                             │
│                                                                              │
│  Client                                                                      │
│    │                                                                         │
│    │ 1. GET /   (entry point — only hardcoded URL)                           │
│    ▼                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  RootController → { _links: { orders, customers, products } }       │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│    │                                                                         │
│    │ 2. Follow rel="orders" → GET /orders                                   │
│    ▼                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  OrderController.getAllOrders()                                      │    │
│  │  → CollectionModel<EntityModel<Order>>                               │    │
│  │  → Each order has: self, cancel/payment/ship (state-dependent)      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│    │                                                                         │
│    │ 3. Follow rel="cancel" → POST /orders/123/cancel                       │
│    ▼                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  OrderController.cancelOrder(123)                                    │    │
│  │  → Returns cancelled order + new set of links                        │    │
│  │  → No cancel link any more (terminal state)                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  Layer stack:                                                                │
│    Controller  → uses OrderModelAssembler.toModel()                         │
│    Assembler   → state-conditional WebMvcLinkBuilder calls                  │
│    Spring HATEOAS → serializes to HAL (application/hal+json)                │
│    Client      → parses _links, uses rel names, ignores hrefs               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Quick Reference

```
HATEOAS concept:
  Resource       → EntityModel<T> (single) / CollectionModel<T> (many)
  Links          → EntityModel.add(Link...) / WebMvcLinkBuilder / Link.of()
  Assembler      → RepresentationModelAssemblerSupport<T, EntityModel<T>>
  Paginated      → PagedModel<T> via PagedResourcesAssembler
  Format         → HAL (application/hal+json) — default in Spring HATEOAS

Golden rules:
  1. Only embed links valid for the current state AND user role
  2. Use WebMvcLinkBuilder for internal controller links (type-safe)
  3. Use Link.of() for external URLs and URI templates
  4. Put link logic in an Assembler, not the Controller
  5. Expose an API root ("/") as the single entry point
  6. Use standard IANA rel names where possible (self, next, prev, first, last)
```