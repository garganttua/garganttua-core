package com.garganttua.core.mapper;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class MapperException extends CoreException {
    private static final long serialVersionUID = 3629256996026750672L;

    public MapperException(String string) {
        super(CoreExceptionCode.MAPPER_ERROR, string);
    }

    public MapperException(String string, Exception e) {
        super(CoreExceptionCode.MAPPER_ERROR, string, e);
    }

    public MapperException(Exception e) {
        super(e);
    }
}
