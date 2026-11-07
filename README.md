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

## Verify

Run `mvn verify`. Integration tests exercise authentication, read-only access, cross-workspace isolation, and authorized creation. GitHub Actions runs the same Maven command.
