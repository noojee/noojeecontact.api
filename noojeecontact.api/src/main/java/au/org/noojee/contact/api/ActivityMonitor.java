package au.org.noojee.contact.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import au.org.noojee.contact.api.NoojeeContactApi.SubscribeResponse;

/**
 * A wrapper for the subscribe REST end point which allows a user to monitor activity for a particular extension.
 * 
 * @author bsutton
 */
public enum ActivityMonitor
{
	SELF;

	NoojeeContactApi api;

	List<Subscriber> subscribers = Collections.synchronizedList(new ArrayList<>());

	private AtomicBoolean running = new AtomicBoolean(false);

	private Thread subscribeThread;

	/**
	 * Start the activity monitor. You will normally start the activity monitor as soon as your application starts and
	 * leave it running until your applications shutsdown. If the activity monitor is disconnected from PBX it will
	 * automatically reconnect however some events will have been lost.
	 * 
	 * @throws NoojeeContactApiException
	 */
	synchronized void start(NoojeeContactApi api) throws NoojeeContactApiException
	{
		this.api = api;

		if (running.get() == true)
			throw new IllegalStateException("The ActivityMonitor is already running.");

		subscribeThread = new Thread(() -> subscribeLoop());
		subscribeThread.start();
	}

	synchronized void stop()
	{
		// checkStart();

		running.set(false);

		// wait for the subscribe loop to stop.
		try
		{
			subscribeThread.join();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		api = null;

	}

	private void checkStart()
	{
		// if (api == null)
		// throw new IllegalStateException("You must call start() first.");

	}

	private void subscribeLoop()
	{
		running.set(true);

		while (running.get() == true)
		{
			Set<EndPoint> endPoints = new HashSet<>();
			
			for (Subscriber subscriber : subscribers)
			{
				endPoints.addAll(subscriber.getEndPoints());
			}

			try
			{
				SubscribeResponse response = api.subscribe(endPoints.stream().collect(Collectors.toList()));
				
				List<EndPointEvent> events = response.getEvents();
				
				for (EndPointEvent event : events)
				{
					EndPoint endPoint = event.getEndPoint();
					switch (event.getStatus())
					{
						case Connected:
							notifyAnswer(endPoint);
							break;
						case DialingOut:
							notifyDialing(endPoint);
							break;
						case Hungup:
							notifyHangup(endPoint);
							break;
						case Ringing:
							notifyRinging(endPoint);
							break;
						default:
							System.out.println("Unknown EndPoint status: " + event.getStatus().toString());
							break;
					}
					
				}
				
				
			}
			catch (NoojeeContactApiException e)
			{
				e.printStackTrace();

				notifyError(e);
			}
		}

	}
	

	private void notifyError(NoojeeContactApiException e)
	{
		subscribers.stream().forEach(subscriber -> subscriber.onError(e));
	}

	void subscribe(Subscriber subscriber)
	{
		subscribers.add(subscriber);
	}

	private void notifyReconnect()
	{
		subscribers.stream().forEach(subscriber -> subscriber.reconnected());
	}

	private void notifyDisconnected()
	{
		subscribers.stream().forEach(subscriber -> subscriber.disconnected());
	}

	private void notifyHangup(EndPoint endPoint)
	{
		subscribers.stream().forEach(subscriber ->
			{
				if (subscriber.isInterested(endPoint))
					subscriber.hungup(endPoint);
			});
	}

	private void notifyDialing(EndPoint endPoint)
	{
		subscribers.stream().forEach(subscriber ->
			{
				if (subscriber.isInterested(endPoint))
					subscriber.dialing(endPoint);
			});
	}

	private void notifyRinging(EndPoint endPoint)
	{
		subscribers.stream().forEach(subscriber ->
			{
				if (subscriber.isInterested(endPoint))
					subscriber.ringing(endPoint);
			});
	}

	private void notifyAnswer(EndPoint endPoint)
	{
		subscribers.stream().forEach(subscriber ->
			{
				if (subscriber.isInterested(endPoint))
					subscriber.answered(endPoint);
			});
	}

}
