package com.example.api.command;

/**
 * Standard response envelope for all API responses.
 *
 * <p>Every API response — success or error — uses this same structure.
 * HTTP status is ALWAYS 200. Business-level status is in the {@code code} field.
 *
 * <h3>Why Always HTTP 200?</h3>
 * <p>HTTP status codes are transport-level concerns. Business logic errors
 * (user not found, invalid input, insufficient permissions) are not transport errors.
 * By always returning HTTP 200, we separate transport from business logic cleanly.
 * Clients check {@code response.code}, not HTTP status.</p>
 *
 * <h3>Response Structure</h3>
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "Success",
 *   "response": { ... },      // business data (null on error)
 *   "responseAt": 1712345678  // Unix timestamp
 * }
 * }</pre>
 */
public class ResponseVO {

    private int code;
    private String message;
    private Object response;
    private long responseAt;

    public ResponseVO() {
        this.responseAt = System.currentTimeMillis() / 1000;
    }

    public ResponseVO(int code, String message, Object response) {
        this.code = code;
        this.message = message;
        this.response = response;
        this.responseAt = System.currentTimeMillis() / 1000;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getResponse() { return response; }
    public void setResponse(Object response) { this.response = response; }

    public long getResponseAt() { return responseAt; }
    public void setResponseAt(long responseAt) { this.responseAt = responseAt; }
}
