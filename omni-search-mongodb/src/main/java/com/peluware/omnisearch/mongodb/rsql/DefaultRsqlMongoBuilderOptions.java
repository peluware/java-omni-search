package com.peluware.omnisearch.mongodb.rsql;

import com.peluware.omnisearch.rsql.DefaultRsqlBuilderOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultRsqlMongoBuilderOptions extends DefaultRsqlBuilderOptions implements RsqlMongoBuilderOptions {

    private RsqlMongoComparisionFilterBuilder filterBuilder;

    public RsqlMongoComparisionFilterBuilder getComparisionFilterBuilder() {
        if (this.filterBuilder == null) {
            this.filterBuilder = new DefaultRsqlMongoComparisionFilterBuilder();
        }
        return this.filterBuilder;
    }

}
