# Daymark

A task planner for work, study, or home. Organize tasks by category, reorder them, and track completed focus sessions. The daily plan picks up to three open tasks by due date.

Run `docker compose up --build` and visit [localhost:8081](http://localhost:8081). Guests save changes in their browser. Sign in to save a separate planner to your account.

Java 21, Spring Boot, Spring Security, Spring Data JPA, JavaScript, and MySQL locally. The hosted configuration supports PostgreSQL.

Run `mvn test` for JUnit, MockMvc, and Mockito checks. Add `-Dbrowser.tests=true` for the Selenium guest workflow in Chrome.
