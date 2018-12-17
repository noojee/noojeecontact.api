package au.org.noojee.contact.api;

public interface Subscriber extends AutoCloseable
{
	void dialing(EndPointEvent event);

	void ringing(EndPointEvent event);

	void connected(EndPointEvent event);

	void hungup(EndPointEvent event);

	void onError(NoojeeContactApiException e);

}
