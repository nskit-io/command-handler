[🇬🇧 English](README.md) · [🇰🇷 한국어](README.ko.md) · [🇨🇳 中文](README.zh.md)

# CommandHandler

**API メソッド名がそのまま API 仕様だったら?**

> 1モジュール = 1サービスクラス。1コマンド = 1メソッド。メソッド名がすなわち API 仕様。

> [**NSKit**](https://github.com/nskit-io/nskit-io) プロダクション・サービスで使用中 — *型にはまっているから、無限に組み合わせられる*。運用中の NSKit サービス(NewMyoung、Pellow、Byeol、SING:ON、BigFoot など)はすべてこのパターンで動いています。

---

## 概要

CommandHandler は Spring Boot バックエンドのための **AI-Native デザインパターン** です。API エンドポイントを、人間の開発者と AI の両方が簡単に理解・ナビゲート・修正できるよう構造化します。ライブラリではなくデザインパターンで、参照実装を含みます。

---

## 伝統的 REST Controller の問題

```java
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    @GetMapping("/profile/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid) { ... }

    @PutMapping("/profile/{uid}")
    public ResponseEntity<?> updateProfile(@PathVariable String uid, @RequestBody ...) { ... }

    // ... 50 個のエンドポイント、50 個の URL マッピング
}
```

AI が主要な協業者になると問題が顕在化:

- **50 エンドポイント = 50 URL マッピング** を維持。それぞれ異なる HTTP メソッド、パス変数、リクエスト形。
- **認証が散らばる**:Spring Security 設定、`@PreAuthorize`、インターセプター、手動 `SecurityContext` チェックなど。
- **AI は複数ファイルを追跡** しないと1つのエンドポイントを理解できない。
- **新エンドポイント追加** = 新 URL + HTTP メソッド選定 + セキュリティルール + DTO + サービス配線 — 1機能で複数ファイル修正。

---

## CommandHandler の解

```java
public class UserCommandService extends AbstractCommandService {

    @CommandHandler  // needAuth=true, allowGuest=false
    public ResponseVO getProfile(CommandContext ctx) throws HandledServiceException {
        String uid = ctx.getUser().getUid();  // 本人の識別はペイロードではなく認証トークンから
        // ...
        return ResponseHelper.response(200, "Success", profile);
    }

    @CommandHandler
    public ResponseVO updateProfile(CommandContext ctx) throws HandledServiceException {
        // ...
    }
}
```

URL は1本のみ: `POST /api/v1/user`。ペイロードの `command` フィールドでどのメソッドを呼ぶか決めます(`kebab-case` → `camelCase` 自動変換)。

**AI は1ファイルだけ読めばそのモジュール全体を理解できる**。

---

## リクエストフロー

```
Frontend (nskit-api.js)
  └→ POST /api/v1/{module}
         payload: { command: "get-profile", commandPayload: [{...}] }
              └→ ServiceApiController
                    └→ AbstractCommandService.handleRequest()
                          ├→ 認証(NSkitAuthorizationFilter)
                          ├→ kebab→camel 変換
                          └→ reflection で UserCommandService.getProfile(ctx) 呼び出し
```

認証(ログイン有無)はフレームワークが保証し、`@CommandHandler` の宣言どおりにメソッド実行前に強制されます。認可(本人のデータか・管理者か)はコマンド内で明示的に判定します。`/admin/` のようなパス接頭辞だけでは何も守られません。

---

## なぜ AI はこのパターンを好むのか

| 側面 | 従来 REST | CommandHandler |
|---|---|---|
| エンドポイント追加 | 複数ファイル修正(URL・セキュリティ設定・DTO・サービス) | 1 ファイルに1メソッド追加 |
| 認証保証 | バラバラ | アノテーションで統一(認可はコマンド内で明示) |
| 理解に必要な読み込み | コントローラ + サービス + セキュリティ + DTO | 1つの Service クラス |
| リファクタ影響 | 影響範囲不明 | 1 クラス内で完結 |

---

## 参照コード

完全な技術内容、認証保証の詳細、ペイロード例、運用規模、参照実装は英語版を参照: **[README (English)](README.md)**

---

<div align="center">

**CommandHandler** · Part of the **[NSKit](https://github.com/nskit-io/nskit-io)** ecosystem

© 2026 Neoulsoft Inc.

</div>

---

## ライセンス

> 本リポジトリは設計パターンとリファレンスを MIT ライセンスで公開するものです。NSKit フレームワーク本体（ランタイム・スキャフォールディング・チェッカー・運用標準）は Neoulsoft Inc. の非公開資産であり、別途商用ライセンスで提供されます。
