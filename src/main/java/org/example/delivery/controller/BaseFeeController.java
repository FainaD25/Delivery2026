package org.example.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.entity.BaseFeeEntity;
import org.example.delivery.repository.BaseFeeRepository;
import org.example.delivery.exceptions.response.ExceptionResponse;
import org.example.delivery.util.VehicleType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.example.delivery.config.DeliveryDomainProperties;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/base-fees")
@RestController
@Slf4j
@Tag(name = "Base Fees", description = "Operations for managing regional base fees")
public class BaseFeeController {
    private final BaseFeeRepository baseFeeRepository;
    private final DeliveryDomainProperties domainProperties;

    @Operation(
            summary = "Get all base fees",
            description = "Retrieves all currently active regional base-fee versions. Deleted historical " +
                    "versions are retained for datetime-based calculations but are not listed."
    )
    @ApiResponse(responseCode = "200", description = "Base fees retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = BaseFeeEntity.class))
            )
    )
    @GetMapping("")
    public ResponseEntity<List<BaseFeeEntity>> getAllBaseFees() {
        log.info("Retrieving all base fees");
        List<BaseFeeEntity> baseFees = baseFeeRepository.findAllByDeletedAtIsNull();
        return ResponseEntity.ok(baseFees);
    }

    @Operation(
            summary = "Get base fee for specific city and vehicle type",
            description = "Retrieves the current active version for a city and vehicle type combination. " +
                    "Historical versions are retained for datetime-based delivery-fee calculations."
    )
    @Parameter(
            name = "city",
            description = "City for which to retrieve the base fee",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "Tallinn")
    )
    @Parameter(
            name = "vehicleType",
            description = "Vehicle type for which to retrieve the base fee",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "Car")
    )
    @ApiResponse(responseCode = "200", description = "Base fee retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseFeeEntity.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Base fee not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    @GetMapping("/{city}/{vehicleType}")
    public ResponseEntity<BaseFeeEntity> getBaseFee(
            @NotBlank(message = "City is required") @PathVariable String city,
            @NotBlank(message = "Vehicle type is required") @PathVariable String vehicleType) {
        log.info("Retrieving base fee for city: {} and vehicle type: {}", city, vehicleType);
        Optional<BaseFeeEntity> baseFee = baseFeeRepository
                .findFirstByCityAndVehicleTypeAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(city, vehicleType);
        if (baseFee.isPresent()) {
            return ResponseEntity.ok(baseFee.get());
        } else {
            log.warn("Base fee not found for city: {} and vehicle type: {}", city, vehicleType);
            throw new IllegalStateException("Base fee not found for city: " + city + " and vehicle type: " + vehicleType);
        }
    }

    @Operation(
            summary = "Create or update a base fee",
            description = "Creates a new version for the specified city and vehicle type. Existing versions " +
                    "are retained so datetime-based calculations remain historically accurate."
    )
    @Parameter(
            name = "city",
            description = "City for the base fee",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "Tallinn")
    )
    @Parameter(
            name = "vehicleType",
            description = "Vehicle type for the base fee",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "Car")
    )
    @Parameter(
            name = "baseFee",
            description = "The non-negative base fee value. Saving creates a new UTC-effective version; " +
                    "previous versions are retained.",
            in = ParameterIn.QUERY,
            required = true,
            schema = @Schema(implementation = Double.class, example = "4.5")
    )
    @ApiResponse(responseCode = "200", description = "Base fee saved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    @PutMapping("/{city}/{vehicleType}")
    public ResponseEntity<String> setBaseFee(
            @NotBlank(message = "City is required") @PathVariable String city,
            @NotBlank(message = "Vehicle type is required") @PathVariable String vehicleType,
            @DecimalMin(value = "0.0", message = "Base fee cannot be negative")
            @RequestParam double baseFee) {
        log.info("Setting base fee for city: {} and vehicle type: {} to value: {}", city, vehicleType, baseFee);

        if (!domainProperties.getStations().containsKey(city)) {
            throw new IllegalArgumentException("Invalid city provided: " + city);
        }
        if (!Double.isFinite(baseFee) || baseFee < 0) {
            throw new IllegalArgumentException("Base fee cannot be negative or non-finite");
        }
        try {
            VehicleType.fromString(vehicleType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid vehicle type provided: {}", vehicleType);
            throw e;
        }

        BaseFeeEntity feeEntity = new BaseFeeEntity();

        feeEntity.setCity(city);
        feeEntity.setVehicleType(vehicleType);
        feeEntity.setBaseFee(baseFee);
        feeEntity.setEffectiveFrom(java.time.Instant.now());

        baseFeeRepository.save(feeEntity);
        log.info("Base fee saved successfully for city: {} and vehicle type: {}", city, vehicleType);
        return ResponseEntity.ok("Base fee saved successfully for city: " + city + " and vehicle type: " + vehicleType);
    }

    @Operation(
            summary = "Delete a base fee",
            description = "Marks all active versions for the city and vehicle type as deleted. Historical " +
                    "calculations before deletion retain the old value; current calculations use the built-in default."
    )
    @Parameter(
            name = "city",
            description = "City for which to delete the base fee",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "Tallinn")
    )
    @Parameter(
            name = "vehicleType",
            description = "Vehicle type for which to delete the base fee",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "Car")
    )
    @ApiResponse(responseCode = "200", description = "Base fee deleted successfully",
            content = @Content(
                    mediaType = "application/json"
            )
    )
    @ApiResponse(responseCode = "404", description = "Base fee not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    @DeleteMapping("/{city}/{vehicleType}")
    public ResponseEntity<String> deleteBaseFee(
            @NotBlank(message = "City is required") @PathVariable String city,
            @NotBlank(message = "Vehicle type is required") @PathVariable String vehicleType) {
        log.info("Deleting base fee for city: {} and vehicle type: {}", city, vehicleType);
        Optional<BaseFeeEntity> baseFee = baseFeeRepository
                .findFirstByCityAndVehicleTypeAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(city, vehicleType);
        if (baseFee.isPresent()) {
            java.time.Instant deletedAt = java.time.Instant.now();
            baseFeeRepository.findAllByCityAndVehicleTypeAndDeletedAtIsNull(city, vehicleType)
                    .forEach(fee -> {
                        fee.setDeletedAt(deletedAt);
                        baseFeeRepository.save(fee);
                    });
            log.info("Base fee deleted successfully for city: {} and vehicle type: {}", city, vehicleType);
            return ResponseEntity.ok("Base fee deleted successfully for city: " + city + " and vehicle type: " + vehicleType);
        } else {
            log.warn("Base fee not found for deletion for city: {} and vehicle type: {}", city, vehicleType);
            throw new IllegalStateException("Base fee not found for city: " + city + " and vehicle type: " + vehicleType);
        }
    }
}