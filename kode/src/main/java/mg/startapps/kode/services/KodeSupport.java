package mg.startapps.kode.services;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import mg.startapps.kode.annotations.AutoIncrement;
import mg.startapps.kode.annotations.Ignore;
import mg.startapps.kode.annotations.PrimaryKey;
import mg.startapps.kode.annotations.Unique;
import mg.startapps.kode.model.KodeList;
import mg.startapps.kode.model.KodeObject;
import mg.startapps.kode.model.KodeQuery;
import mg.startapps.kode.utils.KodeUtils;

/**
 * Created by Kevin Rabefaritra on 05/02/2017.
 */
public class KodeSupport
{
    /***************************** TABLE OPERATIONS ***************************/
    public static String generateTableCreate(@NonNull Class<? extends KodeObject> objectClass)
    {
        Field[] fields = getFields(objectClass);
        String result = String.format("CREATE TABLE %s(", getTableName(objectClass));
        for(int i = 0; i < fields.length; i++)
        {
            if(KodeUtils.isKodeObject(fields[i].getType()))
            {
                result += String.format("id_%s INTEGER", fields[i].getType().getSimpleName().toLowerCase());
                result += ",";
            }
            else
            {
                // we don't store kodelist fields
                if(!fields[i].getType().equals(KodeList.class))
                {
                    result += fields[i].getName().toLowerCase() + " ";
                    if (KodeUtils.isInArray(fields[i].getType().getSimpleName().toLowerCase(), new String[]{"int", "short"}))
                    {
                        result += "INTEGER";
                    }
                    else if (KodeUtils.isInArray(fields[i].getType().getSimpleName().toLowerCase(), new String[]{"double", "float"}))
                    {
                        result += "REAL";
                    }
                    else if (KodeUtils.isInArray(fields[i].getType().getSimpleName().toLowerCase(), new String[]{"string"}))
                    {
                        result += "TEXT";
                    }

                    if (fields[i].isAnnotationPresent(PrimaryKey.class))
                    {
                        result += " PRIMARY KEY";
                    }
					if(fields[i].isAnnotationPresent(Unique.class))
					{
						result += " UNIQUE";
					}
                    result += ",";
                }
            }
        }
        result = result.substring(0, result.length() - 1);
        result += ");";
        Log.d("CREATE TABLE", result);
        return result;
    }

    public static String generateAssociationCreate(Class<? extends KodeObject> objectClass1, Class<? extends KodeObject> objectClass2)
    {
        String result = String.format("CREATE TABLE %s_%s(", getTableName(objectClass1), getTableName(objectClass2));
        result += "id INTEGER PRIMARY KEY,";
        result += String.format("id_%s INTEGER,", getTableName(objectClass1));
        result += String.format("id_%s INTEGER", getTableName(objectClass2));
        result += ");";
        Log.d("CREATE ASSOCIATION", result);
        return result;
    }

    private static int countNonIgnoredFields(Class<? extends KodeObject> objectClass)
    {
        Field[] fields = objectClass.getDeclaredFields();
        int result = fields.length;
        for(int i = 0; i < fields.length; i++)
        {
            if(fields[i].isAnnotationPresent(Ignore.class))
            {
                result--;
            }
        }
        return result;
    }

    public static Field[] getFields(Class<? extends KodeObject> objectClass)
    {
        Field[] fields = objectClass.getDeclaredFields();
        Field[] result = new Field[countNonIgnoredFields(objectClass)];
        for(int i = 0, j = 0; i < fields.length; i++)
        {
            if(!fields[i].isAnnotationPresent(Ignore.class))
            {
                result[j] = fields[i];
                j++;
            }
        }
        return result;
    }

	public static Field[] getFields(Class<? extends KodeObject> objectClass, String[] fieldNames)
	{
		Field[] result = new Field[fieldNames.length];
		Field[] fields = getFields(objectClass);
		int i = 0;
		for(int j = 0; j < fieldNames.length; j++)
		{
			for (int k = 0; k < fields.length; k++)
			{
				if(fields[k].getName().toLowerCase().equals(fieldNames[j].toLowerCase()))
				{
					result[i] = fields[k];
					i++;
				}
			}
		}
		return result;
	}

    public static String generateTableDelete(@NonNull Class<? extends KodeObject> objectClass)
    {
        return String.format("DROP TABLE IF EXISTS %s", getTableName(objectClass));
    }

