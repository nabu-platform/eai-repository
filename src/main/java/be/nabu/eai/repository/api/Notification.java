package be.nabu.eai.repository.api;

import be.nabu.libs.validator.api.Validation;

public interface Notification extends Validation<String> {
	/**
	 * Arbitrary properties that can be attached to notifications
	 */
	public Object getProperties();
	
	/**
	 * The type of notification
	 */
	public String getType();
	
	/**
	 * The user that triggered the notification
	 */
	public String getRealm();
	public String getAlias();
	
	/**
	 * The device that triggered the notification
	 */
	public String getDeviceId();
	
	/**
	 * A unique identifier for this notification
	 */
	public String getIdentifier();
}
