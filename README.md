# Campus Library API (Lab 2, Jakarta EE 11)

A small REST API for a campus library: books, members and loans. Everything is kept
in memory, so the data is gone when the server restarts.

- Java 21, Maven, packaged as a WAR
- Jakarta EE 11 (REST, CDI, Bean Validation, JSON-B), all provided by WildFly 41.0.1.Final
- No `beans.xml`, no `web.xml`, no XML config at all

```text
myFirstApp/                  (this folder)
├── docker-compose.yml
├── docs/
│   ├── api-contract.md      the contract (shared with the .NET lab)
│   ├── api-contract.pdf     the same contract as PDF (for the grader)
│   ├── mvn-test-output.txt  real output of the 8 tests
│   ├── ide-test-run.png     the 8 tests green in IntelliJ
│   ├── learn-by-breaking.md real CDI errors and what they mean
│   └── notes.md             opening questions, checkpoints, CDI note, reflection
└── jakarta/                 the Maven project
    ├── Dockerfile
    ├── requests.http        manual calls for IntelliJ's HTTP client
    └── src/
```

## The layers

All code is under `edu.campus.library`, in four packages:

| Package      | What it does                                                         |
|--------------|----------------------------------------------------------------------|
| `api`        | REST resources and exception mappers. Thin: read the request, call a service, build the response. |
| `service`    | Business rules (`BookService`, `MemberService`, `LoanService`), the `@Logged` interceptor and the `Clock` producer. |
| `repository` | Storage. One interface per type plus an in-memory implementation (`ConcurrentHashMap` + `AtomicLong` for ids). |
| `domain`     | Plain records: `Book`, `Member`, `Loan`, and the request bodies `NewBook`, `NewMember`, `NewLoan`. |

A request travels `api` → `service` → `repository`, and errors come back as exceptions
(`NotFoundException`, `ConflictException`) that the mappers in `api` turn into HTTP responses.
Two more mappers catch errors the REST runtime raises before our code runs (broken JSON,
unknown path, wrong method, wrong `Content-Type`), so every error has the same problem+json body.

## How CDI wires it together

