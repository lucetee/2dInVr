package com.example.nean.whitescreen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Utils {

    public static String getProperty(String propertyName, String defValue) {
        String result = defValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", new Class[] {String.class, String.class});
            Object retVal = method.invoke(null, propertyName, defValue);
            result = String.valueOf(retVal);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
        }
//		Log.i(Constants.LOG_TAG, "getProperty:" + propertyName + " value is:" + result);
        return result;
    }
}
