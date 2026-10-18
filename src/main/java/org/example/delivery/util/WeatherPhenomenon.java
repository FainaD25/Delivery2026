package org.example.delivery.util;

public class WeatherPhenomenon {

    /**
     * Checks if the weather phenomenon is severe enough to forbid certain vehicle types.
     *
     * @param phenomenon the weather phenomenon string (case-insensitive)
     * @return true if the phenomenon contains severe indicators (glaze, hail, thunder)
     */
    public static boolean isSevereForbidden(String phenomenon) {
        if (phenomenon == null || phenomenon.isEmpty()) {
            return false;
        }
        String lowerCase = phenomenon.toLowerCase();
        return lowerCase.contains("glaze") || lowerCase.contains("hail") || lowerCase.contains("thunder");
    }

    /**
     * Checks if the weather phenomenon adds a moderate fee (0.5).
     *
     * @param phenomenon the weather phenomenon string (case-insensitive)
     * @return true if the phenomenon contains rain
     */
    public static boolean isModerateFee(String phenomenon) {
        if (phenomenon == null || phenomenon.isEmpty()) {
            return false;
        }
        String lowerCase = phenomenon.toLowerCase();
        return lowerCase.contains("rain");
    }

    /**
     * Checks if the weather phenomenon adds a severe fee (1.0) for all vehicles.
     *
     * @param phenomenon the weather phenomenon string (case-insensitive)
     * @return true if the phenomenon contains snow or sleet
     */
    public static boolean isSevereFeeAll(String phenomenon) {
        if (phenomenon == null || phenomenon.isEmpty()) {
            return false;
        }
        String lowerCase = phenomenon.toLowerCase();
        return lowerCase.contains("snow") || lowerCase.contains("sleet");
    }
}