package com.nayan.obai.inventory.exception;

import com.nayan.obai.inventory.payload.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler
{
	@ExceptionHandler(InventoryServiceException.class)
	public ResponseEntity<ApiResponse> hanldeResourceNotFoundException(final InventoryServiceException e) {
		final String message = e.getMessage();
		final ApiResponse response = ApiResponse.builder().message(message).success(true).status(HttpStatus.NOT_FOUND).build();
		return new ResponseEntity<ApiResponse>(response, HttpStatus.NOT_FOUND);
	}
}
