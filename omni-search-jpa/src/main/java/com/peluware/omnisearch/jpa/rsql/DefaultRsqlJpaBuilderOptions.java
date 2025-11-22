package com.peluware.omnisearch.jpa.rsql;

import com.peluware.omnisearch.rsql.DefaultRsqlBuilderOptions;


public class DefaultRsqlJpaBuilderOptions extends DefaultRsqlBuilderOptions implements RsqlJpaBuilderOptions {

    private RsqlJpaComparisionPredicateBuilder predicateBuilder;

    public RsqlJpaComparisionPredicateBuilder getComparisionPredicateBuilder() {
        if (this.predicateBuilder == null) {
            this.predicateBuilder = new DefaultRsqlJpaComparisionPredicateBuilder();
        }
        return this.predicateBuilder;
    }

}
