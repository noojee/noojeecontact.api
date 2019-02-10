package au.org.noojee.contact.api;

import au.org.noojee.api.enums.Tech;

public class EndPoint
{
	final String extensionNo;
	final Tech tech;

	public EndPoint(String extensionNo)
	{
		String[] parts = extensionNo.split("/");
		if (parts.length != 2)
		{

			this.tech = Tech.SIP;
			this.extensionNo = extensionNo;
		}
		else
		{
			this.tech = Tech.valueOf(parts[0]);
			this.extensionNo = parts[1];
		}
	}

	public EndPoint(Tech tech, String extensionNo)
	{

		this.extensionNo = extensionNo;
		this.tech = tech;

	}

	String compactString()
	{
		return tech + "/" + extensionNo;
	}

	String compactStringNoTech()
	{
		return extensionNo;
	}

	public String getExtensionNo()
	{
		return extensionNo;
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
		result = prime * result + ((extensionNo == null) ? 0 : extensionNo.hashCode());
		result = prime * result + ((tech == null) ? 0 : tech.hashCode());
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
		EndPoint other = (EndPoint) obj;
		if (extensionNo == null)
		{
			if (other.extensionNo != null)
				return false;
		}
		else if (!extensionNo.equals(other.extensionNo))
			return false;
		if (tech == null)
		{
			if (other.tech != null)
				return false;
		}
		else if (!tech.equals(other.tech))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return compactString();
	}

}
