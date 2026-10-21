package org.example.delivery.util;


import lombok.Getter;

@Getter
public enum VehicleType {
    CAR("Car"),
    SCOOTER("Scooter"),
    BIKE("Bike");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Converts a string to VehicleType enum.
     *
     * @param type the string representation of vehicle type
     * @return the corresponding VehicleType enum
     * @throws IllegalArgumentException if the string doesn't match any vehicle type
     */
    public static VehicleType fromString(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Vehicle type cannot be null or empty");
        }

        for (VehicleType vehicleType : VehicleType.values()) {
            if (vehicleType.displayName.equalsIgnoreCase(type)) {
                return vehicleType;
            }
        }

        throw new IllegalArgumentException("Invalid vehicle type provided: " + type);
    }
}