    public static String generateAssociationDelete(@NonNull Class<? extends KodeObject> objectClass1, @NonNull Class<? extends KodeObject> objectClass2)
    {
        return String.format("DROP TABLE IF EXISTS %s_%s", getTableName(objectClass1), getTableName(objectClass2));
    }

    public static String generateSelectQuery(Class<? extends KodeObject> objectClass)
    {
        List<Field> kodeObjectFields = getKodeObjectsFields(objectClass);
        String query = String.format("SELECT %s FROM %s", generateColumnsListing(objectClass, kodeObjectFields, true), getTableName(objectClass));
        for(Field field : kodeObjectFields)
        {
            query += String.format(" JOIN %s ON %s.id_%s = %s.%s",
                    field.getName().toLowerCase(),
                    objectClass.getSimpleName().toLowerCase(),
                    field.getName().toLowerCase(),
                    field.getName().toLowerCase(),
                    findPrimaryKeyColumn((Class<? extends KodeObject>) field.getType()));
        }
        return query;
    }

    // select from associations
    public static String generateSelectQuery(Class<? extends KodeObject> objectClass1, Class<? extends KodeObject> objectClass2)
    {

        String query = String.format("SELECT %s.* FROM %s JOIN %s ON %s.id_%s = %s.%s",
                    getTableName(objectClass1),
                    getAssociationName(objectClass2, objectClass1),
                    getTableName(objectClass1),
                    getAssociationName(objectClass2, objectClass1),
                    getTableName(objectClass1),
                    getTableName(objectClass1),
                    findPrimaryKeyColumn(objectClass1));
        return query;
    }

    public static String generateDeleteQuery(Class<? extends KodeObject> objectClass)
	{
		return generateDeleteQuery(getTableName(objectClass));
	}

    public static String generateDeleteQuery(String table)
    {
        String query = String.format("DELETE FROM %s", table);
        return query;
    }

	public static String generateCountQuery(Class<? extends KodeObject> objectClass)
	{
		return generateCountQuery(getTableName(objectClass));
	}

	public static String generateCountQuery(String objectClass)
	{
		String query = String.format("SELECT COUNT(*) AS result FROM %s WHERE (1<2", objectClass);
		return query;
	}

	public static String generateQuery(Class<? extends KodeObject> objectClass, KodeQuery.QueryType queryType, @Nullable String suffix)
	{
		String query = "";
		if(queryType == KodeQuery.QueryType.SELECT)
		{
			query = generateSelectQuery(objectClass);
		}
		else if(queryType == KodeQuery.QueryType.DELETE)
		{
			query = generateDeleteQuery(objectClass);
		}
		query = query + (suffix == null ? "" : suffix);
		return query;
	}

    public static String generateColumnsListing(Class<? extends KodeObject> objectClass, @Nullable List<Field> kodeFields, boolean withId)
    {
        String result = "";
        Field[] fields = getFields(objectClass);
        for(int i = 0; i < fields.length; i++)
        {
            // Log.d("CHECK", String.format("is %s a kodelist? %s", fields[i].getName(), KodeUtils.isKodeList(fields[i].getType())));
            // in case we have a foreign key (ie KodeObject as field), we specify its id
            if(KodeUtils.isKodeObject(fields[i].getType()))
            {
                result += objectClass.getSimpleName().toLowerCase() + ".id_" + fields[i].getName().toLowerCase() + ", ";
            }
            else
            {
                // we don't considerate kodelist
                if(!KodeUtils.isKodeList(fields[i].getType()))
                {
                    /* if the field is not a primary key
                    * the objective is to avoid specifying the id of a kode object twice as it may cause a column conflict
                    */
                    if (!fields[i].isAnnotationPresent(PrimaryKey.class))
                    {
                        result += objectClass.getSimpleName().toLowerCase() + "." + fields[i].getName().toLowerCase() + ", ";
                    }
                    else
                    {
                        /* the field is a primary key
                        * if the primary is already specified in the query, we won't add it anymore (because that means that this function calling comes from
                        * a recursive one
                        * if kodeFields is not empty, i.e there are kodeobject fields within the object, so we provide the id
                        */
                        if (withId)
                        {
                            result += objectClass.getSimpleName().toLowerCase() + "." + fields[i].getName().toLowerCase() + ", ";
                        }
                    }
                }
            }
        }
        if(kodeFields != null && !kodeFields.isEmpty())
        {
            for(Field field : kodeFields)
            {
                result += generateColumnsListing((Class<? extends KodeObject>) field.getType(), null, false);
                // should not assume null for 2nd level kodeobject in next projects
            }
            return result;
        }
        return result.substring(0, result.length() - 2);
    }

