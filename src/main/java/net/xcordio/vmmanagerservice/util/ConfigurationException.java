package net.xcordio.vmmanagerservice.util;

/**
 * Thrown every time a problem with reflective configuration is occurred.
 * @see {@link ReflectiveConfig}
 */
public class ConfigurationException extends RuntimeException {
	
	private static final long serialVersionUID = 5656144238034595327L;
	
	public ConfigurationException() {
	}
	
	public ConfigurationException(String message) {
		super (message);
	}
	
	public ConfigurationException(Throwable exception) {
		super (exception);
	}
	
	public ConfigurationException(String message, Throwable exception) {
		super (message, exception);
	}
}
