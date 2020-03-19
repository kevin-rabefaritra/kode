package mg.startapps.kode.model;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import mg.startapps.kode.debug.Debug;
import mg.startapps.kode.services.KodeEngine;
import mg.startapps.kode.services.KodeSupport;

/**
 * Created by kevinRabefaritra on 06/02/17.
 */
public class KodeQuery<K extends KodeObject, T extends KodeObject>
{
	public enum QueryType {
		SELECT, DELETE
	};

    private KodeEngine kodeEngine;
    private Class<K> objectClass1;
    private Class<T> objectClass2;
    private String query;

	public KodeQuery(KodeEngine kodeEngine, Class<K> objectClass, String query)
	{
		this.kodeEngine = kodeEngine;
		this.objectClass1 = objectClass;
		this.query = query;
	}

    public KodeQuery(KodeEngine kodeEngine, Class<K> objectClass, QueryType queryType)
    {
        this(kodeEngine, objectClass, KodeSupport.generateQuery(objectClass, queryType, " WHERE (1<2"));
    }

	public KodeQuery(KodeEngine kodeEngine, String query)
	{
		this.kodeEngine = kodeEngine;
		this.query = query;
	}

    // associations are built from 2 classes
    /* Note about associations : when we select from an association, only the first objectClass will make a JOIN
    * calling this method implies that the user has data about the second object class
    * Using association only returns List<objectClass2 class>
    * E.g. : objectA_objectB association only fetch in objectB table
    */
    public KodeQuery(KodeEngine kodeEngine, Class<K> objectClass1, Class<T> objectClass2)
    {
        this.kodeEngine = kodeEngine;
        this.objectClass1 = objectClass1;
        this.objectClass2 = objectClass2;
        this.query = KodeSupport.generateSelectQuery(objectClass1, objectClass2) + " WHERE (1<2";
    }

    public KodeQuery(KodeEngine kodeEngine, Class<K> objectClass)
	{
		this(kodeEngine, objectClass, QueryType.SELECT);
	}

    public KodeQuery<K, T> where(String field, Object value)
    {
        return this.where(field, "=", value);
    }

	public KodeQuery<K, T> whereNot(String field, Object value)
	{
		return this.where(field, "!=", value);
	}

