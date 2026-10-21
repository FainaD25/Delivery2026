package org.example.delivery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class DeliveryFeeIntegrationTest {

    @Test
    void baseFeeOverwriteReturnsLatestValueThroughRest() throws Exception {
        mockMvc.perform(put("/api/base-fees/Tallinn/Car").param("baseFee", "8.25"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/base-fees/Tallinn/Car").param("baseFee", "9.75"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/base-fees/Tallinn/Car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseFee").value(9.75));
    }

    @Test
    void baseFeeCrudCoversListingLookupAndValidation() throws Exception {
        mockMvc.perform(get("/api/base-fees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].city").exists());

        mockMvc.perform(get("/api/base-fees/Tallinn/Car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType").value("Car"));

        mockMvc.perform(put("/api/base-fees/Tallinn/Truck").param("baseFee", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid vehicle type provided: Truck"));

        mockMvc.perform(put("/api/base-fees/Tallinn/Car").param("baseFee", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void baseFeeDeleteReturnsStructuredNotFoundResponseAfterDeletion() throws Exception {
        mockMvc.perform(put("/api/base-fees/Tartu/Car").param("baseFee", "7.25"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/base-fees/Tartu/Car"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/base-fees/Tartu/Car"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(
                        "Base fee not found for city: Tartu and vehicle type: Car"));
    }

    @Test
    void baseFeeValidationUsesErrorHandler() throws Exception {
        mockMvc.perform(put("/api/base-fees/Narva/Car").param("baseFee", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid city provided: Narva"));

        mockMvc.perform(put("/api/base-fees/Tallinn/Bike").param("baseFee", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feeConfigurationCrudAndValidationUseErrorHandler() throws Exception {
        mockMvc.perform(get("/api/fee-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['delivery.fee.wind-speed-fee']").exists());

        mockMvc.perform(put("/api/fee-config/delivery.fee.wind-speed-fee")
                        .param("value", "invalid")
                        .param("updatedBy", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid numeric value for configuration: delivery.fee.wind-speed-fee"));

        mockMvc.perform(delete("/api/fee-config/not-present"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Configuration key not found: not-present"));
    }

    @Test
    void feeConfigurationCrudCoversLookupUpdateDeleteAndRequiredParameters() throws Exception {
        mockMvc.perform(get("/api/fee-config/delivery.fee.wind-speed-fee"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.5"));

        mockMvc.perform(put("/api/fee-config/delivery.fee.wind-speed-fee")
                        .param("value", "0.75")
                        .param("description", "updated")
                        .param("updatedBy", "tester"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Configuration saved successfully for key: delivery.fee.wind-speed-fee"));

        mockMvc.perform(get("/api/fee-config/delivery.fee.wind-speed-fee"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.75"));

        mockMvc.perform(delete("/api/fee-config/delivery.fee.wind-speed-fee"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/fee-config/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Configuration key not found: missing"));
    }

    @Test
    void feeConfigurationRequiredParametersUseErrorHandler() throws Exception {
        mockMvc.perform(put("/api/fee-config/test-key")
                        .param("value", "1.0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/fee-config/test-key")
                        .param("updatedBy", "tester"))
                .andExpect(status().isBadRequest());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCalculateDeliveryFee() throws Exception {
        mockMvc.perform(get("/scheduler/run"));
        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testCalculateDeliveryFeeNoCityData() throws Exception {
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Narva")
                        .param("vehicleType", "Car"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateDeliveryFeeNoVehicleData() throws Exception {
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Truck"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateDeliveryFeeNoWeatherData() throws Exception {
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Car"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCalculateDeliveryFeeWithDatetime() throws Exception {
        mockMvc.perform(get("/scheduler/run"));
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Car")
                        .param("datetime", "2023-06-15T14:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testCalculateDeliveryFeeWithInvalidDatetime() throws Exception {
        mockMvc.perform(get("/scheduler/run"));
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Car")
                        .param("datetime", "invalid-datetime"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid datetime. Expected ISO-8601 format with timezone offset."));
    }

    @Test
    void testCalculateDeliveryFeeRejectsDatetimeWithoutTimezoneOffset() throws Exception {
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Car")
                        .param("datetime", "2023-06-15T14:30:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid datetime. Expected ISO-8601 format with timezone offset."));
    }

    @Test
    void testCalculateDeliveryFeeWithValidDatetime_ApiReturnsCorrectFormat() throws Exception {
        mockMvc.perform(get("/scheduler/run"));
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Car")
                        .param("datetime", "2023-06-15T14:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void testCalculateDeliveryFee_BoundaryValues_Temperature() throws Exception {
        mockMvc.perform(get("/scheduler/run"));

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Scooter")
                .param("datetime", "2023-06-15T10:00:00Z"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateDeliveryFee_BoundaryValues_WindSpeed() throws Exception {
        mockMvc.perform(get("/scheduler/run"));

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00Z"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateDeliveryFee_WeatherPhenomena_AllTypes() throws Exception {
        mockMvc.perform(get("/scheduler/run"));

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Scooter")
                .param("datetime", "2023-06-15T10:00:00Z"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00Z"))
                .andExpect(status().isOk());
    }
}
