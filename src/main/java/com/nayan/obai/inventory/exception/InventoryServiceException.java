package com.nayan.obai.inventory.exception;

public class InventoryServiceException extends RuntimeException
{
	public InventoryServiceException()
	{
		super("Resource not found");
	}

	public InventoryServiceException(String message)
	{
		super(message);
	}
}
