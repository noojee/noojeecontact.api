module noojeecontact.api
{
	exports au.org.noojee.contact.api;
	exports au.org.noojee.contact.api.internals;
	exports au.org.noojee.api.enums;

	requires com.github.spotbugs.annotations;
	requires com.google.gson;
	requires transitive org.joda.money;
	requires junit;
	requires org.apache.logging.log4j;
}