package net.xcordio.vmmanagerservice.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.*;
import java.math.BigInteger;
import java.math.BigDecimal;

import net.semplar.log.Logger;
import static net.xcordio.vmmanagerservice.util.ReflectiveUtils.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A kinky singleton tool used to apply config to objects tree. It's used instead of huge Spring IoC stuff.
 * Details worth attention:
 * <ul>
 * <li>When JSON has list of objects, like <code>[{...},{...}]</code>, and this needs to be mapped to
 *   Java field like:<code><pre>
 *     public List&lt;Person&gt; persons;
 *   </pre></code>
 *   Where every person can be one of subclasses, like Driver, Chef, Coder and Attendant, there are 2 steps:
 *   <ol>
 *     <li>Add fullType definition in every dict, so it will look like:<code><pre>
 *       "persons": [{
 *         "fullType": "driver",
 *         "licenceYear": 2016
 *         // other driver fields filled in
 *       }, {
 *         "fullType": "chef",
 *         "rank": "blackBeltForPizza"
 *         // other cheef fields filled in
 *       }]
 *     </pre></code></li>
 *     <li>In java code, create a <b>allocator</b> method next to field declaration:<code><pre>
 *       public Person allocPerson(String fullType, JSONObject obj) {
 *         if ("driver".equals(fullType)) return new Driver();
 *         if ("chef".equals(fullType)) return new Chef();
 *         // ... other mappings; you can use any kind of efficient mapping like HashMap or other
 *         throw new IllegalArgumentException("unmapped Person fullType found in configuration");
 *       }
 *     </pre></code></li>
 *   </ol>
 *   Then this tool will automatically handle situation by reflectively calling provided allocator method.
 * </li>
 * <li>When you need map any kind of JSON data to Java data, you can use <b>converter</b> method as follows:<code><pre>
 *     "primaryCoffee": "cappucino",
 *     "alternativeCoffee": "american",
 *   </pre></code>
 *   In java code, create a <b>converter</b> method next to field declaration:<code><pre>
 *     public Coffee primaryCoffee;
 *     public Coffee alternativeCoffee;
 *     public Person convertCoffee(Object obj) { // using Object here, because it can be anything from JSON
 *       if ("cappucino".equals(fullType)) return new Cappucino();
 *       if ("american".equals(fullType)) return new AmericanCoffee();
 *       // ... other mappings; you can use enums or any other kind of efficient mapping like HashMap
 *       throw new IllegalArgumentException("unmapped Coffee found in configuration");
 *     }
 *   </pre></code>
 *   Then this tool will automatically handle situation by reflectively calling provided converter method.
 * </li>
 * <li>To track field configuration writes of field like:<code><pre>
 *     public CarConfiguration carConfig;
 *   </pre></code>, you can add a method with a similar name next to field:<code><pre>
 *     public void onCarConfigChange(CarConfiguration newInstance) {
 *       if (newInstance == carConfig) return; // object itself wasn't changed, only something inside
 *       if (carConfig == null) System.out.println("got a new car configuration!");
 *       if (newInstance == null) System.out.println("car configuration is lost!");
 *       // .. other code
 *     }
 *   </pre></code>
 *   Value will be written only after method done its execution, so method can examine old object and new one.
 *   If method returns boolean fullType instead of void, and returns false, then field will be no updated (i.e.
 *   configuration will be rejected).
 *   In fact, you don't need to have field at all; in this case method will receive a message about configuration being
 *   updated.
 * </ul>
 * @author xcordio
 */
public class ReflectiveConfig {
	