    /********************************************** INSERT ***************************************/
    public static ContentValues fillContentValues(@NonNull KodeObject object)
    {
        ContentValues result = new ContentValues();
        Field[] fields = getFields(object.getClass());
        for(int i = 0; i < fields.length; i++)
        {
            try
            {
                Method getter = object.getClass().getMethod(KodeUtils.generateGetterName(fields[i]));
                Object value = getter.invoke(object);

                // we only insert values which are not autoincrement or kodeobject instances
                if(!fields[i].isAnnotationPresent(AutoIncrement.class) || KodeUtils.isKodeObject(fields[i].getType()))
                {
                    String fieldName = fields[i].getName().toLowerCase();
                    // Log.d("Calling getter", KodeUtils.generateGetterName(fields[i]) + " with value " + value.toString() + " in " + fieldName);
                    if (value instanceof Integer)
                    {
                        result.put(fieldName, Integer.valueOf(value.toString()));
                    }
                    else if(value instanceof Short)
                    {
                        result.put(fieldName, Short.valueOf(value.toString()));
                    }
                    else if (value instanceof String)
                    {
                        result.put(fieldName, (String) value);
                    }
                    else if (value instanceof Double)
                    {
                        result.put(fieldName, Double.valueOf(value.toString()));
                    }
                    else if(value instanceof KodeObject)
                    {
                        // we set put id of the kodeobject (in Sqlite, we have the kodeobject as an FK)
                        KodeObject valueObject = (KodeObject) value;
                        Method getterId = value.getClass().getMethod(KodeUtils.generateGetterName(KodeSupport.findPrimaryKeyColumn(valueObject.getClass())));
                        result.put(String.format("id_%s", value.getClass().getSimpleName().toLowerCase()), Integer.parseInt(getterId.invoke(value).toString()));
                    }
                }
                else if(fields[i].isAnnotationPresent(AutoIncrement.class))
                {
					// we insert autoincrement value if value is provided (!= 0)
					String fieldName = fields[i].getName().toLowerCase();
					Integer fieldValue = Integer.valueOf(value.toString());
					// autoincrement = field is an integer
					if(fieldValue != 0)
					{
						result.put(fieldName, fieldValue);
					}
                }
            }
            catch (NoSuchMethodException n)
            {
                Log.d("fillContentValues", "Method " + KodeUtils.generateGetterName(fields[i]) + " is not defined");
            }
            catch (Exception e)
            {
                Log.d("fillContentValues", "Other exception " + e.getMessage());
            }
        }
        return result;
    }

	public static ContentValues fillContentValues(String[] keys, Object[] values)
	{
		ContentValues result = new ContentValues();
		if(keys.length == values.length)
		{
			for(int i = 0; i < keys.length; i++)
			{
				if (values[i] instanceof Integer)
				{
					result.put(keys[i], Integer.valueOf(values[i].toString()));
				}
				else if(values[i] instanceof Short)
				{
					result.put(keys[i], Short.valueOf(values[i].toString()));
				}
				else if (values[i] instanceof String)
				{
					result.put(keys[i], (String) values[i]);
				}
				else if (values[i] instanceof Double)
				{
					result.put(keys[i], Double.valueOf(values[i].toString()));
				}
			}
			return result;
		}
		else
		{
			return null;
		}
	}

    // filling id only
    public static ContentValues fillContentValuesAssociation(@NonNull KodeObject objectA, @NonNull KodeObject objectB)
    {
        ContentValues result = new ContentValues();
        result.put(String.format("id_%s", getTableName(objectA)), getPrimaryKeyValue(objectA));
        result.put(String.format("id_%s", getTableName(objectB)), getPrimaryKeyValue(objectB));
        return result;
    }

    public static void createTable(@NonNull SQLiteDatabase writableDatabase, @NonNull Class<? extends KodeObject> objectClass)
    {
        writableDatabase.execSQL(generateTableCreate(objectClass));
        List<Class<? extends KodeObject>> kodeListFields = KodeUtils.getKodeListFieldsType(objectClass);
        for(Class<? extends KodeObject> kodeClass : kodeListFields)
        {
            writableDatabase.execSQL(generateAssociationCreate(objectClass, kodeClass));
        }
    }

