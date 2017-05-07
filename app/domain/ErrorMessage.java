package domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Custom Error Response for HTTP requests
 * @author Aurélien Leboulanger
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorMessage {

    private long timestamp;
    private String exception;
    private final int status;
    private final String error;
    private final String path;
    private final String message;

    public ErrorMessage(int status, String error, String path, String message) {
        this.timestamp = System.currentTimeMillis();
        this.status = status;
        this.error = error;
        this.path = path;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }
}