Nobody calls `new` on a service or a repository. CDI (the container's dependency injection)
creates them and passes them in.

```text
BookResource   ──injects──▶ BookService   ──injects──▶ BookRepository   (InMemoryBookRepository)
MemberResource ──injects──▶ MemberService ──injects──▶ MemberRepository (InMemoryMemberRepository)
LoanResource   ──injects──▶ LoanService   ──injects──▶ BookRepository, MemberRepository,
                                                       LoanRepository, Clock (from ClockProducer)
```

- **Scopes.** Repositories and services are `@ApplicationScoped`: one shared instance for the
  whole app. That's why the data survives between requests. Resources are `@RequestScoped`.
- **Discovery.** Without `beans.xml`, CDI only picks up classes that carry a bean-defining
  annotation such as a scope. Forget the scope and the class is not a bean (see case 2 in
  `docs/learn-by-breaking.md`).
- **Interfaces.** Services ask for `BookRepository`, not `InMemoryBookRepository`. CDI finds the
  one bean that implements it. If there are two, deployment fails (case 1).
- **Constructor injection.** Each service has an `@Inject public` constructor that receives what
  it needs. This makes the dependencies obvious.
- **Why the extra `protected` no-arg constructor?** For an `@ApplicationScoped` bean, CDI does not
  hand out the real object. It hands out a *proxy*, a generated subclass that finds the real
  instance on each call. To create that subclass, CDI needs a constructor without parameters
  that is not private. That's the protected one; your code never calls it. Because it sets
  nothing, the fields cannot be `final`, so they are just `private`.
- **Interceptor.** `@Logged` on a service makes `LoggedInterceptor` run around every method call
  and log the method name (for example `-> LoanService.borrow`). `@Priority` turns it on without
  `beans.xml`.
- **Clock.** `ClockProducer` has a `@Produces Clock` method. `LoanService` asks for a `Clock`
  instead of calling `Instant.now()` directly, so a test could inject a fixed clock.

## The contract

Base path `/api`. Full details (fields, rules, error format) are in `docs/api-contract.md`.

| Method | Path                     | Success               | Errors        |
|--------|--------------------------|-----------------------|---------------|
| GET    | `/api/books`             | 200 list              |               |
| GET    | `/api/books/{id}`        | 200                   | 404           |
| POST   | `/api/books`             | 201 + `Location`      | 400           |
| PUT    | `/api/books/{id}`        | 200                   | 400, 404      |
| DELETE | `/api/books/{id}`        | 204                   | 404           |
| GET    | `/api/members`           | 200                   |               |
| POST   | `/api/members`           | 201                   | 400           |
| POST   | `/api/loans`             | 201                   | 400, 404, 409 |
| POST   | `/api/loans/{id}/return` | 200                   | 404, 409      |

Rules: a book can have only one active loan, and a member at most 3 (both give 409).
Every 4xx body is `application/problem+json` (RFC 9457) with `type`, `title`, `status`,
plus an `errors` list when validation fails.

## Run it from IntelliJ

1. Open this folder. If IntelliJ does not see the Maven project, right-click
   `jakarta/pom.xml` → *Add as Maven Project*.
2. Add a run configuration: *JBoss/WildFly Server → Local*, pointing at your WildFly 41 folder.
3. **Deployment** tab: **+** → *Artifact* → `library:war exploded`.
4. Run it. WildFly deploys the app under the context **`/library`**, so the API is at
   `http://localhost:8080/library/api/books`.

Why `/library` and not `/`? IntelliJ's WildFly run configuration has no "Application context"
field (its Tomcat one does). WildFly names the context after the deployment (`library.war`),
and changing it would need a `jboss-web.xml`, which this lab avoids. Docker (below) deploys the
WAR as `ROOT.war`, which WildFly serves at `/`.

Tip: in the run configuration's **Server** tab, set *Open browser* to
`http://localhost:8080/library/api/books`. The default `/library/` shows a 403, because the app
has no start page.

Use `jakarta/requests.http` to try the endpoints by hand.

## Run it from the terminal

With Docker (serves the API at `/api`):

```bash
docker compose up -d --build library-jakarta
cd jakarta
mvn test -Dapi.base=http://localhost:8080
```

With WildFly running from IntelliJ:

```bash
cd jakarta
mvn test -Dapi.base=http://localhost:8080/library
```

The Docker image builds the WAR with `-DskipTests`, because the tests need a running server.

## The 8 tests

`jakarta/src/test/java/edu/campus/library/LibraryApiTest.java` is a black-box test: it
only talks HTTP (`java.net.http.HttpClient`) to a running server, like any client would.
Each test creates its own books and members, so they can run in any order.

| Test | What it proves |
|------|----------------|
| `createValidBookReturns201AndLocation` | A valid POST creates a book: 201, a `Location` header ending in `/api/books/{id}`, and that URL really returns the book. |
| `blankTitleReturns400WithProblemDetails` | `@Valid` + `@NotBlank` work: a blank title gives 400, `application/problem+json`, and an `errors` entry for `title`. |
| `unknownBookIdReturns404` | An unknown id gives 404 as problem+json (`NotFoundMapper`). |
| `deleteBookReturns204ThenBookIsGone` | DELETE gives 204, and a GET afterwards gives 404. |
| `borrowAvailableBookReturns201AndBookBecomesUnavailable` | Borrowing works (201, right `bookId`), and the book is now `available: false`. |
| `borrowSameBookTwiceReturns409` | Rule 1: a book that is already on loan cannot be borrowed again (409). |
| `fourthActiveLoanForMemberReturns409` | Rule 2: three loans are fine, the fourth gives 409. |
| `returnLoanReturns200AndBookIsAvailableAgain` | Returning gives 200 with a `returnedAt` time, and the book is available again. |

## The race in `borrow`

Look at `LoanService.borrow` (`jakarta/src/main/java/edu/campus/library/service/LoanService.java`,
around line 50). It first **checks** (is the book available? does the member have fewer than 3
loans?) and then **acts** (saves the book as unavailable, saves the loan). Nothing stops two
requests from running at the same time: both can pass the checks before either one saves,
so one book gets two loans, or a member gets a fourth. This is a *check-then-act* race. It is
left in on purpose; Lab 3 fixes it with transactions.
