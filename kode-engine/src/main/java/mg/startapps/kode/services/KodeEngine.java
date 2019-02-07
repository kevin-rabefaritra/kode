package mg.startapps.kode.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import mg.startapps.kode.exceptions.DatabaseException;
import mg.startapps.kode.model.KodeObject;
import mg.startapps.kode.model.KodeQuery;
import mg.startapps.kode.utils.KodeUtils;

/**
 * Created by Kevin Rabefaritra on 05/02/2017.
 */
public class KodeEngine
{
    public enum DatabaseType
    {
        DATABASE_READ, DATABASE_WRITE;
    }

    private SQLiteOpenHelper sqLiteBase;
	public SQLiteDatabase writableDatabase;
    public SQLiteDatabase readableDatabase;

    public static <E extends SQLiteOpenHelper> void init(Context context, Class<E> helperClass)
    {
        try
        {
            Constructor constructor = helperClass.getConstructor(Context.class);
            E instance = (E) constructor.newInstance(context);
            instance.close();
        }
        catch (Exception e)
        {
            Log.e("KodeEngine init", "Error while initializing KEngine : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static KodeEngine getInstanceFromDatabase(SQLiteDatabase sqLiteDatabase)
    {
        KodeEngine result = new KodeEngine();
        result.writableDatabase = sqLiteDatabase;
        return result;
    }

    public static KodeEngine getInstance(Context context, DatabaseType databaseType, Class<? extends SQLiteOpenHelper> helperClass)
    {
        KodeEngine result = new KodeEngine();
		try
		{
			result.sqLiteBase = helperClass.getConstructor(Context.class).newInstance(context);
			if(databaseType == DatabaseType.DATABASE_READ)
			{
				result.readableDatabase = result.sqLiteBase.getReadableDatabase();
			}
			if(databaseType == DatabaseType.DATABASE_WRITE)
			{
				result.writableDatabase = result.sqLiteBase.getWritableDatabase();
			}
			return result;
		}
		catch (Exception n)
		{
			return null;
		}
    }

    public static KodeEngine getInstance(Context context, Class<? extends SQLiteOpenHelper> helperClass)
    {
        return getInstance(context, DatabaseType.DATABASE_READ, helperClass);
    }

    public void beginTransaction()
    {
        if(this.writableDatabase != null)
        {
            this.writableDatabase.beginTransaction();
        }
    }

    public void commitTransaction()
    {
        if(this.writableDatabase != null)
        {
            this.writableDatabase.setTransactionSuccessful();
            this.writableDatabase.endTransaction();
        }
    }

    public void close()
    {
        if(this.writableDatabase != null)
        {
            // uncomment while using stathos
            // this.writableDatabase.close();
        }
        if(this.readableDatabase != null)
        {
            // this.readableDatabase.close();
        }
        if(this.sqLiteBase != null)
        {
            // this.sqLiteBase.close();
        }
    }

    public boolean isOpen()
    {
        if(this.writableDatabase != null && this.writableDatabase.isOpen())
        {
            return true;
        }
        else if(this.readableDatabase != null && this.readableDatabase.isOpen())
        {
            return true;
        }
        else return false;
    }

    public void createTables(Class<? extends KodeObject>... objectClasses)
    {
        for(Class<? extends KodeObject> objectClass : objectClasses)
        {
            try
            {
                KodeSupport.createTable(this.writableDatabase, objectClass);
            }
            catch (Exception e)
            {
                Log.d("createTables", "Exception in " + e.getMessage());
                break;
            }
        }
    }

    public void deleteTables(Class<? extends KodeObject>... objectClasses)
    {
        for(Class<? extends KodeObject> objectClass : objectClasses)
        {
            try
            {
                KodeSupport.deleteTable(this.writableDatabase, objectClass);
            }
            catch (Exception e)
            {
                break;
            }
        }
    }

    public int insertOrUpdate(@NonNull KodeObject object)
    {
		String primaryKeyColumn = String.format("%s.%s", KodeSupport.getTableName(object), KodeSupport.findPrimaryKeyColumn(object.getClass()));
        List<KodeObject> kodeObjectList = new ArrayList(this.get(object.getClass()).where(primaryKeyColumn, KodeSupport.getPrimaryKeyValue(object)).findAll());
		if(kodeObjectList.isEmpty())
		{
			this.insert(object);
		}
		else
		{
			this.update(object);
		}
		return KodeSupport.getPrimaryKeyValue(object);
    }

    public int insert(KodeObject object)
    {
        try
        {
			if(this.writableDatabase == null)
			{
				throw new DatabaseException();
			}

            List<KodeObject> objects = KodeSupport.extractKodeObjects(object);
            List<KodeObject> listObjects = KodeSupport.extractKodeListObjects(object);

            // inserting kode objects
            for(KodeObject kodeObject : objects)
            {
                Integer id = KodeSupport.getPrimaryKeyValue(kodeObject);
                // the object haven't been saved yet
                if(id == null || id == 0)
                {
                    id = insert(kodeObject);
                }
                // set the id for the kode object
                Method setter = kodeObject.getClass().getMethod(KodeUtils.generateSetterName(KodeSupport.findPrimaryKeyColumn(kodeObject.getClass())), Integer.TYPE);
                setter.invoke(kodeObject, id);

                // set the kode object in the main object
                setter = object.getClass().getMethod(KodeUtils.generateSetterName(kodeObject.getClass().getSimpleName()), kodeObject.getClass());
                setter.invoke(object, kodeObject);
            }

            ContentValues contentValues = KodeSupport.fillContentValues(object);
            int result = (int) this.writableDatabase.insertOrThrow(KodeSupport.getTableName(object), null, contentValues);

            // we store the result as we need the object autoincrement value
            Method setter = object.getClass().getMethod(KodeUtils.generateSetterName(KodeSupport.findPrimaryKeyColumn(object.getClass())), Integer.TYPE);
            setter.invoke(object, result);

            // inserting kode list
            for(KodeObject kodeObject : listObjects)
            {
                this.writableDatabase.insert(KodeSupport.getAssociationName(object.getClass(), kodeObject.getClass()), null, KodeSupport.fillContentValuesAssociation(object, kodeObject));
            }
            return result;
        }
		catch (SQLiteConstraintException sqLiteConstraintException)
		{
			// eg : primary key or unique
			Log.d("insert", sqLiteConstraintException.getMessage());
			return -1;
		}
        catch (Exception e)
        {
            Log.d("insert", "Error when inserting " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

	public void insert(String table, ContentValues contentValues)
	{
		this.writableDatabase.insertOrThrow(table, null, contentValues);
	}

    public void update(KodeObject object)
	{
        // Log.d("update", "WHERE " + String.format("%s = %s", KodeSupport.findPrimaryKeyColumn(object.getClass()), KodeSupport.getPrimaryKeyValue(object)));
		String primaryKeyColumn = String.format("%s.%s", KodeSupport.getTableName(object), KodeSupport.findPrimaryKeyColumn(object.getClass()));
		this.writableDatabase.update(KodeSupport.getTableName(object),
				KodeSupport.fillContentValues(object),
				String.format("%s = %s", primaryKeyColumn, KodeSupport.getPrimaryKeyValue(object)), null);
	}

    public KodeQuery run(String query)
    {
        return new KodeQuery(this, query);
    }

	public <K extends KodeObject> KodeQuery<K, ? extends KodeObject> delete(Class<K> objectClass)
	{
		return new KodeQuery(this, objectClass, KodeQuery.QueryType.DELETE);
	}

    public <K extends KodeObject> KodeQuery<K, ? extends KodeObject> get(Class<K> objectClass)
    {
        return new KodeQuery<>(this, objectClass);
    }

    public <K extends KodeObject, T extends KodeObject> KodeQuery<K, T> get(Class<K> objectClass1, Class<T> objectClass2)
    {
        return new KodeQuery<>(this, objectClass1, objectClass2);
    }

    public int count(Class<? extends KodeObject> objectClass)
	{
		return this.count(objectClass, null);
	}

    public int count(Class<? extends KodeObject> objectClass, String condition)
	{
		String query = KodeSupport.generateCountQuery(objectClass);
		query += condition == null ? "" : condition;
		// generatecountquery has "where (1<2"
		return new KodeQuery<>(this, objectClass, query).getInt("result");
	}

    public <K extends KodeObject, T extends KodeObject> KodeQuery<K, T> query(String objectClass)
    {
        return new KodeQuery<>(this, objectClass);
    }
}
