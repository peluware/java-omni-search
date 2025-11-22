package com.peluware.omnisearch.rsql;

public class DefaultRsqlBuilderOptions implements RsqlBuilderOptions {

    private RsqlArgumentParser argumentParser;

    public RsqlArgumentParser getArgumentParser() {
        if (this.argumentParser == null) {
            this.argumentParser = new DefaultRsqlArgumentParser();
        }
        return this.argumentParser;
    }
}
