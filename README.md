# Runbook Desk

I built this to keep the steps for recurring QA and support tasks in one place. Create a workspace, write articles, search their titles and instructions, and give teammates view or edit access. The public site has an example library you can browse without signing in.

[Live app](https://team-knowledge-base-api-f2st.onrender.com/)

Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA and vanilla JavaScript. Docker Compose runs MySQL locally. The hosted app uses PostgreSQL on Neon.

## Run it

Run `docker compose up --build` and open [localhost:8081](http://localhost:8081).

## Tests

Run `mvn test` for the JUnit 5, Mockito and Spring MockMvc tests. Run `mvn test -Dbrowser.tests=true` to include Selenium WebDriver tests in headless Chrome. Mockito checks that unauthorized searches never reach the article repository, MockMvc covers search and workspace permissions, and Selenium creates, finds, edits and deletes an article.
