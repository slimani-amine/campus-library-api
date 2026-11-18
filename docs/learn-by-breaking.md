# Learn by breaking

Each case: make one change, redeploy on WildFly 41.0.1.Final (started from IntelliJ),
copy the real error from `standalone/log/server.log`, then undo the change.
Stack-trace frames are cut where you see `[...]`; every other line is copied as printed.

## 1. A second `BookRepository` with no qualifier

Change: added `repository/SecondBookRepository.java`, `@ApplicationScoped`, implementing
`BookRepository`, with no qualifier. (Without `beans.xml` CDI only discovers classes with a
bean-defining annotation, so the scope annotation is needed for it to become a bean at all.)

Result: the deployment fails and is rolled back.

```text
2026-11-18 21:00:06,722 ERROR [org.jboss.msc.service.fail] (MSC service thread 1-7) MSC000001: Failed to start service jboss.deployment.unit."library.war".WeldStartService: org.jboss.msc.service.StartException in service jboss.deployment.unit."library.war".WeldStartService: Failed to start service
[...]
Caused by: org.jboss.weld.exceptions.DeploymentException: Exception List with 2 exceptions:
Exception 0 :
org.jboss.weld.exceptions.DeploymentException: WELD-001409: Ambiguous dependencies for type BookRepository with qualifiers @Default
  at injection point [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] @Inject public edu.campus.library.service.BookService(BookRepository)
  at edu.campus.library.service.BookService.<init>(BookService.java:0)
  Possible dependencies: 
  - Managed Bean [class edu.campus.library.repository.InMemoryBookRepository] with qualifiers [@Any @Default],
  - Managed Bean [class edu.campus.library.repository.SecondBookRepository] with qualifiers [@Any @Default]
[...]
Exception 1 :
org.jboss.weld.exceptions.DeploymentException: WELD-001409: Ambiguous dependencies for type BookRepository with qualifiers @Default
  at injection point [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] @Inject public edu.campus.library.service.LoanService(BookRepository, MemberRepository, LoanRepository, Clock)
[...]
2026-11-18 21:00:06,726 ERROR [org.jboss.as.server] (management-handler-thread - 2) WFLYSRV0021: Deploy of deployment "library.war" was rolled back with the following failure message: 
```

Meaning: two beans match `BookRepository` with the `@Default` qualifier, so CDI cannot choose
which one to inject into `BookService` and `LoanService`, and it refuses to start the application.

Undo: deleted `SecondBookRepository.java`.

## 2. No scope annotation on `InMemoryBookRepository`

Change: commented out `@ApplicationScoped` on `InMemoryBookRepository`.

Result: the deployment fails and is rolled back.

```text
2026-11-18 21:02:46,930 ERROR [org.jboss.msc.service.fail] (MSC service thread 1-4) MSC000001: Failed to start service jboss.deployment.unit."library.war".WeldStartService: org.jboss.msc.service.StartException in service jboss.deployment.unit."library.war".WeldStartService: Failed to start service
[...]
Caused by: org.jboss.weld.exceptions.DeploymentException: Exception List with 2 exceptions:
Exception 0 :
org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type BookRepository with qualifiers @Default
  at injection point [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] @Inject public edu.campus.library.service.LoanService(BookRepository, MemberRepository, LoanRepository, Clock)
  at edu.campus.library.service.LoanService.<init>(LoanService.java:0)
[...]
Exception 1 :
org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type BookRepository with qualifiers @Default
  at injection point [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] @Inject public edu.campus.library.service.BookService(BookRepository)
  at edu.campus.library.service.BookService.<init>(BookService.java:0)
[...]
2026-11-18 21:02:46,935 ERROR [org.jboss.as.server] (management-handler-thread - 2) WFLYSRV0021: Deploy of deployment "library.war" was rolled back with the following failure message: 
```

The two POSTs and the GET, sent after the failed deploy:

```text
POST /library/api/books {"title":"Book 1","author":"A"}  ->  HTTP/1.1 404 Not Found
POST /library/api/books {"title":"Book 2","author":"A"}  ->  HTTP/1.1 404 Not Found
GET  /library/api/books                                  ->  HTTP/1.1 404 Not Found
body of all three: <html><head><title>Error</title></head><body>404 - Not Found</body></html>
```

Meaning: with no `beans.xml`, CDI only discovers classes that carry a bean-defining annotation
(like a scope), so without `@ApplicationScoped` the class is no bean at all, nothing satisfies
`BookRepository`, the app is never deployed, and the GET returns WildFly's plain 404 page, not a list.

Undo: restored `@ApplicationScoped` on `InMemoryBookRepository`.

## 3. Private `@Inject` constructor in `BookService`

Change: made `@Inject public BookService(BookRepository books)` private
(the protected no-arg constructor was left as it is).

Result: the deployment fails and is rolled back.

```text
2026-11-18 21:03:51,649 ERROR [org.jboss.msc.service.fail] (MSC service thread 1-5) MSC000001: Failed to start service jboss.deployment.unit."library.war".WeldStartService: org.jboss.msc.service.StartException in service jboss.deployment.unit."library.war".WeldStartService: Failed to start service
[...]
Caused by: org.jboss.weld.exceptions.DeploymentException: WELD-001436: Type edu.campus.library.service.BookService is not proxyable because it has a private constructor [EnhancedAnnotatedConstructorImpl] @Inject private edu.campus.library.service.BookService(BookRepository) - class edu.campus.library.service.BookService.
[...]
2026-11-18 21:03:51,652 ERROR [org.jboss.as.server] (management-handler-thread - 2) WFLYSRV0021: Deploy of deployment "library.war" was rolled back with the following failure message: 
```

Meaning: an `@ApplicationScoped` bean is reached through a proxy subclass, and Weld refuses to
build that proxy when the bean's `@Inject` constructor is private, even though the protected
no-arg constructor is still there.

Undo: made the `@Inject` constructor public again.
