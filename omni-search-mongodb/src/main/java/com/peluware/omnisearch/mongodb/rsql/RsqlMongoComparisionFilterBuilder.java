package com.peluware.omnisearch.mongodb.rsql;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import org.bson.conversions.Bson;

import java.util.List;

public interface RsqlMongoComparisionFilterBuilder {

    Bson buildComparisionFilter(String path, Class<?> type, ComparisonOperator operator, List<?> arguments);
}
