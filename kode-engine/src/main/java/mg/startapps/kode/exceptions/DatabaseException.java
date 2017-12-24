package mg.startapps.kode.exceptions;

/**
 * Created by Onion Knight on 16/02/2017.
 */
public class DatabaseException extends Exception
{
	public DatabaseException()
	{
		super("Database needed instance is null");
	}
}
