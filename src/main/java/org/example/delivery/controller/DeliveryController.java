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
            description = "Calculates the total fee from the regional base fee and weather-based extra fees. " +
                    "Without datetime, the latest weather and active fee versions are used. With datetime, " +
                    "the weather observation and fee versions valid at that UTC instant are used.",
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
                            description = "Optional ISO-8601 date and time with a required timezone offset. " +
                                    "Use 2026-10-20T14:30:00+03:00 or 2026-10-20T11:30:00Z. " +
                                    "All persisted timestamps are UTC instants. If omitted, current values are used.",
                            in = ParameterIn.QUERY,
                            required = false,
                            schema = @Schema(implementation = String.class, example = "2026-10-20T14:30:00+03:00"),
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "2026-10-20T14:30:00+03:00")
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