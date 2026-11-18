# Lab 2 notes

## Opening questions

1. **Scopes.** The repositories and the services are `@ApplicationScoped`: one shared instance
   holds the data and the rules for everyone. The REST resources are `@RequestScoped`; they hold
   no state, so a fresh one per request is fine.
2. **Two students borrowing at once.** The repository is shared by all request threads. A plain
   `HashMap` can be corrupted by two threads writing at the same time; `ConcurrentHashMap` makes
   each single `get`/`put` safe, but it does not make "check the book is free, then save the loan"
   one atomic step, so both students can still get the same book.
3. **Same behavior in Lab 4.** One written contract (`docs/api-contract.pdf`) and black-box HTTP
   tests that only know a base URL (`-Dapi.base=...`), so the same 8 tests can run against the
   other implementation unchanged.

## Learn by breaking

The three real error excerpts, with what each means, are in `docs/learn-by-breaking.md`.

## Solo checkpoint A: CDI wiring of BookService → BookRepository

```text
BookResource            @Path("/books") @RequestScoped
   │  @Inject constructor(BookService)
   ▼
BookService             @ApplicationScoped @Logged
   │  @Inject public constructor(BookRepository)   + protected no-arg constructor
   ▼
BookRepository          interface (the injection point asks for this type)
   ▲ implements
InMemoryBookRepository  @ApplicationScoped  (ConcurrentHashMap + AtomicLong)
```

The container injects the repository into the service and the service into the resource;
nobody calls `new`.

Why the protected no-arg constructor exists: an `@ApplicationScoped` bean is handed out as a
client proxy, a generated subclass of `BookService` that forwards each call to the real instance.
To create that subclass the container needs a non-private constructor without parameters; the
protected one is only there for the proxy, and my code never calls it.

## Debugging checkpoint

What ran before my code on this request: WildFly's HTTP layer (Undertow) took the request,
RESTEasy matched `POST /api/loans` to `LoanResource`, read the JSON into `NewLoan` and validated
it, CDI's request-scoped resource called the `LoanService` proxy, and the proxy went through
`LoggedInterceptor` (`@Logged`) before `LoanService.borrow` itself started.

The interceptor line is visible in `server.log` for every borrow:
`-> LoanService.borrow`.

Practical note for the demo: the tests and the server run in two different JVMs. A breakpoint
in `LoanService.borrow` only stops if **WildFly** is started with the Debug button (the
`JBoss/WildFly` run configuration); then run `JUnit - contract` normally (or Debug it too).

## Solo checkpoint B

The two annotations that turn an empty title into a 400 with problem+json:

1. `@NotBlank` on the `title` component of the `NewBook` record (`domain` package: a format rule
   on the request body).
2. `@Valid` on the `NewBook` parameter of `BookResource.create` and `update` (`api` package: it
   tells the runtime to validate the body before the method runs).

The failed validation throws a `ConstraintViolationException`, and `ValidationMapper`
(`@Provider`, `api` package) turns it into the problem+json body with the `errors` list.

Why the black-box test still passes on another runtime: it only sends HTTP requests to a base
URL and checks status codes, headers and JSON, so it depends on the contract, not on WildFly,
RESTEasy or any class in the WAR.

## Test evidence

- Terminal: `docs/mvn-test-output.txt` (`Tests run: 8, Failures: 0`).
- IDE: JUnit run configuration `JUnit - contract` (All in package `edu.campus.library`, VM option
  `-Dapi.base=http://localhost:8080/library`). Screenshot of the green run: `docs/ide-test-run.png`
  (8 tests passed).

## What did CDI save me from writing?

Without CDI I would have to build the whole object graph by hand, in the right order, somewhere
at startup: create the three in-memory repositories, create a `Clock`, pass them into
`BookService`, `MemberService` and `LoanService`, and then make sure every REST resource gets
those exact same service instances, because the data lives inside the repositories and two
copies would mean two separate libraries. I would also need a place to keep those singletons
(a static registry or a hand-written factory), and I would have to make that wiring
thread-safe, since WildFly serves requests on many threads. With CDI each class just declares
what it needs in its `@Inject` constructor and how long it should live (`@ApplicationScoped`,
`@RequestScoped`); the container creates the objects, shares them, and checks the whole graph
when the app deploys, so a missing or ambiguous dependency fails at deploy time
(`WELD-001408`, `WELD-001409`) instead of as a `NullPointerException` in the middle of a request.

CDI also saved me from repeating cross-cutting code. The logging line `-> LoanService.borrow`
is written once in `LoggedInterceptor` and switched on with one annotation, `@Logged`, instead
of a log statement copied at the top of every service method. The `Clock` comes from one
`@Produces` method, so `LoanService` never calls `Instant.now()` itself and a test could swap in
a fixed clock without touching the service. Together with JAX-RS exception mappers (which the
container also finds and calls for me), the services can just throw `NotFoundException` or
`ConflictException`, and no resource method needs a `try/catch` to turn errors into
problem+json responses.

## Reflection

**1. Compare with your opening answers: which were right?**
The scope answer held: repositories and services ended up `@ApplicationScoped`, resources
`@RequestScoped`. The contract-plus-black-box-tests answer also held; the tests only know
`api.base`. The concurrency answer needed the extra half: `ConcurrentHashMap` keeps the map
itself from breaking, but the race in `borrow` is still there, because it is two separate steps
(check, then save), not one map operation.

**2. Where did you use constructor injection and why?**
Everywhere: `BookService`, `MemberService` and `LoanService` take their repositories (and the
`Clock`) in an `@Inject public` constructor, and the three resources take their service the same
way. It shows every dependency in one place, makes the class usable with `new` in a plain unit
test, and the container checks it at deploy time. The cost is the extra protected no-arg
constructor for the proxy, which is why the fields are `private` but not `final`.

**3. Which rules did you put in annotations, which in service code?**
Format rules, which only need the request body, are annotations on the records: `@NotBlank`,
`@Size(max = 200)`, `@Pattern(regexp = "\\d{13}")` for the ISBN, `@Email`, `@NotNull` for the
ids, checked with `@Valid` on the resource parameter. Data rules, which need the stored state,
are in the services: the book and member must exist (404), a book can have only one active loan,
a member at most 3, a loan cannot be returned twice (409).

**4. What would break with `@RequestScoped` on the in-memory repository?**
A new repository, with an empty map, would be created for every request and thrown away at the
end. A POST would return 201, but the next GET would return `[]` or 404, and nothing could be
borrowed. (Reasoned, not run.) In our "Learn by breaking" case 2 the class did not become
`@Dependent`: without `beans.xml` a class with no bean-defining annotation is not a bean at all,
so the deployment failed with `WELD-001408`. With an explicit `@Dependent`, each injection point
would get its own instance, so `BookService` and `LoanService` would see different maps: data
would survive between requests through `BookService`, but `LoanService` would answer 404 for a
book that `GET /api/books` shows.

**5. Where exactly is the race in `borrow`, and which pressure does it illustrate?**
In `LoanService.borrow`, between the checks (book available? member under 3 active loans?) and
the two saves: the checks are lines 42-47, the saves lines 53-54, and the gap between them is
marked with the `RACE` comment at line 49. Two requests can both pass the
checks before either saves, so a book is lent twice or a member gets a fourth loan. It
illustrates concurrency: many users share one server and one piece of state at the same time.

**6. What is the container doing that a main-style program would leave to you?**
It listens for HTTP, picks a thread, routes the request to the right method, turns JSON into
objects and validates them, creates and injects the objects my code needs, runs the interceptor
around my method, and turns my exceptions and return values back into HTTP responses.
