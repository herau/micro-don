package services;

import org.apache.http.entity.ContentType;

/**
 * Runtime Exception used when an error occurred with Bankin' API
 * @author Aurélien Leboulanger
 */
public class BankinServiceException extends RuntimeException {

    private final int status;

    private final String body;

    private final String contentType;

    public BankinServiceException(int status, String body, String contentType) {
        super("Bankin API - status " + status);
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    public BankinServiceException(int status, String body) {
        this(status, body, ContentType.APPLICATION_JSON.getMimeType());
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }
}
