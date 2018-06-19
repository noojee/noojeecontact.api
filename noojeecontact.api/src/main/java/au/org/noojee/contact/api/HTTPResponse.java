package au.org.noojee.contact.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HTTPResponse
{
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger();
	private int responseCode;
	private String responseMessage;
	private String responseBody;

	public HTTPResponse(int responseCode, String responseMessage, String responseBody)
	{
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
		this.responseBody = responseBody;
	}

	public int getResponseCode()
	{
		return responseCode;
	}

	public String getResponseMessage()
	{
		return responseMessage;
	}

	String getResponseBody()
	{
		return responseBody;
	}

	@Override
	public String toString()
	{
		return "HttpResponse [responseCode=" + responseCode + ", responseMessage=" + responseMessage + ", responseBody="
				+ responseBody + "]";
	}

}