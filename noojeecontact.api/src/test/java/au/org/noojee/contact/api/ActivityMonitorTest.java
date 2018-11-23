package au.org.noojee.contact.api;

import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

import au.org.noojee.contact.api.NoojeeContactApi.DialResponse;

class ActivityMonitorTest
{
	EndPoint e115 = new EndPoint("115");

	@Test
	void test() throws InterruptedException
	{
		ActivityMonitor monitor = ActivityMonitor.SELF;

		NoojeeContactApi api = new NoojeeContactApi("pentest.clouddialer.com.au",
				"1981a2cc-db08-11e8-a033-0016ec037d28");

		try
		{
			api.getStatistics();
		}
		catch (NoojeeContactApiException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try
		{
			monitor.start(api);

			monitor.subscribe(e115, new SubscriberAdapter()
			{
				@Override
				public void answered(EndPoint endPoint, EndPointEvent event) 
				{
					
					try
					{
						print("Answered");
						Thread.sleep(20000);
						api.hangup(event.getUniqueCallId());
						print("Called hangup");
					}
					catch (NoojeeContactApiException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			});

			print("dialing");
			DialResponse response = api.dial(new PhoneNumber("106"), e115, "From PenTest", AutoAnswer.Yealink,
					new PhoneNumber("0383208100"), true, "A Test Call");

			Thread.sleep(120000);
			
			print("Ending");

			// api.hangup(uniqueCallId);

		}
		catch (NoojeeContactApiException e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void print(String string)
	{
		System.out.println(string);
		
	}

	


}
