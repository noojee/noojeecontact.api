package au.org.noojee.contact.api;

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import au.org.noojee.contact.api.NoojeeContactApi.DialResponse;
import au.org.noojee.contact.api.NoojeeContactApi.SimpleResponse;

class ActivityMonitorTest
{
	EndPoint e100 = new EndPoint("100");
	EndPoint e101 = new EndPoint("101");
	EndPoint e115 = new EndPoint("115");

	// The call we are originating.
	private UniqueCallId uniqueCallIdToMonitor = null;

	private boolean seenHangup;

	@Test
	void test() throws InterruptedException
	{
		ActivityMonitor monitor = ActivityMonitor.SELF;

		NoojeeContactApi api = new NoojeeContactApi("pentest.clouddialer.com.au",
				"1981a2cc-db08-11e8-a033-0016ec037d28");
		try
		{
			monitor.start(api);

			seenHangup = false;

			CountDownLatch answerLatch = new CountDownLatch(1);

			monitor.subscribe(monitor(answerLatch), e115);

			// print("dialing");
			// DialResponse response = api.dial(new PhoneNumber("106"), e115, "From PenTest", AutoAnswer.Yealink,
			// new PhoneNumber("0383208100"), true, "A Test Call");
			//
			// print("Dial sent, now waiting");
			//
			// Thread.sleep(20000);
			//
			// monitor.subscribe(e115, new SubscriberAdapter()
			// {
			// });
			// monitor.subscribe(e115, new SubscriberAdapter()
			// {
			// });
			// monitor.subscribe(e101, new SubscriberAdapter()
			// {
			// });
			// monitor.subscribe(e100, new SubscriberAdapter()
			// {
			// });

			answerLatch.await();
			
			print("Call answered");

			// wait 10 seconds and hangup the call.
			Thread.sleep(10000);

			SimpleResponse hangupResponse = api.hangup(uniqueCallIdToMonitor);
			if (hangupResponse.wasSuccessful())
				print("Hangup call was successful");
			else
				print("Hangup call was failed: " + hangupResponse.getMessage());

			// wait a bit to see hangup succeed.
			Thread.sleep(10000);

			print("Ending");

			// api.hangup(uniqueCallId);

		}
		catch (NoojeeContactApiException e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private SubscriberAdapter monitor(CountDownLatch answerLatch)
	{
		return new SubscriberAdapter()
		{

			@Override
			public void hungup(EndPoint endPoint, EndPointEvent event)
			{
				if (uniqueCallIdToMonitor != null && uniqueCallIdToMonitor.equals(event.getUniqueCallId()))
				{
					if (!seenHangup)
						print("Call was hungup: " + event.getUniqueCallId() + " for EndPoint: "
								+ endPoint.extensionNo);
					seenHangup = true;
					// monitor.stop();
				}
				else
					print("saw old hangup for:" + event.getUniqueCallId());
			}

			@Override
			public void dialing(EndPoint endPoint, EndPointEvent event)
			{
				print("Dialing: " + endPoint.extensionNo + " on " + event.getUniqueCallId());

				uniqueCallIdToMonitor = event.getUniqueCallId();
			}

			@Override
			public void answered(EndPoint endPoint, EndPointEvent event)
			{
				if (uniqueCallIdToMonitor != null && uniqueCallIdToMonitor.equals(event.getUniqueCallId()))
				{
					print("Answered endPoint: " + endPoint.extensionNo + " uniqueCallId:"
							+ event.getUniqueCallId());

					answerLatch.countDown();
				}
			}

			@Override
			public void ringing(EndPoint endPoint, EndPointEvent event)
			{
				if (endPoint.extensionNo.equals("115"))
				{
					uniqueCallIdToMonitor = event.getUniqueCallId();

					print("Ringing endPoint: " + endPoint.extensionNo + " uniqueCallId:"
							+ event.getUniqueCallId());
				}
			}

		};
	}

	private void print(String string)
	{
		System.out.println(string);

	}

}