	public KodeQuery<K, T> where(String field, String comparator, Object value)
	{
		String query;
		if(value instanceof String)
		{
			query = String.format(" AND %s %s '%s'", field, comparator, value.toString());
		}
		else
		{
			query = String.format(" AND %s %s %s", field, comparator, value.toString());
		}
		return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query + query);
	}

	public KodeQuery<K, T> whereRaw(String condition)
	{
		condition = " AND " + condition;
		return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query + condition);
	}

    public KodeQuery<K, T> contains(String field, String value)
    {
        String query = " AND LOWER(" + field + ") LIKE ('%" + value.toLowerCase() + "%')";
        return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query + query);
    }

	public KodeQuery<K, T> in(String field, Object[] values)
	{
		return this.inOrNot(field, values, "IN");
	}

    public KodeQuery<K, T> notIn(String field, Object[] values)
	{
		return this.inOrNot(field, values, "NOT IN");
	}

	private KodeQuery<K, T> inOrNot(String field, Object[] values, String requirement)
	{
		String query = String.format(" AND %s %s (", field, requirement);
		for(Object value : values)
		{
			if(value instanceof String)
			{
				query += String.format("'%s', ", value.toString());
			}
			else
			{
				query += String.format("%s, ", value.toString());
			}
		}
		query = query.substring(0, query.length() - 2);
		query += ")";
		return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query + query);
	}

    public KodeQuery<K, T> orderBy(String field, String order)
    {
		Class fieldType = KodeSupport.getFieldType(this.objectClass1, field);
        String query = String.format(") ORDER BY %s %s", field, order);
		if(fieldType != null && fieldType.equals(String.class))
		{
			query = String.format(") ORDER BY LOWER(%s) %s", field, order);
		}
        return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query + query);
    }

    public KodeQuery<K, T> orderBy(String field)
    {
        return this.orderBy(field, "ASC");
    }

    public KodeQuery<K, T> or()
    {
        return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query + ") OR (1 < 2");
    }

    public KodeQuery<K, T> limit(int limit)
	{
		this.query += this.query.contains("ORDER") ? "" : ")";
		this.query += String.format(" LIMIT %s", limit);
		return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query);
	}

	public KodeQuery<K, T> limit(int limit, int from)
	{
		this.query += this.query.contains("ORDER") ? "" : ")";
		this.query += String.format(" LIMIT %s, %s", from, limit);
		return new KodeQuery<K, T>(this.kodeEngine, this.objectClass1, this.query);
	}

    public List<K> findAll(boolean fetchAll)
    {
        List<K> result = new ArrayList<>();
        this.query += this.query.contains("ORDER") ? "" : ")";
		Debug.log("QUERY", this.query);
        Cursor cursor = this.kodeEngine.readableDatabase == null ? this.kodeEngine.writableDatabase.rawQuery(this.query, null) : this.kodeEngine.readableDatabase.rawQuery(this.query, null);
        if(cursor.moveToFirst())
        {
            do
            {
                try
                {
                    result.add(KodeSupport.attachFromCursor(this.kodeEngine, this.objectClass1.getConstructor().newInstance(), cursor, fetchAll));
                }
                catch (Exception e)
                {
                    Log.e("findAll", "Exception raised in : " + e.getMessage());
                    e.printStackTrace();
                }
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public List<K> findAll()
    {
        return this.findAll(true);
    }

	public List<K> findAll(int top)
	{
		List<K> result = new ArrayList<>();
		this.query += String.format("%s LIMIT %s", this.query.contains("ORDER") ? "" : ")", top);
		Cursor cursor = this.kodeEngine.readableDatabase == null ? this.kodeEngine.writableDatabase.rawQuery(this.query, null) : this.kodeEngine.readableDatabase.rawQuery(this.query, null);
		if(cursor.moveToFirst())
		{
			do
			{
				try
				{
					result.add(KodeSupport.attachFromCursor(this.kodeEngine, this.objectClass1.getConstructor().newInstance(), cursor, true));
				}
				catch (Exception e)
				{
					Log.e("findAll", "Exception raised in : " + e.getMessage());
					e.printStackTrace();
				}
			}
			while (cursor.moveToNext());
		}
		cursor.close();
		return result;
	}

    public K findFirst()
    {
        this.query += String.format("%s LIMIT 1", this.query.contains("ORDER") ? "" : ")");
		Cursor cursor = this.getCursor();
        if(cursor.moveToFirst())
        {
            try
            {
                return KodeSupport.attachFromCursor(this.kodeEngine, this.objectClass1.getConstructor().newInstance(), cursor, true);
            }
            catch (Exception e)
            {
                Log.e("findFirst", "Exception raised in : " + e.getMessage());
                e.printStackTrace();
            }
        }
		Debug.log("QUERY first", this.query);
        cursor.close();
        return null;
    }

	public void execute()
	{
		this.query += ")";
		if(this.kodeEngine.writableDatabase != null)
		{
			this.kodeEngine.writableDatabase.execSQL(this.query);
		}
		else if(this.kodeEngine.readableDatabase != null)
		{
			this.kodeEngine.readableDatabase.execSQL(this.query);
		}
	}

	public int getInt(String column)
	{
		int result = -1;
		this.query += ")";
		Cursor cursor = this.getCursor();
		if(cursor.moveToFirst())
		{
			result = cursor.getInt(cursor.getColumnIndex(column));
		}
		return result;
	}

	public Cursor getCursor()
	{
		Cursor cursor = null;
		if(this.kodeEngine.writableDatabase != null)
		{
			cursor = this.kodeEngine.writableDatabase.rawQuery(this.query, null);
		}
		else if(this.kodeEngine.readableDatabase != null)
		{
			cursor = this.kodeEngine.readableDatabase.rawQuery(this.query, null);
		}
		return cursor;
	}
}
