package au.org.noojee.contact.api;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import au.org.noojee.contact.api.NoojeeContactProtocalImpl.HTTPMethod;

public class NoojeeContactApi
{
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger();

	private String fqdn;
	private String authToken;
	
	public NoojeeContactApi(String fqdn, String authToken)
	{
		this.fqdn = fqdn;
		this.authToken = authToken;
		NoojeeContactProtocalImpl.init();
	}


	public NoojeeContactStatistics getStatistics()
			throws NoojeeContactApiException
	{
		NoojeeContactStatistics status = null;

		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		URL url = gateway.generateURL(fqdn, "systemHealth/test", authToken, null);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		status = GsonForNoojeeContact.fromJson(response.getResponseBody(), NoojeeContactStatistics.class);

		return status;
	}

	/**
	 * Returns a list of shifts that are active as of now.
	 * 
	 * @param fqdn
	 * @param apiKey
	 * @param team 
	 * @return
	 * @throws NoojeeContactApiException
	 */
	public List<Shift> getActiveShifts(String team)
				throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		URL url = gateway.generateURL(fqdn, "rosterApi/getActiveRosters", authToken, "teamName=" + team);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);


		Type type = new TypeToken<Response<Shift>>()
		{
		}.getType();
		
		
		Response<Shift> gsonResponse = GsonForNoojeeContact.fromJsonTypedObject(response.getResponseBody(), type);
		
		if (gsonResponse.getCode() != 0)
			throw new NoojeeContactApiException(gsonResponse.getCode(), gsonResponse.getMessage());

		return gsonResponse.getList();
	}
	
	class Response<E>
	{
		String type;
		int code;
		String message;
		
		List<E> entities;

		public List<E> getList()
		{
			return entities;
		}

		public String getMessage()
		{
			return message;
		}

		public int getCode()
		{
			return code;
		}
	}



}
