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
```
