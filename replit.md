# trackerDetails

Monthly expenditure and savings tracker built with Spring Boot, MongoDB, Thymeleaf, Chart.js, and Apache POI.

## Architecture

- **Backend**: Spring Boot 3.5.0 (Java 17/19)
- **Database**: MongoDB 7.0 (local, started on port 27017)
- **Templating**: Thymeleaf
- **Charts**: Chart.js 4.4.0 (via CDN)
- **Excel Export**: Apache POI 5.3.0

## Project Structure

```
src/
  main/
    java/com/example/trackerdetails/
      TrackerDetailsApplication.java   - Main entry point
      controller/ExpenditureController.java - HTTP endpoints
      model/Expenditure.java           - Data model
      repository/ExpenditureRepository.java - MongoDB repository
      service/ExpenditureService.java  - Business logic
    resources/
      application.properties           - App config (port 5000)
      templates/
        index.html                     - Main dashboard page
        form.html                      - Add/Edit entry form
```

## Running

The app starts via `start.sh` which:
1. Starts MongoDB at `localhost:27017` with data at `/home/runner/data/db`
2. Runs the Spring Boot app with `mvn spring-boot:run`

Server listens on `0.0.0.0:5000`.

## Environment Variables

- `MONGO_URI` - MongoDB connection URI (default: `mongodb://localhost:27017/trackerdb`)
- `PORT` is hardcoded to 5000 in `application.properties`

## API Endpoints

- `GET /` - Main dashboard (Thymeleaf)
- `GET /form` - Add/Edit entry form (Thymeleaf)
- `GET /api/expenditures` - List all records (JSON)
- `POST /api/expenditures` - Create record (JSON)
- `PUT /api/expenditures/{id}` - Update record (JSON)
- `DELETE /api/expenditures/{id}` - Delete record
- `GET /export/excel` - Download Excel file
