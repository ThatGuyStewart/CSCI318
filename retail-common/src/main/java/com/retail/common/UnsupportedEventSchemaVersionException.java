package com.retail.common;

public class UnsupportedEventSchemaVersionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedEventSchemaVersionException(Integer schemaVersion) {
        super("Unsupported event schema version: " + schemaVersion);
    }
}
