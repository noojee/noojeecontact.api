package au.org.noojee.contact.api;

public class Status
{
	private int code;

	private String message;

	public Status(int code, String message)
	{
		this.code = code;
		this.message = message;
	}

	/**
	 * @return the code
	 */
	public int getCode()
	{
		return code;
	}

	/**
	 * @return the message
	 */
	public String getMessage()
	{
		return message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Status [code=" + code + ", message=" + message + "]";
	}


	
	
}
