package au.org.noojee.contact.api;

public interface Subscriber
{
	void dialing(EndPointEvent event);

	void ringing(EndPointEvent event);

	void answered(EndPointEvent event);

	void hungup(EndPointEvent event);

	void onError(NoojeeContactApiException e);

}
