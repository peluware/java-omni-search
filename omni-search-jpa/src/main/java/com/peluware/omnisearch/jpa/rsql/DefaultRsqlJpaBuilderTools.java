package com.peluware.omnisearch.jpa.rsql;

import com.peluware.omnisearch.core.rsql.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultRsqlJpaBuilderTools extends DefaultRsqlBuilderTools implements RsqlJpaBuilderTools {

    private RsqlJpaComparisionPredicate predicateBuilder;

    public RsqlJpaComparisionPredicate getComparisionPredicateBuilder() {
        if (this.predicateBuilder == null) {
            this.predicateBuilder = new DefaultRsqlJpaComparisionPredicate();
        }
        return this.predicateBuilder;
    }

}
