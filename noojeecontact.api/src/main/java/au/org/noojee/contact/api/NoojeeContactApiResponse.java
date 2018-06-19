package au.org.noojee.contact.api;

import java.io.Serializable;

public class NoojeeContactApiResponse implements Serializable
{
	private static final long serialVersionUID = 1L;
	private int responseCode;
	private String responseText;

	private boolean successful = false;
	private String httpResponseBody; 
	
	// When the response is for a payment transaction then this will hold the resulting transaction id.
	private String transactionID;

	public NoojeeContactApiResponse(int responseCode, String responseText, String httpResponseBody)
	{
		this.responseCode = responseCode;
		this.responseText = responseText;
		this.httpResponseBody = httpResponseBody;
		
	}

	public void setSuccessful()
	{
		this.successful = true;
	}

	public String getTransactionID()
	{
		return transactionID;
	}
	
	public int getResponseCode()
	{
		return responseCode;
	}

	public String getResponseText()
	{
		return responseText;
	}

	public boolean isSuccessful()
	{
		return successful;
	}
	
	public String getHTTPResponseBody()
	{
		return httpResponseBody;
	}
	
	@Override
	public String toString()
	{
		return "Code: " + responseCode + " Message: " + responseText;
	}

	public void setTransactionID(String transactionID)
	{
		this.transactionID = transactionID;
		
	}

}
