/**
 * 
 */
package nl.javadude.syringe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that emulates autowiring.
 */
public class Syringe {

	private static Syringe instance;

	/**
	 * Default private constructor. Singleton should be instantiated by factory
	 * method.
	 */
	private Syringe() {
		// Singleton.
	}

	/**
	 * Static Factory method to create the instance.
	 * 
	 * @return The singleton instance of the {@link AutoWirer}.
	 */
	public static Syringe getInstance() {
		if (instance == null) {
			instance = new Syringe();
		}
		return instance;
	}

	/**
	 * Clear the instance so that it has to be recreated.
	 */
	public static void clearInstance() {
		instance = null;
	}

	/**
	 * The {@link AutowireType} strategy for wiring.
	 */
	public enum AutowireType {
		AUTOWIRE_BY_NAME, AUTOWIRE_BY_TYPE;
	};

	private Map<String, WireableObject> mapping = new HashMap<String, WireableObject>();

	/**
	 * Register a class under its simple name.
	 * 
	 * @param clazz
	 *            The clazz to register.
	 */
	public void register(Class<?> clazz) {
		register(clazz.getSimpleName(), clazz);
	}

	/**
	 * Register a class under a specific name.
	 * 
	 * @param key
	 *            The key to register it class under
	 * @param clazz
	 *            The class to register.
	 */
	public void register(String key, Class<?> clazz) {
		try {
			register(key, clazz.newInstance());
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Registers an instance under a key.
	 * 
	 * @param key
	 *            The key to register the instance under.
	 * @param bean
	 *            The instance to register.
	 */
	public void register(String key, Object bean) {
		register(key, bean, true);
	}

	public void register(String key, Object bean, boolean needsWiring) {
		WireableObject w = new WireableObject();
		w.instance = bean;
		w.needsWiring = needsWiring;
		mapping.put(key, w);
	}

	/**
	 * Register an instance under its simple class name.
	 * 
	 * @param bean
	 *            The instance to register.
	 */
	public void register(Object bean) {
		register(bean.getClass().getSimpleName(), bean);
	}

	/**
	 * Autowire an object, using a specified {@link AutowireType} strategy. If
	 * the {@link AutowireType#AUTOWIRE_BY_NAME} is chosen, as a fallback, we
	 * use the byType autowiring. {@link AutowireType#AUTOWIRE_BY_TYPE} only
	 * tries wiring by type.
	 * 
	 * @param o
	 *            The instance to wire.
	 * @param type
	 *            The strategy to wire the instance.
	 */
	public void autowire(Object o, AutowireType type) {
		Class<?> clazz = o.getClass();
		while (clazz != null) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (type.equals(AutowireType.AUTOWIRE_BY_NAME)) {
					autowireByName(o, field);
				} else {
					autowireByType(o, field);
				}
			}
			clazz = clazz.getSuperclass();
		}
	}

	/**
	 * Try autowiring by name, if that fails, use autowiring by type.
	 * 
	 * @param o
	 *            The instance to wire.
	 * @param field
	 *            The field that needs wiring.
	 */
	private void autowireByName(Object o, Field field) {
		if (!doAutoWire(o, field, field.getName())) {
			autowireByType(o, field);
		}
	}

	/**
	 * Try to autowire by type.
	 * 
	 * @param o
	 *            The instance to wire.
	 * @param field
	 *            The field that needs wiring.
	 */
	private void autowireByType(Object o, Field field) {
		doAutoWire(o, field, field.getType().getSimpleName());
	}

	/**
	 * Do the actual wiring.
	 * 
	 * @param o
	 *            The instance to wire.
	 * @param field
	 *            The field that needs wiring.
	 * @param key
	 *            The key under which the to be wired value is stored in the
	 *            wire map.
	 * @return true if the wiring succeeded, false otherwise.
	 */
	private boolean doAutoWire(Object o, Field field, String key) {
		if (mapping.containsKey(key)) {
			field.setAccessible(true);
			try {
				field.set(o, mapping.get(key).instance);
				return true;
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}

	/**
	 * Gets a wired class by name.
	 * 
	 * @param name
	 *            The name of the instance in the mapping.
	 * @return The mapped class if it could be found, null otherwise.
	 */
	public Object get(String name) {
		return mapping.get(name).instance;
	}

	/**
	 * Gets a wired class by type.
	 * 
	 * @param clazz
	 *            The class of the instance in the mapping.
	 * @return The mapped class if it could be found, null otherwise.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> clazz) {
		return (T) get(clazz.getSimpleName());
	}

	/**
	 * Finishes the registration and tries to autowire together all the
	 * instances in the map.
	 */
	public void finishRegistration() {
		for (WireableObject w : mapping.values()) {
			if (w.needsWiring) {
				autowire(w.instance, AutowireType.AUTOWIRE_BY_NAME);
			}
		}
	}

	/**
	 * A WireableObject defines the instance and whether it needs wiring. Some
	 * instances come prewired and don't need us.
	 */
	static class WireableObject {
		private boolean needsWiring = true;
		private Object instance;
	}
}
