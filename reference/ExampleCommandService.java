package com.example.api.command;

import com.google.gson.JsonObject;

/**
 * Example CommandService demonstrating the pattern with different auth levels.
 *
 * <p>This is a reference implementation showing how to build a CommandService.
 * Each method handles one command. The method name IS the command spec:</p>
 * <ul>
 *   <li>{@code "get-profile"} → {@code getProfile()}</li>
 *   <li>{@code "update-profile"} → {@code updateProfile()}</li>
 *   <li>{@code "get-public-info"} → {@code getPublicInfo()}</li>
 *   <li>{@code "health-check"} → {@code healthCheck()}</li>
 * </ul>
 *
 * <h3>Adding a New Command</h3>
 * <p>To add a new API endpoint, just add a method with {@code @CommandHandler}.
 * No URL mapping. No route registration. No config file changes.</p>
 *
 * <pre>{@code
 * @CommandHandler
 * public ResponseVO myNewCommand(CommandContext ctx) {
 *     // That's it. "my-new-command" is now a valid API command.
 * }
 * }</pre>
 */
public class ExampleCommandService extends AbstractCommandService {

    // --------------------------------------------------
    // 1. Authenticated endpoint (default)
    //    needAuth=true, allowGuest=false
    //    → ctx.getUser() is GUARANTEED non-null
    // --------------------------------------------------

    /**
     * Retrieves the authenticated user's profile.
     *
     * <p>Command: {@code "get-profile"}</p>
     * <p>Auth: Required (member only). User is guaranteed.</p>
     *
     * <pre>{@code
     * // Client request:
     * POST /api/v1/user
     * { "command": "get-profile", "commandPayload": [{}] }
     * }</pre>
     */
    @CommandHandler
    public ResponseVO getProfile(CommandContext ctx) throws AbstractCommandService.HandledServiceException {
        // User is GUARANTEED non-null here. No null check needed.
        String uid = ctx.getUser().getUid();

        // In production: query database, build response
        JsonObject profile = new JsonObject();
        profile.addProperty("uid", uid);
        profile.addProperty("nickname", "ExampleUser");
        profile.addProperty("email", "user@example.com");

        return ResponseHelper.success(profile);
    }

    // --------------------------------------------------
    // 2. Authenticated endpoint with mutation
    //    Same auth level, different business logic
    // --------------------------------------------------

    /**
     * Updates the authenticated user's profile.
     *
     * <p>Command: {@code "update-profile"}</p>
     * <p>Auth: Required (member only). User is guaranteed.</p>
     *
     * <pre>{@code
     * // Client request:
     * POST /api/v1/user
     * { "command": "update-profile", "commandPayload": [{ "nickname": "NewName" }] }
     * }</pre>
     */
    @CommandHandler
    public ResponseVO updateProfile(CommandContext ctx) throws AbstractCommandService.HandledServiceException {
        String uid = ctx.getUser().getUid();

        JsonObject payload = ctx.getFirstCommandPayload();
        String nickname = payload.has("nickname") ? payload.get("nickname").getAsString() : null;

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new AbstractCommandService.HandledServiceException(400, "Nickname is required");
        }

        // In production: update database
        JsonObject result = new JsonObject();
        result.addProperty("uid", uid);
        result.addProperty("nickname", nickname);
        result.addProperty("updated", true);

        return ResponseHelper.success("Profile updated", result);
    }

    // --------------------------------------------------
    // 3. Guest-allowed endpoint
    //    needAuth=true, allowGuest=true
    //    → ctx.getUser() MAY be null
    // --------------------------------------------------

    /**
     * Returns public information. Accessible to both members and guests.
     *
     * <p>Command: {@code "get-public-info"}</p>
     * <p>Auth: Token required, but guests are allowed. User may be null.</p>
     *
     * <pre>{@code
     * // Client request (guest or member):
     * POST /api/v1/user
     * { "command": "get-public-info", "commandPayload": [{ "targetUid": "abc" }] }
     * }</pre>
     */
    @CommandHandler(allowGuest = true)
    public ResponseVO getPublicInfo(CommandContext ctx) throws AbstractCommandService.HandledServiceException {
        JsonObject payload = ctx.getFirstCommandPayload();

        JsonObject info = new JsonObject();
        info.addProperty("serviceName", "ExampleService");
        info.addProperty("version", "1.0.0");

        // Guest vs member: personalize if authenticated
        if (ctx.getUser() != null) {
            info.addProperty("greeting", "Welcome back, member!");
            info.addProperty("uid", ctx.getUser().getUid());
        } else {
            info.addProperty("greeting", "Welcome, guest!");
        }

        return ResponseHelper.success(info);
    }

    // --------------------------------------------------
    // 4. Public endpoint (no auth)
    //    needAuth=false
    //    → No authentication at all
    // --------------------------------------------------

    /**
     * Health check endpoint. No authentication required.
     *
     * <p>Command: {@code "health-check"}</p>
     * <p>Auth: None. Fully public.</p>
     *
     * <pre>{@code
     * // Client request (no token needed):
     * POST /api/v1/user
     * { "command": "health-check", "commandPayload": [{}] }
     * }</pre>
     */
    @CommandHandler(needAuth = false)
    public ResponseVO healthCheck(CommandContext ctx) {
        JsonObject status = new JsonObject();
        status.addProperty("status", "healthy");
        status.addProperty("timestamp", System.currentTimeMillis());

        return ResponseHelper.success(status);
    }
}
