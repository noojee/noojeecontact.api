package au.org.noojee.contact.api;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class NoojeeContactStatistics
{

	private int code;

	private String message;
	
	// There will only ever be one entry returned!
	@SerializedName("entities")
	private List<NoojeeStatistics>statistics;
	
	
	public NoojeeContactStatistics(int code, String message)
	{
		this.code = code;
		this.message = message;
	}
	
	
	// Support for older version of the api that don't provide statistics.
	public boolean hasStatistics()
	{
		return statistics != null && statistics.size() > 0;
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
	

	/**
	 * @return the ioWait
	 */
	public long getIoWait()
	{
		return (long)statistics.get(0).ioWait;
	}

	/**
	 * @return the loadAverage
	 */
	public long getLoadAverage()
	{
		return (long)statistics.get(0).loadAverage;
	}

	/**
	 * @return the cpuPercent
	 */
	public int getCpuPercent()
	{
		return statistics.get(0).cpuPercent;
	}

	/**
	 * @return the cores
	 */
	public int getCores()
	{
		return statistics.get(0).cores;
	}

	/**
	 * @return the memoryAvailableMB
	 */
	public long getMemoryAvailableMB()
	{
		return statistics.get(0).memoryAvailableMB;
	}

	/**
	 * @return the memoryTotalMB
	 */
	public long getMemoryTotalMB()
	{
		return statistics.get(0).memoryTotalMB;
	}

	/**
	 * @return the freeDiskSpacePercent
	 */
	public int getFreeDiskSpacePercent()
	{
		return statistics.get(0).freeDiskSpacePercent;
	}

	/**
	 * @return the threadPoolUsagePercent
	 */
	public int getThreadPoolUsagePercent()
	{
		return statistics.get(0).threadPoolUsagePercent;
	}

	/**
	 * @return the schedulerPoolUsagePercent
	 */
	public int getSchedulerPoolUsagePercent()
	{
		return statistics.get(0).schedulerPoolUsagePercent;
	}

	/**
	 * @return the steal
	 */
	public long getSteal()
	{
		return (long)statistics.get(0).steal;
	}

	/**
	 * @return the trunkLag
	 */
	public Map<String, Double> getTrunkLag()
	{
		return statistics.get(0).trunkLag;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Status [code=" + code + ", message=" + message + "]";
	}


	
	public class NoojeeStatistics
	{
		double ioWait;
		double loadAverage;
		int cpuPercent;
		int	cores;
		long memoryAvailableMB;
		long memoryTotalMB;
		
		int freeDiskSpacePercent;
		int threadPoolUsagePercent;
		int schedulerPoolUsagePercent;
		double steal;
		// One entry per trunk
		
		@SerializedName("entities")
		Map<String, Double> trunkLag; // IPAddress:lag in milliseconds.
		
		
		
	}
}
