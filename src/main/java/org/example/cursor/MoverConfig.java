package org.example.cursor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public record MoverConfig(ZonedDateTime startAt, ZonedDateTime endAt, Duration interval, int movePixels) {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT, Locale.ROOT);
    private static final String START_PROPERTY = "cursor.mover.start";
    private static final String END_PROPERTY = "cursor.mover.end";
    private static final String INTERVAL_MS_PROPERTY = "cursor.mover.interval.ms";
    private static final String MOVE_PIXELS_PROPERTY = "cursor.mover.move.pixels";

    public MoverConfig {
        Objects.requireNonNull(startAt, "startAt");
        Objects.requireNonNull(endAt, "endAt");
        Objects.requireNonNull(interval, "interval");
    }

    public void validate() {
        if (!endAt.isAfter(startAt)) {
            var msg = "The end date/time must be after the start date/time.";
            throw new IllegalArgumentException(msg);
        }
        if (!interval.isPositive()) {
            var msg = "The interval must be positive.";
            throw new IllegalArgumentException(msg);
        }
        if (movePixels <= 0) {
            var msg = "The displacement in pixels must be positive.";
            throw new IllegalArgumentException(msg);
        }
    }

    public static MoverConfig fromSystemPropertiesOrEnv() {
        var zoneId = ZoneId.systemDefault();
        var startAt = requiredDateTime(START_PROPERTY, "CURSOR_MOVER_START", zoneId);
        var endAt = requiredDateTime(END_PROPERTY, "CURSOR_MOVER_END", zoneId);
        var interval = Duration.ofMillis(requiredLong(INTERVAL_MS_PROPERTY, "CURSOR_MOVER_INTERVAL_MS"));
        int movePixels = Math.toIntExact(optionalLong(MOVE_PIXELS_PROPERTY, "CURSOR_MOVER_MOVE_PIXELS")
                .orElse(1L));

        var config = new MoverConfig(startAt, endAt, interval, movePixels);
        config.validate();
        return config;
    }

    public static MoverConfig fromPropertiesFile(Path configFile) throws IOException {
        Objects.requireNonNull(configFile, "configFile");

        var properties = new Properties();
        try (var is = Files.newInputStream(configFile)) {
            properties.load(is);
        }

        return fromProperties(properties);
    }

    public static MoverConfig fromProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");

        var zoneId = ZoneId.systemDefault();
        var startAt = optionalProperty(properties, START_PROPERTY)
                .map(value -> parseDateTime(value, START_PROPERTY, zoneId))
                .orElseGet(() -> ZonedDateTime.now(zoneId));
        var endAt = optionalProperty(properties, END_PROPERTY)
                .map(value -> parseDateTime(value, END_PROPERTY, zoneId))
                .orElseGet(() -> startAt.plusDays(1));
        var interval = Duration.ofMillis(parseLong(requiredProperty(properties, INTERVAL_MS_PROPERTY), INTERVAL_MS_PROPERTY));
        int movePixels = Math.toIntExact(
                optionalProperty(properties, MOVE_PIXELS_PROPERTY)
                        .map(value -> parseLong(value, MOVE_PIXELS_PROPERTY))
                        .orElse(1L)
        );

        var config = new MoverConfig(startAt, endAt, interval, movePixels);
        config.validate();
        return config;
    }

    private static ZonedDateTime requiredDateTime(String propertyName, String environmentName, ZoneId zoneId) {
        var rawValue = requiredValue(propertyName, environmentName);
        return parseDateTime(rawValue, propertyName + " / " + environmentName, zoneId);
    }

    private static ZonedDateTime parseDateTime(String rawValue, String configName, ZoneId zoneId) {
        try {
            return LocalDateTime.parse(rawValue, DATE_TIME_FORMATTER).atZone(zoneId);
        } catch (DateTimeParseException e) {
            var msg = "Invalid format for " + configName + ". Expected format: " + DATE_TIME_FORMAT;
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static long requiredLong(String propertyName, String environmentName) {
        var rawValue = requiredValue(propertyName, environmentName);
        return parseLong(rawValue, propertyName + " / " + environmentName);
    }

    private static long parseLong(String rawValue, String configName) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException e) {
            var msg = configName + " must be an integer.";
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static Optional<Long> optionalLong(String propertyName, String environmentName) {
        return optionalValue(propertyName, environmentName).map(rawValue -> {
            try {
                return Long.parseLong(rawValue);
            } catch (NumberFormatException e) {
                var msg = propertyName + " / " + environmentName + " must be an integer.";
                throw new IllegalArgumentException(msg, e);
            }
        });
    }

    private static String requiredValue(String propertyName, String environmentName) {
        var msg = "Missing required configuration: property -D" + propertyName + " or environmental variable " + environmentName;
        return optionalValue(propertyName, environmentName)
                .orElseThrow(() -> new IllegalArgumentException(msg));
    }

    private static String requiredProperty(Properties properties, String propertyName) {
        var msg = "Mandatory configuration missing from the file: " + propertyName;
        return optionalProperty(properties, propertyName)
                .orElseThrow(() -> new IllegalArgumentException(msg));
    }

    private static Optional<String> optionalProperty(Properties properties, String propertyName) {
        return Optional.ofNullable(properties.getProperty(propertyName))
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private static Optional<String> optionalValue(String propertyName, String environmentName) {
        return Optional.ofNullable(System.getProperty(propertyName))
                .or(() -> Optional.ofNullable(System.getenv(environmentName)))
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }
}
