package com.example.api.command;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core dispatch engine for the CommandHandler pattern.
 *
 * <p>This class provides the reflection-based routing that converts incoming
 * kebab-case command names to camelCase method names and invokes them.</p>
 *
 * <h3>How Dispatch Works</h3>
 * <ol>
 *   <li>Client sends: {@code {"command": "get-profile"}}</li>
 *   <li>kebab-to-camelCase: {@code "get-profile"} → {@code "getProfile"}</li>
 *   <li>Method registry lookup: find {@code getProfile()} in the service class</li>
 *   <li>Auth check: verify token based on {@code @CommandHandler} annotation</li>
 *   <li>Invoke: call the method with a populated {@code CommandContext}</li>
 * </ol>
 *
 * <h3>Method Registry</h3>
 * <p>On first use, the service scans itself for all {@code @CommandHandler} methods
 * and caches them in a concurrent map. Subsequent requests are a direct map lookup —
 * no repeated reflection scanning.</p>
 *
 * <h3>Extending</h3>
 * <p>Create a concrete CommandService by extending this class and adding
 * {@code @CommandHandler} methods. Each method name becomes an available command.</p>
 *
 * <pre>{@code
 * public class UserCommandService extends AbstractCommandService {
 *
 *     @CommandHandler
 *     public ResponseVO getProfile(CommandContext ctx) { ... }
 *
 *     @CommandHandler
 *     public ResponseVO updateProfile(CommandContext ctx) { ... }
 * }
 * }</pre>
 */
public abstract class AbstractCommandService {

    /** Cached method registry: commandName (camelCase) → Method */
    private final Map<String, Method> methodRegistry = new ConcurrentHashMap<>();

    /** Cached annotation registry: commandName → CommandHandler annotation */
    private final Map<String, CommandHandler> annotationRegistry = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;

    /**
     * Main entry point. Routes a command request to the appropriate handler method.
     *
     * @param command     the command name in kebab-case (e.g., "get-profile")
     * @param payload     the JSON payload from the client
     * @param user        the authenticated user (may be null for guest/public)
     * @param httpRequest the HTTP request
     * @return ResponseVO from the handler method
     */
    public ResponseVO handleRequest(String command, com.google.gson.JsonObject payload,
                                     CommandContext.UserVO user,
                                     javax.servlet.http.HttpServletRequest httpRequest)
            throws HandledServiceException {

        // Lazy-init method registry on first call
        if (!initialized) {
            initMethodRegistry();
        }

        // 1. Convert kebab-case → camelCase
        String methodName = toCamelCase(command);

        // 2. Look up method
        Method method = methodRegistry.get(methodName);
        if (method == null) {
            throw new HandledServiceException(404, "Unknown command: " + command);
        }

        // 3. Check auth requirements
        CommandHandler annotation = annotationRegistry.get(methodName);
        enforceAuth(annotation, user);

        // 4. Build context and invoke
        CommandContext ctx = new CommandContext(payload, user, httpRequest);
        try {
            return (ResponseVO) method.invoke(this, ctx);
        } catch (Exception e) {
            if (e.getCause() instanceof HandledServiceException) {
                throw (HandledServiceException) e.getCause();
            }
            throw new HandledServiceException(500, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Scans the concrete subclass for @CommandHandler methods and registers them.
     * Called once, thread-safe via volatile flag + ConcurrentHashMap.
     */
    private synchronized void initMethodRegistry() {
        if (initialized) return;

        for (Method method : this.getClass().getDeclaredMethods()) {
            CommandHandler annotation = method.getAnnotation(CommandHandler.class);
            if (annotation != null) {
                method.setAccessible(true);
                methodRegistry.put(method.getName(), method);
                annotationRegistry.put(method.getName(), annotation);
            }
        }
        initialized = true;
    }

    /**
     * Enforces the auth contract declared by @CommandHandler.
     *
     * <ul>
     *   <li>needAuth=true, allowGuest=false → user MUST exist</li>
     *   <li>needAuth=true, allowGuest=true  → token required, user may be guest (null)</li>
     *   <li>needAuth=false                  → no check</li>
     * </ul>
     */
    private void enforceAuth(CommandHandler annotation, CommandContext.UserVO user)
            throws HandledServiceException {

        if (!annotation.needAuth()) {
            return; // Public endpoint, no auth needed
        }

        // Auth is required — at minimum, a valid token must exist
        // (In production, token validation happens in a filter before this point.
        //  This check enforces the user-level guarantee.)

        if (!annotation.allowGuest() && user == null) {
            throw new HandledServiceException(401, "Authentication required");
        }
    }

    /**
     * Converts kebab-case command names to camelCase method names.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "get-profile"} → {@code "getProfile"}</li>
     *   <li>{@code "update-user-settings"} → {@code "updateUserSettings"}</li>
     *   <li>{@code "health-check"} → {@code "healthCheck"}</li>
     * </ul>
     *
     * @param kebab the kebab-case command name
     * @return camelCase method name
     */
    static String toCamelCase(String kebab) {
        if (kebab == null || kebab.isEmpty()) return kebab;

        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : kebab.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Business logic exception with a code and message.
     * Caught by the dispatch engine and converted to a ResponseVO.
     */
    public static class HandledServiceException extends Exception {
        private final int code;

        public HandledServiceException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() { return code; }
    }
}
