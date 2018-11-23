package au.org.noojee.contact.api;

public interface Subscriber
{
	void dialing(EndPoint endPoint, EndPointEvent event);

	void ringing(EndPoint endPoint, EndPointEvent event);

	void answered(EndPoint endPoint, EndPointEvent event);

	void hungup(EndPoint endPoint, EndPointEvent event);

	void onError(NoojeeContactApiException e);


}
