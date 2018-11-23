package au.org.noojee.contact.api;

import java.util.List;

public interface Subscriber
{

	void reconnected();

	void disconnected();

	boolean isInterested(EndPoint endPoint);

	void hungup(EndPoint endPoint);

	void ringing(EndPoint endPoint);

	void answered(EndPoint endPoint);

	List<EndPoint> getEndPoints();

	void onError(NoojeeContactApiException e);

	void dialing(EndPoint endPoint);

}
