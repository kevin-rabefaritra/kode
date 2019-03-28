package mg.startapps.kode.debug;

import android.util.Log;

public class Debug
{
	public static final boolean DEBUG_ENABLED = false;

	public static void log(String tag, String message)
	{
		if(DEBUG_ENABLED)
		{
			Log.d(tag, message);
		}
	}
}
