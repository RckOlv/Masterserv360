## Quick context for AI code contributors

This is a Spring Boot / Java (Maven) monolith that implements a product/commerce backend.
Keep guidance short and concrete — reference project files when possible.

Key facts:
- Application entry: `src/main/java/com/masterserv/productos/ProductosApplication.java`.
- Security and auth: `src/main/java/com/masterserv/productos/config/SecurityConfig.java` uses JWT (see `jwt.secret` in `src/main/resources/application.properties`) and configures route-level rules and CORS (frontend origin: `http://localhost:4200`).
- Persistence: Spring Data JPA with PostgreSQL. Check `spring.datasource.*` and `spring.jpa.*` in `src/main/resources/application.properties`.

High-level architecture (what to know fast):
- Controllers live in `controller/` and accept DTOs from `dto/`.
- Business logic is in `service/` and uses `repository/` for persistence.
- Mapping between entities and DTOs is done in `mapper/` (follow existing mapper patterns).
- Complex filtering uses `specification/` and DTO filter objects (example: `ProductoFiltroDTO` + `ProductoController#filtrar`).

Project-specific conventions and patterns:
- Filter endpoints are POSTs that accept a filter DTO and a `Pageable` parameter (see `ProductoController#filterProductos`). Preserve this pattern when adding new search/filter endpoints.
- Soft deletes: controllers call service methods like `softDelete(id)` rather than deleting rows — search for `softDelete` usages to copy behavior.
- Security rules: `SecurityConfig` encodes route permissions (permitAll, hasRole, authenticated). When adding new endpoints, follow the same registration pattern and place public endpoints in the same/nearby controller.
- DTO validation uses `jakarta.validation` annotations. Keep using `@Valid` on controller request bodies.

Build / run / test (Windows PowerShell, repo root):
Use the Maven wrapper provided in the repo. From the workspace root run:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Notes:
- To run locally you'll need a PostgreSQL database matching `application.properties` or adapt the properties. `server.port=8080` by default.
- JWT secret and expiration are set in `application.properties` — tests and local runs expect those values unless overridden.

Integration points & external deps to watch for:
- Twilio/chatbot related DTOs/controllers (e.g., `TwilioRequestDTO`, `ChatbotController`) — check for external API keys or callbacks in `application.properties` or environment variables.
- Any third-party clients should be wired through services; prefer adding configuration properties to `application.properties` and retrieving them via `@Value` or a `@ConfigurationProperties` bean.

Editing guidance for AI agents (concrete):
1. Preserve package structure and bean names. New components should go under the appropriate package (controller/service/repository/dto/mapper/specification).
2. When changing security behavior, update `SecurityConfig` and run integration tests — route rules are explicit and can break access if omitted.
3. For new APIs that return lists, prefer `Page<DTO>` + `Pageable` where pagination is useful; follow existing controllers for naming and HTTP verbs.
4. When adding DB migrations or schema changes, prefer to document SQL or use the same approach as existing code (there's no Flyway/Liquibase by default).

Where to look first (examples to open):
- `src/main/java/com/masterserv/productos/config/SecurityConfig.java` — auth/CORS/roles
- `src/main/java/com/masterserv/productos/controller/ProductoController.java` — filter endpoint, pageable, role annotations
- `src/main/resources/application.properties` — DB, JWT, server.port
- `src/main/java/com/masterserv/productos/mapper/` — how DTO mapping is done

When unsure, prefer minimal, well-scoped changes and include unit tests in `src/test/java/...` mirroring the project's test style.

If any of this is unclear or you'd like me to add examples for newly added endpoints, tell me which controller/service you plan to edit and I will expand the instructions.
