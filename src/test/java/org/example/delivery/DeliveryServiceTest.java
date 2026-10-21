package org.example.delivery;

import static org.junit.jupiter.api.Assertions.*;

import org.example.delivery.entity.BaseFeeEntity;
import org.example.delivery.entity.WeatherEntity;
import org.example.delivery.config.DeliveryDomainProperties;
import org.example.delivery.repository.BaseFeeRepository;
import org.example.delivery.repository.WeatherRepository;
import org.example.delivery.service.DeliveryService;
import org.example.delivery.service.FeeConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private BaseFeeRepository baseFeeRepository;

    @Mock
    private FeeConfigurationService feeConfigurationService;

    @Mock
    private DeliveryDomainProperties domainProperties;

    @InjectMocks
    private DeliveryService deliveryService;

    private BaseFeeEntity tallinnCarBaseFee;
    private BaseFeeEntity tallinnScooterBaseFee;
    private BaseFeeEntity tallinnBikeBaseFee;
    private BaseFeeEntity tartuCarBaseFee;
    private BaseFeeEntity tartuScooterBaseFee;
    private BaseFeeEntity tartuBikeBaseFee;
    private BaseFeeEntity parnuCarBaseFee;
    private BaseFeeEntity parnuScooterBaseFee;
    private BaseFeeEntity parnuBikeBaseFee;

    @BeforeEach
    void setUp() {
        lenient().when(domainProperties.getStations()).thenReturn(Map.of(
                "Tallinn", "Tallinn-Harku",
                "Tartu", "Tartu-Tõravere",
                "Pärnu", "Pärnu"));
        lenient().when(domainProperties.stationFor(anyString()))
                .thenAnswer(invocation -> Map.of(
                        "Tallinn", "Tallinn-Harku",
                        "Tartu", "Tartu-Tõravere",
                        "Pärnu", "Pärnu")
                        .get(invocation.getArgument(0)));
        lenient().when(domainProperties.defaultBaseFee(anyString(), anyString()))
                .thenAnswer(invocation -> Map.of(
                        "Tallinn|Car", new java.math.BigDecimal("4.00"),
                        "Tallinn|Scooter", new java.math.BigDecimal("3.50"),
                        "Tallinn|Bike", new java.math.BigDecimal("3.00"),
                        "Tartu|Car", new java.math.BigDecimal("3.50"),
                        "Tartu|Scooter", new java.math.BigDecimal("3.00"),
                        "Tartu|Bike", new java.math.BigDecimal("2.50"),
                        "Pärnu|Car", new java.math.BigDecimal("3.00"),
                        "Pärnu|Scooter", new java.math.BigDecimal("2.50"),
                        "Pärnu|Bike", new java.math.BigDecimal("2.00"))
                        .get(invocation.getArgument(0) + "|" + invocation.getArgument(1)));
        tallinnCarBaseFee = createBaseFeeEntity("Tallinn", "Car" , 4.0);
        tallinnScooterBaseFee = createBaseFeeEntity("Tallinn", "Scooter" , 3.5);
        tallinnBikeBaseFee = createBaseFeeEntity("Tallinn", "Bike" , 3.0);

        tartuCarBaseFee = createBaseFeeEntity("Tartu", "Car", 3.5);
        tartuScooterBaseFee = createBaseFeeEntity("Tartu", "Scooter", 3.0);
        tartuBikeBaseFee = createBaseFeeEntity("Tartu", "Bike", 2.5);

        parnuCarBaseFee = createBaseFeeEntity("Pärnu", "Car", 3.0);
        parnuScooterBaseFee = createBaseFeeEntity("Pärnu", "Scooter", 2.5);
        parnuBikeBaseFee = createBaseFeeEntity("Pärnu","Bike", 2.0);

        setThresholdValues();
    }

    private void setThresholdValues() {
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.wind-speed-high-threshold", 20.0)).thenReturn(20.0);
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.wind-speed-moderate-threshold", 10.0)).thenReturn(10.0);

        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.cold-temperature-threshold", -10.0)).thenReturn(-10.0);
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.freezing-temperature-threshold", 0.0)).thenReturn(0.0);

        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.wind-speed-fee", 0.5)).thenReturn(0.5);
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.cold-temperature-fee", 0.5)).thenReturn(0.5);
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.freezing-temperature-fee", 1.0)).thenReturn(1.0);
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.precipitation-fee", 0.5)).thenReturn(0.5);
        lenient().when(feeConfigurationService.getDoubleValue("delivery.fee.severe-precipitation-fee", 1.0)).thenReturn(1.0);
        lenient().when(feeConfigurationService.getDoubleValue(anyString(), anyDouble(), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private WeatherEntity createWeatherEntity(String stationName, double temperature, double windSpeed, String phenomenon) {
        WeatherEntity entity = new WeatherEntity();
        entity.setStationName(stationName);
        entity.setAirTemperature(temperature);
        entity.setWindSpeed(windSpeed);
        entity.setWeatherPhenomenon(phenomenon);
        entity.setTimestamp(Instant.now());
        return entity;
    }

    private BaseFeeEntity createBaseFeeEntity(String city, String vehicleType, double baseFee) {
        BaseFeeEntity baseFeeEntity = new BaseFeeEntity();
        baseFeeEntity.setCity(city);
        baseFeeEntity.setVehicleType(vehicleType);
        baseFeeEntity.setBaseFee(baseFee);
        return baseFeeEntity;
    }

    @Test
    void testCalculateFee_Tallinn_NormalConditions() {
        setThresholdValues();
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Clear");

        lenient().when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_Tartu_NormalConditions() {
        setThresholdValues();
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 5, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Bike", "Tartu"));
    }

    @Test
    void testCalculateFee_Parnu_NormalConditions() {
        setThresholdValues();
        WeatherEntity entity = createWeatherEntity("Pärnu", 5, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals(2.0, deliveryService.calculateTotalFee("Bike", "Pärnu"));
    }

    @Test
    void testCalculateFee_Tallinn_LowTemperature() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", -10, 5, "Clear");

        lenient().when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_Tartu_LowTemperature() {
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 0, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Bike", "Tartu"));
    }

    @Test
    void testCalculateFee_Parnu_LowTemperature() {
        WeatherEntity entity = createWeatherEntity("Pärnu", -5, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Bike", "Pärnu"));
    }

    @Test
    void testCalculateFee_Tallinn_VeryLowTemperature() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", -15, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(4.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_Tartu_VeryLowTemperature() {
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", -15, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tartu"));
    }

    @Test
    void testCalculateFee_Parnu_VeryLowTemperature() {
        WeatherEntity entity = createWeatherEntity("Pärnu", -15, 5, "Clear");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Bike", "Pärnu"));
    }

    @Test
    void testCalculateFee_Tallinn_StrongWind() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 10, "Clear");

        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_Tartu_StrongWind() {
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 5, 15, "Clear");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Bike", "Tartu"));
    }

    @Test
    void testCalculateFee_Parnu_StrongWind() {
        WeatherEntity entity = createWeatherEntity("Pärnu", 5, 20, "Clear");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Bike", "Pärnu"));
    }

    @Test
    void testCalculateFee_Tallinn_VeryStrongWind() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 25, "Clear");

        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Bike", "Tallinn"));
        assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
    }

    @Test
    void testCalculateFee_Tartu_VeryStrongWind() {
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 5, 25, "Clear");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Bike", "Tartu"));
        assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
    }

    @Test
    void testCalculateFee_Parnu_VeryStrongWind() {
        WeatherEntity entity = createWeatherEntity("Pärnu", 5, 25, "Clear");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Bike", "Pärnu"));
        assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
    }

    @Test
    void testCalculateFee_Tallinn_SnowOrSleet() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Moderate snowfall");

        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(4.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_Tartu_SnowOrSleet() {
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 5, 5, "Light sleet");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tartu"));
    }

    @Test
    void testCalculateFee_Parnu_SnowOrSleet() {
        WeatherEntity entity = createWeatherEntity("Pärnu", 5, 5, "Heavy snowfall");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Bike", "Pärnu"));
    }

    @Test
    void testCalculateFee_Tallinn_Rain() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Light rain");

        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_Tartu_Rain() {
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 5, 5, "Moderate rain");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Bike", "Tartu"));
    }

    @Test
    void testCalculateFee_Parnu_Rain() {
        WeatherEntity entity = createWeatherEntity("Pärnu", 5, 5, "Heavy rain");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        assertEquals(3.0, deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals(2.5, deliveryService.calculateTotalFee("Bike", "Pärnu"));
    }

    @Test
    void testCalculateFee_Tallinn_Glaze() {
        setThresholdValues();
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Glaze");

        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        IllegalArgumentException scooterException = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals("Usage of selected vehicle type is forbidden", scooterException.getMessage());
        IllegalArgumentException bikeException = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Bike", "Tallinn"));
        assertEquals("Usage of selected vehicle type is forbidden", bikeException.getMessage());
    }

    @Test
    void testCalculateFee_Tartu_Hail() {
        setThresholdValues();
        WeatherEntity entity = createWeatherEntity("Tartu-Tõravere", 5, 5, "Hail");

        when(weatherRepository.findLatestByStationName("Tartu-Tõravere")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Car")).thenReturn(Optional.of(tartuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Scooter")).thenReturn(Optional.of(tartuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike")).thenReturn(Optional.of(tartuBikeBaseFee));

        assertEquals(3.5, deliveryService.calculateTotalFee("Car", "Tartu"));
        IllegalArgumentException scooterException = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Scooter", "Tartu"));
        assertEquals("Usage of selected vehicle type is forbidden", scooterException.getMessage());
        IllegalArgumentException bikeException = assertThrows(IllegalArgumentException.class, () ->deliveryService.calculateTotalFee("Bike", "Tartu"));
        assertEquals("Usage of selected vehicle type is forbidden", bikeException.getMessage());
    }

    @Test
    void testCalculateFee_Parnu_Thunder() {
        setThresholdValues();
        WeatherEntity entity = createWeatherEntity("Pärnu", 5, 5, "Thunder");

        when(weatherRepository.findLatestByStationName("Pärnu")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Car")).thenReturn(Optional.of(parnuCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter")).thenReturn(Optional.of(parnuScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Pärnu", "Bike")).thenReturn(Optional.of(parnuBikeBaseFee));

        assertEquals(3.0, deliveryService.calculateTotalFee("Car", "Pärnu"));
        IllegalArgumentException scooterException = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Scooter", "Pärnu"));
        assertEquals("Usage of selected vehicle type is forbidden", scooterException.getMessage());
        IllegalArgumentException bikeException = assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Bike", "Pärnu"));
        assertEquals("Usage of selected vehicle type is forbidden", bikeException.getMessage());
    }

    @Test
    void testCalculateFee_InvalidCity() {
        assertThrows(IllegalArgumentException.class, () -> deliveryService.calculateTotalFee("Car", "Narva"));
    }

    @Test
    void testCalculateFee_RejectsNullAndBlankInputs() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> deliveryService.calculateTotalFee(null, "Tallinn")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> deliveryService.calculateTotalFee("", "Tallinn")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> deliveryService.calculateTotalFee("Car", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> deliveryService.calculateTotalFee("Car", ""))
        );
    }

    @Test
    void testCalculateFee_UsesDefaultBaseFeeWhenDatabaseValueIsMissing() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Clear");
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.empty());

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
    }

    @Test
    void testCalculateFee_PreservesHistoricalZeroExtraFee() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Clear");
        when(weatherRepository.findByStationNameAndTimestampBefore(anyString(), any())).thenReturn(entity);
        lenient().when(baseFeeRepository.findByCityAndVehicleTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                eq("Tallinn"), eq("Car"), any())).thenReturn(java.util.List.of(tallinnCarBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn", "2026-01-01T12:00:00Z"));
    }

    @Test
    void testCalculateFee_NoWeatherDataAvailable() {
        assertThrows(IllegalStateException.class, () -> deliveryService.calculateTotalFee("Car", "Pärnu"));
    }

    @Test
    void testCalculateFee_RejectsIncompleteWeatherData() {
        WeatherEntity missingTemperature = createWeatherEntity("Tallinn-Harku", 5, 5, "Clear");
        missingTemperature.setAirTemperature(null);
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(missingTemperature);

        assertThrows(IllegalStateException.class,
                () -> deliveryService.calculateTotalFee("Car", "Tallinn"));

        WeatherEntity missingWind = createWeatherEntity("Tallinn-Harku", 5, 5, "Clear");
        missingWind.setWindSpeed(null);
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(missingWind);

        assertThrows(IllegalStateException.class,
                () -> deliveryService.calculateTotalFee("Car", "Tallinn"));
    }

    @Test
    void testCalculateFee_WithValidDatetime() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Light rain");

        when(weatherRepository.findByStationNameAndTimestampBefore(anyString(), any())).thenReturn(entity);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn", "2023-06-15T14:30:00Z"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tallinn", "2023-06-15T14:30:00Z"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn", "2023-06-15T14:30:00Z"));

        verify(weatherRepository, times(3)).findByStationNameAndTimestampBefore(anyString(), any());
    }

    @Test
    void testCalculateFee_WithInvalidDatetime_IsRejected() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Light rain");

        lenient().when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertThrows(IllegalArgumentException.class,
                () -> deliveryService.calculateTotalFee("Car", "Tallinn", "invalid-datetime"));
    }

    @Test
    void testCalculateFee_TemperatureBoundaryValues() {
        WeatherEntity entityAtColdThreshold = createWeatherEntity("Tallinn-Harku", -10.0, 5, "Clear");
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entityAtColdThreshold);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));

        WeatherEntity entityAtFreezingThreshold = createWeatherEntity("Tallinn-Harku", 0.0, 5, "Clear");
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entityAtFreezingThreshold);

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(4.0, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_WindSpeedBoundaryValues() {
        WeatherEntity entityAtWindModerate = createWeatherEntity("Tallinn-Harku", 5, 10.0, "Clear");
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entityAtWindModerate);
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));

        WeatherEntity entityAtWindHigh = createWeatherEntity("Tallinn-Harku", 5, 20.0, "Clear");
        when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entityAtWindHigh);

        assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Scooter", "Tallinn"));
        assertEquals(3.5, deliveryService.calculateTotalFee("Bike", "Tallinn"));
    }

    @Test
    void testCalculateFee_InvalidDatetimeFormats() {
        WeatherEntity entity = createWeatherEntity("Tallinn-Harku", 5, 5, "Clear");

        lenient().when(weatherRepository.findLatestByStationName("Tallinn-Harku")).thenReturn(entity);
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car")).thenReturn(Optional.of(tallinnCarBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Scooter")).thenReturn(Optional.of(tallinnScooterBaseFee));
        lenient().when(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Bike")).thenReturn(Optional.of(tallinnBikeBaseFee));

        String[] invalidDatetimes = {
            "",                           // Empty string
            "not-a-date",                 // Completely invalid
            "2023-13-45T25:70:80",        // Invalid date/time values
            "2023-06-15",                 // Missing time component
            "15/06/2023 14:30:00",        // Wrong format
            "2023/06/15 14:30:00",        // Wrong separator
            "abcd-ef-gh ij:kl:mn",        // Non-numeric
            "2023-06-15 14:30:00.123",    // With milliseconds
        };

        for (String invalidDatetime : invalidDatetimes) {
            if (invalidDatetime.isEmpty()) {
                assertEquals(4.0, deliveryService.calculateTotalFee("Car", "Tallinn", invalidDatetime));
            } else {
                assertThrows(IllegalArgumentException.class,
                        () -> deliveryService.calculateTotalFee("Car", "Tallinn", invalidDatetime),
                        "Failed for invalid datetime: " + invalidDatetime);
            }
        }
    }
}