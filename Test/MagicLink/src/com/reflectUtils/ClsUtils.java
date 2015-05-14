package com.reflectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.util.Log;

public class ClsUtils {	
	
	@SuppressWarnings("rawtypes")
	public Object getProperty(Object owner, String fieldName) throws Exception {
	    Class ownerClass = owner.getClass();
	    Field field = ownerClass.getField(fieldName);
	    Object property = field.get(owner);
	    return property;
	}
	
	@SuppressWarnings("rawtypes")
	public Object getStaticProperty(String className, String fieldName)throws Exception {
	    Class ownerClass = Class.forName(className);	
	    Field field = ownerClass.getField(fieldName);	
	    Object property = field.get(ownerClass);	
	    return property;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public Object invokeMethod(Object owner, String methodName) throws Exception {
		Class ownerClass = owner.getClass();
		Method method = ownerClass.getMethod(methodName,null);
		return method.invoke(owner,null);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public Object invokeMethod(Object owner, String methodName, Object arg) throws Exception {
		Class ownerClass = owner.getClass();
		if(arg != null) {
			Class argClass = arg.getClass();
			Method method = ownerClass.getMethod(methodName, argClass);
			return method.invoke(owner, arg);
		}
		else {
		    throw new Exception(owner.toString()+"."+methodName+": Invalid argument!");
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public Object invokeMethod(Object owner, String methodName, Object[] args) throws Exception {
		Class ownerClass = owner.getClass();
		if(args!=null) {
			Class[] argsClass = new Class[args.length];
			for (int i = 0, j = args.length; i < j; i++) {
				argsClass[i] = args[i].getClass();
			}
			Method method = ownerClass.getMethod(methodName, argsClass);
			return method.invoke(owner, args);
		}
		else {
		    throw new Exception(owner.toString()+"."+methodName+": Invalid argument!");
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public Object invokeStaticMethod(String className, String methodName) throws Exception {
	    Class ownerClass = Class.forName(className);
	    Method method = ownerClass.getMethod(methodName);
		return method.invoke(null,null);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public Object invokeStaticMethod(String className, String methodName, Object arg) throws Exception {
	    Class ownerClass = Class.forName(className);
	    if(arg != null) {
		    Class argClass = arg.getClass();
		    Method method = ownerClass.getMethod(methodName, argClass);
		    return method.invoke(null, arg);
	    }
	    else {    	
		    throw new Exception(className+"."+methodName+": Invalid argument!");
	    }
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public Object invokeStaticMethod(String className, String methodName, Object[] args) throws Exception {
	    Class ownerClass = Class.forName(className);
	    if(args != null) {
		    Class[] argsClass = new Class[args.length];
		    for (int i = 0, j = args.length; i < j; i++) {
		        argsClass[i] = args[i].getClass();
		    }
		    Method method = ownerClass.getMethod(methodName, argsClass);
		    return method.invoke(null, args);
	    }
	    else {    	
	    	throw new Exception(className+"."+methodName+": Invalid argument!");
	    }
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public Object newInstance(String className, Object[] args) throws Exception {
	    Class newoneClass = Class.forName(className);
	    if(args!=null) {
		    Class[] argsClass = new Class[args.length];
		    for (int i = 0, j = args.length; i < j; i++) {
		        argsClass[i] = args[i].getClass();
		    }
		    Constructor cons = newoneClass.getConstructor(argsClass);
		    return cons.newInstance(args);
	    }
	    else {
	    	return newoneClass.newInstance();	    	
	    }
	}
	
	/**
	 * 
	 * @param clsShow
	 */
	@SuppressWarnings("rawtypes")
	static public void printAllInform(Class clsShow) {
		Log.e("reflect", clsShow.getName());
		try {
			// 取得所有方法
			Method[] hideMethod = clsShow.getMethods();
			int i = 0;
			for (; i < hideMethod.length; i++) {
				Log.e("reflect", hideMethod[i].getName());
			}
			// 取得所有常量
			Field[] allFields = clsShow.getFields();
			for (i = 0; i < allFields.length; i++) {
				Log.e("reflect", allFields[i].getName());
			}
		} catch (SecurityException e) {
			// throw new RuntimeException(e.getMessage());
			Log.e("reflect", e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// throw new RuntimeException(e.getMessage());
			Log.e("reflect", e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("reflect", e.getMessage());
			e.printStackTrace();
		}
	}
}

