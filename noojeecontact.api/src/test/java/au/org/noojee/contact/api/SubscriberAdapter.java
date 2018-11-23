package au.org.noojee.contact.api;

import java.util.List;

public abstract class SubscriberAdapter implements Subscriber
{

	

	@Override
	public void dialing(EndPoint endPoint, EndPointEvent event)
	{

	}

	@Override
	public void ringing(EndPoint endPoint, EndPointEvent event)
	{

	}

	@Override
	public void answered(EndPoint endPoint, EndPointEvent event)
	{
	}

	@Override
	public void hungup(EndPoint endPoint, EndPointEvent event)
	{

	}


	@Override
	public void onError(NoojeeContactApiException e)
	{
	}


}
