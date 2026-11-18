package edu.campus.library;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Needs a running server: mvn test -Dapi.base=http://localhost:8080/library (IntelliJ)
// or -Dapi.base=http://localhost:8080 (Docker).
class LibraryApiTest {

    private static final String BASE = System.getProperty("api.base", "http://localhost:8080");
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Test
    void createValidBookReturns201AndLocation() throws Exception {
        HttpResponse<String> response = post("/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"isbn\":\"9780441172719\"}");

        assertEquals(201, response.statusCode());
        long id = idOf(response.body());
        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location.endsWith("/api/books/" + id), "Location was: " + location);
        assertEquals(200, get(location.substring(location.indexOf("/api/"))).statusCode());
    }

    @Test
    void blankTitleReturns400WithProblemDetails() throws Exception {
        HttpResponse<String> response = post("/api/books",
                "{\"title\":\"  \",\"author\":\"Someone\",\"isbn\":\"9780441172719\"}");

        assertEquals(400, response.statusCode());
        assertProblemJson(response, 400);
        assertTrue(response.body().matches("(?s).*\"errors\"\\s*:\\s*\\[.*\"field\"\\s*:\\s*\"title\".*"),
                "Body was: " + response.body());
    }

    @Test
    void unknownBookIdReturns404() throws Exception {
        HttpResponse<String> response = get("/api/books/999999999");

        assertEquals(404, response.statusCode());
        assertProblemJson(response, 404);
    }

    @Test
    void deleteBookReturns204ThenBookIsGone() throws Exception {
        long bookId = createBook("To Delete");

        HttpResponse<String> response = send(request("/api/books/" + bookId).DELETE().build());

        assertEquals(204, response.statusCode());
        assertEquals(404, get("/api/books/" + bookId).statusCode());
    }

    @Test
    void borrowAvailableBookReturns201AndBookBecomesUnavailable() throws Exception {
        long bookId = createBook("Borrow Me");
        long memberId = createMember();

        HttpResponse<String> response = borrow(bookId, memberId);

        assertEquals(201, response.statusCode());
        assertTrue(response.body().matches("(?s).*\"bookId\"\\s*:\\s*" + bookId + "\\b.*"), response.body());
        assertTrue(get("/api/books/" + bookId).body().matches("(?s).*\"available\"\\s*:\\s*false.*"));
    }

    @Test
    void borrowSameBookTwiceReturns409() throws Exception {
        long bookId = createBook("Popular Book");
        assertEquals(201, borrow(bookId, createMember()).statusCode());

        HttpResponse<String> response = borrow(bookId, createMember());

        assertEquals(409, response.statusCode());
        assertProblemJson(response, 409);
    }

    @Test
    void fourthActiveLoanForMemberReturns409() throws Exception {
        long memberId = createMember();
        for (int i = 1; i <= 3; i++) {
            assertEquals(201, borrow(createBook("Book " + i), memberId).statusCode());
        }

        HttpResponse<String> response = borrow(createBook("Book 4"), memberId);

        assertEquals(409, response.statusCode());
        assertProblemJson(response, 409);
    }

    @Test
    void returnLoanReturns200AndBookIsAvailableAgain() throws Exception {
        long bookId = createBook("Return Me");
        long loanId = idOf(borrow(bookId, createMember()).body());

        HttpResponse<String> response = post("/api/loans/" + loanId + "/return", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().matches("(?s).*\"returnedAt\"\\s*:\\s*\"[^\"]+\".*"), response.body());
        assertTrue(get("/api/books/" + bookId).body().matches("(?s).*\"available\"\\s*:\\s*true.*"));
    }

    private static long createBook(String title) throws Exception {
        HttpResponse<String> response = post("/api/books",
                "{\"title\":\"" + title + "\",\"author\":\"Test Author\",\"isbn\":\"9780000000000\"}");
        assertEquals(201, response.statusCode(), response.body());
        return idOf(response.body());
    }

    private static long createMember() throws Exception {
        HttpResponse<String> response = post("/api/members",
                "{\"name\":\"Test Member\",\"email\":\"member" + System.nanoTime() + "@campus.edu\"}");
        assertEquals(201, response.statusCode(), response.body());
        return idOf(response.body());
    }

    private static HttpResponse<String> borrow(long bookId, long memberId) throws Exception {
        return post("/api/loans", "{\"bookId\":" + bookId + ",\"memberId\":" + memberId + "}");
    }

    private static void assertProblemJson(HttpResponse<String> response, int status) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.startsWith("application/problem+json"), "Content-Type was: " + contentType);
        String body = response.body();
        assertTrue(body.matches("(?s).*\"type\"\\s*:\\s*\"[^\"]+\".*"), "Body was: " + body);
        assertTrue(body.matches("(?s).*\"title\"\\s*:\\s*\"[^\"]+\".*"), "Body was: " + body);
        assertTrue(body.matches("(?s).*\"status\"\\s*:\\s*" + status + "\\b.*"), "Body was: " + body);
    }

    private static long idOf(String json) {
        Matcher matcher = ID.matcher(json);
        assertTrue(matcher.find(), "No id in: " + json);
        return Long.parseLong(matcher.group(1));
    }

    private static HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(BASE + path)).header("Accept", "application/json");
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return send(request(path).GET().build());
    }

    private static HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest.Builder builder = request(path);
        if (json == null) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
        }
        return send(builder.build());
    }

    private static HttpResponse<String> send(HttpRequest request) throws Exception {
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
