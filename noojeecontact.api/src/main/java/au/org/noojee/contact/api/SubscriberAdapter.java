package au.org.noojee.contact.api;

<<<<<<< HEAD
public abstract class SubscriberAdapter implements Subscriber
{
=======
import java.util.List;

public abstract class SubscriberAdapter implements Subscriber
{

	
>>>>>>> branch 'master' of git@github.com:noojee/noojeecontact.api.git

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