    public static void deleteTable(@NonNull SQLiteDatabase writableDatabase, @NonNull Class<? extends KodeObject> objectClass)
    {
        writableDatabase.execSQL(generateTableDelete(objectClass));
        List<Class<? extends KodeObject>> kodeListFields = KodeUtils.getKodeListFieldsType(objectClass);
        for(Class<? extends KodeObject> kodeClass : kodeListFields)
        {
            writableDatabase.execSQL(generateAssociationDelete(objectClass, kodeClass));
        }
    }

    public static String getTableName(Class<? extends KodeObject> object)
    {
        return object.getSimpleName().toLowerCase();
    }

    public static String getAssociationName(Class<? extends KodeObject> object1, Class<? extends KodeObject> object2)
    {
        return String.format("%s_%s", object1.getSimpleName().toLowerCase(), object2.getSimpleName().toLowerCase());
    }

    public static String getTableName(KodeObject object)
    {
        return getTableName(object.getClass());
    }

    public static String findPrimaryKeyColumn(Class<? extends KodeObject> objectClass)
    {
        Field[] fields = objectClass.getDeclaredFields();
        for(int i = 0; i < fields.length; i++)
        {
            if(fields[i].isAnnotationPresent(PrimaryKey.class))
            {
                return fields[i].getName().toLowerCase();
            }
        }
        return null;
    }

    public static Integer getPrimaryKeyValue(KodeObject object)
    {
        String primaryKeyField = findPrimaryKeyColumn(object.getClass());
        if(primaryKeyField != null)
        {
            try
            {
                Method getter = object.getClass().getMethod(KodeUtils.generateGetterName(primaryKeyField));
                return Integer.parseInt(getter.invoke(object).toString());
            }
            catch (Exception e)
            {
                Log.e("getPrimaryKeyValue", e.getMessage());
				e.printStackTrace();
            }
        }
        return null;
    }

    public static List<KodeObject> extractKodeObjects(KodeObject object)
    {
        List<KodeObject> result = new ArrayList<>();
        Field[] fields = getFields(object.getClass());
        for(int i = 0; i < fields.length; i++)
        {
            if(KodeUtils.isKodeObject(fields[i].getType()))
            {
                try
                {
                    Method getter = object.getClass().getMethod(KodeUtils.generateGetterName(fields[i]));
                    KodeObject value = (KodeObject) getter.invoke(object);
                    result.add(value);
                }
                catch (Exception e)
                {
                    Log.e("extractKodeObjects", "Error calling : " + KodeUtils.generateGetterName(fields[i]));
                }
            }
        }
        return result;
    }