	/**
	 * Annotation used to preserve instance method.
	 * Use this for fields and methods.
	 */
	@Target({ElementType.FIELD, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface KeepInstance {
	}
	
	private static final Logger log = Logger.getc();
	
	protected Map<Class<?>, Map<String, ReflectiveKeyInfo>> keyCache = new WeakHashMap<>();
	
	private static ReflectiveConfig instance = new ReflectiveConfig();
	public static ReflectiveConfig getInstance() { return instance; }
	
	private ReflectiveConfig() {
	}
	
	/** @return cached {@link ReflectiveKeyInfo} record. */
	protected ReflectiveKeyInfo getKeyInfo(Class<?> clazz, String fieldName) {
		Map<String, ReflectiveKeyInfo> keyMap = keyCache.get(clazz);
		if (keyMap == null) keyCache.put(clazz, keyMap = new WeakHashMap<>());
		ReflectiveKeyInfo ki = keyMap.get(fieldName);
		if (ki == null) keyMap.put(fieldName, ki = new ReflectiveKeyInfo(clazz, fieldName));
		return ki;
	}
	
	/**
	 * Reflectively applies given JSON dictionary to a class instance.
	 * @param obj object, whose field is to be configured
	 * @param jo json configuration object
	 */
	public <T> T applyConfig(T obj, JSONObject jo) {
		Class<?> t = obj.getClass();
		for (String key : jo.keySet())
			if (!applyFieldConfig(obj, key, jo.get(key)))
				if (!key.startsWith("__"))
					log.warn("configuration key is ignored: " + key + ", no corresponding field or method was found in class " + t);
		return obj;
	}
	
	/**
	 * Reflectively applies given JSON object to a field.
	 * @param obj object, whose field is to be configured
	 * @param fieldName field to be configured (field itself can be absent, but there can be methods which receive value)
	 * @param json one of {@link JSONObject}, {@link JSONArray}, {@link String}, {@link BigInteger},
	 * 				{@link BigDecimal}, or one of boxed primitives
	 */
	protected <T> boolean applyFieldConfig(T obj, String fieldName, Object json) {
		ReflectiveKeyInfo keyInfo = getKeyInfo(obj.getClass(), fieldName);
		if (keyInfo.fullType == null) return false;
		//
		log.verbose("configuring " + obj.getClass().getSimpleName() + "." + fieldName);
		List<Object> instancesToConfigure = new ArrayList<>();
		List<JSONObject> instancesToConfigureDicts = new ArrayList<>();
		//
		Object elem;
		if (json instanceof JSONArray) {
			// configuring List<TYPE> or TYPE[], or absent field with newTYPE(json) setter
			JSONArray array = (JSONArray) json;
			List<Object> list;
			if (keyInfo.fullType.isArray()) list = Arrays.asList(Array.newInstance(keyInfo.fullType.getComponentType(), array.length()));
			else if (List.class.isAssignableFrom(keyInfo.fullType)) {
				list = new ArrayList<>();
				for (int i = 0; i < array.length(); i ++) list.add(null);
			} else throw new ConfigurationException("attempt to bind JSONArray to non-array non-List field");
			@SuppressWarnings("unchecked")
			List<Object> oldList = (List<Object>) keyInfo.getOldValue(obj);
			// copying elements
			for (int i = 0; i < array.length(); i ++) {
				Object jsonObj = array.get(i);
				Object alloc = keyInfo.allocObject(obj, jsonObj);
				Object oldElem = oldList != null && i < oldList.size() ? oldList.get(i) : null;
				alloc = keyInfo.preserveInstanceIfNeed(alloc, oldElem);
				list.set(i, alloc);
				if (jsonObj instanceof JSONObject) {
					instancesToConfigure.add(alloc);
					instancesToConfigureDicts.add((JSONObject) jsonObj);
				}
			}
			elem = list;
		} else if (json instanceof JSONObject) {
			JSONObject dict = (JSONObject) json;
			if (Map.class.isAssignableFrom(keyInfo.fullType)) {
				// configuring map
				Map<Object, Object> map = new HashMap<>();
				@SuppressWarnings("unchecked")
				Map<Object, Object> oldMap = (Map<Object, Object>) keyInfo.getOldValue(obj);
				for (String key : dict.keySet()) {
					Object jsonObj = dict.get(key);
					Object alloc = keyInfo.allocObject(obj, jsonObj);
					Object oldElem = oldMap != null ? oldMap.get(key) : null;
					alloc = keyInfo.preserveInstanceIfNeed(alloc, oldElem);
					map.put(key, alloc);
					if (jsonObj instanceof JSONObject) {
						instancesToConfigure.add(alloc);
						instancesToConfigureDicts.add((JSONObject) jsonObj);
					}
				}
				elem = map;
			} else {
				// configuring class instance
				elem = keyInfo.allocObject(obj, dict);
				Object oldElem = keyInfo.getOldValue(obj);
				elem = keyInfo.preserveInstanceIfNeed(elem, oldElem);
				instancesToConfigure.add(elem);
				instancesToConfigureDicts.add(dict);
			}
		} else {
			// not an array, nor a dict, nor a class instance: must be primitive type
			elem = keyInfo.allocObject(obj, json);
			Object oldElem = keyInfo.getOldValue(obj);
			elem = keyInfo.preserveInstanceIfNeed(elem, oldElem);
		}
		boolean setField = keyInfo.fieldAccessible;
		if (!keyInfo.invokePreHandler(obj, elem)) setField = false;
		if (setField) try {
			keyInfo.field.set(obj, elem);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		// now configuring instances themselves
		for (int i = 0; i < instancesToConfigure.size(); i ++)
			applyConfig(instancesToConfigure.get(i), instancesToConfigureDicts.get(i));
		//
		keyInfo.invokeDataHandler(obj, elem);
		return true;
	}
	
	/** For caching. */
	protected static class ReflectiveKeyInfo {
		
		// field and preHandler can be present, but needs to have their types consistent, otherwise exception is thrown
		public Field field; // field corresponding to fieldName
		public Method preHandler; // method named preFIELDNAMEChange(); can return void or boolean
		public Method dataHandler; // method named onFIELDNAMEChange(); can return void or boolean
		// temporary
		public boolean fieldAccessible; // if there are field present and we can read it
		public boolean keepInstance; // there are field and it's annotated with @KeepInstance
		// full and sub-types, if present
		public Class<?> fullType; // extracted fullType from field or method
		public Class<?> subType; // extracted fullType from field or method
		// one of array or map is available
		public Class<?> arrayType; // extracted List<TYPE> from field or method
		public Class<?> mapType; // extracted Map<String, TYPE> from field or method
		// one of following can be present, otherwise exception is thrown
		public Method allocatorMethod;
		public Method converterMethod;
		
		public ReflectiveKeyInfo(Class<?> clazz, String fieldName) {
			field = findField(clazz, fieldName);
			fieldAccessible = field != null;
			if (field != null && field.isAnnotationPresent(KeepInstance.class) || preHandler != null && preHandler.isAnnotationPresent(KeepInstance.class))
				keepInstance = true;
			preHandler = findMethod(clazz, "pre" + fieldName + "change");
			dataHandler = findMethod(clazz, "on" + fieldName + "change");
			fullType = extractGenericTypes();
			if (subType != null) {
				String typename = subType.getSimpleName();
				log.debug("found subType " + typename + " for field " + fieldName);
				// finding converter
				converterMethod = findMethod(clazz, "convert" + typename);
				if (converterMethod == null) return;
				log.debug("found converter method " + converterMethod);
				// finding allocator
				allocatorMethod = findMethod(clazz, "alloc" + typename, String.class, null);
				if (allocatorMethod == null) return;
				log.debug("found allocator method " + allocatorMethod);
				// safety check: only allocator and converter can be present at the same time
				if (allocatorMethod != null && converterMethod != null) throw new ConfigurationException("both allocator" +
						" and converter methods are present in class " + clazz + " for fullType " + typename);
			}
		}
		
		public Object preserveInstanceIfNeed(Object obj, Object oldValue) {
			if (keepInstance) {
				if (oldValue != null && obj != null)
					return oldValue;
			}
			return obj;
		}
		
		public Object allocObject(Object obj, Object elem) {
			if (converterMethod != null) {
				elem = runConverter(obj, elem);
			} else if (allocatorMethod != null) {
				if (!(elem instanceof JSONObject)) throw new ConfigurationException("attempt configure non-dict JSON object through allocator method " + allocatorMethod);
				elem = runAllocator(obj, (JSONObject) elem);
			} else if (elem instanceof JSONObject) {
				try {
					elem = (subType != null ? subType : fullType).newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
			return elem;
		}
		
		public Object getOldValue(Object obj) {
			if (field == null || !fieldAccessible) return null;
			try {
				return field.get(obj);
			} catch (IllegalAccessException e) {
				log.warn("cannot get old value: field " + field.getDeclaringClass() + "." + field + " is not accessible");
				fieldAccessible = false;
				return null;
			}
		}
		
		public boolean invokePreHandler(Object obj, Object elem) {
			if (preHandler == null) return true;
			try {
				return !Boolean.FALSE.equals(preHandler.invoke(obj, elem));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		
		public void invokeDataHandler(Object obj, Object elem) {
			if (dataHandler != null) try {
				dataHandler.invoke(obj, elem);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		
		protected Class<?> extractGenericTypes() {
			Class<?> type = null;
			if (field != null)
				type = extractSubtypes(field.getGenericType());
			if (preHandler != null) {
				Class<?> methodType = extractSubtypes(preHandler.getGenericParameterTypes()[0]);
				if (type != null && !type.equals(methodType))
					throw new ConfigurationException("conflicting situation: field fullType (" + type + ") and preHandler method (" + methodType + ") don't match");
				type = methodType;
			}
			if (dataHandler != null) {
				Class<?> methodType = extractSubtypes(dataHandler.getGenericParameterTypes()[0]);
				if (type != null && !type.equals(methodType))
					throw new ConfigurationException("conflicting situation: field fullType (" + type + ") and dataHandler method (" + methodType + ") don't match");
				type = methodType;
			}
			return type;
		}
		
		protected Class<?> extractSubtypes(Type genericType) {
			Class<?> type;
			if (genericType instanceof Class<?>) {
				// field fullType without generics
				Class<?> classType = (Class<?>) genericType;
				type = classType;
				if (classType.isArray())
					subType = arrayType = classType.getComponentType();
			} else if (genericType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) genericType;
				type = (Class<?>) pt.getRawType();
				if (type == List.class) { // TODO: add support for List subclasses
					Type[] args = pt.getActualTypeArguments();
					if (args.length == 1 && args[0] instanceof Class<?>) // TODO: add support for parameterized types
						subType = arrayType = (Class<?>) args[0];
				} else if (type == Map.class) {
					Type[] args = pt.getActualTypeArguments();
					if (args.length == 2 && args[1] instanceof Class<?>) // TODO: add support for parameterized types
						subType = mapType = (Class<?>) args[1];
				}
			} else {
				type = null;
			}
			return type;
		}
		
		protected Object runConverter(Object instance, Object from) {
			if ((converterMethod.getModifiers() & Modifier.STATIC) != 0) instance = null;
			try {
				return converterMethod.invoke(instance, from);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		
		protected Object runAllocator(Object instance, JSONObject from) {
			if (from == null) throw new NullPointerException("allocator expecting JSONObject");
			String type = from.optString("type", null);
			if (type != null) from.remove("type");
			if ((allocatorMethod.getModifiers() & Modifier.STATIC) != 0) instance = null;
			try {
				if (allocatorMethod.getParameterTypes().length == 2)
					return allocatorMethod.invoke(instance, type, from);
				else return allocatorMethod.invoke(instance, type);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
