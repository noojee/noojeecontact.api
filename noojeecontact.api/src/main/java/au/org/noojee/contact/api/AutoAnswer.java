package au.org.noojee.contact.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public enum AutoAnswer
{

	Yealink("Call-info:answer-after=0")
	, Grandstream("Call-Info:\\\\; answer-after=0")
	, Snom("Call-Info:\\\\; answer-after=0")
	, Polycom("Alert-Info: Ring Answer")
	, Aastra("Call-Info:\\\\; answer-after=0")
	, Linksys("Call-Info:\\\\; answer-after=0");

	
	String header;
	
	AutoAnswer(String header)
	{
		this.header = header;
	}
	
	String getHeader()
	{
		return this.header;
	}

	public String getEncodedHeader()
	{
		String encodedHeader = header;
		try
		{
			encodedHeader =  URLEncoder.encode(this.header, "UTF-8");
		}
		catch (UnsupportedEncodingException e1)
		{
			// won't happen
		}
		
		return encodedHeader;

	}
	

}
