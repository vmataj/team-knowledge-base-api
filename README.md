# Team Knowledge Base API

A Spring Boot web app and API for sharing incident runbooks across a distributed support team.

## Run

Use Java 21 and Maven with `mvn spring-boot:run` (in-memory H2), or use `docker compose up --build` (persistent MySQL). Open http://localhost:8081.

As **editor**, open the seeded support runbook or create one using the form. Select an existing runbook to update or delete it. Create a workspace, then use **Workspace members** to grant or revoke another demo account's access. Switch accounts to verify what an editor or viewer can do.

## API

Base path: `/api/workspaces/{workspace}/runbooks`. Send HTTP Basic credentials. Demo passwords are all `demo-pass`.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | Base path | List workspace runbooks |
| GET | Base path + /{id} | Read a runbook |
| POST | Base path | Create with title and body |
| PUT | Base path + /{id} | Update title and body |
| DELETE | Base path + /{id} | Remove a runbook |

Workspace endpoints include `GET /api/workspaces`, `POST /api/workspaces` with a slug, and owner-only membership management under `/api/workspaces/{workspace}/members`. Workspace membership is persisted in the database. Queries scope each runbook ID to its workspace after checking membership. Accounts are fixed local fixtures for repeatable demos.

## Registered accounts

For the registered-user workflow, set `APP_PROFILE=private` in the shell before running
`docker compose up --build`. Create an account and a workspace. Share access with another registered username from the workspace owner panel.
Docker Compose supplies persistent MySQL and allows cookies over local HTTP.
The default profile uses the original local demo workflow.

For HTTPS hosting, activate the `private` Spring profile and supply
`SERVER_ADDRESS=0.0.0.0`, `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.
Use a dedicated database for this app and inject the password from the hosting
provider's secret store. The service honors `PORT` and requires HTTPS cookies by default.
The bundled Cloud SQL connector accepts a JDBC URL of the form
`jdbc:mysql:///DATABASE?cloudSqlInstance=PROJECT:REGION:INSTANCE&socketFactory=com.google.cloud.sql.mysql.SocketFactory&cloudSqlRefreshStrategy=lazy`.
For a first Cloud Run deployment, limit the service to one instance because login sessions are held in memory.

## Free hosting on Render and Neon

Create a Neon Free PostgreSQL project for this app. Connect this repository to
Render as a Blueprint and select the Free plan. The Blueprint activates the
registered-account profile and asks for these database settings:

- `SPRING_DATASOURCE_URL` is the Neon JDBC URL, such as `jdbc:postgresql://HOST/DATABASE?sslmode=require`.
- `SPRING_DATASOURCE_USERNAME` is the Neon database role.
- `SPRING_DATASOURCE_PASSWORD` is the role password.

Enter the database credentials in Render, where they are stored as environment
variables. Use a separate Neon project for each app. Local Docker Compose
continues to use MySQL. Login sessions reset when the free web service restarts.

## Tests

Run `mvn verify`. Integration tests exercise authentication, read-only access, cross-workspace isolation, and authorized creation. GitHub Actions runs the same Maven command.
