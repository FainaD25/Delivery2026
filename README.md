# Delivery

This is a delivery fee calculation application built with Java, Gradle and an H2 database.

## Overview

This is an application built with Java, Gradle and an in-memory H2 database. This README will guide you through the steps to build and run the application.

## Used Technologies

* Spring Boot
* Gradle - build tool
* Lombok
* Liquibase
* H2 - database (in-memory by default)
* Springdoc OpenAPI / Swagger UI

## Configuration

The application includes configurable parameters for fee calculation that can be managed in two ways:

1. **Default values** in `src/main/resources/application.properties` (used as fallbacks)
2. **Runtime management** via REST API endpoints (takes precedence over defaults)

### Default Configuration (application.properties)
```
# These values serve as defaults when no database configuration is present
delivery.fee.cold-temperature-threshold=-10.0
delivery.fee.freezing-temperature-threshold=0.0
delivery.fee.wind-speed-moderate-threshold=10.0
delivery.fee.wind-speed-high-threshold=20.0
delivery.fee.cold-temperature-fee=0.5
delivery.fee.freezing-temperature-fee=1.0
delivery.fee.wind-speed-fee=0.5
delivery.fee.precipitation-fee=0.5
delivery.fee.severe-precipitation-fee=1.0

delivery.stations.Tallinn=Tallinn-Harku
delivery.stations.Tartu=Tartu-Tõravere
delivery.stations.Pärnu=Pärnu
delivery.base-fees[Tallinn|Car]=4.00
delivery.base-fees[Tallinn|Scooter]=3.50
delivery.base-fees[Tallinn|Bike]=3.00
delivery.base-fees[Tartu|Car]=3.50
delivery.base-fees[Tartu|Scooter]=3.00
delivery.base-fees[Tartu|Bike]=2.50
delivery.base-fees[Pärnu|Car]=3.00
delivery.base-fees[Pärnu|Scooter]=2.50
delivery.base-fees[Pärnu|Bike]=2.00
```

The database fee configuration takes precedence over these application-property
fallbacks. Historical requests continue to use the version that was valid at
the selected instant.

## Prerequisites

Before you begin, ensure you have met the following requirements:

- Java JDK 25

The project includes the Gradle Wrapper, which will automatically download and use the correct Gradle version when building.

## How to Build and Run the Application

1. Clone the repository

2. To build the application:
   - On macOS/Linux (or Windows with Git Bash/WSL):
     ```
     ./gradlew clean build
     ```
   - On Windows Command Prompt/PowerShell:
     ```
     gradlew.bat clean build
     ```

3. To run the application (after building):
   ```
   java -jar build/libs/Delivery-0.0.1-SNAPSHOT.jar
   ```

4. Access the backend application at http://localhost:8080

5. Access the database console at http://localhost:8080/h2-console/
   JDBC URL: jdbc:h2:mem:testdb; <br>
   DB_CLOSE_DELAY=-1 <br>
   username: sa <br>
   leave the password field empty

6. After running the application, the documentation can be seen at http://localhost:8080/swagger-ui.html
   The raw OpenAPI document is available at http://localhost:8080/api-docs.

The default H2 database is in-memory, so data, including imported weather
history, is reset when the application stops. The application still preserves
every weather import and every fee version without overwriting earlier rows
while it is running.

## API Endpoints

### Get Delivery Fee
```
GET /api/public/fee?city={city}&vehicleType={vehicleType}&datetime={datetime}
```

**Parameters:**
- `city`: City for which the fee needs to be calculated (Tallinn, Tartu, or Pärnu)
- `vehicleType`: Type of vehicle to calculate fees for (Car, Scooter, or Bike)
- `datetime`: Optional date and time for which to calculate the fee (if not provided, uses current time)
  Format: ISO 8601 with a timezone offset (e.g., `2026-10-20T14:30:00+03:00`
  or `2026-10-20T11:30:00Z`). Offset-less values are rejected as ambiguous.

If `datetime` is provided without a timezone offset or is not valid ISO 8601, the API returns HTTP 400
with an error response instead of silently using the current weather.

**Response:**
- Returns the calculated delivery fee as a JSON number

**Examples:**
```
GET /api/public/fee?city=Tallinn&vehicleType=Car
GET /api/public/fee?city=Tallinn&vehicleType=Car&datetime=2023-06-15T14:30:00%2B03:00
```

### Trigger Weather Update (Manual)
```
GET /scheduler/run
```

Manually triggers the weather data fetch process (normally runs hourly at HH:15:00).
The scheduled import reads
`https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php` and stores
observations for Tallinn-Harku, Tartu-Tõravere, and Pärnu.

### Manage Fee Configuration (CRUD Operations)
Configure fee parameters at runtime through these endpoints:

#### Get All Configuration Values
```
GET /api/fee-config
```
Returns a JSON map of all fee configuration key-value pairs.

#### Get Specific Configuration Value
```
GET /api/fee-config/{key}
```
**Parameters:**
- `key`: Configuration key (e.g., `delivery.fee.cold-temperature-threshold`)

**Response:**
- Returns the configuration value as a plain text string

#### Create or Update Configuration Value
```
PUT /api/fee-config/{key}?value={value}&description={description}&updatedBy={updatedBy}
```
**Parameters:**
- `key`: Configuration key (e.g., `delivery.fee.cold-temperature-threshold`)
- `value`: Configuration value (string)
- `description`: Optional description for the configuration
- `updatedBy`: Who is making this update (required)

