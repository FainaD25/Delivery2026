package org.example.delivery;

import org.example.delivery.util.WeatherPhenomenon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherPhenomenonTest {

    @Test
    void nullAndEmptyPhenomenaDoNotMatchAnyRule() {
        assertFalse(WeatherPhenomenon.isSevereForbidden(null));
        assertFalse(WeatherPhenomenon.isSevereForbidden(""));
        assertFalse(WeatherPhenomenon.isModerateFee(null));
        assertFalse(WeatherPhenomenon.isModerateFee(""));
        assertFalse(WeatherPhenomenon.isSevereFeeAll(null));
        assertFalse(WeatherPhenomenon.isSevereFeeAll(""));
    }

    @Test
    void phenomenonRulesAreCaseInsensitive() {
        assertTrue(WeatherPhenomenon.isSevereForbidden("HAIL"));
        assertTrue(WeatherPhenomenon.isModerateFee("Heavy RAIN"));
        assertTrue(WeatherPhenomenon.isSevereFeeAll("Light SNOW"));
    }
}
