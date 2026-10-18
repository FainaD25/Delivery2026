# Delivery

This is a delivery fee calculation application built with Java, Gradle and an in-memory H2 database.

## Overview

This is an application built with Java, Gradle and an in-memory H2 database. This README will guide you through the steps to build and run the application.

## Used Technologies

* Spring Boot
* Gradle - build tool
* Lombok
* Liquibase
* H2 - database

## Configuration

The application includes configurable parameters for fee calculation in `src/main/resources/application.properties`:

```
# Delivery fee calculation configuration
delivery.fee.cold-temperature-threshold=-10.0
delivery.fee.freezing-temperature-threshold=0.0
delivery.fee.wind-speed-moderate-threshold=10.0
delivery.fee.wind-speed-high-threshold=20.0
delivery.fee.cold-temperature-fee=0.5
delivery.fee.freezing-temperature-fee=1.0
delivery.fee.wind-speed-fee=0.5
delivery.fee.precipitation-fee=0.5
delivery.fee.severe-precipitation-fee=1.0
```

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
   JDBC URL: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1 <br>
   username: sa <br>
   leave the password field empty

6. After running the application, the documentation can be seen at http://localhost:8080/swagger-ui.html

## API Endpoints

### Get Delivery Fee
```
GET /api/public/fee?city={city}&vehicleType={vehicleType}&datetime={datetime}
```

**Parameters:**
- `city`: City for which the fee needs to be calculated (Tallinn, Tartu, or Pärnu)
- `vehicleType`: Type of vehicle to calculate fees for (Car, Scooter, or Bike)
- `datetime`: Optional date and time for which to calculate the fee (if not provided, uses current time)
  Format: ISO 8601 (e.g., 2023-06-15T14:30:00)

**Response:**
- Returns the calculated delivery fee as a JSON number

**Examples:**
```
GET /api/public/fee?city=Tallinn&vehicleType=Car
GET /api/public/fee?city=Tallinn&vehicleType=Car&datetime=2023-06-15T14:30:00
```

### Trigger Weather Update (Manual)
```
GET /scheduler/run
```

Manually triggers the weather data fetch process (normally runs hourly at HH:15:00).

## Fee Calculation Logic

The delivery fee is calculated as:
**Total Fee = Base Fee + Extra Fee**

### Base Fee
- Retrieved from database based on city and vehicle type
- Pre-populated data for Tallinn, Tartu, and Pärnu with Car, Scooter, and Bike types

### Extra Fee (Weather-Based)
Applied based on current weather conditions:

#### Temperature Fees (applies to all vehicle types):
- Temperature < -10.0°C: +1.0 fee
- -10.0°C ≤ Temperature ≤ 0.0°C: +0.5 fee
- Temperature > 0.0°C: +0.0 fee

#### Wind Speed Fees (applies to Bike only):
- Wind Speed ≥ 10.0 m/s: +0.5 fee
- Wind Speed > 20.0 m/s: Usage forbidden (throws exception)

#### Weather Phenomenon Fees:
- Snow/Sleet: +1.0 fee (applies to all vehicles)
- Rain: +0.5 fee (applies to all vehicles)
- Glaze/Hail/Thunder: Usage forbidden (throws exception for all vehicles)

## Sample Data

The application is pre-loaded with the following base fees:

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