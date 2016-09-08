package net.xcordio.vmmanagerservice.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * A set of static utils for working with classes reflectively.
 * List of functionalities:
 * <ul>
 *     <li>find field by name;</li>
 *     <li>find method by name and arguments;</li>
 * </ul>
 */
public class ReflectiveUtils {
	
	protected static Field findField(Class<?> clazz, String fieldName) {
		Field found = null;
		for (Field f : clazz.getFields()) {
			// not touching static, final nor transient fields
			if ((f.getModifiers() & Modifier.STATIC) != 0 ||
					(f.getModifiers() & Modifier.FINAL) != 0 ||
					(f.getModifiers() & Modifier.TRANSIENT) != 0) continue;
			if (f.getName().equalsIgnoreCase(fieldName))
				if (found == null) found = f;
				else throw new ConfigurationException("multiple matching fields found; looking for `" + fieldName
						+ "`, found `" + found.getName() + "` and `" + f.getName() + "`, configuration can't be done"
						+ " in this ambiguous situation");
		}
		return found;
	}
	
	/** Finds methods with given name, no matter of how much arguments it has. */
	protected static Method findMethod(Class<?> clazz, String methodName) {
		return findMethod(clazz, methodName, (Class<?>[]) null);
	}
	
	protected static Method findMethod(Class<?> clazz, String methodName, Class<?>... argTypes) {
		Method found = null;
		int maxArgs = argTypes != null ? argTypes.length : Integer.MAX_VALUE;
		int minArgs = argTypes != null ? maxArgs : 0;
		while (minArgs > 0 && argTypes[minArgs - 1] == null) minArgs --;
		method_search:
		for (Method m : clazz.getMethods()) {
			if (!m.getName().equalsIgnoreCase(methodName)) continue;
			if (argTypes != null) {
				Type[] types = m.getParameterTypes();
				if (types.length < minArgs || types.length > maxArgs) continue;
				for (int i = 0; i < types.length; i ++)
					if (argTypes[i] != null && !types[i].equals(argTypes[i])) continue method_search;
			}
			if (found == null) found = m;
			else throw new ConfigurationException("ambiguous situation: multiple matching method found; looking for `" + methodName
					+ "`, found `" + found.getName() + "` and `" + m.getName() + "`, configuration can't be done");
		}
		return found;
	}
}
