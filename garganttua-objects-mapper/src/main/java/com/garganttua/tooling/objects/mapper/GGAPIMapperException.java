package com.garganttua.tooling.objects.mapper;

import com.garganttua.api.spec.GGAPICoreException;
import com.garganttua.api.spec.GGAPICoreExceptionCode;

public class GGAPIMapperException extends GGAPICoreException {

	private static final long serialVersionUID = 3629256996026750672L;

	public GGAPIMapperException(GGAPICoreExceptionCode code, String message, Exception exception) {
		super(code, message, exception);
	}
	
	public GGAPIMapperException(GGAPICoreExceptionCode code, String message) {
		super(code, message);
	}
	
	public GGAPIMapperException(Exception exception) {
		super(exception);
	}

}
