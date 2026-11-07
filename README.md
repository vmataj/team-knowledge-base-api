# Team Knowledge Base API

A Spring Boot API and browser demo for sharing incident runbooks across a distributed support team.

## Run

Use Java 21 and Maven with `mvn spring-boot:run` (in-memory H2), or use `docker compose up --build` (MySQL). Open http://localhost:8080.

Select **viewer**, read the seeded runbook, then try an edit to see HTTP 403. Switch to **editor** to verify the same operation succeeds. The **outsider** belongs to the billing workspace.

## API

Base path: `/api/workspaces/{workspace}/runbooks`. Send HTTP Basic credentials. Demo passwords are all `demo-pass`.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | Base path | List workspace runbooks |
| GET | Base path + /{id} | Read a runbook |
| POST | Base path | Create with title and body |
| PUT | Base path + /{id} | Update title and body |

Workspace roles are fixed demo fixtures. Queries scope the runbook ID to the workspace even after the caller passes the membership check. This app is intended for a local portfolio demo with fictional data.

## Verify

Run `mvn verify`. Integration tests exercise authentication, read-only access, cross-workspace isolation, and authorized creation. GitHub Actions runs the same Maven command.
