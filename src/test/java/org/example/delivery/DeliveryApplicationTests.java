package org.example.delivery;

import org.example.delivery.config.DeliveryDomainProperties;
import org.example.delivery.config.DeliveryFeeDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DeliveryApplicationTests {
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private DeliveryFeeDefaults feeDefaults;
	@Autowired
	private DeliveryDomainProperties domainProperties;

	@Test
	void contextLoads() {
		assertNotNull(applicationContext);
	}

	@Test
	void feeDefaultsAreLoadedFromApplicationProperties() {
		assertNotNull(feeDefaults);
		assertEquals(-10.0, feeDefaults.getColdTemperatureThreshold());
		assertEquals(0.5, feeDefaults.getColdTemperatureFee().doubleValue());
	}

	@Test
	void domainDefaultsAreLoadedFromApplicationProperties() {
		assertEquals("Tallinn-Harku", domainProperties.stationFor("Tallinn"));
		assertEquals(new java.math.BigDecimal("4.00"),
				domainProperties.defaultBaseFee("Tallinn", "Car"));
	}

}