Known numeric fee and threshold keys are validated when updated. Fee values
must be finite and non-negative; invalid values return HTTP 400.

**Response:**
- Returns success message

#### Delete Configuration
```
DELETE /api/fee-config/{key}
```
**Parameters:**
- `key`: Configuration key to delete

**Response:**
- Returns success message
- The active version is marked deleted rather than physically removed. Historical
  calculations before the deletion time continue to use the deleted version;
  current and future calculations fall back to the default configured value.

### Manage Regional Base Fees (CRUD Operations)
Configure regional base fees at runtime through these endpoints:

#### Get All Base Fees
```
GET /api/base-fees
```
Returns a JSON array of all base fee objects.

#### Get Specific Base Fee
```
GET /api/base-fees/{city}/{vehicleType}
```
**Parameters:**
- `city`: City name (Tallinn, Tartu, or Pärnu)
- `vehicleType`: Vehicle type (Car, Scooter, or Bike)

**Response:**
- Returns the base fee object

#### Create or Update Base Fee
```
PUT /api/base-fees/{city}/{vehicleType}?baseFee={value}
```
**Parameters:**
- `city`: City name (Tallinn, Tartu, or Pärnu)
- `vehicleType`: Vehicle type (Car, Scooter, or Bike)
- `baseFee`: The base fee value

**Response:**
- Returns success message
- A new base-fee version is created; previous versions are retained for
  historical calculations.

#### Delete Base Fee
```
DELETE /api/base-fees/{city}/{vehicleType}
```
**Parameters:**
- `city`: City name (Tallinn, Tartu, or Pärnu)
- `vehicleType`: Vehicle type (Car, Scooter, or Bike)

**Response:**
- Returns success message

## Fee Calculation Logic

The delivery fee is calculated as:
**Total Fee = Base Fee + Extra Fee**

### Base Fee
- Retrieved from database based on city and vehicle type
- Pre-populated data for Tallinn, Tartu, and Pärnu with Car, Scooter, and Bike types
- Can be managed at runtime via `/api/base-fees` endpoints
- Updates create new versions with an effective UTC instant, so a request with
  `datetime` uses the base fee that was active at that instant.

### Extra Fee (Weather-Based)
Applied based on current weather conditions:

#### Temperature Fees (applies to Scooter and Bike only):
- Temperature < -10.0°C: +1.0 fee
- -10.0°C ≤ Temperature ≤ 0.0°C: +0.5 fee
- Temperature > 0.0°C: +0.0 fee
- Thresholds and fees configurable via REST API

#### Wind Speed Fees (applies to Bike only):
- Wind Speed ≥ 10.0 m/s: +0.5 fee
- Wind Speed > 20.0 m/s: Usage forbidden (throws exception)
- Thresholds and fees configurable via REST API

#### Weather Phenomenon Fees (applies to Scooter and Bike only):
- Snow/Sleet: +1.0 fee
- Rain: +0.5 fee
- Glaze/Hail/Thunder: Usage forbidden (throws exception)
- Fees configurable via REST API

All timestamps are stored as UTC instants. Weather observations, base-fee
versions, and fee-configuration versions therefore use the same temporal
representation. All weather thresholds and weather fees are also versioned when changed through
`/api/fee-config`. When `datetime` is supplied to `/api/public/fee`, the
calculation uses the configuration and base-fee versions effective at that
instant, together with the weather observation selected for that instant.
Requests without `datetime` use the current active versions and latest
available weather observation.

Deleting a base fee or fee configuration is also time-aware. All active
versions are marked as deleted at the time of the REST request rather than
physically removed, so historical requests before deletion still use the
appropriate version. Current and future requests use the built-in or
application-property default when no active database value exists.

> **Note**: Cars are exempt from all weather-based fees.

### Error responses

Errors are returned as JSON objects with a `message` property:

```json
{"message": "Usage of selected vehicle type is forbidden"}
```

- `400 Bad Request`: missing or invalid parameters, invalid datetime, invalid
  city/vehicle type, invalid fee values, or forbidden vehicle usage
- `404 Not Found`: no applicable base fee or weather data is available
- `409 Conflict`: a request cannot be persisted because of a data-integrity
  conflict
- `502 Bad Gateway`: the manual weather import failed
- `500 Internal Server Error`: an unexpected server error

## Sample Data

The application is preloaded with the following base fees:

| City   | Vehicle Type | Base Fee |
|--------|--------------|----------|
| Tallinn| Car          | 4.0      |
| Tallinn| Scooter      | 3.5      |
| Tallinn| Bike         | 3.0      |
| Tartu  | Car          | 3.5      |
| Tartu  | Scooter      | 3.0      |
| Tartu  | Bike         | 2.5      |
| Pärnu  | Car          | 3.0      |
| Pärnu  | Scooter      | 2.5      |
| Pärnu  | Bike         | 2.0      |

## Runtime Configuration Examples

### Update cold temperature threshold:
```
PUT /api/fee-config/delivery.fee.cold-temperature-threshold?value=-15.0&description=Adjusted for colder climate&updatedBy=admin
```

### Retrieve all base fees:
```
GET /api/base-fees
```

### Set base fee for Tartu Bike:
```
PUT /api/base-fees/Tartu/Bike?baseFee=2.7
```

### Delete precipitation fee configuration:
```
DELETE /api/fee-config/delivery.fee.precipitation-fee
```