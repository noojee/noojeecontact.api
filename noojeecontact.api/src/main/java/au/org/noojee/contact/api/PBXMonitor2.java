package au.org.noojee.contact.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.org.noojee.contact.api.NoojeeContactApi.DialResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SimpleResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SubscribeResponse;

/**
 * A wrapper for the subscribe REST end point which allows a user to monitor activity for a particular extension.
 * 
 * @author bsutton
 */
public enum PBXMonitor2
{
	SELF;

	Logger logger = LogManager.getLogger();

	NoojeeContactApi api;

	/**
	 * Each EndPoint can have multiple subscribers.
	 */
	Map<EndPoint, List<Subscriber>> subscriptions = Collections.synchronizedMap(new HashMap<>());

	private AtomicBoolean running = new AtomicBoolean(false);

	private Future<Void> future;

	private ExecutorService subscriptionLoopPool = Executors.newFixedThreadPool(10);

	private ExecutorService subscriberCallbackPool = Executors.newFixedThreadPool(1);

	private Semaphore semaphore = new Semaphore(1);

	/**
	 * Start the activity monitor. You will normally start the activity monitor as soon as your application starts and
	 * leave it running until your applications shutsdown. If the activity monitor is disconnected from PBX it will
	 * automatically reconnect however some events will have been lost.
	 * 
	 * @throws NoojeeContactApiException
	 */
	synchronized public void start(String fqdn, String apiToken) throws NoojeeContactApiException
	{
		this.api = new NoojeeContactApi(fqdn, apiToken);

		if (running.get() == true)
			throw new IllegalStateException("The PBXMonitor is already running.");

		subscribeLoop();

		running.set(true);
	}

	synchronized public void stop()
	{
		// checkStart();

		try
		{
			semaphore.acquire();
			running.set(false);
			future.cancel(true);
			semaphore.release();

			api = null;

		}
		catch (InterruptedException e)
		{
			logger.error(e, e);
		}
	}

	private void checkStart()
	{
		// if (api == null)
		// throw new IllegalStateException("You must call start() first.");

	}

	synchronized public void subscribe(EndPoint endPoint, Subscriber subscriber) throws NoojeeContactApiException
	{
		subscribe(subscriber, endPoint);
	}

