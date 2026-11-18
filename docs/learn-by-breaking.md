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
