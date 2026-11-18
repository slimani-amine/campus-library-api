# Campus Library API — Contract v1

This contract is shared by the Jakarta EE implementation (Lab 2) and the .NET rebuild.
Both must behave exactly the same. Base path: `/api`. All bodies are JSON.

## Endpoints

| Method | Path                      | Success                  | Errors        |
|--------|---------------------------|--------------------------|---------------|
| GET    | `/api/books`              | 200 list of Book         |               |
| GET    | `/api/books/{id}`         | 200 Book                 | 404           |
| POST   | `/api/books`              | 201 Book + `Location`    | 400           |
| PUT    | `/api/books/{id}`         | 200 Book                 | 400, 404      |
| DELETE | `/api/books/{id}`         | 204 (no body)            | 404           |
| GET    | `/api/members`            | 200 list of Member       |               |
| POST   | `/api/members`            | 201 Member + `Location`  | 400           |
| POST   | `/api/loans`              | 201 Loan + `Location`    | 400, 404, 409 |
| POST   | `/api/loans/{id}/return`  | 200 Loan                 | 404, 409      |

`Location` is the absolute URL of the new resource, for example
`http://localhost:8080/api/books/7`.

## Resources

```text
Book   { id, title, author, isbn, available }
Member { id, name, email }
Loan   { id, bookId, memberId, loanedAt, returnedAt }
```

| Field        | Type                 | Notes                                          |
|--------------|----------------------|------------------------------------------------|
| `id`         | integer              | Set by the server, starts at 1                 |
| `title`      | string               | Required, not blank, max 200 characters        |
| `author`     | string               | Required, not blank                            |
| `isbn`       | string               | Optional; if present, exactly 13 digits        |
| `available`  | boolean              | Set by the server; `false` while on loan       |
| `name`       | string               | Required, not blank                            |
| `email`      | string               | Required, not blank, valid email               |
| `bookId`     | integer              | Required                                       |
| `memberId`   | integer              | Required                                       |
| `loanedAt`   | string (ISO-8601 UTC)| Set by the server when the loan is created     |
| `returnedAt` | string or `null`     | `null` while the loan is active                |

Request bodies:

- `POST/PUT /api/books`: `{ "title", "author", "isbn" }`
- `POST /api/members`: `{ "name", "email" }`
- `POST /api/loans`: `{ "bookId", "memberId" }`
- `POST /api/loans/{id}/return`: no body

## Business rules

1. A book has at most one active loan. Borrowing a book that is not available → **409**.
2. A member has at most 3 active loans. A fourth borrow → **409**.
3. Borrowing with an unknown `bookId` or `memberId` → **404**.
4. Returning an unknown loan → **404**; returning a loan twice → **409**.
5. Borrowing marks the book `available: false`; returning marks it `available: true`.

## Errors (RFC 9457 problem details)

Every 4xx response has `Content-Type: application/problem+json` and this body:

```json
{
  "type": "https://tools.ietf.org/html/rfc9110#section-15.5.5",
  "title": "Not Found",
  "status": 404,
  "detail": "Book 42 does not exist"
}
```

| Status | `type`                                                 | `title`       |
|--------|--------------------------------------------------------|---------------|
| 400    | `https://tools.ietf.org/html/rfc9110#section-15.5.1`   | `Bad Request` |
| 404    | `https://tools.ietf.org/html/rfc9110#section-15.5.5`   | `Not Found`   |
| 409    | `https://tools.ietf.org/html/rfc9110#section-15.5.10`  | `Conflict`    |

`type`, `title` and `status` are always present. `detail` is a human-readable sentence.
Validation failures (400) also carry an `errors` list, one entry per invalid field:

```json
{
  "type": "https://tools.ietf.org/html/rfc9110#section-15.5.1",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

The `message` text comes from the validation framework and may differ between
implementations; the `field` names must match.
