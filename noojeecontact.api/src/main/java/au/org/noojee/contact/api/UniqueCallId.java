package au.org.noojee.contact.api;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class UniqueCallId
{
	String uniqueCallId;

	public UniqueCallId(String uniqueCallId)
	{
		this.uniqueCallId = uniqueCallId;
	}

	// for gson.
	public UniqueCallId()
	{
	}

	
	@Override
	public String toString()
	{
		return uniqueCallId;
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
		result = prime * result + ((uniqueCallId == null) ? 0 : uniqueCallId.hashCode());
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
		UniqueCallId other = (UniqueCallId) obj;
		if (uniqueCallId == null)
		{
			if (other.uniqueCallId != null)
				return false;
		}
		else if (!uniqueCallId.equals(other.uniqueCallId))
			return false;
		return true;
	}

	
}
