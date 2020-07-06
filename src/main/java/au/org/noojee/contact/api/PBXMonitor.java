package au.org.noojee.contact.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.org.noojee.api.enums.Protocol;
import au.org.noojee.contact.api.NoojeeContactApi.DialResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SimpleResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SubscribeResponse;

/**
 * A wrapper for the subscribe REST end point which allows a user to monitor activity for a particular extension.
 * 
 * @author bsutton
 */
public enum PBXMonitor
{
	SELF;

	Logger logger = LogManager.getLogger();

	NoojeeContactApi api;

	long seqenceNo = 0;

	/**
	 * Each EndPoint can have multiple subscribers.
	 */
	Map<EndPointWrapper, List<Subscriber>> subscriptions = Collections.synchronizedMap(new HashMap<>());

	private AtomicBoolean running = new AtomicBoolean(false);

	private ExecutorService subscriptionLoopPool = Executors.newFixedThreadPool(10);

	private ExecutorService subscriberCallbackPool = Executors.newFixedThreadPool(1);

	PBXMonitor()
	{
		logger.info("Starting PBX Monitor");
		// Throwable trace = new Throwable();
		// logger.error(trace, trace);
	}

	/**
	 * Start the activity monitor. You will normally start the activity monitor as soon as your application starts and
	 * leave it running until your applications shutsdown. If the activity monitor is disconnected from PBX it will
	 * automatically reconnect however some events will have been lost.
	 * 
	 * @throws NoojeeContactApiException
	 */
	synchronized public void start(NoojeeContactApi api) throws NoojeeContactApiException
	{
		this.api = api;

		if (running.get() == true)
			throw new IllegalStateException("The PBXMonitor is already running.");

		running.set(true);

		new Thread(() -> mainSubscribeLoop("PBXMonitor-start()"), "PBXMonitor:mainSubscribeLoop").start();
	}

	
	synchronized public void start(String fqdn, String apiToken) throws NoojeeContactApiException
	{
		start(fqdn, apiToken, Protocol.HTTPS);
	}

	synchronized public void start(String fqdn, String apiToken, Protocol protocol) throws NoojeeContactApiException
	{
		this.api = new NoojeeContactApi(fqdn, apiToken, protocol);

		if (running.get() == true)
			throw new IllegalStateException("The PBXMonitor is already running.");

		running.set(true);

		new Thread(() -> mainSubscribeLoop("PBXMonitor-start()"), "PBXMonitor:mainSubscribeLoop").start();
	}

	synchronized public void stop()
	{
		logger.info("Stopping PBXMonitor.");
		running.set(false);
		api = null;
	}

	// private void checkStart()
	// {
	// // if (api == null)
	// // throw new IllegalStateException("You must call start() first.");
	//
	// }

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
				// new subscriber
				List<Subscriber> subscribers = new ArrayList<>();
				subscribers.add(subscriber);
				subscriptions.put(wrapper, subscribers);

