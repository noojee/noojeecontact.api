package au.org.noojee.contact.api;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.money.Money;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import au.org.noojee.contact.api.internals.Conversions;

public class GsonForNoojeeContact
{
	Logger logger = LogManager.getLogger();

	static public <E extends NoojeeContactEntity<E>> String toJson(NoojeeContactEntity<E> e)
	{
		Gson gson = create();
		return gson.toJson(e);
	}

	static public <R> R fromJson(StringReader json, Class<R> responseClass)
	{
		Gson gson = create();
		return gson.fromJson(json, responseClass);
	}

	static public <R> R fromJson(String json, Class<R> responseClass)
	{
		Gson gson = create();
		return gson.fromJson(json, responseClass);
	}

	public static <E> List<E> fromJson(String responseBody, Type listType)
	{
		Gson gson = create();

		List<E> list = gson.fromJson(responseBody, listType);

		return list;
	}

	public static <E> E fromJsonTypedObject(String responseBody, Type type)
	{
		Gson gson = create();

		E object = gson.fromJson(responseBody, type);

		return object;
	}

	// public static <E> Map<String, List<E>> fromJson(String responseBody, Type listType)
	// {
	// Gson gson = create();
	//
	// Map<String, List<E>> map = gson.fromJson(responseBody, listType);
	//
	// return map;
	// }

	public static String toJson(List<Object> operands)
	{
		Gson gson = create();
		return gson.toJson(operands);
	}

	static private Gson create()
	{
		// Register type adaptors for special conversions and enums requiring a conversion.
		GsonBuilder builder = new GsonBuilder()
				.registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
				.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
				.registerTypeAdapter(Money.class, new MoneySerializer())
				.registerTypeAdapter(Money.class, new MoneyDeserializer())
				.registerTypeAdapter(UniqueCallId.class, new UniqueCallIdSerializer())
				.registerTypeAdapter(UniqueCallId.class, new UniqueCallIdDeserializer());

		return builder.create();
	}

	/**
	 * Special Gson Adaptors for  types that gson doesn't support out of the box.
	 */

	/**
	 * LocalDate
	 */
	static private class LocalDateSerializer implements JsonSerializer<LocalDate>
	{

		@Override
		public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context)
		{
			Long longDate = Conversions.toLong(date);
			return new JsonPrimitive(longDate.toString());
		}
	}

	static private class LocalDateDeserializer implements JsonDeserializer<LocalDate>
	{

		@Override
		public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			DateTimeFormatter formatter;

			String string = json.getAsString();
			if (string.length() == 10)
				formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			else
			{
				// 2018-04-01T04:00:07+10:00
				formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
				string = json.getAsString().substring(0, 19);
			}
			LocalDate localDate = LocalDate.parse(string, formatter);
			return localDate;

		}
	}

	/**
	 * LocalDateTime
	 */
	static private class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime>
	{

		@Override
		public JsonElement serialize(LocalDateTime date, Type typeOfSrc, JsonSerializationContext context)
		{
			Long longDate = Conversions.toLong(date);
			return new JsonPrimitive(longDate.toString());
		}
	}

	static private class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime>
	{

		@Override
		public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			// LocalDateTime localDateTime = Conversions.toLocalDateTime(json.getAsLong());
			// "2016-05-10T03:36:34.615Z

			String value = json.getAsString();

			DateTimeFormatter formatter;

			switch (value.length())
			{
				case 16:
					formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					break;
				case 21:
					formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
					break;
				case 19:
					formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					break;
				default:
					formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					break;
			}

			// if (value.length() > 19)
			// value = value.substring(0, 19);

			value = value.replace("/", "-");
			value = value.replaceAll("  ", "");
			value = value.trim();

			LocalDateTime localDateTime = null;

			if (value.length() > 0)
				localDateTime = LocalDateTime.parse(value, formatter);

			// String date = json.getAsString();
			// if (date.endsWith("Z"))
			// date = date.substring(0, date.length()-1);
			// //LocalDateTime localDateTime = LocalDateTime.parse(json.getAsString(),formatter);
			// LocalDateTime localDateTime = LocalDateTime.parse(date);
			// // DateTimeFormatter.ISO_INSTANT);

			return localDateTime;

		}
	}

	/**
	 * Money
	 */
	static private class MoneySerializer implements JsonSerializer<Money>
	{

		@Override
		public JsonElement serialize(Money money, Type typeOfSrc, JsonSerializationContext context)
		{
			String stringMoney = "" + money.getAmountMajorLong() + "." + money.getAmountMinorLong();
			return new JsonPrimitive(stringMoney);
		}
	}

	static private class MoneyDeserializer implements JsonDeserializer<Money>
	{

		@Override
		public Money deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			Money localDate = Conversions.toMoney(json.getAsBigDecimal());

			return localDate;

		}
	}

	/**
	 * Money
	 */
	static private class UniqueCallIdSerializer implements JsonSerializer<UniqueCallId>
	{

		@Override
		public JsonElement serialize(UniqueCallId uniqueCallId, Type typeOfSrc, JsonSerializationContext context)
		{

			return new JsonPrimitive(uniqueCallId.uniqueCallId);
		}
	}

	static private class UniqueCallIdDeserializer implements JsonDeserializer<UniqueCallId>
	{

		@Override
		public UniqueCallId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			UniqueCallId uniqueCallId = new UniqueCallId(json.getAsString());

			return uniqueCallId;

		}
	}

}
