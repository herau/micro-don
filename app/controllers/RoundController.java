package controllers;

import com.google.inject.Inject;
import domain.ErrorMessage;
import domain.Pagination;
import domain.Resources;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.http.HttpEntity;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.BankinService;
import services.BankinServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Aurélien Leboulanger
 */
public class RoundController extends Controller {

    private static final Logger logger = LoggerFactory.getLogger(RoundController.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-dd");

    private final Configuration configuration;

    private final BankinService bankinService;

    private final MessagesApi messagesApi;

    @Inject
    public RoundController(Configuration configuration, BankinService bankinService, MessagesApi messagesApi) {
        this.configuration = configuration;
        this.bankinService = bankinService;
        this.messagesApi = messagesApi;
    }

    /**
     * Return all users rounds transactions between two dates
     * @param since Limit to transactions created after the specified date.
     * @param until Limit to transactions created before the specified date.
     * @return
     */
    public Result rounds(String since, String until) {
        try {
            LocalDate.parse(since, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            ErrorMessage errorMessage = new ErrorMessage(BAD_REQUEST, "bad request", request().path(),
                                                         "wrong since date format");
            return badRequest(Json.toJson(errorMessage));
        }

        try {
            LocalDate.parse(until, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            ErrorMessage errorMessage = new ErrorMessage(BAD_REQUEST, "bad request", request().path(),
                                                         "wrong since date format");
            return badRequest(Json.toJson(errorMessage));
        }

        List<Map<String, Object>> users = configuration.getObjectList("users");

        List<CompletableFuture<Resources>> requests = users.stream().map(user -> bankinService
                .getRoundedTransactions((String) user.get("email"),
                                        ((String) user.get("password")), null, null, null, since, until)
                .toCompletableFuture()).collect(Collectors.toList());

        return requests.get(0).thenCombineAsync(requests.get(1), (resources, resources2) -> {
            resources.getResources().addAll(resources2.getResources());
            return resources;
        }).thenCombineAsync(requests.get(2), (resources, resources2) -> {
            resources.getResources().addAll(resources2.getResources());
            return resources;
        }).handle((resources, throwable) -> {
            if (throwable == null) {
                return ok(Json.toJson(resources));
            }

            Throwable cause = throwable.getCause();
            if (cause instanceof BankinServiceException) {
                BankinServiceException exception = (BankinServiceException) cause;
                logger.warn("Unable to fetch all rounded transactions (since={}, until={})", since, until, exception);
                return new Result(exception.getStatus(),
                                  HttpEntity.fromString(exception.getBody(), UTF_8.toString()))
                        .withHeader("content-type", exception.getContentType());
            }
            return internalServerError();
        }).join();
    }

    public CompletionStage<Result> round(String email, String password) {
        final String path = request().path();
        if (password.length() < 6 || password.length() > 255) {
            ErrorMessage errorMessage = new ErrorMessage(BAD_REQUEST, "bad request", path,
                                                         messagesApi.get(Lang.defaultLang(),
                                                                         "error.user-password-length"));
            return CompletableFuture.completedFuture(badRequest(Json.toJson(errorMessage)));
        }

        final String after = request().getQueryString("after");
        final String before = request().getQueryString("before");
        final String limit = request().getQueryString("limit");

        return bankinService.getRoundedTransactions(email, password, after, before, limit, null, null)
                            .thenApply(resources -> {
                                try {
                                    Pagination pagination = resources.getPagination();
                                    URI previousUri = pagination.getPreviousUri();
                                    if (previousUri != null) {
                                        pagination.setPreviousUri(new URIBuilder(previousUri).setPath(path).build());
                                    }

                                    URI nextUri = pagination.getNextUri();
                                    if (nextUri != null) {
                                        pagination.setNextUri(new URIBuilder(nextUri).setPath(path).build());
                                    }
                                    if (previousUri != null) {
                                        pagination.setPreviousUri(new URIBuilder(previousUri).setPath(path).build());
                                    }
                                } catch (URISyntaxException e) {
                                    logger.error("Unable to map Bankin pagination uri with current path", e);
                                }
                                return ok(Json.toJson(resources));
                            }).handle((result, throwable) -> {
                                if (throwable == null) {
                                    return result;
                                }

                                Throwable cause = throwable.getCause();
                                if (cause instanceof BankinServiceException) {
                                    BankinServiceException exception = (BankinServiceException) cause;
                                    logger.warn("Unable to fetch transactions for {}", email, exception);
                                    return new Result(exception.getStatus(),
                                                      HttpEntity.fromString(exception.getBody(), UTF_8.toString()))
                                            .withHeader("content-type", exception.getContentType());
                                }
                                return internalServerError();
                            });
    }
}
