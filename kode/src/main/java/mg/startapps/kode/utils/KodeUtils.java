package mg.startapps.kode.utils;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import mg.startapps.kode.model.KodeList;
import mg.startapps.kode.model.KodeObject;
import mg.startapps.kode.services.KodeSupport;

/**
 * Created by Kevin Rabefaritra on 05/02/2017.
 */
public class KodeUtils
{
    public static boolean isInArray(Object value, Object[] array)
    {
        for(Object object : array)
        {
            if(object.equals(value))
            {
                return true;
            }
        }
        return false;
    }

    public static String generateGetterName(String field)
    {
        return String.format("get%s%s", field.substring(0, 1).toUpperCase(), field.substring(1));
    }

    public static String generateGetterName(Field field)
    {
        return generateGetterName(field.getName());
    }

    public static String generateSetterName(String field)
    {
        return String.format("set%s%s", field.substring(0, 1).toUpperCase(), field.substring(1));
    }

    public static String generateSetterName(Field field)
    {
        return generateSetterName(field.getName());
    }

    // build a list of K from list of ids (as Primary Key). We assume PK are Integer.TYPE
    public static <K extends KodeObject> KodeList<K> buildKodeModel(Class<K> objectClass, int[] ids)
    {
        KodeList<K> result = new KodeList<>();
        for(int i = 0; i < ids.length; i++)
        {
            try
            {
                K object = objectClass.getConstructor().newInstance();
                Method setter = objectClass.getMethod(generateSetterName(KodeSupport.findPrimaryKeyColumn(objectClass)), Integer.TYPE);
                setter.invoke(object, ids[i]);
                result.add(object);
            }
            catch(Exception e)
            {
                Log.d("buildKodeModel", e.getMessage());
                e.printStackTrace();
            }
        }
        return result;
    }

    public static List<Class<? extends KodeObject>> getKodeListFieldsType(Class<? extends KodeObject> objectClass)
    {
        List<Class<? extends KodeObject>> result = new ArrayList<>();
        Field[] fields = KodeSupport.getFields(objectClass);
        for(int i = 0; i < fields.length; i++)
        {
            if(fields[i].getType().equals(KodeList.class))
            {
                if (fields[i].getGenericType() instanceof ParameterizedType)
                {
                    result.add(getKodeListParameterizedType(fields[i]));
                }
            }
        }
        return result;
    }

    public static Class<? extends KodeObject> getKodeListParameterizedType(Field field)
    {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        for (Type type : typeArguments)
        {
            Class<? extends KodeObject> aClass = (Class<? extends KodeObject>) type;
            // KodeList is a parameterized type
            return aClass;
        }
        return null;
    }

    public static List<Field> getKodeListFields(Class<? extends KodeObject> objectClass)
    {
        List<Field> result = new ArrayList<>();
        Field[] fields = KodeSupport.getFields(objectClass);
        for (Field field : fields)
        {
            if (field.getType().equals(KodeList.class))
            {
                result.add(field);
            }
        }
        return result;
    }

    public static boolean isKodeObject(Class objectClass)
    {
        return !objectClass.isPrimitive() && objectClass.getSuperclass().equals(KodeObject.class);
    }

    public static boolean isKodeList(Class objectClass)
    {
        return !objectClass.isPrimitive() && objectClass.equals(KodeList.class);
    }
}