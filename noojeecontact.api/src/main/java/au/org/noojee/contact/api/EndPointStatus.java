package au.org.noojee.contact.api;

import com.google.gson.annotations.SerializedName;

public enum EndPointStatus
{

	@SerializedName("Dialing Out")
	DialingOut, Connected, Ringing, Hungup, Unknown;
}
