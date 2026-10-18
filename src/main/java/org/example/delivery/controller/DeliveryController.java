package org.example.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.service.DeliveryService;
import org.example.delivery.util.VehicleType;
import org.example.delivery.exceptions.response.ExceptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RequestMapping("api")
@RestController
@Slf4j
@Tag(name = "Delivery", description = "Operations related to deliveries")
public class DeliveryController {
    private final DeliveryService deliveryService;

    @Operation(
            summary = "Get delivery fee from database based on chosen vehicle type and city",
            description = "Retrieves a base fee for given combination of vehicle and city, calculates additional fees based on weather data (latest or at specified time), and returns the total fee",
            parameters = {
                    @Parameter(
                            name = "city",
                            description = "City for which the fee needs to be calculated (Tallinn, Tartu, or Pärnu)",
                            in = ParameterIn.QUERY,
                            required = true,
                            schema = @Schema(implementation = String.class, example = "Tallinn"),
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "Tallinn")
                            )
                    ),
                    @Parameter(
                            name = "vehicleType",
                            description = "Type of vehicle to calculate fees for (Car, Scooter, or Bike)",
                            in = ParameterIn.QUERY,
                            required = true,
                            schema = @Schema(implementation = String.class, example = "Car"),
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "Car")
                            )
                    ),
                    @Parameter(
                            name = "datetime",
                            description = "Optional date and time for which to calculate the fee (if not provided, uses current time)",
                            in = ParameterIn.QUERY,
                            required = false,
                            schema = @Schema(implementation = String.class, example = "2023-01-01T12:00:00"),
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "2023-06-15T14:30:00")
                            )
                    )
            }
    )
    @ApiResponse(responseCode = "200", description = "Fee has been calculated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Double.class),
                    examples = @ExampleObject(value = "3.50")
    ))
    @ApiResponse(responseCode = "400", description = "Invalid input or forbidden vehicle usage",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"message\": \"Invalid vehicle type provided: Truck\"}")
    ))
    @ApiResponse(responseCode = "404", description = "Weather data not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"message\": \"No weather data available for Tallinn\"}")
    ))
    @GetMapping("public/fee")
    public ResponseEntity<Double> getDeliveryFee(
            @NotBlank(message = "City is required") @RequestParam(value = "city") String city,
            @NotBlank(message = "Vehicle type is required") @RequestParam(value = "vehicleType") String type,
            @RequestParam(required = false) String datetime
    ) {
        log.info("Received request to calculate delivery fee for city: {}, vehicle type: {}", city, type);

        try {
            VehicleType.fromString(type);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid vehicle type provided: {}", type);
            throw e;
        }

        double deliveryFee = deliveryService.calculateTotalFee(type, city, datetime);
        log.info("Calculated delivery fee: {}", deliveryFee);
        return ResponseEntity.ok(deliveryFee);
    }
}