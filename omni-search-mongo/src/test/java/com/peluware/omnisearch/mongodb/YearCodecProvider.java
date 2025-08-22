package com.peluware.omnisearch.mongodb;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.time.Year;

// SOLUCIÓN 2: CodecProvider personalizado para Year
public class YearCodecProvider implements CodecProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == Year.class) {
            return (Codec<T>) new YearCodec();
        }
        return null;
    }
}