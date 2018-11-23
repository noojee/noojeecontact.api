package au.org.noojee.contact.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
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

	/**
	 * Each EndPoint can have multiple subscribers.
	 */
	Map<EndPoint, List<Subscriber>> subscriptions = Collections.synchronizedMap(new HashMap<>());

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
			Set<EndPoint> endPoints = subscriptions.keySet();

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
							notifyAnswer(endPoint, event);
							break;
						case DialingOut:
							notifyDialing(endPoint, event);
							break;
						case Hungup:
							notifyHangup(endPoint, event);
							break;
						case Ringing:
							notifyRinging(endPoint, event);
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

	void subscribe(EndPoint endPoint, Subscriber subscriber)
	{

		synchronized (subscriptions)
		{
			if (subscriptions.containsKey(endPoint))
			{
				List<Subscriber> subscribers = subscriptions.get(endPoint);

				if (subscribers.contains(subscriber))
					throw new IllegalStateException("The passed subscriber is already subscribed");

				subscribers.add(subscriber);
			}
			else
			{
				List<Subscriber> subscribers = new ArrayList<>();
				subscribers.add(subscriber);
				subscriptions.put(endPoint, subscribers);
			}
		}

	}

	private void notifyError(NoojeeContactApiException e)
	{

		List<Subscriber> subscribers = getCopyOfAllSubscribers();

		subscribers.forEach(subscriber -> subscriber.onError(e));
	}

	private void notifyHangup(EndPoint endPoint, EndPointEvent event)
	{

		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				subscriber.hungup(endPoint, event);
			});
	}

	private List<Subscriber> getCopyOfSubscribers(EndPoint endPoint)
	{
		List<Subscriber> subscribers = null;

		synchronized (subscriptions)
		{
			subscribers = subscriptions.get(endPoint).stream().collect(Collectors.toList());
		}
		return subscribers;

	}

	private List<Subscriber> getCopyOfAllSubscribers()
	{
		List<Subscriber> subscribers = new ArrayList<>();

		synchronized (subscriptions)
		{
			for (EndPoint endPoint : subscriptions.keySet())
			{
				subscribers.addAll(subscriptions.get(endPoint).stream().collect(Collectors.toList()));
			}
		}
		return subscribers;

	}

	private void notifyDialing(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				subscriber.dialing(endPoint, event);
			});
	}

	private void notifyRinging(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				subscriber.ringing(endPoint, event);
			});
	}

	private void notifyAnswer(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				subscriber.answered(endPoint, event);
			});
	}

}
