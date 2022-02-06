package com.nextzakir.demospringbootapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
	private static final long serialVersionUID = -2927762559336817363L;

	public ConflictException(String errorMessage) {
		super(errorMessage);
	}
}
