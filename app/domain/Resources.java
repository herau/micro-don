package domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * List of user's transactions.
 * @author Aurélien Leboulanger
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Resources {

    private List<Transaction> resources = new ArrayList<>();

    private Pagination pagination;

    public List<Transaction> getResources() {
        return resources;
    }

    public void setResources(List<Transaction> resources) {
        this.resources = resources;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
