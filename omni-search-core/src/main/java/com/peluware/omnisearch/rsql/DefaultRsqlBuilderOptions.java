package com.peluware.omnisearch.rsql;

import lombok.Data;

@Data
public class DefaultRsqlBuilderOptions implements RsqlBuilderOptions {

    private RsqlArgumentParser argumentParser;

    public RsqlArgumentParser getArgumentParser() {
        if (this.argumentParser == null) {
            this.argumentParser = new DefaultRsqlArgumentParser();
        }
        return this.argumentParser;
    }
}
