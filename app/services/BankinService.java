package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import domain.Resources;
import domain.Transaction;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Bankin API Facade
 * @author Aurélien Leboulanger
 */
public class BankinService {

    private static final Logger logger = LoggerFactory.getLogger(BankinService.class);

    private final WSClient wsClient;

    /**
     * Base url of the Bankin' API service
     */
    private final String apiUrl;

    /**
     * required headers for each request to Bankin' service
     */
    private final List<Map<String, Object>> headers;

    /**
     * required query parameters for each request to Bankin' service
     */
    private final List<Map<String, Object>> parameters;

    @Inject
    public BankinService(WSClient wsClient, Configuration configuration) {
        this.wsClient = wsClient;
        this.apiUrl = configuration.getString("bankin.api_url");
        this.headers = configuration.getObjectList("bankin.headers");
        this.parameters = configuration.getObjectList("bankin.query-param");
    }

    /**
     * Returns the amount of the rounded transaction of the current accounts of a given user.
     * @param email User's email.
     * @param password User's password.
     * @param after Cursor pointing to the start of the desired set.
     * @param before Cursor pointing to the end of the desired set.
     * @param limit Number of records to return. Accepted values: 1 - 500.
     * @param since Limit to transactions created after the specified date.
     * @param until Limit to transactions created before the specified date.
     * @return {@link CompletionStage} of {@link Resources}
     */
    public CompletionStage<Resources> getRoundedTransactions(String email, String password, String after, String before, String limit, String since, String until) {
        return authenticatedRequest("/transactions", email, password).thenCompose(request -> {
            WSRequest transactionRequest = request.setQueryParameter("email", email)
                                                  .setQueryParameter("password", password);

            if (isNotEmpty(after)) {
                transactionRequest.setQueryParameter("after", after);
            }
            if (isNotEmpty(before)) {
                transactionRequest.setQueryParameter("before", before);
            }

            if (isNotEmpty(limit)) {
                transactionRequest.setQueryParameter("limit", limit);
            }

            if (isNotEmpty(since)) {
                transactionRequest.setQueryParameter("since", since);
            }

            if (isNotEmpty(until)) {
                transactionRequest.setQueryParameter("until", until);
            }

            return transactionRequest.get().thenApply(response -> {
                if (response.getStatus() != 200) {
                    logger.info("Bankin API response [{}]", response.getStatus());
                    throw new BankinServiceException(response.getStatus(), response.getBody(), response.getHeader("content-type"));
                }

                try {
                    Resources resources = Json.mapper().readValue(response.getBodyAsStream(), Resources.class);
                    resources.setResources(resources.getResources().stream()
                                                    .filter(transaction -> transaction.getAmount() < 0)
                                                    .map(round())
                                                    .collect(Collectors.toList()));
                    return resources;
                } catch (IOException e) {
                    logger.error("Unable to parse Bankin API JSON response", e);
                    throw new BankinServiceException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "JSON Parsing error");
                }
            });
        });
    }

    /**
     * Authenticate a user, with its email and password, for a request to a specific path of the Bankin' API.
     * @param path URI path of the Bankin' API.
     * @param email User's email address.
     * @param password User's password.
     * @return a CompletionStage to compute in order to execute the request to the specific path
     */
    public CompletionStage<WSRequest> authenticatedRequest(String path, String email, String password) {
        WSRequest authRequest = wsClient.url(apiUrl + "/authenticate");

        setupRequirements(authRequest);

        authRequest.setQueryParameter("email", email)
                   .setQueryParameter("password", password);

        return authRequest.execute("POST").thenApply(response -> {
            if (response.getStatus() != 200) {
                logger.info("Bankin authentication failed [{}] with credentials [email={}, password={}]", response.getStatus(), email, password);
                throw new BankinServiceException(response.getStatus(), response.getBody(), response.getHeader("content-type"));
            }
            JsonNode json = response.asJson();
            String accessToken = json.findPath("access_token").asText();

            WSRequest authenticatedRequest = wsClient.url(apiUrl + path);
            setupRequirements(authenticatedRequest);
            authenticatedRequest.setHeader("Authorization", "Bearer " + accessToken);
            return authenticatedRequest;
        });
    }

    private Function<Transaction, Transaction> round() {
        return transaction -> {
            double amount = 10 * (Math.ceil(Math.abs(transaction.getAmount() / 10)));
            transaction.setAmount(-amount);
            return transaction;
        };
    }

    /**
     * To interact with the Bankin' API, we must set some headers and query parameters for every request
     *
     * @param request request to Bankin' API
     */
    private void setupRequirements(WSRequest request) {
        headers.forEach(map -> map.forEach((key, value) -> request.setHeader(key, String.valueOf(value))));
        parameters.forEach(map -> map.forEach((key, value) -> request.setQueryParameter(key, String.valueOf(value))));
    }

}
