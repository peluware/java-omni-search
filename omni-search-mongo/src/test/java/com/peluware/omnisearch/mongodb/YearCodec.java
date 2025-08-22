package com.peluware.omnisearch.mongodb;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Year;

public class YearCodec implements Codec<Year> {

    @Override
    public Year decode(BsonReader reader, DecoderContext decoderContext) {
        return Year.of(reader.readInt32());
    }

    @Override
    public void encode(BsonWriter writer, Year year, EncoderContext encoderContext) {
        writer.writeInt32(year.getValue());
    }

    @Override
    public Class<Year> getEncoderClass() {
        return Year.class;
    }
}