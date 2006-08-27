package net.sf.jmoney.copier;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class CopierPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static CopierPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	private static DatastoreManager savedSessionManager = null;
	
	/**
	 * The constructor.
	 */
	public CopierPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("net.sf.jmoney.copier.CopierPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static CopierPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = CopierPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * @param sessionManager
	 */
	public static void setSessionManager(DatastoreManager sessionManager) {
		savedSessionManager = sessionManager;
	}

	/**
	 * @return
	 */
	public static DatastoreManager getSessionManager() {
		return savedSessionManager;
	}
	
    /**
     * Copy the contents of one session to another.
     *
     * The two sessions may be interfaces into different implementations.
     * This is therefore more than just a deep copy.  It is a conversion.
     */
    public void populateSession(Session newSession, Session oldSession) {
        Map objectMap = new Hashtable();
        
        PropertySet propertySet = PropertySet.getPropertySet(oldSession.getClass());
    	populateObject(propertySet, oldSession, newSession, objectMap);
    }

    private void populateObject(PropertySet propertySet, ExtendableObject oldObject, ExtendableObject newObject, Map objectMap) {
    	// For all non-extension properties (including properties
    	// in base classes), read the property value from the
    	// old object and write it to the new object.
    	for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
    		PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
    		if (!propertyAccessor.getPropertySet().isExtension()) {
    			copyProperty(propertyAccessor, oldObject, newObject, objectMap);
    		}
    	}
    	
    	// Now copy the extensions.  This is done by looping through the extensions
    	// in the old object and, for every extension that exists in the old object,
    	// copy the properties to the new object.
    	for (Iterator extensionIter = oldObject.getExtensionIterator(); extensionIter.hasNext(); ) {
    		Map.Entry mapEntry = (Map.Entry)extensionIter.next();
    		PropertySet extensionPropertySet = (PropertySet)mapEntry.getKey();
    		for (Iterator propertyIter = extensionPropertySet.getPropertyIterator1(); propertyIter.hasNext(); ) {
    			PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIter.next();
    			copyProperty(propertyAccessor, oldObject, newObject, objectMap);
    		}
    	}
    
    	// TODO: This code works because
    	// Commodity and Account objects are the
    	// only objects referenced by other objects.
    	// Plug-ins could change this, thus breaking this code.
    	if (oldObject instanceof Commodity
    			|| oldObject instanceof Account) {
    		objectMap.put(oldObject, newObject);
    	}
    }

    private void copyProperty(PropertyAccessor propertyAccessor, ExtendableObject oldObject, ExtendableObject newObject, Map objectMap) {
    	if (propertyAccessor.isScalar()) {
    		Object oldValue = oldObject.getPropertyValue(propertyAccessor);
    		Object newValue;
    		if (oldValue instanceof ExtendableObject) {
    			newValue = objectMap.get(oldValue);
    		} else {
    			newValue = oldValue;
    		}
			newObject.setPropertyValue(
    				propertyAccessor,
					newValue);
    	} else {
    		// Property is a list property.
    		ObjectCollection newList = newObject.getListPropertyValue(propertyAccessor);
    		for (Iterator listIter = oldObject.getListPropertyValue(propertyAccessor).iterator(); listIter.hasNext(); ) {
    			ExtendableObject oldSubObject = (ExtendableObject)listIter.next();
    			PropertySet listElementPropertySet = PropertySet.getPropertySet(oldSubObject.getClass());
    			ExtendableObject newSubObject = newList.createNewElement(listElementPropertySet);
    			populateObject(listElementPropertySet, oldSubObject, newSubObject, objectMap);
    		}
    	}
    }
}
