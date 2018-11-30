package au.org.noojee.contact.api;

public abstract class SubscriberAdapter implements Subscriber
{
	@Override
	public void dialing(EndPointEvent event)
	{

	}

	@Override
	public void ringing(EndPointEvent event)
	{

	}

	@Override
	public void connected(EndPointEvent event)
	{
	}

	@Override
	public void hungup(EndPointEvent event)
	{

	}


	@Override
	public void onError(NoojeeContactApiException e)
	{
	}


}
