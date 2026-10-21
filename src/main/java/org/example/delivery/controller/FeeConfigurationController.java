package org.example.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.entity.FeeConfigurationEntity;
import org.example.delivery.service.FeeConfigurationService;
import org.example.delivery.exceptions.response.ExceptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/fee-config")
@RestController
@Slf4j
@Tag(name = "Fee Configuration", description = "Operations for managing fee configuration")
public class FeeConfigurationController {

    private final FeeConfigurationService feeConfigurationService;
    @Operation(
            summary = "Get all fee configuration values",
            description = "Retrieves the currently active fee configuration key-value pairs. Deleted historical " +
                    "versions are retained for datetime-based calculations but are not listed."
    )
    @ApiResponse(responseCode = "200", description = "Configurations retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class)
            )
    )
    @GetMapping("")
    public ResponseEntity<Map<String, String>> getAllConfigurations() {
        log.info("Retrieving all fee configurations");
        return ResponseEntity.ok(feeConfigurationService.getAllConfigurations());
    }

    @Operation(
            summary = "Get a specific fee configuration value",
            description = "Retrieves the value for a specific fee configuration key"
    )
    @Parameter(
            name = "key",
            description = "The configuration key to retrieve",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "delivery.fee.cold-temperature-threshold")
    )
    @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Configuration not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    @GetMapping("/{key}")
    public ResponseEntity<String> getConfiguration(
            @NotBlank(message = "Configuration key is required") @PathVariable String key) {
        log.info("Retrieving configuration for key: {}", key);
        String value = feeConfigurationService.getStringValue(key, null);
        if (value == null) {
            log.warn("Configuration key not found: {}", key);
            throw new IllegalStateException("Configuration key not found: " + key);
        }
        return ResponseEntity.ok(value);
    }

    @Operation(
            summary = "Create or update a fee configuration value",
            description = "Creates a new UTC-effective configuration version. Previous versions are retained " +
                    "for datetime-based calculations."
    )
    @Parameter(
            name = "key",
            description = "The configuration key to set",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "delivery.fee.cold-temperature-threshold")
    )
    @Parameter(
            name = "value",
            description = "The configuration value to set",
            in = ParameterIn.QUERY,
            required = true,
            schema = @Schema(implementation = String.class, example = "-15.0")
    )
    @Parameter(
            name = "description",
            description = "Optional description for the configuration",
            in = ParameterIn.QUERY,
            required = false,
            schema = @Schema(implementation = String.class, example = "Temperature threshold for freezing conditions")
    )
    @Parameter(
            name = "updatedBy",
            description = "Who is making this update",
            in = ParameterIn.QUERY,
            required = true,
            schema = @Schema(implementation = String.class, example = "admin")
    )
    @ApiResponse(responseCode = "200", description = "Configuration saved successfully",
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
    @PutMapping("/{key}")
    public ResponseEntity<String> setConfiguration(
            @NotBlank(message = "Configuration key is required") @PathVariable String key,
            @NotBlank(message = "Configuration value is required") @RequestParam String value,
            @RequestParam(required = false) String description,
            @NotBlank(message = "Updated by is required") @RequestParam String updatedBy) {
        log.info("Setting configuration for key: {} to value: {}", key, value);
        feeConfigurationService.setConfiguration(key, value, description, updatedBy);
        return ResponseEntity.ok("Configuration saved successfully for key: " + key);
    }

    @Operation(
            summary = "Delete a fee configuration",
            description = "Marks all active versions for the key as deleted. Historical calculations before " +
                    "deletion retain the old value; current calculations use the application default."
    )
    @Parameter(
            name = "key",
            description = "The configuration key to delete",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String.class, example = "delivery.fee.cold-temperature-threshold")
    )
    @ApiResponse(responseCode = "200", description = "Configuration deleted successfully",
            content = @Content(
                    mediaType = "application/json"
            )
    )
    @ApiResponse(responseCode = "404", description = "Configuration not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteConfiguration(
            @PathVariable String key) {
        log.info("Deleting configuration for key: {}", key);
        if (key == null || key.isBlank()) {
            log.warn("Configuration key is required");
            throw new IllegalArgumentException("Configuration key is required");
        }
        FeeConfigurationEntity config = feeConfigurationService.getConfiguration(key);
        if (config == null) {
            log.warn("Configuration key not found for deletion: {}", key);
            throw new IllegalStateException("Configuration key not found: " + key);
        }
        feeConfigurationService.deleteConfiguration(key);
        log.info("Configuration deleted successfully for key: {}", key);
        return ResponseEntity.ok("Configuration deleted successfully for key: " + key);
    }
}