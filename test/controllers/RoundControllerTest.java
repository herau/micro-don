package controllers;

import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import static org.junit.Assert.assertEquals;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.GET;
import static play.test.Helpers.route;

public class RoundControllerTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    @Test
    public void round_withoutCredentials_badRequest() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/round");

        Result result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void round_withWrongPassword_baqRequest() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/round?email=user2@mail.com&password=only5");

        Result result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void round_withWrongCredentials_unauthorized() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/round?email=user2@mail.com&password=wrongPassword");

        Result result = route(app, request);
        assertEquals(UNAUTHORIZED, result.status());
    }

    @Test
    public void round_withCredentials_ok() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/round?email=user2@mail.com&password=a!Strongp%23assword2");

        Result result = route(app, request);
        assertEquals(OK, result.status());
    }

    @Test
    public void rounds_withoutParameters_badRequest() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/rounds");

        Result result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void rounds_withoutWrongParameters_badRequest() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/rounds?since=yesterday&until=today");

        Result result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void rounds_withParameters_ok() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/rounds?since=2015-01-01&until=2017-01-01");

        Result result = route(app, request);
        assertEquals(OK, result.status());
    }
}
