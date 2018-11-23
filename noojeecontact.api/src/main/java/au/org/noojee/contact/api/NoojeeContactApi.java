package au.org.noojee.contact.api;

import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import au.org.noojee.contact.api.NoojeeContactProtocalImpl.HTTPMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class NoojeeContactApi
{
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger();

	private String fqdn;
	private String authToken;
	
	// used by subscribe to avoid missing events.
	private long lastSequenceNo = 0;

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

	public DialResponse dial(PhoneNumber phoneNumber, EndPoint endPoint, String phoneCaption, AutoAnswer autoAnswer,
			PhoneNumber clid, boolean recordCall, String tagCall)
			throws NoojeeContactApiException
	{

		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "number=" + phoneNumber.compactString()
				+ "&extenOrUniqueId=" + endPoint.compactString()
				+ "&callerId=" + clid.compactString()
				+ "&phoneCaption=" + phoneCaption
				+ "&autoAnswer=" + autoAnswer.getHeader();

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/dial", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null, "application/x-www-form-urlencoded");

		DialResponse dialResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), DialResponse.class);

		return dialResponse;
	}

	public SimpleResponse hangup(UniqueCallId uniqueCallId)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + uniqueCallId.toString();

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/hangup", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse answer(UniqueCallId uniqueCallId, EndPoint endPoint, AutoAnswer autoAnswer)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "uniqueId=" + uniqueCallId.toString()
				+ "&exten=" + endPoint.compactString()
				+ "&answerString" + autoAnswer.getHeader();

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/answer", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse startRecording(UniqueCallId uniqueCallId, String username, String tag)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + uniqueCallId.toString()
				+ "&tag" + tag
				+ "&agentLoginName=" + username;

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/start", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse startRecording(EndPoint endPoint, String username, String tag)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + endPoint.compactString()
				+ "&tag" + tag
				+ "&agentLoginName=" + username;

		// agentLoginName?

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/start", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse stopRecording(UniqueCallId uniqueCallId, String username)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + uniqueCallId.toString()
				+ "&agentLoginName=" + username;

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/stop", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse stopRecording(EndPoint endPoint, String username)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		// agentLoginName?
		String query = "extenOrUniqueId=" + endPoint.compactString()
				+ "&agentLoginName=" + username;

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/stop", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SubscribeResponse subscribe(List<EndPoint> endPoints)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String extensions = "";
		
		for (EndPoint endPoint : endPoints)
		{
			if (extensions.length() > 0)
				extensions += ",";
			extensions += endPoint.compactStringNoTech();
		}
		
		String query = "exten=" + extensions
				+ "&lastSequenceNumber=" + lastSequenceNo
				+ "&timeOut=30";

		URL url = gateway.generateURL(fqdn, "CallManagementAPI/subscribe", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null);

		SubscribeResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SubscribeResponse.class);

		return hangupResponse;
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

	class SimpleResponse
	{
		String Message;
		int Code;
	}

	class DialResponse
	{
		String SessionID;
		String Message;
		int Code;
	}

	class Response<E>
	{
		String type;
		int code;
		String message;

		@SuppressFBWarnings
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
	
	public class CallData
	{
		boolean canAnswer;
		String callerId;
		UniqueCallId uniqueCallId;
		LocalDateTime callStartTime;
		boolean isQueueCall;
		boolean isClickToDialCall;
		boolean inbound;
		EndPointStatus status;
		
		public EndPointStatus getStatus()
		{
			return status;
		}
		
	}
	
	public class Event
	{
		CallData CallData;
		int CallID;
		int Code;
		public EndPointStatus getStatus()
		{
			return CallData.getStatus();
		}
	}
	
	public class SubscribeResponse
	{
		@SerializedName("Data")
		HashMap<String, Event> events;
		
		int seq;
		int Code;
		public List<EndPointEvent> getEvents()
		{
			List<EndPointEvent> endPointEvents = new ArrayList<>();
			
			for (String extensionNo : events.keySet())
			{
				EndPointEvent endPointEvent = new EndPointEvent(extensionNo, events.get(extensionNo));
				endPointEvents.add(endPointEvent);
			}
			
			return endPointEvents;
			
		}
	}

}
