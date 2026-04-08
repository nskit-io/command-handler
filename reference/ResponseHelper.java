package com.example.api.command;

/**
 * Factory methods for creating standardized API responses.
 *
 * <p>All command handlers return responses through this helper,
 * ensuring consistent response structure across the entire API.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Success with data
 * return ResponseHelper.response(200, "Success", userData);
 *
 * // Success with no data
 * return ResponseHelper.response(200, "Deleted", null);
 *
 * // Business error (still HTTP 200)
 * throw new HandledServiceException(404, "User not found");
 * }</pre>
 */
public class ResponseHelper {

    /**
     * Creates a successful response with data.
     *
     * @param code    business status code (200 for success)
     * @param message human-readable message
     * @param data    response payload (can be null)
     * @return ResponseVO ready to be serialized as JSON
     */
    public static ResponseVO response(int code, String message, Object data) {
        return new ResponseVO(code, message, data);
    }

    /**
     * Shorthand for a standard success response.
     *
     * @param data the response data
     * @return ResponseVO with code=200
     */
    public static ResponseVO success(Object data) {
        return new ResponseVO(200, "Success", data);
    }

    /**
     * Shorthand for a success response with a custom message.
     *
     * @param message success message
     * @param data    the response data
     * @return ResponseVO with code=200
     */
    public static ResponseVO success(String message, Object data) {
        return new ResponseVO(200, message, data);
    }
}
