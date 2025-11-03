package com.garganttua.core.mapper;

public class MapperException extends Exception {
    private static final long serialVersionUID = 3629256996026750672L;

    public MapperException(String string) {
        super(string);
    }

    public MapperException(String string, Exception e) {
        super(string, e);
    }

    public MapperException(Exception e) {
        super(e);
    }
}
