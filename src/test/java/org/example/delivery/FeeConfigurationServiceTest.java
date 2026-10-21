package org.example.delivery;

import org.example.delivery.repository.FeeConfigurationRepository;
import org.example.delivery.service.FeeConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;

import org.example.delivery.entity.FeeConfigurationEntity;

@ExtendWith(MockitoExtension.class)
class FeeConfigurationServiceTest {

    @Mock
    private FeeConfigurationRepository feeConfigurationRepository;

    @Test
    void rejectsNonNumericKnownFeeValue() {
        FeeConfigurationService service = new FeeConfigurationService(feeConfigurationRepository);

        assertThrows(IllegalArgumentException.class, () -> service.setConfiguration(
                "delivery.fee.wind-speed-fee", "not-a-number", null, "admin"));
    }

    @Test
    void rejectsNegativeKnownFeeValue() {
        FeeConfigurationService service = new FeeConfigurationService(feeConfigurationRepository);

        assertThrows(IllegalArgumentException.class, () -> service.setConfiguration(
                "delivery.fee.wind-speed-fee", "-1", null, "admin"));
    }

    @Test
    void rejectsNullAndBlankConfigurationArguments() {
        FeeConfigurationService service = new FeeConfigurationService(feeConfigurationRepository);

        assertThrows(IllegalArgumentException.class,
                () -> service.setConfiguration(null, "1", null, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.setConfiguration("key", null, null, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.setConfiguration("key", "1", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.setConfiguration(" ", "1", null, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.setConfiguration("key", " ", null, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.setConfiguration("key", "1", null, " "));
    }

    @Test
    void returnsDefaultsForMissingOrMalformedNumericConfiguration() {
        FeeConfigurationService service = new FeeConfigurationService(feeConfigurationRepository);
        when(feeConfigurationRepository
                .findByFeeKeyAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc("missing"))
                .thenReturn(List.of());

        assertEquals(2.5, service.getDoubleValue("missing", 2.5));

        FeeConfigurationEntity malformed = new FeeConfigurationEntity();
        malformed.setFeeKey("delivery.fee.wind-speed-fee");
        malformed.setFeeValue("not-a-number");
        malformed.setEffectiveFrom(Instant.now());
        when(feeConfigurationRepository
                .findByFeeKeyAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc("delivery.fee.wind-speed-fee"))
                .thenReturn(List.of(malformed));

        assertEquals(0.5, service.getDoubleValue("delivery.fee.wind-speed-fee", 0.5));
    }

    @Test
    void historicalLookupUsesDefaultWhenNoVersionIsAvailable() {
        FeeConfigurationService service = new FeeConfigurationService(feeConfigurationRepository);
        when(feeConfigurationRepository
                .findByFeeKeyAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        "delivery.fee.wind-speed-fee", Instant.parse("2026-01-01T00:00:00Z")))
                .thenReturn(List.of());

        assertEquals(0.5, service.getDoubleValue(
                "delivery.fee.wind-speed-fee", 0.5, Instant.parse("2026-01-01T00:00:00Z")));
    }
}
