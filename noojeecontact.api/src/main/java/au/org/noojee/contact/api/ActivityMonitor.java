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

import au.org.noojee.contact.api.NoojeeContactApi.SubscribeResponse;

/**
 * A wrapper for the subscribe REST end point which allows a user to monitor activity for a particular extension.
 * 
 * @author bsutton
 */
public enum ActivityMonitor
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

	private ExecutorService subscriptionLoopPool = Executors.newFixedThreadPool(2);

	private ExecutorService subscriberCallbackPool = Executors.newFixedThreadPool(1);

	private Semaphore semaphore = new Semaphore(1);

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

		subscribeLoop();

		running.set(true);
	}

	synchronized void stop()
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

	private void subscribeLoop()
	{
		try
		{
			// lock threads out until doSubscribe has a chance to get a copy
			// of 'future'.
			logger.error("Acquiring semaphore id:" + Thread.currentThread().getId());
			
			semaphore.acquire();
			logger.error("Acquired semaphore id:" + Thread.currentThread().getId());
			future = subscriptionLoopPool.submit(() -> doSubscribe());
		}
		catch (InterruptedException e)
		{
		}
	}

	private Void doSubscribe()
	{

		List<EndPoint> endPoints = getCopyOfAllEndPoints();

		// we have our own copy of future so we can let other threads in now.
		Future<Void> myfuture = future;
		semaphore.release();
		logger.error("Released semaphore id:" + Thread.currentThread().getId());

		try
		{
			// subscribe to the list of end points.
			SubscribeResponse response = api.subscribe(endPoints.stream().collect(Collectors.toList()), 5);

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
		finally
		{
			// If our future has been cancelled then we don't try to restart it.
			if (!myfuture.isCancelled())
			{
				logger.error("Resubscribing at end of subscribeLoop: " + Thread.currentThread().getId());
			}
			subscribeLoop();
		}

		return null;
	}

	synchronized void subscribe(EndPoint endPoint, Subscriber subscriber)
	{
		if (!running.get())
			throw new IllegalStateException("The Montior is not running. Call ActivityMonitor.start()");

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

			// We need to replace the current subscribe loop as
			// we now have a new subscription to support.
			if (future != null)
				future.cancel(true);
			logger.error("Starting new subscribeLoop: " + endPoint.extensionNo);
			subscribeLoop();
		}

		logger.error("Added subscription for: " + endPoint.extensionNo);

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
					subscriber.hungup(endPoint, event);
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
					subscriber.dialing(endPoint, event);
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
					subscriber.ringing(endPoint, event);
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
					subscriber.answered(endPoint, event);
				});
		}
		catch (Throwable e)
		{
			logger.error(e, e);

		}
	}

}
