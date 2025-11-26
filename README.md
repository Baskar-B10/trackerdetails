# trackerDetails

This is a Spring Boot application to track monthly expenditure and savings. It uses MongoDB to persist data, Thymeleaf for pages, Chart.js for charts, and Apache POI to export Excel.

Quick start (local):

1. Ensure Java 17 and Maven are installed.
2. Start MongoDB locally or set `MONGODB_URI` env var.

Run:

```bash
mvn spring-boot:run
```

Or build and run jar:

```bash
mvn package
java -jar target/trackerDetails-0.0.1-SNAPSHOT.jar
```

Set `MONGODB_URI` to point to your MongoDB (default: `mongodb://localhost:27017/trackerdb`).

GitHub Actions:
- The workflow `.github/workflows/ci-and-publish.yml` builds the project on push and PR.
- On push to `main` it builds and pushes a Docker image to GitHub Container Registry (GHCR). To publish to GHCR, ensure `GITHUB_TOKEN` has `packages: write` permission or use a PAT configured in `secrets`.

Deployment options:
- Use the pushed Docker image and run in any container host (Render, DigitalOcean App, Cloud Run, etc.)
# trackerDetails