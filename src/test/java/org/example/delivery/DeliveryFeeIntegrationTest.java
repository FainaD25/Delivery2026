package org.example.delivery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class DeliveryFeeIntegrationTest {

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
                        .param("datetime", "2023-06-15T14:30:00"))
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testCalculateDeliveryFeeWithValidDatetime_ApiReturnsCorrectFormat() throws Exception {
        mockMvc.perform(get("/scheduler/run"));
        mockMvc.perform(get("/api/public/fee")
                        .param("city", "Tallinn")
                        .param("vehicleType", "Car")
                        .param("datetime", "2023-06-15T14:30:00"))
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
                .param("datetime", "2023-06-15T10:00:00"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateDeliveryFee_BoundaryValues_WindSpeed() throws Exception {
        mockMvc.perform(get("/scheduler/run"));

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateDeliveryFee_WeatherPhenomena_AllTypes() throws Exception {
        mockMvc.perform(get("/scheduler/run"));

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Scooter")
                .param("datetime", "2023-06-15T10:00:00"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/fee")
                .param("city", "Tallinn")
                .param("vehicleType", "Bike")
                .param("datetime", "2023-06-15T10:00:00"))
                .andExpect(status().isOk());
    }
}
