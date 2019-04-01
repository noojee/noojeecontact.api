package au.org.noojee.contact.api;

public class Shift
{
	private int id;
	
	private String login;

	private String extension;
	
	private String phoneNumber;
	
	private String mobileNumber;
	
	private String email;
	
	private int statckPostion;


	
	
	/**
	 * @return the id
	 */
	public int getId()
	{
		return id;
	}




	/**
	 * @return the login
	 */
	public String getLogin()
	{
		return login;
	}




	/**
	 * @return the extension
	 */
	public String getExtension()
	{
		return extension;
	}




	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber()
	{
		return phoneNumber;
	}


	/**
	 * @return the mobileNumber
	 */
	public String getMobileNumber()
	{
		return mobileNumber;
	}




	/**
	 * @return the email
	 */
	public String getEmail()
	{
		return email;
	}




	/**
	 * @return the statckPostion
	 */
	public int getStatckPostion()
	{
		return statckPostion;
	}




	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Shift [id=" + id + ", login=" + login + ", extension=" + extension + ", phoneNumber=" + phoneNumber
				+ ", mobileNumber=" + mobileNumber + ", email=" + email + ", statckPostion=" + statckPostion + "]";
	}

	
	
	
	

	
	
}
