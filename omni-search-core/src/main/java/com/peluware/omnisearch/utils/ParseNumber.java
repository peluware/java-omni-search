package com.peluware.omnisearch.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@UtilityClass
public class ParseNumber {

    /**
     * List of converters to transform strings into numeric types.
     */
    public static final List<Parser<? extends Number>> PARSERS = List.of(
            Parser.of(Integer.class, Integer::parseInt),
            Parser.of(Long.class, Long::parseLong),
            Parser.of(Double.class, Double::parseDouble),
            Parser.of(Float.class, Float::parseFloat),
            Parser.of(Short.class, Short::parseShort),
            Parser.of(Byte.class, Byte::parseByte),
            Parser.of(BigDecimal.class, BigDecimal::new),
            Parser.of(int.class, Integer::parseInt),
            Parser.of(long.class, Long::parseLong),
            Parser.of(double.class, Double::parseDouble),
            Parser.of(float.class, Float::parseFloat),
            Parser.of(short.class, Short::parseShort),
            Parser.of(byte.class, Byte::parseByte)
    );

    public record Parser<T>(Class<T> type, Function<String, T> converter) {
        static <T> Parser<T> of(Class<T> type, Function<String, T> converter) {
            return new Parser<>(type, converter);
        }

        public T parse(String value) {
            return converter.apply(value);
        }
    }
}
