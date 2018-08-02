package au.org.noojee.contact.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class NoojeeContactStatistics
{

	private int code;

	private String message;

	// There will only ever be one entry returned!
	@SerializedName("entities")
	private List<NoojeeStatistics> statistics;

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

	private NoojeeStatistics get()
	{
		return statistics.get(0);
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
		return (long) get().ioWait;
	}

	/**
	 * @return the loadAverage
	 */
	public Long getLoadAverage()
	{
		return Long.valueOf((long) (get().loadAverage));
	}

	/**
	 * @return the cpuPercent
	 */
	public Long getCpuPercent()
	{
		return Long.valueOf((long) (get().cpuPercent));
	}

	/**
	 * @return the cores
	 */
	public long getCores()
	{
		return get().cores;
	}

	/**
	 * @return the memoryAvailableMB
	 */
	public long getMemoryAvailableMB()
	{
		return get().memoryAvailableMB;
	}

	/**
	 * @return the memoryTotalMB
	 */
	public long getMemoryTotalMB()
	{
		return get().memoryTotalMB;
	}

	/**
	 * @return the freeDiskSpacePercent
	 */
	public Long getFreeDiskSpacePercent()
	{
		return Long.valueOf((long) (get().freeDiskSpacePercent));
	}

	/**
	 * @return the threadPoolUsagePercent
	 */
	public Long getThreadPoolUsagePercent()
	{
		return Long.valueOf((long) (get().threadPoolUsagePercent));
	}

	/**
	 * @return the schedulerPoolUsagePercent
	 */
	public Long getSchedulerPoolUsagePercent()
	{
		return Long.valueOf((long) (get().schedulerPoolUsagePercent));
	}

	/**
	 * @return the steal
	 */
	public long getSteal()
	{
		return (long) get().steal;
	}

	/**
	 * @return the trunkLag
	 */
	public Map<String, Double> getTrunkLag()
	{
		return get().trunkLag;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Status [code=" + code + ", message=" + message + "]";
	}

	public boolean didBackupSucceed()
	{
		NoojeeStatistics stats = get();

		LocalDateTime now = LocalDateTime.now();

		boolean success = true;

		if (!isBackupRunning())
		{
			if (stats.lastBackupSuccess.isBefore(stats.lastBackupAttempt))
				success = false;

			LocalDateTime midnight = LocalDate.now().atStartOfDay();
			LocalDateTime _7am = LocalDate.now().atStartOfDay().plusHours(7);

			if (now.isAfter(_7am))
			{
				// no attempt in 24 hours.
				if (stats.lastBackupAttempt.isBefore(midnight))
					success = false;
			}
		}

		return success;
	}

	public boolean isBackupRunning()
	{
		NoojeeStatistics stats = get();

		LocalDateTime now = LocalDateTime.now();
		return stats.backupStarting.plusHours(1).isAfter(now);
	}

	public class NoojeeStatistics
	{
		double ioWait;
		double loadAverage;
		int cpuPercent;
		int cores;
		long memoryAvailableMB;
		long memoryTotalMB;

		int freeDiskSpacePercent;
		int threadPoolUsagePercent;
		int schedulerPoolUsagePercent;
		double steal;

		double dbPoolSize;
		double dbPoolUsage;

		// Last time the a db back was attempted
		LocalDateTime lastBackupAttempt; // date yyyy/MM/dd HH:mm
		// Last time a db back succeeded
		LocalDateTime lastBackupSuccess; // date yyyy/MM/dd HH:mm

		// the time when a backup starts - this includes recording backup.
		// Most backups take no longer than 30 minutes these days as we are duing
		// recording migration off the back of a call completing.
		LocalDateTime backupStarting; // date yyyy/MM/dd HH:mm

		// One entry per trunk
		@SerializedName("pingData") // ? pingData
		Map<String, Double> trunkLag; // IPAddress:lag in milliseconds.

	}

	public Long getDbPoolSize()
	{
		return Long.valueOf((long) (get().dbPoolSize));
	}

	public long getDbPoolUsage()
	{
		return (long) get().dbPoolUsage;
	}

}
