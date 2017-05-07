package domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

/**
 * pagination information for an endpoint
 * @author Aurélien Leboulanger
 */
public class Pagination {

    @JsonProperty("previous_uri")
    private URI previousUri;

    @JsonProperty("next_uri")
    private URI nextUri;

    public URI getPreviousUri() {
        return previousUri;
    }

    public void setPreviousUri(URI previousUri) {
        this.previousUri = previousUri;
    }

    public URI getNextUri() {
        return nextUri;
    }

    public void setNextUri(URI nextUri) {
        this.nextUri = nextUri;
    }
}
