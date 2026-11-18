# ShortLink

A URL shortener REST API built with **Spring Boot 3, Spring Data JPA and MySQL**, with custom aliases, link expiry and click analytics.

## Features
- Shorten any URL (random 7-char Base62 code, collision-checked)
- Optional custom alias and expiry (`expiryDays`)
- `302` redirect that records each click (time, user agent, referrer)
- Stats per link: total clicks, clicks in the last 24 hours, last click
- Validation and a global exception handler (400 / 404 / 409 / 410)
- Swagger UI at `/swagger-ui.html`

## Tech stack
Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation, MySQL, springdoc-openapi

## Setup
1. Install Java 17+, Maven and MySQL.
2. Edit `src/main/resources/application.properties` with your MySQL username and password.
3. Run: `mvn spring-boot:run`
4. Open http://localhost:8080/swagger-ui.html

## API
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/shorten` | Create a short link |
| GET | `/{code}` | Redirect to the original URL |
| GET | `/api/urls/{code}/stats` | Click analytics |

### Example
```json
POST /api/shorten
{ "originalUrl": "https://www.example.com/some/very/long/path", "customAlias": "mylink", "expiryDays": 7 }
```
```json
201 Created
{ "shortCode": "mylink", "shortUrl": "http://localhost:8080/mylink", "originalUrl": "https://www.example.com/some/very/long/path", "expiresAt": "2026-11-25T10:30:00" }
```

## Design notes
- Constructor injection throughout.
- DTOs (records) keep entities out of the API.
- Click counter is incremented with an atomic `UPDATE ... SET clickCount = clickCount + 1`, safe under concurrent clicks.
- Expired links return `410 Gone`.
