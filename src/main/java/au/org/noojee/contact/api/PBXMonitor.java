package au.org.noojee.contact.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.org.noojee.api.enums.Protocol;
import au.org.noojee.contact.api.NoojeeContactApi.DialResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SimpleResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SubscribeResponse;

/**
 * A wrapper for the subscribe REST end point which allows a user to monitor
 * activity for a particular extension.
 * 
 * @author bsutton
 */
public enum PBXMonitor
{
	SELF;

	private final Logger logger = LogManager.getLogger();

	private NoojeeContactApi api;

	private final AtomicLong seqenceNo = new AtomicLong();

	/**
	 * Each EndPoint can have multiple subscribers.
	 */
	private final Map<EndPointWrapper, List<Subscriber>> subscriptions = new ConcurrentHashMap<>();

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final ExecutorService subscriptionLoopPool = Executors.newFixedThreadPool(10);

	private final ExecutorService subscriberCallbackPool = Executors.newFixedThreadPool(1);

	private PBXMonitor()
	{
		logger.info("Starting PBX Monitor");
	}

	/**
	 * Start the activity monitor. You will normally start the activity monitor
	 * as soon as your application starts and leave it running until your
	 * applications shutsdown. If the activity monitor is disconnected from PBX
	 * it will automatically reconnect however some events will have been lost.
	 * 
	 * @throws NoojeeContactApiException
	 */
	synchronized public void start(NoojeeContactApi api) throws NoojeeContactApiException
	{

		if (running.get() == true)
			throw new IllegalStateException("The PBXMonitor is already running.");

		this.api = api;

		running.set(true);

		new Thread(() -> mainSubscribeLoop("PBXMonitor-start()"), "PBXMonitor:mainSubscribeLoop").start();
	}

	public void start(String fqdn, String apiToken) throws NoojeeContactApiException
	{
		start(fqdn, apiToken, Protocol.HTTPS);
	}

	public void start(String fqdn, String apiToken, Protocol protocol) throws NoojeeContactApiException
	{
		start(new NoojeeContactApi(fqdn, apiToken, protocol));
	}

	synchronized public void stop()
	{
		logger.info("Stopping PBXMonitor.");
		running.set(false);
		api = null;
	}

	synchronized public void subscribe(EndPoint endPoint, Subscriber subscriber, String source)
	{
		subscribe(subscriber, source, endPoint);
	}

	synchronized public void subscribe(Subscriber subscriber, String source, EndPoint... endPoints)
	{
		if (!running.get())
		{
			IllegalStateException e = new IllegalStateException(
					"The Montior is not running. Call " + this.name() + ".start()");

			logger.error(e, e);
			throw e;
		}

		List<EndPointWrapper> oneOffSubscription = new ArrayList<>();

		for (EndPoint endPoint : endPoints)
		{

			EndPointWrapper wrapper = new EndPointWrapper(endPoint);
			if (subscriptions.containsKey(wrapper))
			{
				List<Subscriber> subscribers = subscriptions.get(wrapper);

				if (subscribers.contains(subscriber))
					throw new IllegalStateException("The passed subscriber is already subscribed");

				subscribers.add(subscriber);
			}
			else
			{
				logger.warn("Adding new EndPoint " + endPoint);
				// new subscriber
				List<Subscriber> subscribers = new ArrayList<>();
				subscribers.add(subscriber);
				subscriptions.put(wrapper, subscribers);

				oneOffSubscription.add(wrapper);
			}
			logger.debug("Added subscription for: " + endPoint.extensionNo);

		}

		synchronized (mainLoopLatch)
		{
			// wake the main loop in case it has gone to sleep.
			mainLoopLatch.countDown();
		}

		if (!oneOffSubscription.isEmpty())
		{
			// We found a new end point so we do a one off subscription.
			// This allows the subscriber to get immediate subscriptions without
			// waiting for the next subscribeLoop (takes 30 seconds).
			// On the next subscribe loop these new end points will be include
			// hence we only need to do this once.
			logger.debug("Triggering one off subscription. on Thread " + Thread.currentThread().getId());

			// we are using a limited pool which could become a bottle neck if a
			// lot of
			// new handsets are subscribed simultaneously (via separate
			// subscribe calls).
			subscriptionLoopPool.submit(() -> shortSubscribeLoop(oneOffSubscription, source));
		}
	}

