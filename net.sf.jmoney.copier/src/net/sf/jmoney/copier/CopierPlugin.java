package net.sf.jmoney.copier;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ExtensionPropertySet;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.ReferenceViolationException;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionInfo;

import org.eclipse.core.runtime.Assert;
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
	 * The two sessions may be interfaces into different implementations. This
	 * is therefore more than just a deep copy. It is a conversion.
	 * 
	 * The entire copy is done in a single transaction because there may be
	 * constraints that will be violated at intermediate points otherwise. This
	 * does take a lot of memory but this does not appear to be a problem even
	 * with quite large datastores.
	 */
    public void populateSession(Session newSession, Session oldSession) {
    	/*
		 * We want to clear the old default currency. However, we cannot delete
		 * the currency until the default currency in the session has been set
		 * to something else (and we cannot set the default currency to null
		 * because the default currency is required to be non-null). We
		 * therefore save the default currency and delete it after the new data
		 * has been copied in.
		 */
    	TransactionManager transaction = new TransactionManager(newSession.getDataManager());
    	Session newSessionInTrans = transaction.getSession();
    	
    	Currency previousDefaultCurrency = newSessionInTrans.getDefaultCurrency(); 
    	
        Map<ExtendableObject, ExtendableObject> objectMap = new HashMap<ExtendableObject, ExtendableObject>();
        
    	populateObject(SessionInfo.getPropertySet(), oldSession, newSessionInTrans, objectMap);

    	/*
    	 * Now we can delete the previous default currency.
    	 */
    	try {
			newSessionInTrans.deleteCommodity(previousDefaultCurrency);
		} catch (ReferenceViolationException e) {
			/*
			 * If we can't delete the previous default currency because of references
			 * that we haven't dealt with, leave it.
			 */
			throw new RuntimeException("Internal error", e);
		}
    	
    	transaction.commit();
    }

    private void populateObject(ExtendablePropertySet<?> propertySet, ExtendableObject oldObject, ExtendableObject newObject, Map<ExtendableObject, ExtendableObject> objectMap) {
    	/*
		 * For all non-extension properties (including properties in base
		 * classes), read the property value from the old object and write it to
		 * the new object.
		 * 
		 * The list properties are copied before the scalar properties. This
		 * helps to solve the problem where a scalar property that references an
		 * extendable object is copied, but the referenced object has not yet
		 * been copied. The copy of the object will thus not yet exist and will
		 * not be in the object map.
		 */

    	// TODO: This code works because
    	// Commodity and Account objects are the
    	// only objects referenced by other objects.
    	// Plug-ins could change this, thus breaking this code.

    	// The list properties
    	for (ListPropertyAccessor<?> listAccessor: propertySet.getListProperties3()) {
    		if (!listAccessor.getPropertySet().isExtension()) {
    			if (listAccessor.getElementPropertySet().getImplementationClass().isAssignableFrom(Commodity.class)
    					|| listAccessor.getElementPropertySet().getImplementationClass().isAssignableFrom(Account.class)) {
    				createListObjects(newObject, oldObject, listAccessor, objectMap);
    			}
    		}
    	}
    	
    	for (ListPropertyAccessor<?> listAccessor: propertySet.getListProperties3()) {
    		if (!listAccessor.getPropertySet().isExtension()) {
    			if (listAccessor.getElementPropertySet().getImplementationClass().isAssignableFrom(Commodity.class)
    					|| listAccessor.getElementPropertySet().getImplementationClass().isAssignableFrom(Account.class)) {
    				fillListObjects(newObject, oldObject, listAccessor, objectMap);
    			} else {
    				copyList(newObject, oldObject, listAccessor, objectMap);
    			}
    		}
    	}

    	// The scalar properties
    	for (ScalarPropertyAccessor<?> scalarAccessor: propertySet.getScalarProperties3()) {
    		if (!scalarAccessor.getPropertySet().isExtension()) {
				copyScalarProperty(scalarAccessor, oldObject, newObject, objectMap);
    		}
    	}
    	
    	/*
		 * Now copy the extensions. This is done by looping through the
		 * extensions in the old object and, for every extension that exists in
		 * the old object, copy the properties to the new object.
		 */
    	for (ExtensionPropertySet<?> extensionPropertySet: oldObject.getExtensions()) {
    		for (PropertyAccessor propertyAccessor: extensionPropertySet.getProperties1()) {
    			if (propertyAccessor.isScalar()) {
					ScalarPropertyAccessor<?> scalarAccessor = (ScalarPropertyAccessor<?>)propertyAccessor;
					copyScalarProperty(scalarAccessor, oldObject, newObject, objectMap);
				} else {
					// Property is a list property.
					ListPropertyAccessor<?> listAccessor = (ListPropertyAccessor<?>)propertyAccessor;
					copyList(newObject, oldObject, listAccessor, objectMap);
				}
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

    /**
     * Creates child lists in an object but with empty values, except child lists are created recursively.
     * 
     * @param propertySet
     * @param oldObject
     * @param newObject
     * @param objectMap
     */
    private void createObject(ExtendablePropertySet<?> propertySet, ExtendableObject oldObject, ExtendableObject newObject, Map<ExtendableObject, ExtendableObject> objectMap) {
    	/*
		 * For all non-extension properties (including properties in base
		 * classes), read the property value from the old object and write it to
		 * the new object.
		 * 
		 * The list properties are copied before the scalar properties. This
		 * helps to solve the problem where a scalar property that references an
		 * extendable object is copied, but the referenced object has not yet
		 * been copied. The copy of the object will thus not yet exist and will
		 * not be in the object map.
		 */

    	// The list properties
    	for (ListPropertyAccessor<?> listAccessor: propertySet.getListProperties3()) {
    		if (!listAccessor.getPropertySet().isExtension()) {
    			createListObjects(newObject, oldObject, listAccessor, objectMap);
    		}
    	}

    	/*
		 * Now copy the extensions. This is done by looping through the
		 * extensions in the old object and, for every extension that exists in
		 * the old object, copy the properties to the new object.
		 */
    	for (ExtensionPropertySet<?> extensionPropertySet: oldObject.getExtensions()) {
    		for (PropertyAccessor propertyAccessor: extensionPropertySet.getProperties1()) {
    			if (propertyAccessor.isList()) {
					// Property is a list property.
					ListPropertyAccessor<?> listAccessor = (ListPropertyAccessor<?>)propertyAccessor;
					createListObjects(newObject, oldObject, listAccessor, objectMap);
				}
    		}
    	}

    	objectMap.put(oldObject, newObject);
    }

    /**
     * Fills in the values into an object where the lists have already been created with empty marker objects.
     * 
     * @param propertySet
     * @param oldObject
     * @param newObject
     * @param objectMap
     */
    private void fillObject(ExtendablePropertySet<?> propertySet, ExtendableObject oldObject, ExtendableObject newObject, Map<ExtendableObject, ExtendableObject> objectMap) {
    	/*
		 * For all non-extension properties (including properties in base
		 * classes), read the property value from the old object and write it to
		 * the new object.
		 * 
		 * The list properties are copied before the scalar properties. This
		 * helps to solve the problem where a scalar property that references an
		 * extendable object is copied, but the referenced object has not yet
		 * been copied. The copy of the object will thus not yet exist and will
		 * not be in the object map.
		 */

    	// The list properties
    	for (ListPropertyAccessor<?> listAccessor: propertySet.getListProperties3()) {
    		if (!listAccessor.getPropertySet().isExtension()) {
   				fillListObjects(newObject, oldObject, listAccessor, objectMap);
    		}
    	}

    	// The scalar properties
    	for (ScalarPropertyAccessor<?> scalarAccessor: propertySet.getScalarProperties3()) {
    		if (!scalarAccessor.getPropertySet().isExtension()) {
				copyScalarProperty(scalarAccessor, oldObject, newObject, objectMap);
    		}
    	}
    	
    	/*
		 * Now copy the extensions. This is done by looping through the
		 * extensions in the old object and, for every extension that exists in
		 * the old object, copy the properties to the new object.
		 */
    	for (ExtensionPropertySet<?> extensionPropertySet: oldObject.getExtensions()) {
    		for (PropertyAccessor propertyAccessor: extensionPropertySet.getProperties1()) {
    			if (propertyAccessor.isScalar()) {
					ScalarPropertyAccessor<?> scalarAccessor = (ScalarPropertyAccessor<?>)propertyAccessor;
					copyScalarProperty(scalarAccessor, oldObject, newObject, objectMap);
				} else {
					// Property is a list property.
					ListPropertyAccessor<?> listAccessor = (ListPropertyAccessor<?>)propertyAccessor;
					fillListObjects(newObject, oldObject, listAccessor, objectMap);
				}
    		}
    	}
    }

    @SuppressWarnings("unchecked")
    private <V> void copyScalarProperty(ScalarPropertyAccessor<V> propertyAccessor, ExtendableObject oldObject, ExtendableObject newObject, Map objectMap) {
    		V oldValue = oldObject.getPropertyValue(propertyAccessor);
    		V newValue;
    		if (oldValue instanceof ExtendableObject) {
    			newValue = propertyAccessor.getClassOfValueObject().cast(objectMap.get(oldValue));
    			Assert.isNotNull(newValue);
    		} else {
    			newValue = oldValue;
    		}
			newObject.setPropertyValue(
					propertyAccessor,
					newValue);
    }
    
    @SuppressWarnings("unchecked")
    private <E extends ExtendableObject> void copyList(ExtendableObject newParent, ExtendableObject oldParent, ListPropertyAccessor<E> listAccessor, Map<ExtendableObject, ExtendableObject> objectMap) {
		ObjectCollection<E> newList = newParent.getListPropertyValue(listAccessor);
		for (E oldSubObject: oldParent.getListPropertyValue(listAccessor)) {
			ExtendablePropertySet<? extends E> listElementPropertySet = listAccessor.getElementPropertySet().getActualPropertySet((Class<? extends E>)oldSubObject.getClass());
			ExtendableObject newSubObject = newList.createNewElement(listElementPropertySet);
			populateObject(listElementPropertySet, oldSubObject, newSubObject, objectMap);
		}
    }

    /**
     * Lists of commodities and accounts are done in two passes.  The first pass creates the objects.  The
     * second fills the values.  This is necessary because commodities have properties that reference other commodities
     * (bonds have a reference to the currency of the bond), and accounts have references to other accounts (a bank
     * account has a reference to the income account for the interest).
     * 
     * @param <E>
     * @param newParent
     * @param oldParent
     * @param listAccessor
     * @param objectMap
     */
    private <E extends ExtendableObject> void createListObjects(ExtendableObject newParent, ExtendableObject oldParent, ListPropertyAccessor<E> listAccessor, Map<ExtendableObject, ExtendableObject> objectMap) {
		ObjectCollection<E> newList = newParent.getListPropertyValue(listAccessor);
		for (E oldSubObject: oldParent.getListPropertyValue(listAccessor)) {
			ExtendablePropertySet<? extends E> listElementPropertySet = listAccessor.getElementPropertySet().getActualPropertySet((Class<? extends E>)oldSubObject.getClass());
			ExtendableObject newSubObject = newList.createNewElement(listElementPropertySet);
			createObject(listElementPropertySet, oldSubObject, newSubObject, objectMap);
		}
    }

    private <E extends ExtendableObject> void fillListObjects(ExtendableObject newParent, ExtendableObject oldParent, ListPropertyAccessor<E> listAccessor, Map<ExtendableObject, ExtendableObject> objectMap) {
		ObjectCollection<E> newList = newParent.getListPropertyValue(listAccessor);
		for (E oldSubObject: oldParent.getListPropertyValue(listAccessor)) {
			ExtendablePropertySet<? extends E> listElementPropertySet = listAccessor.getElementPropertySet().getActualPropertySet((Class<? extends E>)oldSubObject.getClass());
			ExtendableObject newSubObject = objectMap.get(oldSubObject);
			fillObject(listElementPropertySet, oldSubObject, newSubObject, objectMap);
		}
    }
}
