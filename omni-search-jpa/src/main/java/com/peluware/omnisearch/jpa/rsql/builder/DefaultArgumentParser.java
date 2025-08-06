package com.peluware.omnisearch.jpa.rsql.builder;


import com.peluware.omnisearch.jpa.rsql.RsqlArgumentFormatException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Custom ArgumentParser implementation for JPA with comprehensive type support.
 * Supports all common Java types including Java 8+ time API, collections, and custom types.
 *
 * @author luidmidev
 */
@Slf4j
public class DefaultArgumentParser implements ArgumentParser {


    @Getter
    @Setter
    private static DefaultArgumentParser instance = new DefaultArgumentParser();


    // Date formatters for flexible parsing
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );

    private static final List<DateTimeFormatter> TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm")
    );

    // Custom parsers registry
    protected final Map<Class<?>, Function<String, ?>> customParsers = new ConcurrentHashMap<>();

    public DefaultArgumentParser() {
        registerDefaultParsers();
    }

    @Override
    public <T> T parse(String argument, Class<T> type) throws RsqlArgumentFormatException, IllegalArgumentException {

        log.debug("Parsing argument ''{}'' as type {}", argument, type.getSimpleName());

        // Handle null values
        if (argument == null || argument.isBlank()) {
            return null;
        }

        argument = argument.trim();

        try {
            // Check custom parsers first
            if (customParsers.containsKey(type)) {
                @SuppressWarnings("unchecked")
                var result = (T) customParsers.get(type).apply(argument);
                return result;
            }

            // Handle primitive and wrapper types
            var result = parsePrimitiveAndWrapperTypes(argument, type);
            if (result != null) return result;

            // Handle time types
            result = parseTimeTypes(argument, type);
            if (result != null) return result;

            // Handle other common types
            result = parseCommonTypes(argument, type);
            if (result != null) return result;

            // Handle enums
            if (type.isEnum()) {
                @SuppressWarnings("unchecked")
                T enumValue = (T) Enum.valueOf((Class<? extends Enum>) type, argument);
                return enumValue;
            }

            // Try valueOf method via reflection
            return parseViaValueOf(argument, type);

        } catch (Exception e) {
            log.info("Failed to parse argument ''{}'' as {}: {}", argument, type.getSimpleName(), e.getMessage());
            throw new RsqlArgumentFormatException(argument, type);
        }
    }

    @Override
    public <T> List<T> parse(List<String> arguments, Class<T> type) throws RsqlArgumentFormatException, IllegalArgumentException {
        var result = new ArrayList<T>(arguments.size());
        for (var argument : arguments) {
            result.add(parse(argument, type));
        }
        return result;
    }

    /**
     * Parse primitive and wrapper types
     */
    @SuppressWarnings({"unchecked", "java:S3776"})
    private <T> T parsePrimitiveAndWrapperTypes(String argument, Class<T> type) {
        if (type.equals(String.class)) return (T) argument;

        if (type.equals(Integer.class) || type.equals(int.class)) return (T) Integer.valueOf(argument);
        if (type.equals(Long.class) || type.equals(long.class)) return (T) Long.valueOf(argument);
        if (type.equals(Double.class) || type.equals(double.class)) return (T) Double.valueOf(argument);
        if (type.equals(Float.class) || type.equals(float.class)) return (T) Float.valueOf(argument);
        if (type.equals(Boolean.class) || type.equals(boolean.class)) return (T) Boolean.valueOf(argument);
        if (type.equals(Byte.class) || type.equals(byte.class)) return (T) Byte.valueOf(argument);
        if (type.equals(Short.class) || type.equals(short.class)) return (T) Short.valueOf(argument);
        if (type.equals(Character.class) || type.equals(char.class)) {
            if (argument.length() != 1) {
                throw new IllegalArgumentException("Character argument must be exactly one character");
            }
            return (T) Character.valueOf(argument.charAt(0));
        }

        if (type.equals(BigDecimal.class)) return (T) new BigDecimal(argument);
        if (type.equals(BigInteger.class)) return (T) new BigInteger(argument);
        return null;
    }

    /**
     * Parse Java 8+ time types
     */
    @SuppressWarnings("unchecked")
    private <T> T parseTimeTypes(String argument, Class<T> type) {
        if (type.equals(LocalDate.class)) return (T) parseLocalDate(argument);
        if (type.equals(LocalDateTime.class)) return (T) parseLocalDateTime(argument);
        if (type.equals(LocalTime.class)) return (T) parseLocalTime(argument);
        if (type.equals(Instant.class)) return (T) parseInstant(argument);
        if (type.equals(ZonedDateTime.class)) return (T) ZonedDateTime.parse(argument);
        if (type.equals(OffsetDateTime.class)) return (T) OffsetDateTime.parse(argument);
        if (type.equals(OffsetTime.class)) return (T) OffsetTime.parse(argument);
        if (type.equals(Duration.class)) return (T) Duration.parse(argument);
        if (type.equals(Period.class)) return (T) Period.parse(argument);

        // Legacy Date support
        if (type.equals(Date.class)) {
            var dateTime = parseLocalDateTime(argument);
            return (T) Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        }

        return null;
    }

    /**
     * Parse other common types
     */
    @SuppressWarnings("unchecked")
    private <T> T parseCommonTypes(String argument, Class<T> type) {
        if (type.equals(UUID.class)) return (T) UUID.fromString(argument);
        if (type.equals(byte[].class)) return (T) argument.getBytes();

        // Handle arrays of primitives
        if (type.isArray()) {
            return parseArray(argument, type);
        }

        return null;
    }

    /**
     * Parse arrays (simple comma-separated values)
     */
    @SuppressWarnings("unchecked")
    private <T> T parseArray(String argument, Class<T> type) {
        Class<?> componentType = type.getComponentType();
        String[] parts = argument.split(",");

        if (componentType.equals(String.class)) {
            return (T) parts;
        } else if (componentType.equals(Integer.class) || componentType.equals(int.class)) {
            Integer[] result = new Integer[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.valueOf(parts[i].trim());
            }
            return (T) result;
        }
        // Add more array types as needed

        return null;
    }

    /**
     * Parse via valueOf method using reflection
     */
    private <T> T parseViaValueOf(String argument, Class<T> type) {
        try {
            Method method = type.getMethod("valueOf", String.class);
            @SuppressWarnings("unchecked")
            T result = (T) method.invoke(null, argument);
            return result;
        } catch (InvocationTargetException e) {
            throw new RsqlArgumentFormatException(argument, type);
        } catch (ReflectiveOperationException e) {
            log.warn("{} does not have method valueOf(String s) or method is inaccessible", type);
            throw new IllegalArgumentException("Cannot parse argument type " + type);
        }
    }

    /**
     * Parse LocalDate with multiple format support
     */
    private LocalDate parseLocalDate(String argument) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(argument, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }
        throw new DateTimeParseException("Unable to parse LocalDate: " + argument, argument, 0);
    }

    /**
     * Parse LocalDateTime with multiple format support
     */
    private LocalDateTime parseLocalDateTime(String argument) {
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(argument, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        // Try parsing as LocalDate and convert to LocalDateTime at start of day
        try {
            LocalDate date = parseLocalDate(argument);
            return date.atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // Continue
        }

        throw new DateTimeParseException("Unable to parse LocalDateTime: " + argument, argument, 0);
    }

    /**
     * Parse LocalTime with multiple format support
     */
    private LocalTime parseLocalTime(String argument) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(argument, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }
        throw new DateTimeParseException("Unable to parse LocalTime: " + argument, argument, 0);
    }

    /**
     * Parse Instant with flexible format support
     */
    private Instant parseInstant(String argument) {
        // Try ISO format first
        try {
            return Instant.parse(argument);
        } catch (DateTimeParseException ignored) {
            // Continue
        }

        // Try as epoch milliseconds
        try {
            long epochMilli = Long.parseLong(argument);
            return Instant.ofEpochMilli(epochMilli);
        } catch (NumberFormatException ignored) {
            // Continue
        }

        // Try as epoch seconds
        try {
            long epochSecond = Long.parseLong(argument);
            return Instant.ofEpochSecond(epochSecond);
        } catch (NumberFormatException ignored) {
            // Continue
        }

        throw new DateTimeParseException("Unable to parse Instant: " + argument, argument, 0);
    }

    /**
     * Register default custom parsers
     */
    protected void registerDefaultParsers() {
        // You can add any default custom parsers here
    }

    /**
     * Add a custom parser for a specific type
     */
    public <T> void addCustomParser(Class<T> type, Function<String, T> parser) {
        customParsers.put(type, parser);
        log.debug("Added custom parser for type: {}", type.getSimpleName());
    }

    /**
     * Remove a custom parser for a specific type
     */
    public void removeCustomParser(Class<?> type) {
        customParsers.remove(type);
        log.debug("Removed custom parser for type: {}", type.getSimpleName());
    }

    /**
     * Check if a custom parser exists for the given type
     */
    public boolean hasCustomParser(Class<?> type) {
        return customParsers.containsKey(type);
    }

    /**
     * Get all registered custom parser types
     */
    public Set<Class<?>> getCustomParserTypes() {
        return Collections.unmodifiableSet(customParsers.keySet());
    }
}