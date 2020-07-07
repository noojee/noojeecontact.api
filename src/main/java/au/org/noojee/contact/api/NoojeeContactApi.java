package au.org.noojee.contact.api;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import au.org.noojee.api.enums.Protocol;
import au.org.noojee.contact.api.NoojeeContactProtocalImpl.HTTPMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class NoojeeContactApi
{
	private Logger logger = LogManager.getLogger();

	private String fqdn;
	private String authToken;
	private Protocol protocol;

	public NoojeeContactApi(String fqdn, String authToken)
	{
		this(fqdn, authToken, Protocol.HTTPS);
	}

	public NoojeeContactApi(String fqdn, String authToken, Protocol protocol)
	{
		this.fqdn = fqdn;
		this.authToken = authToken;
		this.protocol = protocol;
		NoojeeContactProtocalImpl.init();
	}

	public NoojeeContactStatistics getStatistics() throws NoojeeContactApiException
	{
		NoojeeContactStatistics status = null;

		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		URL url = gateway.generateURL(protocol, fqdn, "systemHealth/test", authToken, null);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		status = GsonForNoojeeContact.fromJson(response.getResponseBody(), NoojeeContactStatistics.class);

		return status;
	}

	public DialResponse dial(NJPhoneNumber phoneNumber, EndPoint endPoint, String phoneCaption, AutoAnswer autoAnswer,
			NJPhoneNumber clid, boolean recordCall, String tagCall) throws NoojeeContactApiException
	{

		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "number=" + phoneNumber.compactString() + "&extenOrUniqueId=" + endPoint.compactString()
				+ "&callerId=" + clid.compactString() + "&phoneCaption=" + getEncoded(phoneCaption) + "&autoAnswer="
				+ autoAnswer.getEncodedHeader();

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/dial", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null, "application/x-www-form-urlencoded");

		DialResponse dialResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), DialResponse.class);

		return dialResponse;
	}

	public DialResponse internalDial(EndPoint DialedEndPoint, EndPoint DialingEndPoint, String phoneCaption,
			AutoAnswer autoAnswer, EndPoint clid, boolean recordCall, String tagCall) throws NoojeeContactApiException
	{

		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "number=" + DialedEndPoint.compactStringNoTech() + "&extenOrUniqueId="
				+ DialingEndPoint.compactString() + "&callerId=" + clid.compactStringNoTech() + "&phoneCaption="
				+ getEncoded(phoneCaption) + "&autoAnswer=" + autoAnswer.getEncodedHeader();

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/dial", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null, "application/x-www-form-urlencoded");

		DialResponse dialResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), DialResponse.class);

		return dialResponse;

	}

	public SimpleResponse hangup(UniqueCallId uniqueCallId) throws NoojeeContactApiException
	{
		return hangup(uniqueCallId.toString());
	}

	public SimpleResponse hangup(EndPoint endPoint) throws NoojeeContactApiException
	{
		return hangup(endPoint.extensionNo);

	}

	public SimpleResponse hangup(String extenOrUniqueId) throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + extenOrUniqueId;

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/hangup", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null, "application/x-www-form-urlencoded");

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		logger.debug("hangup for " + extenOrUniqueId + " result : " + hangupResponse.Code + " Message: "
				+ hangupResponse.Message);
		return hangupResponse;
	}

	public SimpleResponse answer(UniqueCallId uniqueCallId, EndPoint endPoint, AutoAnswer autoAnswer)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "uniqueId=" + uniqueCallId.toString() + "&exten=" + endPoint.compactString() + "&answerString="
				+ autoAnswer.getEncodedHeader();

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/answer", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null, "application/x-www-form-urlencoded");

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse startRecording(UniqueCallId uniqueCallId, String username, String tag)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + uniqueCallId.toString() + "&tag" + tag + "&agentLoginName=" + username;

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/start", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse startRecording(EndPoint endPoint, String username, String tag)
			throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + endPoint.compactString() + "&tag" + tag + "&agentLoginName=" + username;

		// agentLoginName?

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/start", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse stopRecording(UniqueCallId uniqueCallId, String username) throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		String query = "extenOrUniqueId=" + uniqueCallId.toString() + "&agentLoginName=" + username;

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/stop", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SimpleResponse stopRecording(EndPoint endPoint, String username) throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		// agentLoginName?
		String query = "extenOrUniqueId=" + endPoint.compactString() + "&agentLoginName=" + username;

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/stop", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		SimpleResponse hangupResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(), SimpleResponse.class);

		return hangupResponse;
	}

	public SubscribeResponse subscribe(List<EndPoint> endPoints, long sequenceNo, int timeout, String debugArg)
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

		String query = "exten=" + extensions + "&lastSequenceNumber=" + sequenceNo + "&timeOut=" + timeout
				+ "&xDebugArg=" + debugArg;

		URL url = gateway.generateURL(protocol, fqdn, "CallManagementAPI/subscribe", authToken, query);

		HTTPResponse response = gateway.request(HTTPMethod.POST, url, null, "application/x-www-form-urlencoded");

		logger.debug("Subscribe response {}", response.getResponseBody());

		SubscribeResponse subscribeResponse = GsonForNoojeeContact.fromJson(response.getResponseBody(),
				SubscribeResponse.class);

		return subscribeResponse;
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
	public List<Shift> getActiveShifts(String team) throws NoojeeContactApiException
	{
		NoojeeContactProtocalImpl gateway = NoojeeContactProtocalImpl.getInstance();

		URL url = gateway.generateURL(protocol, fqdn, "rosterApi/getActiveRosters", authToken, "teamName=" + team);

		HTTPResponse response = gateway.request(HTTPMethod.GET, url, null);

		Type type = new TypeToken<Response<Shift>>()
		{
		}.getType();

		Response<Shift> gsonResponse = GsonForNoojeeContact.fromJsonTypedObject(response.getResponseBody(), type);

		if (gsonResponse.getCode() != 0)
			throw new NoojeeContactApiException(gsonResponse.getCode(), gsonResponse.getMessage());

		return gsonResponse.getList();
	}

	public class SimpleResponse
	{
		private String Message;
		private int Code;

		public boolean wasSuccessful()
		{
			return Code == 0;
		}

		public String getMessage()
		{
			return Message;
		}
	}

	public class DialResponse
	{
		public final String SessionID;
		public final String Message;
		public final int Code;

		public DialResponse(String sessionID, String message, int code)
		{
			super();
			SessionID = sessionID;
			Message = message;
			Code = code;
		}

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
		private boolean canAnswer;
		private String callerId;
		/**
		 * Unique id of the primary channel - the end point (normally an extension) that the call is originated from.
		 */
		@SerializedName(value = "uniqueCallId")
		private UniqueCallId primaryUniqueCallId;

		// when a call starts 'ringing' or is answered the 'ringing' and 'connected'
		// events are generated and this field
		// will
		// then contain the uniqueCallid of the 2channel (usually the remote phone
		// number that we are dialing)
		private UniqueCallId secondaryUniqueCallId;
		private LocalDateTime callStartTime;
		private boolean isQueueCall;
		private boolean isClickToDialCall;
		private boolean inbound;
		private EndPointStatus status;

		public EndPointStatus getStatus()
		{
			return status;
		}

		public UniqueCallId getPrimaryUniqueCallId()
		{
			return primaryUniqueCallId;
		}

		public UniqueCallId getSecondaryUniqueCallId()
		{
			return secondaryUniqueCallId;
		}

		/**
		 * @return the canAnswer
		 */
		public boolean isCanAnswer()
		{
			return canAnswer;
		}

		/**
		 * @return the callerId
		 */
		public String getCallerId()
		{
			return callerId;
		}

		/**
		 * @return the callStartTime
		 */
		public LocalDateTime getCallStartTime()
		{
			return callStartTime;
		}

		/**
		 * @return the isQueueCall
		 */
		public boolean isQueueCall()
		{
			return isQueueCall;
		}

		/**
		 * @return the isClickToDialCall
		 */
		public boolean isClickToDialCall()
		{
			return isClickToDialCall;
		}

		/**
		 * @return the inbound
		 */
		public boolean isInbound()
		{
			return inbound;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "CallData [canAnswer=" + canAnswer + ", callerId=" + callerId + ", primaryUniqueCallId="
					+ primaryUniqueCallId + ", secondaryUniqueCallId=" + secondaryUniqueCallId + ", callStartTime="
					+ callStartTime + ", isQueueCall=" + isQueueCall + ", isClickToDialCall=" + isClickToDialCall
					+ ", inbound=" + inbound + ", status=" + status + "]";
		}

	}

	public class Event
	{
		@SerializedName(value = "CallData")
		CallData callData;
		@SerializedName(value = "CallID")
		int callID;
		@SerializedName(value = "Code")
		int code;

		public EndPointStatus getStatus()
		{

			return (callData != null ? callData.getStatus() : EndPointStatus.Unknown);
		}

		NJPhoneNumber getCallerId()
		{
			return new NJPhoneNumber(callData.callerId);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "Event [CallData=" + callData + ", CallID=" + callID + ", Code=" + code + "]";
		}

	}

	public class SubscribeResponse
	{
		// {"Data":{"115":[{"CallData":{"callerId":"106","uniqueCallId":"1543022361.57","callStartTime":"","isQueueCall":false,"isClickToDialCall":false,"inbound":false,"status":"Dialing
		// Out","otherPartyCallerId":"106","connectedCallerIdNum":"","outboundDestination":"106"},"CallID":27,"Code":0}]},"seq":114,"Code":0}

		@SerializedName("Data")
		HashMap<String, List<Event>> endPointEventMap;

		long seq;
		int Code;

		public List<EndPointEvent> getEvents()
		{
			List<EndPointEvent> allEvents = new ArrayList<>();

			if (endPointEventMap != null)
			{

				for (String extensionNo : endPointEventMap.keySet())
				{
					List<Event> endPointEvents = endPointEventMap.get(extensionNo);

					for (Event event : endPointEvents)
					{
						EndPointEvent endPointEvent = new EndPointEvent(extensionNo, event);
						allEvents.add(endPointEvent);
					}
				}
			}

			return allEvents;

		}
	}

	public String getEncoded(String value)
	{
		String encoded = value;
		try
		{
			encoded = URLEncoder.encode(value, "UTF-8");
		}
		catch (UnsupportedEncodingException e1)
		{
			// won't happen
		}

		return encoded;

	}

}
