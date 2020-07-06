package au.org.noojee.contact.api;

// import org.junit.jupiter.api.Test;


public class NoojeeContactApiTest
{

	/**
	@Test
	public void test()
	{
		NoojeeContactApi api = new NoojeeContactApi("robtest3.clouddialer.com.au", "8d26f182-978d-11e3-9620-40f2e910e0c0");

		try
		{
			NoojeeContactStatistics status = api.getStatistics();
			
			System.out.println("Status:" + status);
			
			List<Shift> shifts = api.getActiveShifts("Support");
			
			if (shifts.size() == 0)
				System.out.println("No shifts found");
			
			for (Shift shift : shifts)
			{
				System.out.println("Shift: " + shift);
			}
		}
		catch (NoojeeContactApiException e)
		{
			e.printStackTrace();
			fail("Not yet implemented");

		}
	}
	*/

}