	synchronized public void unsubscribe(Subscriber subscriber)
	{
		// unsubscribe from all end points.
		for (Entry<EndPointWrapper, List<Subscriber>> entry : subscriptions.entrySet())
		{
			List<Subscriber> subscribers = entry.getValue();
			entry.getValue().remove(subscriber);
			if (subscribers.isEmpty())
			{
				logger.warn("Removing EndPoint " + entry.getKey());
				// no more subscribers for this end point so remove the end
				// point.
				subscriptions.remove(entry.getKey());
			}
		}
	}

	/**
	 * Used to satisfy a new subscription until the main poll loop returns and
	 * can take over.
	 * 
	 * @param endPoints
	 * @return
	 */
	private void shortSubscribeLoop(List<EndPointWrapper> endPoints, String source)
	{
		try
		{
			logger.warn("shortSubscribeLoop is starting for "
					+ endPoints.stream().map(e -> e.getExtensionNo()).collect(Collectors.joining(",")));

			do
			{
				// subscribe to the list of end points.
				_subscribe(endPoints, "short+" + source);
			}
			while (running.get() && isShortLoopStillRequired(endPoints));
		}
		catch (Throwable e)
		{
			logger.error(e, e);
		}
		finally
		{
			logger.info("shortSubscribeLoop is exiting");
		}
	}

	// use synchronized to ensure visibility of servicedByMainLoop
	synchronized private boolean isShortLoopStillRequired(List<EndPointWrapper> endPoints)
	{
		// first check that short loop is still needed.
		boolean required = false;
		for (EndPointWrapper wrapper : endPoints)
		{
			if (wrapper.servicedByMainLoop == false)
			{
				required = true;
				break;
			}
		}
		if (!required)
		{
			logger.info("ShortSubscribeLoop no longer required on Thread " + Thread.currentThread().getId());
		}
		return required;
	}

	private CountDownLatch mainLoopLatch = new CountDownLatch(1);

	private Void mainSubscribeLoop(String source)
	{

		try
		{
			logger.debug("#######################################################");
			logger.debug("mainSubscribeLoop is starting");
			logger.debug("#######################################################");

			while (running.get())
			{
				// subscribe to the list of end points.
				if (subscriptions.isEmpty())
				{
					logger.debug("mainSubscribeLoop sleeping as no endPoints to monitor");
					synchronized (mainLoopLatch)
					{
						mainLoopLatch = new CountDownLatch(1);
						mainLoopLatch.await(30, TimeUnit.SECONDS);
					}
				}
				else
				{
					markAllEndPoints();
					_subscribe(subscriptions.keySet(), "main+" + source);
				}

				// logger.error("Looping on Thread " +
				// Thread.currentThread().getId());

			}
		}
		catch (Throwable e)
		{
			logger.error(e, e);
		}

		finally
		{
			logger.debug("mainSubscribeLoop is exiting");
			if (running.get() == true)
			{
				// looks like we terminated abnormally so restart the loop.
				logger.warn("#######################################################");
				logger.warn("mainSubscribeLoop is restarting after abnormal termination");
				logger.warn("#######################################################");

				subscriptionLoopPool.submit(() -> mainSubscribeLoop(source));
			}
		}

		return null;
	}

