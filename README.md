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
