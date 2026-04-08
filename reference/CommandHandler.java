package com.example.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a command handler in a CommandService.
 *
 * <p>The method name determines the command it handles.
 * Incoming kebab-case commands are converted to camelCase:
 * {@code "get-profile"} → {@code getProfile()}.
 *
 * <h3>Auth Guarantee</h3>
 * <ul>
 *   <li>{@code needAuth=true, allowGuest=false} (default):
 *       User is GUARANTEED non-null. {@code ctx.getUser()} is always valid.</li>
 *   <li>{@code needAuth=true, allowGuest=true}:
 *       Auth required but guests allowed. User MAY be null.</li>
 *   <li>{@code needAuth=false}:
 *       No authentication. Public endpoint.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @CommandHandler
 * public ResponseVO getProfile(CommandContext ctx) {
 *     // ctx.getUser() is NEVER null here
 *     String uid = ctx.getUser().getUid();
 *     ...
 * }
 *
 * @CommandHandler(allowGuest = true)
 * public ResponseVO getPublicInfo(CommandContext ctx) {
 *     // ctx.getUser() MAY be null
 *     if (ctx.getUser() != null) { ... }
 * }
 *
 * @CommandHandler(needAuth = false)
 * public ResponseVO healthCheck(CommandContext ctx) {
 *     // No auth at all. Public endpoint.
 *     return ResponseHelper.response(200, "OK", null);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandler {

    /**
     * Whether authentication is required.
     * When true, the framework validates the token BEFORE the method runs.
     * Default: true.
     */
    boolean needAuth() default true;

    /**
     * Whether guest (anonymous) tokens are allowed.
     * Only meaningful when {@code needAuth=true}.
     * When false, {@code ctx.getUser()} is guaranteed non-null.
     * Default: false.
     */
    boolean allowGuest() default false;
}
