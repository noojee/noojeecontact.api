package au.org.noojee.contact.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.apache.logging.log4j.util.Strings;
import org.javamoney.moneta.Money;

public interface Conversions
{
	static final CurrencyUnit currencyUnit = Monetary.getCurrency(Locale.getDefault());

	/**
	 * Local Date
	 * 
	 * @param dateToSeconds
	 * @return A LocalDate representing the epoc in the current system timezone. If the dateToSeconds is zero then the
	 *         constant Constants.DATEZERO is returned.
	 */
	public static LocalDate toLocalDate(long dateToSeconds)
	{
		LocalDate localDate = Instant.ofEpochSecond(dateToSeconds).atZone(ZoneId.systemDefault()).toLocalDate();

		return localDate;
	}

	public static long toLong(LocalDate localDate)
	{
		return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();

	}

	/**
	 * LocalDateTime
	 * 
	 * @param dateToSeconds
	 * @return A LocalDateTime representing the epoc in the current system timezone. If the dateToSeconds is zero then
	 *         the constant Constants.DATETIMEZERO is returned.
	 */
	public static LocalDateTime toLocalDateTime(long dateToSeconds)
	{
		LocalDateTime localDateTime = Instant.ofEpochSecond(dateToSeconds).atZone(ZoneId.systemDefault())
				.toLocalDateTime();

		return localDateTime;
	}

	public static Long toLong(LocalDateTime localDateTime)
	{
		return localDateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();

	}

	/**
	 * Date
	 * 
	 * @param localDate
	 * @return
	 */
	public static Date toDate(LocalDate localDate)
	{
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	public static LocalDateTime toLocalDateTime(LocalDate cutoffDate)
	{
		return LocalDateTime.of(cutoffDate, LocalTime.of(0, 0));
	}

	public static LocalTime toLocalTime(String time)
	{
		if (time == null || time.trim().length() == 0)
			return null;
		else
		{
			DateTimeFormatter dtf =  DateTimeFormatter.ofPattern("h:mma");
			return LocalTime.parse(time.toUpperCase(), dtf);
		}
	}

	public static Duration toDuration(String days)
	{
		return Duration.ofDays(Integer.valueOf(days));
	}


	public static Money toMoney(double value)
	{
		return Money.of(value, currencyUnit);
	}

	public static Money toMoney(String value)
	{
		if (Strings.isBlank(value))
			return Money.of(new BigDecimal("0"), currencyUnit);
		
		return Money.of(new BigDecimal(value), currencyUnit);
	}

	public static Money toMoney(BigDecimal value)
	{
		return Money.of(value, currencyUnit);
	}


}