				oneOffSubscription.add(wrapper);
			}
			logger.debug("Added subscription for: " + endPoint.extensionNo);

		}

		if (!oneOffSubscription.isEmpty())
		{
			// We found a new end point so we do a one off subscription.
			// This allows the subscriber to get immediate subscriptions without
			// waiting for the next subscribeLoop (takes 30 seconds).
			// On the next subscribe loop these new end points will be include
			// hence we only need to do this once.
			logger.debug("Triggering one off subscription. on Thread " + Thread.currentThread().getId());

			// we are using a limited pool which could become a bottle neck if a lot of
			// new handsets are subscribed simultaneously (via separate subscribe calls).
			subscriptionLoopPool.submit(() -> shortSubscribeLoop(oneOffSubscription, source));
		}
	}

	synchronized public void unsubscribe(Subscriber subscriber)
	{
		// unsubscribe from all end points.
		List<EndPointWrapper> endPoints = subscriptions.keySet().stream().collect(Collectors.toList());
		for (EndPointWrapper endPoint : endPoints)
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

	/**
	 * Used to satisfy a new subscription until the main poll loop returns and can take over.
	 * 
	 * @param endPoints
	 * @return
	 */
	private Void shortSubscribeLoop(List<EndPointWrapper> endPoints, String source)
	{
		try
		{
			logger.debug("#######################################################");
			logger.debug("shortSubscribeLoop is starting for "
					+ endPoints.stream().map(e -> e.getExtensionNo()).collect(Collectors.joining(",")));
			logger.debug("#######################################################");

			while (running.get())
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
					logger.debug("ShortSubscribeLoop no longer required on Thread " + Thread.currentThread().getId());
					break;
				}

				// subscribe to the list of end points.
				_subscribe(endPoints, "short+" + source);

				// logger.error("ShortSubscribeLoop looping on Thread " + Thread.currentThread().getId());

			}
		}
		catch (Throwable e)
		{
			logger.error(e, e);
		}
		finally
		{
			logger.debug("#######################################################");
			logger.debug("shortSubscribeLoop is exiting");
			logger.debug("#######################################################");

		}

		return null;
	}

	private Void mainSubscribeLoop(String source)
	{

		try
		{
			logger.debug("#######################################################");
			logger.debug("mainSubscribeLoop is starting");
			logger.debug("#######################################################");

			while (running.get())
			{
				List<EndPointWrapper> endPoints = getCopyAndMarkAllEndPoints();

				// subscribe to the list of end points.
				if (endPoints.size() == 0)
				{
					logger.debug("mainSubscribeLoop sleeping as no endPoints to monitor");
					Thread.sleep(30000);
				}
				else
					_subscribe(endPoints, "main+" + source);

				// logger.error("Looping on Thread " + Thread.currentThread().getId());

			}
		}
		catch (Throwable e)
		{
			logger.error(e, e);
		}

		finally
		{
			logger.debug("#######################################################");
			logger.debug("mainSubscribeLoop is exiting");
			logger.debug("#######################################################");

			// looks like we terminated abnormally so restart the loop.
			if (running.get() == true)
			{
				logger.warn("#######################################################");
				logger.warn("mainSubscribeLoop is restarting after abnormal termination");
				logger.warn("#######################################################");

				subscriptionLoopPool.submit(() -> mainSubscribeLoop(source));
			}
			else
			{
				logger.debug("#######################################################");
				logger.debug("mainSubscribeLoop is not restarting as running = false");
				logger.debug("#######################################################");
			}

		}

		return null;
	}

	private Void _subscribe(List<EndPointWrapper> endPoints, String debugArg)
	{
		try
		{
			// logger.error("http subscribe request sent for "
			// + endPoints.stream().map(e -> e.getExtensionNo()).collect(Collectors.joining(",")) + " on Thread"
			// + Thread.currentThread().getId());

			SubscribeResponse response = api.subscribe(
					endPoints.stream().map(w -> w.getEndPoint()).collect(Collectors.toList()), seqenceNo,
					30, debugArg);

			if (response == null)
			{
				logger.error("Subscribe returned null. PBX is probably down for maintenance");
				return null; // pbx is probably down
			}

			// update the sequence no. from the response so we don't miss any data.
			seqenceNo = response.seq;

			// logger.error("http subscribe response recieved for "
			// + endPoints.stream().map(e -> e.getExtensionNo()).collect(Collectors.joining(",")) + " on Thread"
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
					// we use a thread pool (of 1) to do the callbacks as the doSubscribe thread can be
					// interrupted but we don't want end user code to be interrupted in unexpected manner.
					// The thread pool isolates the user code from our problems when we get cancelled.
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
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		return null;

	}

	private List<Subscriber> getCopyOfSubscribers(EndPoint endPoint)
	{
		List<Subscriber> subscribers = null;

		synchronized (subscriptions)
		{
			EndPointWrapper wrapper = new EndPointWrapper(endPoint);
			List<Subscriber> list = subscriptions.computeIfAbsent(wrapper, w -> new ArrayList<>());
			subscribers = list.stream().collect(Collectors.toList());
		}
		return subscribers;

	}

	/*
	 * Get all of the end points that we have subscriptions on.
	 */
	private List<EndPointWrapper> getCopyAndMarkAllEndPoints()
	{
		List<EndPointWrapper> endPoints = new ArrayList<>();

		synchronized (subscriptions)
		{
			// We need to mark the end points as now being managed by the
			// main subscribe loop so any short term subscribe loops can shut down.
			Set<EndPointWrapper> subscribed = subscriptions.keySet();
			for (EndPointWrapper wrapper : subscribed)
			{
				wrapper.servicedByMainLoop = true;
			}

			endPoints.addAll(subscribed);
		}
		return endPoints;

	}

	private List<Subscriber> getCopyOfAllSubscribers()
	{

		List<Subscriber> subscribers = new ArrayList<>();

		synchronized (subscriptions)
		{
			for (EndPointWrapper endPoint : subscriptions.keySet())
			{
				subscribers.addAll(subscriptions.get(endPoint).stream().collect(Collectors.toList()));
			}
		}
		return subscribers;

	}

	private void notifyError(NoojeeContactApiException e)
	{

		List<Subscriber> subscribers = getCopyOfAllSubscribers();

		subscribers.forEach(subscriber ->
			{
				try
				{
					subscriber.onError(e);
				}
				catch (Throwable e2)
				{
					logger.error(e, e2);
				}
			});

	}

	private void notifyHangup(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				try
				{
					subscriber.hungup(event);
				}
				catch (Throwable e)
				{
					logger.error(e, e);

				}

			});

	}

	private void notifyDialing(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				try
				{

					subscriber.dialing(event);
				}
				catch (Throwable e)
				{
					logger.error(e, e);

				}

			});

	}

	private void notifyRinging(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				try
				{

					subscriber.ringing(event);
				}
				catch (Throwable e)
				{
					logger.error(e, e);
				}
			});

	}

	private void notifiyConnected(EndPoint endPoint, EndPointEvent event)
	{
		List<Subscriber> subscribers = getCopyOfSubscribers(endPoint);

		subscribers.forEach(subscriber ->
			{
				try
				{
					subscriber.connected(event);
				}
				catch (Throwable e)
				{
					logger.error(e, e);

				}
			});

	}

	private static class EndPointWrapper
	{
		EndPoint endPoint;
		boolean servicedByMainLoop;

		public EndPointWrapper(EndPoint endPoint)
		{
			this.endPoint = endPoint;
			this.servicedByMainLoop = false;
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