    public static List<KodeObject> extractKodeListObjects(KodeObject object)
    {
        List<KodeObject> result = new ArrayList<>();
        Field[] fields = getFields(object.getClass());
        for(int i = 0; i < fields.length; i++)
        {
            if(KodeUtils.isKodeList(fields[i].getType()))
            {
                try
                {
                    Method getter = object.getClass().getMethod(KodeUtils.generateGetterName(fields[i]));
                    KodeList<? extends KodeObject> value = (KodeList<? extends KodeObject>) getter.invoke(object);
					if(value != null)
					{
						for (KodeObject kodeObject : value)
						{
							result.add(kodeObject);
						}
					}
                }
                catch (Exception e)
                {
                    Log.e("extractKodeListObjects", "Error calling : " + KodeUtils.generateGetterName(fields[i]));
					e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static List<Field> getKodeObjectsFields(Class<? extends KodeObject> objectClass)
    {
        List<Field> result = new ArrayList<>();
        Field[] fields = getFields(objectClass);
        for(int i = 0; i < fields.length; i++)
        {
            if(KodeUtils.isKodeObject(fields[i].getType()))
            {
                result.add(fields[i]);
            }
        }
        return result;
    }

    // for reading data
    // v1.1 : cursor also includes join
    public static <K extends KodeObject> K attachFromCursor(KodeEngine kodeEngine, K object, Cursor cursor, boolean fetchAll)
    {
        try
        {
            K kodeObject = (K) object.getClass().getConstructor().newInstance();
            Field[] fields = getFields(kodeObject.getClass());
            for(int i = 0; i < fields.length; i++)
            {
                Method setter = object.getClass().getMethod(KodeUtils.generateSetterName(fields[i]), fields[i].getType());

				// column control
				// e.g : field "place", if doesn't exist (columnIndex == -1), we search for id_place
				int columnIndex = cursor.getColumnIndex(fields[i].getName().toLowerCase());

                /*if(columnIndex == -1)
                {
                    columnIndex = cursor.getColumnIndex("id_" + fields[i].getName().toLowerCase());
                }*/

                if (fields[i].getType().equals(Integer.TYPE))
                {
					if(fields[i].isAnnotationPresent(PrimaryKey.class))
					{
						// if primary key, we serach for id_place, then id if doesnt exists
						columnIndex = cursor.getColumnIndex("id_" + getTableName(kodeObject));
						if(columnIndex == -1)
						{
							columnIndex = cursor.getColumnIndex(fields[i].getName().toLowerCase());
						}
					}
                    setter.invoke(kodeObject, cursor.getInt(columnIndex));
                }
                else if (fields[i].getType().equals(String.class))
                {
                    setter.invoke(kodeObject, cursor.getString(columnIndex));
                }
                else if (fields[i].getType().equals(Double.TYPE))
                {
                    setter.invoke(kodeObject, cursor.getDouble(columnIndex));
                }
                else if (fields[i].getType().equals(Short.TYPE))
                {
                    setter.invoke(kodeObject, cursor.getShort(columnIndex));
                }
                else if (KodeUtils.isKodeObject(fields[i].getType()))
                {
                    KodeObject object1 = (KodeObject) fields[i].getType().getConstructor().newInstance();
                    setter.invoke(kodeObject, attachFromCursor(kodeEngine, object1, cursor, true));
                }
                else if (KodeUtils.isKodeList(fields[i].getType()))
                {
                    List<? extends KodeObject> listValue = new ArrayList<>();

                    // if field is a kodelist, we'll fetch the data
                    Method kodeListSetter = kodeObject.getClass().getMethod(KodeUtils.generateSetterName(fields[i]), KodeList.class);

                    if(fetchAll)
                    {
                        int columnIdIndex = cursor.getColumnIndex(findPrimaryKeyColumn(kodeObject.getClass()));
						if(columnIdIndex == -1)
						{
							// in case the object is a foreign key. eg : Place -> id_place (instead of id)
							columnIdIndex = cursor.getColumnIndex(String.format("id_%s", getTableName(kodeObject.getClass())));
						}
                        int idObject = cursor.getInt(columnIdIndex);

                        Class parameterizedType = KodeUtils.getKodeListParameterizedType(fields[i]);
                        listValue = kodeEngine.get(parameterizedType, kodeObject.getClass()).
                                where(String.format("id_%s", getTableName(kodeObject.getClass())), idObject).findAll();
                    }
                    kodeListSetter.invoke(kodeObject, new KodeList(listValue));
                }
            }
            return kodeObject;
        }
        catch (Exception e)
        {
            Log.e("attachFromCursor", "Error : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

	public static <E extends KodeObject> E buildKodeObject(JSONObject jsonObject, Field[] fields, String[] jsonFields, Class<E> objectClass)
	{
		try
		{
			E result = objectClass.getConstructor().newInstance();
			for(int i = 0; i < fields.length; i++)
			{
				// Log.d("CAL", "CAlling " + fields[i].getName() + " with " + jsonFields[i]);
				Method setter = objectClass.getMethod(KodeUtils.generateSetterName(fields[i]), fields[i].getType());
				if (fields[i].getType().equals(Integer.TYPE))
				{
					setter.invoke(result, jsonObject.getInt(jsonFields[i]));
				}
				else if (fields[i].getType().equals(String.class))
				{
					setter.invoke(result, jsonObject.getString(jsonFields[i]));
				}
				else if (fields[i].getType().equals(Double.TYPE))
				{
					setter.invoke(result, jsonObject.getDouble(jsonFields[i]));
				}
				else if (fields[i].getType().equals(Short.TYPE))
				{
					setter.invoke(result, (short) jsonObject.getInt(jsonFields[i]));
				}
				// we can't parse kodeObject and kodeList fields
			}
			return result;
		}
		catch (Exception e)
		{
			Log.e("buildKodeObject", e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}