package mg.startapps.kode.utils;

/**
 * Created by Kevin Rabefaritra on 04/04/2018.
 */

public class StringUtils
{
	public static String escape(String string)
	{
		return string.replaceAll("'", "\'");
	}

	public static boolean containsDot(String string)
	{
		return string.contains(".");
	}

	public static String strAfterFirstNeedle(String string, String needle)
	{
		return string.substring(string.indexOf(needle) + 1);
	}
}
