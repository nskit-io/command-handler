[English](README.md)

# CommandHandler

**API 메서드 이름이 곧 API 스펙이라면?**

> 모듈 1개 = 서비스 클래스 1개. 커맨드 1개 = 메서드 1개. 메서드 이름이 곧 API 스펙.

CommandHandler는 AI-Native 백엔드 디자인 패턴입니다. API 엔드포인트를 사람 개발자와 AI 모두가 쉽게 이해하고, 탐색하고, 수정할 수 있도록 구조화합니다. 라이브러리가 아닌 디자인 패턴이며, 레퍼런스 구현 코드를 포함합니다.

---

## 목차

- [기존 REST Controller의 문제점](#기존-rest-controller의-문제점)
- [CommandHandler 솔루션](#commandhandler-솔루션)
- [요청 흐름](#요청-흐름)
- [Auth Guarantee (인증 보장)](#auth-guarantee-인증-보장)
- [AI가 이 패턴을 좋아하는 이유](#ai가-이-패턴을-좋아하는-이유)
- [비교표](#비교표)
- [실제 운영 규모](#실제-운영-규모)
- [레퍼런스 코드](#레퍼런스-코드)
- [링크](#링크)

---

## 기존 REST Controller의 문제점

Spring의 `@RestController` 방식은 하나의 URL에 하나의 메서드를 매핑합니다:

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

    // ... 50개 이상의 엔드포인트, 50개 이상의 URL 매핑
}
```

사람 개발자에게는 잘 동작합니다. 하지만 AI가 주요 코드 작성자일 때, 마찰이 누적됩니다:

- **50개 엔드포인트 = 50개 URL 매핑** -- 각각 다른 HTTP 메서드, 경로 변수, 요청 바디 형태
- **인증이 여러 곳에 분산** -- Spring Security 설정, `@PreAuthorize` 어노테이션, 인터셉터, 수동 `SecurityContext` 체크
- **AI가 하나의 엔드포인트를 이해하려면 여러 파일을 탐색** -- 컨트롤러, 서비스 레이어, 리포지토리, 보안 설정, DTO 정의
- **새 엔드포인트 추가 시** -- 새 URL, HTTP 메서드 결정, 보안 규칙 추가, DTO 생성, 서비스 연결 등 여러 파일 수정 필요

기존 방식은 HTTP 의미론과 RESTful 순수성에 최적화되어 있습니다. CommandHandler는 **가독성, 격리, 예측 가능성**에 최적화됩니다 -- 사람과 AI의 이해도를 직접적으로 높이는 특성들입니다.

---

## CommandHandler 솔루션

비즈니스 모듈당 URL 하나. 이름으로 커맨드 디스패치. 메서드별 인증 선언.

```java
// 하나의 클래스가 모든 user 관련 커맨드를 처리
public class UserCommandService extends AbstractCommandService {

    @CommandHandler                          // needAuth=true, allowGuest=false (기본값)
    public ResponseVO getProfile(CommandContext ctx) {
        String uid = ctx.getUser().getUid(); // user는 반드시 존재 (null 아님)
        // ... 프로필 조회 및 반환
    }

    @CommandHandler
    public ResponseVO updateProfile(CommandContext ctx) {
        JsonObject payload = ctx.getFirstCommandPayload();
        // ... 검증 및 업데이트
    }

    @CommandHandler(allowGuest = true)       // 게스트도 접근 가능
    public ResponseVO getPublicInfo(CommandContext ctx) {
        if (ctx.getUser() != null) { /* 회원이면 개인화 */ }
        // ... 공개 정보 반환
    }

    @CommandHandler(needAuth = false)        // 완전 공개
    public ResponseVO healthCheck(CommandContext ctx) {
        return ResponseHelper.success("OK");
    }
}
```

**새 엔드포인트 추가:** 메서드 하나 작성하고 `@CommandHandler` 추가. URL 매핑도, 보안 설정 변경도, DTO 클래스도 필요 없습니다. 메서드 이름이 _곧_ API 스펙입니다.

---

## 요청 흐름

<img src="https://mermaid.ink/img/Zmxvd2NoYXJ0IFRECiAgICBBWyJDbGllbnQKUE9TVCAvYXBpL3YxL3VzZXIKe2NvbW1hbmQ6IGdldC1wcm9maWxlfSJdIC0tPiBCWyJGcm9udCBTZXJ2ZXIKQXBpUHJveHlDb250cm9sbGVyIl0KICAgIEIgLS0-IENbIkFQSSBTZXJ2ZXIKU2VydmljZUFwaUNvbnRyb2xsZXIiXQogICAgQyAtLT4gRFsiL3VzZXIg4oaSIFVzZXJDb21tYW5kU2VydmljZSJdCiAgICBEIC0tPiBFWyJBYnN0cmFjdENvbW1hbmRTZXJ2aWNlCi5oYW5kbGVSZXF1ZXN0KCkiXQogICAgRSAtLT4gRlsia2ViYWIg4oaSIGNhbWVsQ2FzZQpnZXQtcHJvZmlsZSDihpIgZ2V0UHJvZmlsZSJdCiAgICBGIC0tPiBHeyJAQ29tbWFuZEhhbmRsZXIgY2hlY2sKbmVlZEF1dGg_IGFsbG93R3Vlc3Q_In0KICAgIEcgLS0-fEF1dGggT0t8IEhbImdldFByb2ZpbGUoY3R4KQpSZWZsZWN0aW9uIGludm9rZSJdCiAgICBHIC0tPnxBdXRoIEZhaWx8IElbIjQwMSBVbmF1dGhvcml6ZWQiXQogICAgSCAtLT4gSlsiUmVzcG9uc2VWTwp7Y29kZToyMDAsIHJlc3BvbnNlOnsuLi59fSJdCgogICAgc3R5bGUgQSBmaWxsOiNlM2YyZmQsc3Ryb2tlOiMxNTY1YzAsY29sb3I6IzAwMAogICAgc3R5bGUgRSBmaWxsOiNmZmYzZTAsc3Ryb2tlOiNlNjUxMDAsY29sb3I6IzAwMAogICAgc3R5bGUgRyBmaWxsOiNmY2U0ZWMsc3Ryb2tlOiNjNjI4MjgsY29sb3I6IzAwMAogICAgc3R5bGUgSiBmaWxsOiNlOGY1ZTksc3Ryb2tlOiMyZTdkMzIsY29sb3I6IzAwMAogICAgc3R5bGUgSSBmaWxsOiNmZmNkZDIsc3Ryb2tlOiNjNjI4MjgsY29sb3I6IzAwMA==" alt="CommandHandler 요청 흐름" />

### 단계별 설명:

1. **클라이언트가 POST** 요청을 `/api/v1/user`로 전송: `{"command": "get-profile", "commandPayload": [{"uid": "123"}]}`
2. **Front 서버가 프록시** -- API 서버로 전달 (Frontend는 절대 DB에 접근하지 않음)
3. **ServiceApiController**가 URL 경로 `/user`를 `UserCommandService.class`로 매핑
4. **AbstractCommandService**가 처리:
   - `"get-profile"` → `"getProfile"` 변환 (kebab-to-camelCase)
   - 메서드 레지스트리에서 `getProfile()` 조회
   - `@CommandHandler` 어노테이션에서 인증 요구사항 확인
   - 인증 적용 (미인증 시 메서드 실행 전 거부)
   - `CommandContext`를 채워서 메서드 호출
5. **메서드 실행** -- 비즈니스 로직 처리 후 `ResponseVO` 반환
6. **응답**은 항상 HTTP 200, 비즈니스 `code`는 바디 내부

---

## Auth Guarantee (인증 보장)

이 패턴에서 가장 중요한 개념입니다. `@CommandHandler` 어노테이션은 단순한 힌트가 아니라 -- 메서드 실행 전에 **프레임워크가 강제하는 계약**입니다.

<img src="https://mermaid.ink/img/Zmxvd2NoYXJ0IFRECiAgICBBWyJJbmNvbWluZyBSZXF1ZXN0Il0gLS0-IEJ7IkBDb21tYW5kSGFuZGxlcgpwcmVzZW50PyJ9CiAgICBCIC0tPnxOb3wgWlsiNDA0OiBVbmtub3duIGNvbW1hbmQiXQogICAgQiAtLT58WWVzfCBDeyJuZWVkQXV0aD8ifQogICAgQyAtLT58ZmFsc2V8IEhbIk5vIGF1dGggY2hlY2sKUHVibGljIGVuZHBvaW50Il0KICAgIEggLS0-IElbIkludm9rZSBtZXRob2QKY3R4LmdldFVzZXIoKSA9IG51bGwiXQogICAgQyAtLT58dHJ1ZXwgRHsiVG9rZW4gdmFsaWQ_In0KICAgIEQgLS0-fE5vfCBZWyI0MDE6IEF1dGhlbnRpY2F0aW9uIHJlcXVpcmVkIl0KICAgIEQgLS0-fFllc3wgRXsiYWxsb3dHdWVzdD8ifQogICAgRSAtLT58dHJ1ZXwgRlsiR3Vlc3QgT0sKY3R4LmdldFVzZXIoKSBtYXkgYmUgbnVsbCJdCiAgICBGIC0tPiBKWyJJbnZva2UgbWV0aG9kIl0KICAgIEUgLS0-fGZhbHNlfCBHeyJVc2VyIGV4aXN0cwppbiB0b2tlbj8ifQogICAgRyAtLT58Tm98IFhbIjQwMTogTWVtYmVyIHJlcXVpcmVkIl0KICAgIEcgLS0-fFllc3wgS1siVXNlciBHVUFSQU5URUVECmN0eC5nZXRVc2VyKCkgbmV2ZXIgbnVsbCJdCiAgICBLIC0tPiBMWyJJbnZva2UgbWV0aG9kIl0KCiAgICBzdHlsZSBIIGZpbGw6I2U4ZjVlOSxzdHJva2U6IzJlN2QzMixjb2xvcjojMDAwCiAgICBzdHlsZSBGIGZpbGw6I2ZmZjNlMCxzdHJva2U6I2U2NTEwMCxjb2xvcjojMDAwCiAgICBzdHlsZSBLIGZpbGw6I2UzZjJmZCxzdHJva2U6IzE1NjVjMCxjb2xvcjojMDAwCiAgICBzdHlsZSBaIGZpbGw6I2ZmZWJlZSxzdHJva2U6I2M2MjgyOCxjb2xvcjojMDAwCiAgICBzdHlsZSBZIGZpbGw6I2ZmZWJlZSxzdHJva2U6I2M2MjgyOCxjb2xvcjojMDAwCiAgICBzdHlsZSBYIGZpbGw6I2ZmZWJlZSxzdHJva2U6I2M2MjgyOCxjb2xvcjojMDAw" alt="인증 결정 트리" />

### 3가지 인증 레벨

**레벨 1: 인증 필수 (기본값)**
```java
@CommandHandler  // needAuth=true, allowGuest=false
public ResponseVO getProfile(CommandContext ctx) {
    // ctx.getUser()는 절대 null이 아님.
    // 프레임워크가 이 라인에 도달하기 전에 미인증 요청을 거부함.
    String uid = ctx.getUser().getUid();  // 항상 안전
}
```

**레벨 2: 게스트 허용**
```java
@CommandHandler(allowGuest = true)  // needAuth=true, allowGuest=true
public ResponseVO getProductDetail(CommandContext ctx) {
    // 게스트 토큰은 필수 (완전 공개 아님), 회원 로그인은 선택.
    // 게스트면 ctx.getUser()는 null, 로그인 회원이면 존재.
    
    JsonObject payload = ctx.getFirstCommandPayload();
    Product product = productService.getDetail(payload.get("productId").getAsString());
    
    // 기본 응답: 모든 사용자에게 제공 (게스트 + 회원)
    Map<String, Object> result = new HashMap<>();
    result.put("product", product);
    
    // payload에 uid가 있으면, DB 조회 후 개인화 데이터 append
    String uid = payload.has("uid") ? payload.get("uid").getAsString() : null;
    if (uid != null) {
        UserVO user = userService.getUserByUid(uid);
        if (user != null) {
            result.put("liked", likeService.isLiked(uid, product.getId()));
            result.put("recentView", viewService.getRecent(uid));
        }
    }
    
    return ResponseHelper.response(200, "Success", result);
}
```

패턴: **기본 응답은 모든 사용자에게 제공하고, payload에 uid가 포함되면 DB 조회 후 사용자별 데이터를 append.** 로그인 없이 탐색은 가능하면서, 알려진 사용자에게는 풍부한 경험을 제공합니다.

**레벨 3: 공개**
```java
@CommandHandler(needAuth = false)
public ResponseVO healthCheck(CommandContext ctx) {
    // 인증 없음. 누구나 호출 가능.
    // ctx.getUser()는 항상 null.
}
```

### 왜 중요한가

기존 Spring Security에서는 인증을 `SecurityConfig`에서 설정하고, `@PreAuthorize`를 추가하고, `SecurityContextHolder.getContext().getAuthentication()`이 기대한 값을 반환하기를 바랍니다. 인증 로직과 비즈니스 로직이 다른 파일에 있고, 규약과 신뢰로 연결됩니다.

CommandHandler에서는 메서드 위의 어노테이션이 **곧 인증 설정**입니다. 프레임워크가 강제합니다. 매개변수 없는 `@CommandHandler`를 보면, `ctx.getUser()`가 null이 아니라는 것을 **절대적으로 확신**할 수 있습니다. 이 확실성이 패턴을 AI 친화적으로 만듭니다: 어노테이션만으로 AI가 인증에 대해 알아야 할 모든 것을 알 수 있으며, 다른 파일을 읽을 필요가 없습니다.

---

## AI가 이 패턴을 좋아하는 이유

### 1. API 스펙 = 메서드 이름 (모호함 제로)

```
Command: "get-user-settings"  →  Method: getUserSettings()
Command: "update-nickname"    →  Method: updateNickname()
Command: "delete-account"     →  Method: deleteAccount()
```

AI가 클라이언트 코드에서 커맨드 이름을 보면, 즉시 어떤 메서드를 찾아야 하는지 알 수 있습니다. URL 패턴 매칭도, HTTP 메서드 추측도, 경로 변수 파싱도 필요 없습니다.

### 2. 어노테이션으로 사용자 보장 (null 체크 추측 불필요)

AI가 "여기서 사용자가 인증되어 있나?"를 궁금해할 필요가 없습니다. 어노테이션이 선언합니다. AI는 어노테이션을 읽고 그에 맞게 코드를 작성합니다. 보안 미들웨어를 추적할 필요가 없습니다.

### 3. 메서드 1개 = 완전한 비즈니스 로직 (자기 완결)

각 `@CommandHandler` 메서드가 해당 커맨드의 전체 스토리입니다. 읽기, 검증, 처리, 응답 -- 모두 하나의 메서드 안에. `getProfile()` 수정이 `updateProfile()`을 절대 깨뜨리지 않습니다. AI가 확신을 가지고 하나의 메서드를 수정합니다.

### 4. 어디서나 같은 구조 (컨벤션)

모든 프로젝트의 모든 CommandService가 같은 패턴을 따릅니다:
- `AbstractCommandService` 확장
- `@CommandHandler` 메서드 추가
- `CommandContext`로 입력 받기
- `ResponseVO` 반환

AI가 한 번 배우면 모든 모듈, 모든 프로젝트에 적용합니다.

### 5. 추적할 import 체인 없음 (컨텍스트는 CommandContext에)

메서드가 필요한 모든 것이 `CommandContext`로 전달됩니다: 페이로드, 사용자, 요청. AI가 주입된 서비스, 자동 와이어링된 리포지토리, 임포트된 유틸리티를 추적하며 어떤 데이터가 사용 가능한지 파악할 필요가 없습니다.

---

## 비교표

| | 기존 REST | CommandHandler |
|---|---|---|
| **엔드포인트 정의** | 메서드당 `@GetMapping`, `@PostMapping` | 모듈당 URL 하나, 커맨드 이름으로 디스패치 |
| **커맨드 라우팅** | URL 경로 세그먼트 + HTTP 메서드 | JSON `command` 필드 + 리플렉션 |
| **인증 선언** | `SecurityConfig` + `@PreAuthorize` (별도 파일) | 메서드 위 `@CommandHandler(needAuth, allowGuest)` |
| **사용자 사용 가능성** | `SecurityContext` 수동 체크, null 체크 필요 | 프레임워크가 보장 (또는 명시적 선택) |
| **AI 이해도** | 컨트롤러 + 서비스 + 보안 설정 읽어야 함 | 메서드 하나 읽으면 어노테이션이 모든 것을 알려줌 |
| **새 엔드포인트 추가** | 새 URL + HTTP 메서드 + 보안 규칙 + DTO | `@CommandHandler` 메서드 추가 |
| **응답 형식** | 다양함 (`ResponseEntity`, 커스텀 DTO, 예외) | 항상 `ResponseVO` (code, message, response) |
| **HTTP 상태** | 다양함 (200, 201, 400, 404, 500...) | 항상 200. 비즈니스 코드는 응답 바디 안에. |

---

## 실제 운영 규모

이 패턴은 이론이 아닙니다. [NSKit](https://nskit.io) 프레임워크 기반으로 [네올소프트](https://neoulsoft.com)의 10개 이상 프로덕션 프로젝트에서 사용 중입니다.

| 지표 | 값 |
|---|---|
| 이 패턴을 사용하는 프로덕션 프로젝트 | 10+ |
| 가장 큰 프로젝트 (서비스당 커맨드 수) | 15-20 메서드 |
| AI와 함께한 평균 커맨드 구현 시간 | 3-5분 |
| 일반적인 CommandService 파일 크기 | 200-500줄 |
| 도입 후 인증 관련 버그 | 거의 제로 (프레임워크가 보장) |

패턴은 모듈 추가(더 많은 CommandService 클래스)로 확장되지, 기존 모듈의 복잡도를 높이는 방식이 아닙니다. 각 서비스는 자신의 비즈니스 도메인에 집중합니다.

---

## 레퍼런스 코드

[`/reference`](./reference) 디렉토리에 각 컴포넌트의 간결한 구현이 있습니다:

| 파일 | 역할 |
|---|---|
| [`CommandHandler.java`](reference/CommandHandler.java) | `needAuth`와 `allowGuest`가 있는 어노테이션 정의 |
| [`AbstractCommandService.java`](reference/AbstractCommandService.java) | 핵심 디스패치 엔진: 리플렉션, kebab-to-camelCase, 메서드 레지스트리, 인증 적용 |
| [`CommandContext.java`](reference/CommandContext.java) | 요청 컨텍스트: 페이로드 접근, 사용자 접근, 사용자 보장 계약 |
| [`ResponseVO.java`](reference/ResponseVO.java) | 응답 봉투: code, message, response, responseAt |
| [`ResponseHelper.java`](reference/ResponseHelper.java) | 응답 생성 정적 팩토리 메서드 |
| [`ExampleCommandService.java`](reference/ExampleCommandService.java) | 3가지 인증 레벨을 보여주는 동작 예제 |

이들은 패턴을 설명하기 위한 레퍼런스 구현입니다. 제네릭 패키지(`com.example.api.command`)를 사용하며 프로젝트별 코드는 포함하지 않습니다.

---

## 링크

- [AI-Native Design](https://github.com/nskit-io/ai-native-design) -- 이 패턴의 근간인 디자인 철학
- [Growing Pool Cache](https://github.com/nskit-io/growing-pool-cache) -- AI 친화적 캐싱 패턴
- [CSW](https://github.com/nskit-io/csw) -- Claude Subscription Worker
- [NSKit](https://nskit.io) -- 이 원칙들로 만들어진 프레임워크

---

## 라이선스

[MIT](LICENSE)

---

<p align="center">
  <i>"최고의 API 문서는 코드 그 자체다.<br/>CommandHandler는 코드를 설계 단계에서 자기 문서화되게 만든다."</i>
</p>
