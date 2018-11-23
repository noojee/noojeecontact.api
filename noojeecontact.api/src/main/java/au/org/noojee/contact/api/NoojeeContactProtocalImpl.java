package au.org.noojee.contact.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NoojeeContactProtocalImpl
{
	private Logger logger = LogManager.getLogger();
	public static final int PAGE_SIZE = 50;

	private static volatile NoojeeContactProtocalImpl self;
	
	public enum HTTPMethod
	{
		GET, POST, PUT, DELETE
	}

	static synchronized public NoojeeContactProtocalImpl getInstance()
	{
		return NoojeeContactProtocalImpl.self;
	}

	static synchronized public void init()
	{
		if (self == null)
		{
			self = new NoojeeContactProtocalImpl();
		}
	}
	private NoojeeContactProtocalImpl()
	{
		System.setProperty("http.maxConnections", "8"); // set globally only
														// once
	}

	
	public URL generateURL(String fqdn, String entity, String apiKey, String query)
	{
		URL url = null;
		try
		{
			url = new URL("https://" + fqdn + "/servicemanager/rest/" + entity + "?apiKey=" + apiKey + (query != null ? "&" + query : ""));
		}
		catch (MalformedURLException e)
		{
			logger.error(e,e);
		}
		
		return url;
		
	}


	
	HTTPResponse request(HTTPMethod method, URL url, String jsonBody) throws NoojeeContactApiException
	{
		return request(method, url, jsonBody,  "application/json");
	}


	HTTPResponse request(HTTPMethod method, URL url, String jsonBody, String contentType) throws NoojeeContactApiException
	{

		HTTPResponse response;
		response = _request(method, url, jsonBody, contentType);

		if (response.getResponseCode() >= 300)
		{
			throw new NoojeeContactApiException(response.getResponseCode(), response.getResponseBody());
		}

		return response;
	}
	
	
	/**
	 * Returns a raw response string.
	 * 
	 * @throws NoojeeContactApiException
	 */
	private HTTPResponse _request(HTTPMethod method, URL url, String jsonBody, String contentType) throws NoojeeContactApiException
	{
		HTTPResponse response = null;

		try
		{

			logger.debug(method + " url: " + url);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

			connection.setRequestMethod(method.toString());
			connection.setDoOutput(true);
			connection.setAllowUserInteraction(false); // no users here so don't do
														// anything silly.
			
			connection.setConnectTimeout(5000);

			connection.setRequestProperty("Content-Type", contentType + "; charset=UTF-8");
			
			connection.connect();

			// Write the body if any exist.
			if (jsonBody != null)
			{
				logger.debug("jsonBody: " + jsonBody);
				try (OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8"))
				{
					osw.write(jsonBody.toString());
					osw.flush();
					osw.close();
				}

			}

			int responseCode = connection.getResponseCode();

			// 404 returns HTML so no point trying to parse it.
			if (responseCode == 404)
				throw new NoojeeContactApiException("The passed url was not found " + url.toString());

			String body = "";
			String error = "";

			try (InputStream streamBody = connection.getInputStream())
			{
				body = fastStreamReader(streamBody);
			}
			catch (IOException e)
			{
				try (InputStream streamError = connection.getErrorStream())
				{
					error = fastStreamReader(streamError);
				}
			}

			// Read the response.
			if (responseCode < 300)
			{
				// logger.error("Response body" + body);
				response = new HTTPResponse(responseCode, connection.getResponseMessage(), body);
			}
			else
			{

				response = new HTTPResponse(responseCode, connection.getResponseMessage(), error);

				logger.error(response);
				logger.error("EndPoint responsible for error: " + method.toString() + " " + url);
				logger.error("Subumitted body responsible for error: " + jsonBody);
			}
		}
		catch (IOException e)
		{
			throw new NoojeeContactApiException(e);
		}

		return response;

	}

	String fastStreamReader(InputStream inputStream) throws NoojeeContactApiException
	{
		if (inputStream != null)
		{
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[4000];
			int length;
			try
			{
				while ((length = inputStream.read(buffer)) != -1)
				{
					result.write(buffer, 0, length);
				}
				return result.toString(StandardCharsets.UTF_8.name());

			}
			catch (IOException e)
			{
				throw new NoojeeContactApiException(e);
			}

		}
		return "";
	}


}