	private void _subscribe(Collection<EndPointWrapper> endPoints, String debugArg)
	{
		try
		{
			// logger.error("http subscribe request sent for "
			// + endPoints.stream().map(e ->
			// e.getExtensionNo()).collect(Collectors.joining(",")) + " on
			// Thread"
			// + Thread.currentThread().getId());

			SubscribeResponse response = api.subscribe(
					endPoints.stream().map(w -> w.getEndPoint()).collect(Collectors.toList()), seqenceNo.get(), 30,
					debugArg);

			if (response != null)
			{

				// update the sequence no. from the response so we don't miss
				// any
				// data.
				seqenceNo.set(response.seq);

				// logger.error("http subscribe response recieved for "
				// + endPoints.stream().map(e ->
				// e.getExtensionNo()).collect(Collectors.joining(",")) + " on
				// Thread"
				// + Thread.currentThread().getId());

				List<EndPointEvent> events = response.getEvents();

				for (EndPointEvent event : events)
				{
					// logger.error("Event: " + event);
					EndPoint endPoint = event.getEndPoint();

					if (event.getStatus() == null)
					{
						logger.warn("_subscribe event status == null {}", event);
						continue;
					}

					switch (event.getStatus())
					{
					// we use a thread pool (of 1) to do the callbacks as the
					// doSubscribe thread can be
					// interrupted but we don't want end user code to be
					// interrupted
					// in unexpected manner.
					// The thread pool isolates the user code from our problems
					// when
					// we get cancelled.
					case Connected:
						subscriberCallbackPool.execute(() -> notifiyConnected(endPoint, event));
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
			else
			{
				logger.error("Subscribe returned null. PBX is probably down for maintenance");
			}
		}
		catch (NoojeeContactApiException e)
		{
			logger.error(e, e);
			notifyError(e);

			try
			{
				// Likely a network error so sleep a bit and hope it recovers.
				Thread.sleep(5000);
			}
			catch (InterruptedException e1)
			{
				logger.error(e1);
			}
		}

	}

	private List<Subscriber> getCopyOfSubscribers(EndPoint endPoint)
	{
		EndPointWrapper wrapper = new EndPointWrapper(endPoint);
		List<Subscriber> result = new LinkedList<>();
		result.addAll(subscriptions.computeIfAbsent(wrapper, w -> new ArrayList<>()));
		return result;
	}

	/*
	 * Mark all current subscriptions as serviced by the main loop
	 */
	// use synchronized to ensure visibility of servicedByMainLoop
	synchronized private void markAllEndPoints()
	{
		// We need to mark the end points as now being managed by the
		// main subscribe loop so any short term subscribe loops can shut
		// down.
		Set<EndPointWrapper> subscribed = subscriptions.keySet();
		for (EndPointWrapper wrapper : subscribed)
		{
			wrapper.servicedByMainLoop = true;
		}
	}

	private List<Subscriber> getCopyOfAllSubscribers()
	{
		List<Subscriber> subscribers = new ArrayList<>();
		for (Entry<EndPointWrapper, List<Subscriber>> entry : subscriptions.entrySet())
		{
			subscribers.addAll(entry.getValue());
		}
		return subscribers;

	}

	void safeRun(Runnable runnable)
	{
		try
		{
			runnable.run();
		}
		catch (Throwable e2)
		{
			logger.error(e2, e2);
		}
	}

	private void notifyError(NoojeeContactApiException e)
	{
		List<Subscriber> subscribers = getCopyOfAllSubscribers();

		subscribers.forEach(subscriber -> {
			safeRun(() -> subscriber.onError(e));
		});
	}

	private void notifyHangup(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber -> {
			safeRun(() -> subscriber.hungup(event));
		});
	}

	private void notifyDialing(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);
		subscribers.forEach(subscriber -> {
			safeRun(() -> subscriber.dialing(event));
		});
	}

	private void notifyRinging(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);
		subscribers.forEach(subscriber -> {
			safeRun(() -> subscriber.ringing(event));
		});
	}

	private void notifiyConnected(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);
		subscribers.forEach(subscriber -> {
			safeRun(() -> subscriber.connected(event));
		});
	}

	private static class EndPointWrapper
	{
		EndPoint endPoint;
		boolean servicedByMainLoop = false;

		public EndPointWrapper(EndPoint endPoint)
		{
			this.endPoint = endPoint;
		}

		public EndPoint getEndPoint()
		{
			return this.endPoint;
		}

		public String getExtensionNo()
		{
			return this.endPoint.extensionNo;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((endPoint == null) ? 0 : endPoint.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EndPointWrapper other = (EndPointWrapper) obj;
			if (endPoint == null)
			{
				if (other.endPoint != null)
					return false;
			}
			else if (!endPoint.equals(other.endPoint))
				return false;
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "EndPointWrapper [endPoint=" + endPoint + ", servicedByMainLoop=" + servicedByMainLoop + "]";
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

	public DialResponse dial(NJPhoneNumber phoneNumber, EndPoint endPoint, String phoneCaption, AutoAnswer autoAnswer,
			NJPhoneNumber clid, boolean recordCall, String tagCall) throws NoojeeContactApiException
	{
		return api.dial(phoneNumber, endPoint, phoneCaption, autoAnswer, clid, recordCall, tagCall);
	}

	public DialResponse internalDial(EndPoint DialedEndPoint, EndPoint DialingEndPoint, String phoneCaption,
			AutoAnswer autoAnswer, EndPoint clid, boolean recordCall, String tagCall) throws NoojeeContactApiException
	{
		return api.internalDial(DialedEndPoint, DialingEndPoint, phoneCaption, autoAnswer, clid, recordCall, tagCall);
	}

	public SimpleResponse hangup(EndPoint endPoint) throws NoojeeContactApiException
	{
		return api.hangup(endPoint);
	}

}