	synchronized public void subscribe(Subscriber subscriber, EndPoint... endPoints) throws NoojeeContactApiException
	{
		if (!running.get())
			throw new IllegalStateException("The Montior is not running. Call " + this.name() + ".start()");

		List<EndPoint> oneOffSubscription = new ArrayList<>();

		for (EndPoint endPoint : endPoints)
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
				// new subsriber
				List<Subscriber> subscribers = new ArrayList<>();
				subscribers.add(subscriber);
				subscriptions.put(endPoint, subscribers);

				oneOffSubscription.add(endPoint);

			}
			logger.error("Added subscription for: " + endPoint.extensionNo);
		}

		if (!oneOffSubscription.isEmpty())
		{
			// We found a new end point so we do a one off subscription.
			// This allows the subscriber to get immediate subscriptions without
			// waiting for the next subscribeLoop (takes 30 seconds).
			// On the next subscribe loop these new end points will be include
			// hence we only need to do this once.
			logger.error("Triggering one off subscription.");
			
			// we are using a limited pool which could become a bottle neck if a lot of
			// new handsets are subscribed simultaneously (via separate subscribe calls).
			subscriptionLoopPool.submit(() -> _subscribe(oneOffSubscription));
		}

	}

	synchronized public void unsubscribe(Subscriber subscriber)
	{
		// unsubscribe from all end points.
		List<EndPoint> endPoints = subscriptions.keySet().stream().collect(Collectors.toList());
		for (EndPoint endPoint : endPoints)
		{
			List<Subscriber> subscribers = subscriptions.get(endPoint);

			if (subscribers.contains(subscriber))
				subscribers.remove(subscriber);

			if (subscribers.isEmpty())
			{
				// no more subscribers for this end point so remove the end point.
				subscriptions.remove(endPoint);
			}
		}
	}

	private void subscribeLoop()
	{
		while (running.get())
		{
			List<EndPoint> endPoints = getCopyOfAllEndPoints();

			// subscribe to the list of end points.
			subscriptionLoopPool.submit(() -> _subscribe(endPoints));

		}
	}

	private void _subscribe(List<EndPoint> endPoints)
	{
		try
		{
			logger.error("http subscribe request sent for " + endPoints.stream().map(e -> e.extensionNo).reduce(", ", String::concat) + " on Thread" + Thread.currentThread().getId());
			
			SubscribeResponse response = api.subscribe(endPoints.stream().collect(Collectors.toList()), 30);
			
			logger.error("http subscribe response recieved for " + endPoints.stream().map(e -> e.extensionNo).reduce(", ", String::concat) + " on Thread" + Thread.currentThread().getId());

			List<EndPointEvent> events = response.getEvents();

			for (EndPointEvent event : events)
			{
				EndPoint endPoint = event.getEndPoint();
				switch (event.getStatus())
				{
					// we use a thread pool (of 1) to do the callbacks as the doSubscribe thread can be
					// interrupted but we don't want end user code to be interrupted in unexpected manner.
					// The thread pool isolates the user code from our problems when we get cancelled.
					case Connected:
						subscriberCallbackPool.execute(() -> notifyAnswer(endPoint, event));
						break;
					case DialingOut:
						subscriberCallbackPool.execute(() -> notifyDialing(endPoint, event));
						break;
					case Hungup:
						subscriberCallbackPool.execute(() -> notifyHangup(endPoint, event));
						break;
					case Ringing:
						subscriberCallbackPool.execute(() -> notifyRinging(endPoint, event));
						break;
					default:
						System.out.println("Unknown EndPoint status: " + event.getStatus().toString());
						break;
				}
			}
		}
		catch (NoojeeContactApiException e)
		{
			logger.error(e, e);
			notifyError(e);
		}

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

	/*
	 * Get all of the end points that we have subscriptions on.
	 */
	private List<EndPoint> getCopyOfAllEndPoints()
	{
		List<EndPoint> endPoints = new ArrayList<>();

		synchronized (subscriptions)
		{

			endPoints.addAll(subscriptions.keySet());
		}
		return endPoints;

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

	private void notifyError(NoojeeContactApiException e)
	{
		try
		{
			List<Subscriber> subscribers = getCopyOfAllSubscribers();

			subscribers.forEach(subscriber -> subscriber.onError(e));
		}
		catch (Throwable e2)
		{
			logger.error(e, e2);

		}

	}

	private void notifyHangup(EndPoint endPoint, EndPointEvent event)
	{
		try
		{
			List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

			subscribers.forEach(subscriber ->
				{
					subscriber.hungup(event);
				});
		}
		catch (Throwable e)
		{
			logger.error(e, e);

		}

	}

	private void notifyDialing(EndPoint endPoint, EndPointEvent event)
	{
		try
		{
			List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

			subscribers.forEach(subscriber ->
				{
					subscriber.dialing(event);
				});
		}
		catch (Throwable e)
		{
			logger.error(e, e);

		}

	}

	private void notifyRinging(EndPoint endPoint, EndPointEvent event)
	{
		try
		{
			List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

			subscribers.forEach(subscriber ->
				{
					subscriber.ringing(event);
				});
		}
		catch (Throwable e)
		{
			logger.error(e, e);
		}
	}

	private void notifyAnswer(EndPoint endPoint, EndPointEvent event)
	{
		try
		{
			List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

			subscribers.forEach(subscriber ->
				{
					subscriber.answered(event);
				});
		}
		catch (Throwable e)
		{
			logger.error(e, e);

		}
	}

	/**
	 * convenience methods as the Monitor wraps the api.
	 * 
	 * @param uniqueCallId
	 * @return
	 * @throws NoojeeContactApiException
	 */
	public SimpleResponse hangup(UniqueCallId uniqueCallId) throws NoojeeContactApiException
	{
		return api.hangup(uniqueCallId);
	}

	public DialResponse dial(PhoneNumber phoneNumber, EndPoint endPoint, String phoneCaption, AutoAnswer autoAnswer,
			PhoneNumber clid, boolean recordCall, String tagCall) throws NoojeeContactApiException
	{
		return api.dial(phoneNumber, endPoint, phoneCaption, autoAnswer, clid, recordCall, tagCall);
	}
}
