package au.org.noojee.contact.api;

import java.io.IOException;

public class NoojeeContactApiException extends Exception
{
	private static final long serialVersionUID = 1L;
	private String errorMessage;
	private int errorCode;


	public NoojeeContactApiException(String errorMessage)
	{
		super(errorMessage);
		
		this.errorMessage = errorMessage;
	}

	public NoojeeContactApiException(IOException e)
	{
		super(e);
		this.errorMessage = e.getMessage();
	}

	public NoojeeContactApiException(int errorCode, String errorMessage)
	{
		super(errorMessage + " errorCode:" + errorCode);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	
	int getErrorCode()
	{
		return errorCode;
	}
	
	public String getErrorMessage()
	{
		return errorMessage;
	}

}
