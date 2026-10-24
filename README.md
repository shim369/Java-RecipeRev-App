# RecipeRev

[English](README.md) | [日本語](README.ja.md)

**RecipeRev** (Recipe + Revision) is a recipe version-control web app. Every time you tweak a recipe, a new version is recorded. Compare any two versions side by side, log how each attempt turned out, and find the formulation that tastes best.

> **Status:** Work in progress. See the [Roadmap](#roadmap) for current progress.

## Concept

Cooking is iterative: a little less sugar, a bit more soy sauce, a different cooking time. RecipeRev treats each recipe like a managed document with a revision history, a diff view, and an approval flow, so you can see exactly what changed and whether it made the dish better.

This project is also a portfolio piece. It reproduces the typical shape of an industrial "recipe management" system (change history, diff, approval) with an everyday subject, and keeps the full development process (requirements, design, implementation, testing, release) as documented deliverables.

## Features

- **Recipe management**: create, edit, and delete recipes (name, description, ingredients with quantities, steps)
- **Automatic versioning**: every edit creates a new version; ingredients and steps are stored per version
- **Diff view**: pick any two versions and see added (green), removed (red), and changed (yellow) ingredients, quantities, and steps
- **Approval flow**: `Draft` -> `Testing` -> `Final`. Only one Final version per recipe; promoting a new one moves the previous Final back to Testing
- **Cooking log**: record date, rating (1-5), taste notes, and one photo per attempt, linked to a version
- **Rating chart**: average rating per version as a line chart
- **Trend analysis**: a Python script analyzes the correlation between ingredient quantities and ratings (for example, "versions with more sugar tend to score higher") and the result is shown in the UI

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.x, Spring MVC, Spring Data JPA, Maven |
| Frontend | Thymeleaf, JavaScript, Chart.js, HTML/CSS |
| Database | MySQL 8 (via Docker Compose) |
| Analysis | Python 3 (`analysis/`) |
| Testing | JUnit 5, Spring Boot Test, Testcontainers or H2 |

## Architecture

```
controller  ->  service  ->  repository  ->  MySQL
   (MVC)     (@Transactional,   (JPA / SQL)
              diff logic)
```

- Layered packages: `controller` / `service` / `repository` / `domain` / `dto`
- Diff logic is isolated in a dedicated service and covered by unit tests
- Transaction boundaries are defined in the service layer
- Input validation with Bean Validation; exceptions handled centrally via `@ControllerAdvice`
- Python analysis is decoupled from the app (batch execution with JSON/CSV exchange)

## Database

Minimal schema:

- `recipes`: recipe master
- `recipe_versions`: version number, steps, created date, status (unique on `recipe_id` + version number; at most one Final per recipe)
- `version_ingredients`: ingredients and quantities per version
- `cook_logs`: version ID, rating, notes, cooked date, photo path

See [docs/04_er_diagram.md](docs/04_er_diagram.md) for the ER diagram, constraints, and version-comparison SQL.

## Getting Started

### Prerequisites

- JDK 21
- Docker and Docker Compose
- Python 3 (only for trend analysis)

### Run

```bash
# 1. Clone
git clone https://github.com/shim369/Java-RecipeRev-App.git
cd Java-RecipeRev-App

# 2. Start MySQL
docker compose up -d

# 3. Start the app
./mvnw spring-boot:run
```

Then open http://localhost:8080.

If you prefer a local MySQL instead of Docker, create a database and adjust the datasource settings in `src/main/resources/application.properties`.

### Run tests

```bash
./mvnw test
```

## Project Structure

```
Java-RecipeRev-App/
├── docs/              # requirements, screen list, transitions, ER diagram, test specs
├── analysis/          # Python trend analysis
├── src/
│   ├── main/java/     # controller / service / repository / domain / dto
│   ├── main/resources/ # templates, static assets, application settings
│   └── test/java/
├── docker-compose.yml
├── pom.xml
├── README.md
└── README.ja.md
```

## Roadmap

- [x] Phase 1: Requirements, screen list, screen transitions, ER diagram (`docs/`)
- [x] Phase 2: Project setup, Docker Compose, recipe CRUD
- [x] Phase 3: Versioning, diff view, approval flow
- [x] Phase 4: Cooking log (with photo), rating chart
- [x] Phase 5: Python trend analysis integration
- [ ] Phase 6: Unit/integration tests, test specifications, documentation

## License

This project is licensed under the [MIT License](LICENSE).
