# noojeecontact.api
Java API for the Noojee Contact PBX

Dial example

```
/**
 * Example of how to dial 
 */
class DialTest 
{
	@Test
	void test() throws InterruptedException
	{
		NoojeeContactApi api = new NoojeeContactApi("somenoojeepbx", "<api key goes here>");
		
		try
		{
			DialResponse response = api.dial(
          			new PhoneNumber("0383208100")    // external no. to dial
				  , EndPoint("115")   // the extension to dial from
				  , "From Unit Test"
				  , AutoAnswer.Yealink // The type of handset.
				  , new PhoneNumber("0383208100")
				  , true
				  , "A Test Call");
		}
		catch (NoojeeContactApiException e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

####################################################################
# Demonstrate monitoring activity.
# Use the PBXMonitor rather than directly using the subscribe method
#
#####################################################################

package au.org.noojee.contact.api;

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import au.org.noojee.contact.api.NoojeeContactApi.SimpleResponse;

public class PBXMonitorTest
{
	EndPoint e100 = new EndPoint("100");
	EndPoint e101 = new EndPoint("101");
	EndPoint e115 = new EndPoint("115");
	EndPoint e106 = new EndPoint("106");

	// The call we are originating.
	private UniqueCallId uniqueCallIdToMonitor = null;

	private boolean seenHangup;

	public PBXMonitorTest()
	{

	}

	@Test
	public void test() throws InterruptedException
	{
		PBXMonitor monitor = PBXMonitor.SELF;

		try
		{
			monitor.start("testpbx.noojee.com.au",
					"xxxxxx-xxxxxx-xxxxxxx-xxxxxxx");

			seenHangup = false;

			CountDownLatch answerLatch = new CountDownLatch(1);

			monitor.subscribe(e115, monitor(answerLatch), "PBXMonitorTest");

			monitor.subscribe(e106, monitor(answerLatch), "PBXMonitorTest");

			monitor.subscribe(e115, new SubscriberAdapter()
			{
			}, "PBXMonitorTest");
			monitor.subscribe(e101, new SubscriberAdapter()
			{
			}, "PBXMonitorTest");
			monitor.subscribe(e100, new SubscriberAdapter()
			{
			}, "PBXMonitorTest");

			print("dialing");
			monitor.dial(new NJPhoneNumber("106"), e115, "From PenTest", AutoAnswer.Yealink,
					new NJPhoneNumber("0383208100"), true, "A Test Call");

			print("Dial sent, now waiting");

			answerLatch.await();

			print("Call connected");

			// wait 10 seconds and hangup the call.
			Thread.sleep(10000);

			SimpleResponse hangupResponse = monitor.hangup(uniqueCallIdToMonitor);
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
			public void hungup(EndPointEvent event)
			{
				if (uniqueCallIdToMonitor != null && uniqueCallIdToMonitor.equals(event.getPrimaryUniqueCallId()))
				{
					if (!seenHangup)
						print("Call was hungup: " + event.getPrimaryUniqueCallId() + " for EndPoint: "
								+ event.getEndPoint().extensionNo);
					seenHangup = true;
					// monitor.stop();
				}
				else
					print("saw old hangup for:" + event.getPrimaryUniqueCallId());
			}

			@Override
			public void dialing(EndPointEvent event)
			{
				print("Received Dial Event: " + event.getEndPoint().extensionNo + " on "
						+ event.getPrimaryUniqueCallId());

				uniqueCallIdToMonitor = event.getPrimaryUniqueCallId();
			}

			@Override
			public void connected(EndPointEvent event)
			{
				print("Saw Connected endPoint: " + event.getEndPoint().extensionNo + " uniqueCallId:"
						+ event.getPrimaryUniqueCallId());
				if (uniqueCallIdToMonitor != null && uniqueCallIdToMonitor.equals(event.getPrimaryUniqueCallId()))
				{
					print("Connected endPoint: " + event.getEndPoint().extensionNo + " uniqueCallId:"
							+ event.getPrimaryUniqueCallId());

					answerLatch.countDown();
				}
			}

			@Override
			public void ringing(EndPointEvent event)
			{
				// if (event.getEndPoint().extensionNo.equals("115"))
				{
					uniqueCallIdToMonitor = event.getPrimaryUniqueCallId();

					print("Ringing endPoint: " + event.getEndPoint().extensionNo + " uniqueCallId:"
							+ event.getPrimaryUniqueCallId());
				}
			}

		};
	}

	private void print(String string)
	{
		System.out.println(string);

	}

}

################################################################################
#
# Direct use of subscribe method. (PBXMonitor is easier and optimises connections)
#
################################################################################

class DetectAnswerTest
{
	@Test
	void test() throws InterruptedException
	{
		NoojeeContactApi api = new NoojeeContactApi("somenoojeepbx", "<api key goes here>");
		
		// we want to monitor the following extensions for answers.
		EndPoint e100 = new EndPoint("100");
		EndPoint e115 = new EndPoint("115");
		
		try
		{
			monitor.start(api);
			monitor.subscribe(monitor(), e115, e100);
		}
		catch (NoojeeContactApiException e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		// wait for a while so dev can test making a call to 115 or 100.
		Thread.sleep(60000);
	}
	
	private SubscriberAdapter monitor()
	{
		return new SubscriberAdapter()
		{
			@Override
			public void ringing(EndPoint endPoint, EndPointEvent event)
			{
				if (endPoint.equals(e115) || endPoint.equals(e100))
				{
					print("Ringing endPoint: " + endPoint.extensionNo + " uniqueCallId:"
							+ event.getUniqueCallId());
				}
			}

			@Override
			public void answered(EndPoint endPoint, EndPointEvent event)
			{
				if (endPoint.equals(e115) || endPoint.equals(e100))
				{
					print("Answered endPoint: " + endPoint.extensionNo + " uniqueCallId:"
							+ event.getUniqueCallId());

				}
			}


		};
	}



}

```
