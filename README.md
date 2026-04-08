[한국어](README.ko.md)

# CommandHandler

**What if your API method name WAS the API spec?**

> One module = one service class. One command = one method. The method name IS the API spec.

CommandHandler is an AI-Native backend design pattern for structuring API endpoints so that both human developers and AI can easily understand, navigate, and modify backend code. This is not a library -- it is a design pattern with reference implementations.

---

## Table of Contents

- [The Problem with Traditional REST Controllers](#the-problem-with-traditional-rest-controllers)
- [The CommandHandler Solution](#the-commandhandler-solution)
- [Request Flow](#request-flow)
- [The Auth Guarantee](#the-auth-guarantee)
- [Why AI Loves This Pattern](#why-ai-loves-this-pattern)
- [Comparison](#comparison)
- [Real-World Scale](#real-world-scale)
- [Reference Code](#reference-code)
- [Links](#links)

---

## The Problem with Traditional REST Controllers

Spring's `@RestController` approach maps one URL to one method:

```java
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @GetMapping("/profile/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid) { ... }

    @PutMapping("/profile/{uid}")
    public ResponseEntity<?> updateProfile(@PathVariable String uid, @RequestBody ...) { ... }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(Authentication auth) { ... }

    @PostMapping("/settings")
    public ResponseEntity<?> updateSettings(Authentication auth, @RequestBody ...) { ... }

    // ... 50 more endpoints, 50 more URL mappings
}
```

This works well for human developers. But when AI is your primary code collaborator, the friction compounds:

- **50 endpoints = 50 URL mappings** to maintain, each with different HTTP methods, path variables, and request body shapes
- **Auth is scattered** across Spring Security config, `@PreAuthorize` annotations, interceptors, and sometimes manual `SecurityContext` checks
- **AI must trace multiple files** to understand a single endpoint: the controller, service layer, repository, security configuration, and DTO definitions
- **Adding a new endpoint** requires: new URL, decide HTTP method, add security rule, create DTO, wire service -- multiple files touched for one feature

The traditional approach optimizes for HTTP semantics and RESTful purity. CommandHandler optimizes for **readability, isolation, and predictability** -- qualities that directly improve both human and AI comprehension.

---

## The CommandHandler Solution

One URL per business module. Commands dispatched by name. Auth declared per method.

```java
// ONE class handles ALL user-related commands
public class UserCommandService extends AbstractCommandService {

    @CommandHandler                          // needAuth=true, allowGuest=false (default)
    public ResponseVO getProfile(CommandContext ctx) {
        String uid = ctx.getUser().getUid(); // user is GUARANTEED non-null
        // ... query and return profile
    }

    @CommandHandler
    public ResponseVO updateProfile(CommandContext ctx) {
        JsonObject payload = ctx.getFirstCommandPayload();
        // ... validate and update
    }

    @CommandHandler(allowGuest = true)       // guests can access this
    public ResponseVO getPublicInfo(CommandContext ctx) {
        if (ctx.getUser() != null) { /* personalize for members */ }
        // ... return public data
    }

    @CommandHandler(needAuth = false)        // fully public
    public ResponseVO healthCheck(CommandContext ctx) {
        return ResponseHelper.success("OK");
    }
}
```

**Adding a new endpoint:** write one method, add `@CommandHandler`. No URL mapping, no security config changes, no DTO classes. The method name _is_ the API spec.

---

## Request Flow

<img src="https://mermaid.ink/img/Zmxvd2NoYXJ0IExSCiAgICBBWyJQT1NUIC9hcGkvdjEvdXNlcgp7Y29tbWFuZDogZ2V0LXByb2ZpbGV9Il0gLS0-IEJbIkZyb250IFNlcnZlcgpBcGlQcm94eUNvbnRyb2xsZXIiXQogICAgQiAtLT4gQ1siQVBJIFNlcnZlcgpTZXJ2aWNlQXBpQ29udHJvbGxlciJdCiAgICBDIC0tPiBEWyIvdXNlciDihpIgVXNlckNvbW1hbmRTZXJ2aWNlIl0KICAgIEQgLS0-IEVbIkFic3RyYWN0Q29tbWFuZFNlcnZpY2UKLmhhbmRsZVJlcXVlc3QoKSJdCiAgICBFIC0tPiBGWyJrZWJhYuKGkmNhbWVsQ2FzZQpnZXQtcHJvZmlsZSDihpIgZ2V0UHJvZmlsZSJdCiAgICBGIC0tPiBHWyJAQ29tbWFuZEhhbmRsZXIgY2hlY2sKbmVlZEF1dGg_IGFsbG93R3Vlc3Q_Il0KICAgIEcgLS0-IEhbImdldFByb2ZpbGUoY3R4KQppbnZva2VkIHZpYSByZWZsZWN0aW9uIl0KICAgIEggLS0-IElbIlJlc3BvbnNlVk8Ke2NvZGU6MjAwLCByZXNwb25zZTp7Li4ufX0iXQoKICAgIHN0eWxlIEEgZmlsbDojZTNmMmZkLHN0cm9rZTojMTU2NWMwLGNvbG9yOiMwMDAKICAgIHN0eWxlIEUgZmlsbDojZmZmM2UwLHN0cm9rZTojZTY1MTAwLGNvbG9yOiMwMDAKICAgIHN0eWxlIEcgZmlsbDojZmNlNGVjLHN0cm9rZTojYzYyODI4LGNvbG9yOiMwMDAKICAgIHN0eWxlIEkgZmlsbDojZThmNWU5LHN0cm9rZTojMmU3ZDMyLGNvbG9yOiMwMDA=" alt="CommandHandler Request Flow" />

### Step by step:

1. **Client sends a POST** to `/api/v1/user` with `{"command": "get-profile", "commandPayload": [{"uid": "123"}]}`
2. **Front Server proxies** the request to the API server (Frontend never touches the database)
3. **ServiceApiController** maps the URL path `/user` to `UserCommandService.class`
4. **AbstractCommandService** takes over:
   - Converts `"get-profile"` to `"getProfile"` (kebab-to-camelCase)
   - Looks up `getProfile()` in the method registry
   - Reads the `@CommandHandler` annotation for auth requirements
   - Enforces auth (rejects before method runs if unauthorized)
   - Invokes the method with a populated `CommandContext`
5. **Method executes** business logic and returns a `ResponseVO`
6. **Response** is always HTTP 200 with a business `code` inside the body

### URL-to-Service mapping

```java
@RequestMapping("/api/v1")
public class ServiceApiController {

    @RequestMapping("/user")
    public ResponseVO apiUser(...) {
        return processRequest(payload, request, auth, UserCommandService.class);
    }

    @RequestMapping("/order")
    public ResponseVO apiOrder(...) {
        return processRequest(payload, request, auth, OrderCommandService.class);
    }

    @RequestMapping("/admin/user")
    public ResponseVO apiAdminUser(...) {
        return processRequest(payload, request, auth, UserAdminCommandService.class);
    }
}
```

One line per module. The ServiceApiController is the only routing configuration in the entire application.

---

## The Auth Guarantee

This is the most important concept in the pattern. The `@CommandHandler` annotation is not a suggestion -- it is a **contract enforced by the framework** before your method runs.

<img src="https://mermaid.ink/img/Zmxvd2NoYXJ0IFRECiAgICBBWyJJbmNvbWluZyBSZXF1ZXN0Il0gLS0-IEJ7IkBDb21tYW5kSGFuZGxlcgpwcmVzZW50PyJ9CiAgICBCIC0tPnxOb3wgWlsiNDA0OiBVbmtub3duIGNvbW1hbmQiXQogICAgQiAtLT58WWVzfCBDeyJuZWVkQXV0aD8ifQogICAgQyAtLT58ZmFsc2V8IEhbIk5vIGF1dGggY2hlY2sKUHVibGljIGVuZHBvaW50Il0KICAgIEggLS0-IElbIkludm9rZSBtZXRob2QKY3R4LmdldFVzZXIoKSA9IG51bGwiXQogICAgQyAtLT58dHJ1ZXwgRHsiVG9rZW4gdmFsaWQ_In0KICAgIEQgLS0-fE5vfCBZWyI0MDE6IEF1dGhlbnRpY2F0aW9uIHJlcXVpcmVkIl0KICAgIEQgLS0-fFllc3wgRXsiYWxsb3dHdWVzdD8ifQogICAgRSAtLT58dHJ1ZXwgRlsiR3Vlc3QgT0sKY3R4LmdldFVzZXIoKSBtYXkgYmUgbnVsbCJdCiAgICBGIC0tPiBKWyJJbnZva2UgbWV0aG9kIl0KICAgIEUgLS0-fGZhbHNlfCBHeyJVc2VyIGV4aXN0cwppbiB0b2tlbj8ifQogICAgRyAtLT58Tm98IFhbIjQwMTogTWVtYmVyIHJlcXVpcmVkIl0KICAgIEcgLS0-fFllc3wgS1siVXNlciBHVUFSQU5URUVECmN0eC5nZXRVc2VyKCkgbmV2ZXIgbnVsbCJdCiAgICBLIC0tPiBMWyJJbnZva2UgbWV0aG9kIl0KCiAgICBzdHlsZSBIIGZpbGw6I2U4ZjVlOSxzdHJva2U6IzJlN2QzMixjb2xvcjojMDAwCiAgICBzdHlsZSBGIGZpbGw6I2ZmZjNlMCxzdHJva2U6I2U2NTEwMCxjb2xvcjojMDAwCiAgICBzdHlsZSBLIGZpbGw6I2UzZjJmZCxzdHJva2U6IzE1NjVjMCxjb2xvcjojMDAwCiAgICBzdHlsZSBaIGZpbGw6I2ZmZWJlZSxzdHJva2U6I2M2MjgyOCxjb2xvcjojMDAwCiAgICBzdHlsZSBZIGZpbGw6I2ZmZWJlZSxzdHJva2U6I2M2MjgyOCxjb2xvcjojMDAwCiAgICBzdHlsZSBYIGZpbGw6I2ZmZWJlZSxzdHJva2U6I2M2MjgyOCxjb2xvcjojMDAw" alt="Auth Decision Tree" />

### Three auth levels

**Level 1: Authenticated (default)**
```java
@CommandHandler  // needAuth=true, allowGuest=false
public ResponseVO getProfile(CommandContext ctx) {
    // ctx.getUser() is NEVER null.
    // The framework rejected unauthenticated requests before reaching this line.
    String uid = ctx.getUser().getUid();  // safe, always
}
```

**Level 2: Guest-allowed**
```java
@CommandHandler(allowGuest = true)  // needAuth=true, allowGuest=true
public ResponseVO getPublicInfo(CommandContext ctx) {
    // ctx.getUser() MAY be null.
    // A valid token is required, but it can be a guest token.
    if (ctx.getUser() != null) {
        // personalize for authenticated member
    }
}
```

**Level 3: Public**
```java
@CommandHandler(needAuth = false)
public ResponseVO healthCheck(CommandContext ctx) {
    // No auth at all. Anyone can call this.
    // ctx.getUser() is always null.
}
```

### Why this matters

In traditional Spring Security, you configure auth in `SecurityConfig`, possibly add `@PreAuthorize`, and then hope that `SecurityContextHolder.getContext().getAuthentication()` returns what you expect. The auth logic and the business logic are in different files, connected by convention and trust.

In CommandHandler, the annotation on the method **is** the auth configuration. The framework enforces it. When you see `@CommandHandler` with no parameters, you know -- with absolute certainty -- that `ctx.getUser()` is not null. This certainty is what makes the pattern AI-friendly: the annotation alone tells the AI everything it needs about auth, without reading any other file.

---

## Why AI Loves This Pattern

### 1. API spec = method name (zero ambiguity)

```
Command: "get-user-settings"  →  Method: getUserSettings()
Command: "update-nickname"    →  Method: updateNickname()
Command: "delete-account"     →  Method: deleteAccount()
```

AI sees a command name in the client code, instantly knows which method to find. No URL pattern matching, no HTTP method guessing, no path variable parsing.

### 2. User guaranteed by annotation (no null-check guessing)

AI does not need to wonder "is the user authenticated here?" The annotation declares it. AI reads the annotation, writes code accordingly. No tracing through security middleware.

### 3. One method = complete business logic (self-contained)

Each `@CommandHandler` method is the entire story for that command. Read, validate, process, respond -- all in one method. Changing `getProfile()` cannot break `updateProfile()`. AI modifies one method with confidence.

### 4. Same structure everywhere (convention)

Every CommandService in every project follows the same pattern:
- Extend `AbstractCommandService`
- Add `@CommandHandler` methods
- Use `CommandContext` for input
- Return `ResponseVO`

AI learns this once and applies it to every module in every project.

### 5. No import chain to trace (context in CommandContext)

Everything the method needs arrives in `CommandContext`: the payload, the user, the request. AI does not need to trace through injected services, auto-wired repositories, or imported utilities to understand what data is available.

---

## Comparison

| | Traditional REST | CommandHandler |
|---|---|---|
| **Endpoint definition** | `@GetMapping`, `@PostMapping` per method | One URL per module, methods dispatched by command name |
| **Command routing** | URL path segments + HTTP methods | JSON `command` field + reflection |
| **Auth declaration** | `SecurityConfig` + `@PreAuthorize` (separate files) | `@CommandHandler(needAuth, allowGuest)` on the method |
| **User availability** | Check `SecurityContext` manually, null-check required | Guaranteed by framework (or explicitly optional) |
| **AI comprehension** | Must read controller + service + security config | Read ONE method, annotation tells everything |
| **Adding new endpoint** | New URL + HTTP method + security rule + DTO | Add a method with `@CommandHandler` |
| **Response format** | Varies (`ResponseEntity`, custom DTOs, exceptions) | Always `ResponseVO` (code, message, response) |
| **HTTP status** | Varies (200, 201, 400, 404, 500...) | Always 200. Business code in response body. |

### Why always HTTP 200?

HTTP status codes are transport-level concerns. "User not found" is not a transport error -- it is a business logic result. By always returning HTTP 200, the transport layer and business layer are cleanly separated. Clients check `response.code`, not the HTTP status.

```json
// Success
{ "code": 200, "message": "Success", "response": { "uid": "123", "nickname": "Alice" }, "responseAt": 1712345678 }

// Business error (still HTTP 200)
{ "code": 404, "message": "User not found", "response": null, "responseAt": 1712345678 }
```

---

## Real-World Scale

This pattern is not theoretical. It powers 10+ production projects at [Neoulsoft](https://neoulsoft.com), built on the [NSKit](https://nskit.io) framework.

| Metric | Value |
|---|---|
| Production projects using this pattern | 10+ |
| Largest project (commands per service) | 15-20 methods |
| Average command implementation time (with AI) | 3-5 minutes |
| Typical CommandService file | 200-500 lines |
| Auth-related bugs since adoption | Near zero (guaranteed by framework) |

The pattern scales by adding modules (more CommandService classes), not by adding complexity to existing ones. Each service stays focused on its business domain.

---

## Reference Code

The [`/reference`](./reference) directory contains simplified, clean implementations of each component:

| File | Purpose |
|---|---|
| [`CommandHandler.java`](reference/CommandHandler.java) | Annotation definition with `needAuth` and `allowGuest` |
| [`AbstractCommandService.java`](reference/AbstractCommandService.java) | Core dispatch engine: reflection, kebab-to-camelCase, method registry, auth enforcement |
| [`CommandContext.java`](reference/CommandContext.java) | Request context: payload access, user access, the user guarantee contract |
| [`ResponseVO.java`](reference/ResponseVO.java) | Response envelope: code, message, response, responseAt |
| [`ResponseHelper.java`](reference/ResponseHelper.java) | Static factory methods for creating responses |
| [`ExampleCommandService.java`](reference/ExampleCommandService.java) | Working example with all three auth levels |

These are reference implementations intended to illustrate the pattern. They use generic packages (`com.example.api.command`) and contain no project-specific code.

---

## Links

- [AI-Native Design](https://github.com/nskit-io/ai-native-design) -- The design philosophy behind this pattern
- [Growing Pool Cache](https://github.com/nskit-io/growing-pool-cache) -- AI-friendly caching pattern
- [CSW](https://github.com/nskit-io/csw) -- Claude Subscription Worker
- [NSKit](https://nskit.io) -- The framework built on these principles

---

## License

[MIT](LICENSE)

---

<p align="center">
  <i>"The best API documentation is the code itself.<br/>CommandHandler makes the code self-documenting by design."</i>
</p>
