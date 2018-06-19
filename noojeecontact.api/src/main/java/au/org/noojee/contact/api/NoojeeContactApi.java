package au.org.noojee.contact.api;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.org.noojee.contact.api.NoojeeContactProtocalImpl.HTTPMethod;

public class NoojeeContactApi
{
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger();

	static private NoojeeContactApi self;

	public static void init()
	{
		self = new NoojeeContactApi();
		NoojeeContactProtocalImpl.init();
	}

	static public NoojeeContactApi getInstance()
	{
		return self;
	}


	public Status getStatus(String fqdn, String apiKey)
			throws NoojeeContactApiException
	{
		Status status = null;

		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		URL url = gateway.generateURL(fqdn, "systemHealth/test", apiKey, null);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		status = GsonForNoojeeContact.fromJson(response.getResponseBody(), Status.class);

		return status;
	}


}
