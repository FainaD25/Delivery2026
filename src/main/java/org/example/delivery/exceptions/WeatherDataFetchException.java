package org.example.delivery.exceptions;

public class WeatherDataFetchException extends RuntimeException {
    public WeatherDataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
