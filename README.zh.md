[🇬🇧 English](README.md) · [🇰🇷 한국어](README.ko.md) · [🇯🇵 日本語](README.ja.md)

# CommandHandler

**如果 API 方法名本身就是 API 规范呢?**

> 一个模块 = 一个服务类。一条命令 = 一个方法。方法名即 API 规范。

> 用于 [**NSKit**](https://github.com/nskit-io/nskit-io) 生产服务 — *有结构,才有无限组合*。所有运行中的 NSKit 服务(NewMyoung、Pellow、Byeol、SING:ON、BigFoot 等)都采用此模式。

---

## 概览

CommandHandler 是用于 Spring Boot 后端的 **AI-Native 设计模式**。它以让人类开发者和 AI 都能轻松理解、导航、修改 API 端点的方式来组织后端代码。这不是一个库,而是一种设计模式,附带参考实现。

---

## 传统 REST Controller 的问题

```java
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    @GetMapping("/profile/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid) { ... }

    @PutMapping("/profile/{uid}")
    public ResponseEntity<?> updateProfile(@PathVariable String uid, @RequestBody ...) { ... }

    // ... 50 个端点,50 条 URL 映射
}
```

当 AI 成为主要协作者,摩擦成倍增加:

- **50 个端点 = 50 条 URL 映射** 要维护,每个都有不同的 HTTP 方法、路径变量、请求体结构。
- **认证散落** 在 Spring Security 配置、`@PreAuthorize`、拦截器、手动 `SecurityContext` 检查之中。
- **AI 必须追踪多个文件** 才能理解一个端点:控制器、服务层、仓储层、安全配置、DTO 定义。
- **新增一个端点** 需要:新 URL + 选 HTTP 方法 + 加安全规则 + 建 DTO + 接服务 — 一个功能修改多个文件。

---

## CommandHandler 的解法

```java
public class UserCommandService extends AbstractCommandService {

    @CommandHandler  // needAuth=true, allowGuest=false
    public ResponseVO getProfile(CommandContext ctx) throws HandledServiceException {
        String uid = ctx.getUser().getUid();  // 身份来自认证令牌,而不是 payload
        // ...
        return ResponseHelper.response(200, "Success", profile);
    }

    @CommandHandler
    public ResponseVO updateProfile(CommandContext ctx) throws HandledServiceException {
        // ...
    }
}
```

URL 只有一条:`POST /api/v1/user`。payload 中的 `command` 字段决定调用哪个方法(`kebab-case` → `camelCase` 自动转换)。

**AI 只读一个文件就理解整个模块**。

---

## 请求流程

```
Frontend (nskit-api.js)
  └→ POST /api/v1/{module}
         payload: { command: "get-profile", commandPayload: [{...}] }
              └→ ServiceApiController
                    └→ AbstractCommandService.handleRequest()
                          ├→ 认证(NSkitAuthorizationFilter)
                          ├→ kebab→camel 转换
                          └→ 反射调用 UserCommandService.getProfile(ctx)
```

认证(是否登录)由框架保证,按 `@CommandHandler` 的声明在方法执行前强制执行。授权(是否本人数据、是否管理员)在命令内显式判定。仅靠 `/admin/` 之类的路径前缀什么也保护不了。

---

## 为什么 AI 偏爱这种模式

| 方面 | 传统 REST | CommandHandler |
|---|---|---|
| 新增端点 | 改多个文件(URL、安全配置、DTO、服务) | 在一个文件里加一个方法 |
| 认证保证 | 分散 | 注解统一(授权在命令内显式判定) |
| 理解所需阅读 | 控制器 + 服务 + 安全 + DTO | 一个 Service 类 |
| 重构影响 | 影响范围不明 | 在一个类内收敛 |

---

## 参考代码

完整技术内容、认证保证详情、payload 示例、实际运行规模、参考实现请查看英文版: **[README (English)](README.md)**

---

<div align="center">

**CommandHandler** · Part of the **[NSKit](https://github.com/nskit-io/nskit-io)** ecosystem

© 2026 Neoulsoft Inc.

</div>
