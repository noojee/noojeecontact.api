package au.org.noojee.contact.api;

import au.org.noojee.contact.api.NoojeeContactApi.Event;

public class EndPointEvent
{

	final private String extensionNo;
	final private Event event;

	public EndPointEvent(String extensionNo, Event event)
	{
		this.extensionNo = extensionNo;
		this.event = event;
	}

	public EndPointStatus getStatus()
	{
		return event.getStatus();
	}

	public EndPoint getEndPoint()
	{
		return new EndPoint(extensionNo);
	}
	
	public UniqueCallId getUniqueCallId()
	{
		return event.CallData.getUniqueCallId();
	}
	

	public NJPhoneNumber getCallerId()
	{
		return event.getCallerId();
	}

	
	public boolean isInbound()
	{
		return event.CallData.isInbound();
	}

	
	

	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "EndPointEvent [extensionNo=" + extensionNo + ", event=" + event + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((extensionNo == null) ? 0 : extensionNo.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		EndPointEvent other = (EndPointEvent) obj;
		if (event == null)
		{
			if (other.event != null)
				return false;
		}
		else if (!event.equals(other.event))
			return false;
		if (extensionNo == null)
		{
			if (other.extensionNo != null)
				return false;
		}
		else if (!extensionNo.equals(other.extensionNo))
			return false;
		return true;
	}

		
	

}
