package com.example.api.command;

import com.google.gson.JsonObject;
import javax.servlet.http.HttpServletRequest;

/**
 * Encapsulates everything a command handler needs to process a request.
 *
 * <p>CommandContext is the SINGLE parameter passed to every {@code @CommandHandler} method.
 * It replaces the need for multiple injected parameters (request, auth principal,
 * request body, etc.) with one unified object.
 *
 * <h3>The User Guarantee</h3>
 * <p>When a method is annotated with {@code @CommandHandler} (default: needAuth=true, allowGuest=false),
 * the framework guarantees that {@code getUser()} returns a valid, non-null UserVO.
 * This check happens BEFORE your method is called. You never need to null-check the user
 * in authenticated endpoints.</p>
 *
 * <h3>Payload Access</h3>
 * <pre>{@code
 * @CommandHandler
 * public ResponseVO updateProfile(CommandContext ctx) {
 *     JsonObject payload = ctx.getFirstCommandPayload();
 *     String nickname = payload.get("nickname").getAsString();
 *     String uid = ctx.getUser().getUid();  // guaranteed non-null
 *     ...
 * }
 * }</pre>
 */
public class CommandContext {

    private final JsonObject payload;
    private final UserVO user;
    private final HttpServletRequest httpRequest;

    public CommandContext(JsonObject payload, UserVO user, HttpServletRequest httpRequest) {
        this.payload = payload;
        this.user = user;
        this.httpRequest = httpRequest;
    }

    /**
     * Returns the first command payload from the request body.
     * This is the primary data object sent by the client.
     *
     * @return JsonObject containing the command's input data
     */
    public JsonObject getFirstCommandPayload() {
        if (payload != null && payload.has("commandPayload")) {
            return payload.getAsJsonArray("commandPayload").get(0).getAsJsonObject();
        }
        return new JsonObject();
    }

    /**
     * Returns the full payload (all command payloads).
     *
     * @return JsonObject containing the entire request payload
     */
    public JsonObject getPayload() {
        return payload;
    }

    /**
     * Returns the authenticated user.
     *
     * <p><b>Auth Guarantee:</b></p>
     * <ul>
     *   <li>{@code @CommandHandler} (default) → NEVER null</li>
     *   <li>{@code @CommandHandler(allowGuest = true)} → MAY be null</li>
     *   <li>{@code @CommandHandler(needAuth = false)} → always null</li>
     * </ul>
     *
     * @return the authenticated user, or null for guest/public endpoints
     */
    public UserVO getUser() {
        return user;
    }

    /**
     * Returns the underlying HTTP request.
     * Rarely needed — most data is available through payload and user.
     *
     * @return the HttpServletRequest
     */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Minimal user value object.
     * In production, this includes encrypted fields, roles, and tenant info.
     */
    public static class UserVO {
        private final String uid;
        private final String clientId;

        public UserVO(String uid, String clientId) {
            this.uid = uid;
            this.clientId = clientId;
        }

        /** Returns the user's unique identifier. */
        public String getUid() { return uid; }

        /** Returns the tenant/client identifier (multi-tenant isolation). */
        public String getClientId() { return clientId; }
    }
}
