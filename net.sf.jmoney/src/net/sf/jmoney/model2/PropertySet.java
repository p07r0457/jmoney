/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.model2;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.CoreException;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

//TODO: do we really need the info to be kept here at all????
public class PropertySet {
	
	private String propertySetId;
	private IExtensionPropertySetInfo propertySetInfo;
	
	private Vector properties = new Vector();  // type: PropertyAccessor
	
	boolean isExtension;
	
	// If there is no extension then certain methods that obtain
	// property values should fetch default values.
	// We determine the default values by constructing an extension
	// object using the default constructor.  To avoid constructing
	// a default extension multiple times, one is cached inside this
	// object.
	// This default extension should never be passed outside this package
	// because plugins have no need for it and can cause chaos if
	// they alter it.  However it is useful for use inside the
	// package.
	private ExtensionObject defaultExtension;  // defined only if an extension property set

	/**
	 * true if further property sets must be derived from this property set,
	 * false if property sets cannot be derived from this property set.
	 */
	private boolean derivablePropertySet; // defined only if an extension property set
	
	private static Map allPropertySetsMap = new HashMap();   // type: String (property set id) to PropertySet

	/**
	 * Map extendable classes to property sets.
	 */
	private static Map classToPropertySetMap = new HashMap();   // type: String (property set id) to PropertySet
	
	// TODO clean this up (i.e. remove this)
	Map extensionPropertySets = null;  // Set only if this is an extendable property set

	// Valid for extendable property sets only
	PropertySet basePropertySet;
	
	// Valid for extension property sets only
	PropertySet extendablePropertySet;	

	// Valid for extendable property sets only
	// TODO: Add values to this.  Currently this is always empty!
	/**
	 * Set of property sets that are derived from this property set.
	 */
	private Vector derivedPropertySets = new Vector();

	// Valid for all property sets except those that are
	// extendable and must be derived from.
	private Constructor implementationClassConstructor;
	
	/**
	 * All properties in this and base property sets that are
	 * passed to the constructor.  The order of properties in
	 * this vector object is the same as the order in which the
	 * properties are passed as parameters into the constructor. 
	 * <P>
	 * Note that constructors for extendable objects take extra
	 * parameters at the start before the property parameters.
	 * There are no elements in this Vector corresponding to 
	 * these parameters.  Therefore the
	 * indexes into this Vector will not correspond to the index into
	 * the parameter list.
	 * <P>
	 * This field is undefined for derivable property sets.
	 */
	Vector constructorProperties;
	
	private Method theDefaultPropertiesMethod;

	
	/**
	 * Loads the property sets.
	 * All property sets (both base and extensions) are added to the 
	 * net.sf.jmoney.fields extension point.
	 */
	public static void init() {
		// Load the property set extensions.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.fields");
		IExtension[] extensions = extensionPoint.getExtensions();
		
		// TODO: They may be not much point in processing extendable classes before extension
		// classes.  Eclipse, I believe, will always iterate extension info from a plug-in
		// before extensions from plug-ins that depend on that plug-in, so we don't have the
		// problem of the extendable not being processed before the extension.
		// We do have other problems, however, which have required a second pass thru
		// the property sets.
		
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("extendable-property-set")) {
					try {
						Object listener = elements[j].createExecutableExtension("info-class");
						if (listener instanceof IExtensionPropertySetInfo) {
							IExtensionPropertySetInfo pageListener = (IExtensionPropertySetInfo)listener;
							
							String fullPropertySetId = extensions[i].getNamespace();
							String id = elements[j].getAttribute("id");
							if (id != null && id.length() != 0) {
								fullPropertySetId = fullPropertySetId + '.' + id;
							}
							
							String basePropertySetId = elements[j].getAttribute("base-property-set");
							if (basePropertySetId != null && basePropertySetId.length() == 0) {
								basePropertySetId = null;
							}
							registerExtendablePropertySet(fullPropertySetId, basePropertySetId, pageListener);
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("extension-property-set")) {
					try {
						Object listener = elements[j].createExecutableExtension("info-class");
						if (listener instanceof IExtensionPropertySetInfo) {
							IExtensionPropertySetInfo pageListener = (IExtensionPropertySetInfo)listener;
							
							String fullPropertySetId = extensions[i].getNamespace();
							String id = elements[j].getAttribute("id");
							if (id != null && id.length() != 0) {
								fullPropertySetId = fullPropertySetId + '.' + id;
							}
							
							String extendablePropertySetId = elements[j].getAttribute("extendable-property-set");
							if (extendablePropertySetId != null) {
								registerExtensionPropertySet(fullPropertySetId, extendablePropertySetId, pageListener);
							} else {
								// TODO plug-in error
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// Make a second pass thru the property sets.
// TODO: is this right?		
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			propertySet.initProperties();
		}
		
	}
	
	
	private PropertySet(String propertySetId, boolean isExtension, String baseOrExtendablePropertySetId, IExtensionPropertySetInfo propertySetInfo) {
		this.propertySetId = propertySetId;
		this.propertySetInfo  = propertySetInfo;
		this.isExtension = isExtension;
		
		if (allPropertySetsMap.containsKey(propertySetId)) {
			throw new MalformedPluginException("More than one property set has an id of " + propertySetId);
		}
		allPropertySetsMap.put(propertySetId, this);

		if (!isExtension) {
			extensionPropertySets = new HashMap();

			if (baseOrExtendablePropertySetId != null) {
				basePropertySet = (PropertySet)allPropertySetsMap.get(baseOrExtendablePropertySetId);
				if (basePropertySet == null) {
					throw new RuntimeException("No extendable property set with an id of " + baseOrExtendablePropertySetId + " exists.");
				}
				
				if (basePropertySet.isExtension()) {
					// TODO should this be MalformedPluginException?
					throw new RuntimeException(baseOrExtendablePropertySetId + " is a base property set.  Extension property sets cannot be extended, but " + propertySetId + " is declared as a base of " + baseOrExtendablePropertySetId + ".");
				}
			} else {
				basePropertySet = null;
			}
			
		} else {
			extendablePropertySet = (PropertySet)allPropertySetsMap.get(baseOrExtendablePropertySetId);
			if (extendablePropertySet == null) {
				throw new RuntimeException("No extendable property set with an id of " + baseOrExtendablePropertySetId + " exists.");
			}
			
			if (extendablePropertySet.isExtension()) {
				throw new RuntimeException(baseOrExtendablePropertySetId + " is an extension property set.  Extension property sets cannot be extended, but " + propertySetId + " is declared as an extension of " + baseOrExtendablePropertySetId + ".");
			}
			
			if (extendablePropertySet.extensionPropertySets.containsKey(propertySetId)) {
				throw new RuntimeException("internal error - More than one property set has an id of " + propertySetId);
			}
			
			extendablePropertySet.extensionPropertySets.put(propertySetId, this);

			// Set up the extension that contains the default property values.
			if (propertySetInfo != null) {
				try {
					if (propertySetInfo.getInterfaceClass() != null) {
						defaultExtension = (ExtensionObject)
						propertySetInfo.getInterfaceClass().newInstance();
						// TODO: plugin error if null is returned
					}
				} catch (Exception e) {
					// TODO: deal with this error
					// Plug-in error if no default constructor.
					e.printStackTrace();
					throw new RuntimeException();
				}
			} else {
				defaultExtension = null;
			}
		}
		
		// Add to the map that maps the extendable classes
		// to the extendable property sets.
		// Only final property sets (ones which do not define an enumerated type
		// to control derived classes) are put in the map.  This is important because
		// when we are looking for the property set for an instance of an object,
		// we want to be sure we find only the final property set for that object.
		if (!isExtension && !derivablePropertySet) {
			Class interfaceClass = propertySetInfo.getInterfaceClass();
			if (classToPropertySetMap.containsKey(interfaceClass)) {
				throw new MalformedPluginException("More than one property set uses " + interfaceClass + " as the Java implementation class.");
			}
			classToPropertySetMap.put(interfaceClass, this);
		}
		
		derivablePropertySet = false;
		
		if (propertySetInfo != null) {
			// Set up the list of properties.
			// This is done by calling the registerExtensionProperties
			// method of the supplied interface.  We must pass an
			// IPropertyRegistrar implementation.  registerExtensionProperties
			// will call back into this interface to register each
			// property.
			
			propertySetInfo.registerProperties(
					new IPropertyRegistrar() {
						
						public EnumerationAccessor addEnumeratedValue(PropertyAccessor propertyAccessor, String internalName, String displayName) {
							// TODO Auto-generated method stub
							return null;
						}
						
						public EnumerationAccessor addEnumeratedValue(PropertyAccessor propertyAccessor, String internalName, String displayName, Class derivedClass) {
							// TODO Auto-generated method stub
							return null;
						}
						
						// TODO change all shortDescription to displayName
						public PropertyAccessor addProperty(String name, String shortDescription, double width, IPropertyControlFactory propertyControlFactory, Class editor, IPropertyDependency propertyDependency) {
							PropertyAccessor accessor = new PropertyAccessorImpl(PropertySet.this, name, shortDescription, width, propertyControlFactory, editor, propertyDependency);
							properties.add(accessor);
							return accessor;
						}

						public PropertyAccessor addPropertyList(String name, String shortDescription, Class listItemClass, IPropertyDependency propertyDependency) {
							PropertyAccessor accessor = new PropertyAccessorImpl(PropertySet.this, name, shortDescription, listItemClass, propertyDependency);
							properties.add(accessor);
							return accessor;
						}
						
						public PropertyAccessor setDerivableInfo(String name, String displayName) {
							derivablePropertySet = true;
							// TODO Auto-generated method stub
							return null;
						}
						
						public PropertyAccessor setDerivableInfo() {
							derivablePropertySet = true;
							return null;
						}

					}
			);
		}

		
	}

	
	/**
	 * This method is called to complete the initialization of this object.
	 * Some parts of the initialization require access to a complete list of
	 * all the PropertySet objects and therefore cannot be done in the
	 * PropertySet constructor.
	 */
	private void initProperties() {
		// Find the constructor method.
		
		// The properties from any base property set come first in
		// the constructor, so add those first.
		int parameterIndex = 0;
		int scalarIndex = 0;
		int totalPropertyCount = 0;
		if (!isExtension) {
			parameterIndex += 3;
			
			for (PropertySet propertySet2 = getBasePropertySet(); propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
				parameterIndex += propertySet2.getPropertyCount();
				totalPropertyCount += propertySet2.getPropertyCount();
				// Count the scalar properties
				for (Iterator iter = propertySet2.getPropertyIterator1(); iter.hasNext(); ) {
					PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
					if (propertyAccessor.isScalar()) {
						scalarIndex++;
					}
				}
			}
		}
		
		totalPropertyCount += getPropertyCount();
		
		// For each property in this property set, set the index
		// of that property in the constructor parameter list.
		for (Iterator iter = properties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			propertyAccessor.setIndexIntoConstructorParameters(parameterIndex++);
			if (propertyAccessor.isScalar()) {
				propertyAccessor.setIndexIntoScalarProperties(scalarIndex++);
			}
		}

		// Find the full constructor (unless this is a derivable
		// property set, in which case no constructor is needed).
		if (isExtension || !derivablePropertySet) {
			// Build the list of properties that are passed to
			// the constructor.
			
			constructorProperties = new Vector();
			constructorProperties.setSize(totalPropertyCount);
			
			// The properties must be added in the same order as they
			// were registered, which is the same order as they are
			// returned by the iterator.
			if (!isExtension()) {
				int startOfPropertySet2 = totalPropertyCount;
				for (PropertySet propertySet2 = this; propertySet2 != null; propertySet2 = propertySet2.getBasePropertySet()) {
					startOfPropertySet2 -= propertySet2.getPropertyCount(); 
					int index2 = startOfPropertySet2;
					for (Iterator iter = propertySet2.getPropertyIterator1(); iter.hasNext(); ) {
						PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
						constructorProperties.setElementAt(propertyAccessor, index2++);
					}
				}
			} else {
				int index2 = 0;
				for (Iterator iter = properties.iterator(); iter.hasNext(); ) {
					PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
					constructorProperties.setElementAt(propertyAccessor, index2++);
				}
			}
			
			// Build the list of types of the constructor parameters.
			int i = 0;
			int parameterCount = constructorProperties.size();
			if (!isExtension) {
				parameterCount += 3;
			}
			Class parameters[] = new Class[parameterCount];

			// In the extendable objects, the first parameters are always:
			// - the key to this object
			// - a map of extensions
			// - the key to the object which is the parent of this object
			if (!isExtension) {
				parameters[i++] = IObjectKey.class;
				parameters[i++] = Map.class;
				parameters[i++] = IObjectKey.class;
			}
			
			for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
				if (propertyAccessor.isScalar()) {
					if (propertyAccessor.getValueClass().isPrimitive()
					 || propertyAccessor.getValueClass() == String.class
					 || propertyAccessor.getValueClass() == Long.class
					 || propertyAccessor.getValueClass() == Date.class) {
						parameters[i] = propertyAccessor.getValueClass();
					} else {
						parameters[i] = IObjectKey.class; 
					}
				} else {
					parameters[i] = IListManager.class; 
				}
				i++;
			}
			
			try {
				implementationClassConstructor =
					propertySetInfo.getImplementationClass().getConstructor(parameters);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				String parameterText = "";
				for (int paramIndex = 0; paramIndex < parameters.length; paramIndex++) {
					if (paramIndex > 0) {
						parameterText = parameterText + ", ";
					}
					parameterText = parameterText + parameters[paramIndex].getName();
				}
				throw new MalformedPluginException("The " + propertySetInfo.getImplementationClass().getName() + " class must have a constructor that takes parameters of types (" + parameterText + ").");
			}
		}
		
		// Find the getDefaultProperties method.
				
		// If this is a derivable property set then no default properties method is needed.
		if (isExtension || !derivablePropertySet) {
			
			Class parameters[] = new Class[] {};
			
			try {
				theDefaultPropertiesMethod =
					propertySetInfo.getImplementationClass().getDeclaredMethod("getDefaultProperties", parameters);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				String parameterText = "";
				for (int paramIndex = 0; paramIndex < parameters.length; paramIndex++) {
					if (paramIndex > 0) {
						parameterText = parameterText + ", ";
					}
					if (parameters[paramIndex].isArray()) {
						parameterText = parameterText + "net.sf.jmoney.model2.ExtensionProperties[]";
					} else {
						parameterText = parameterText + parameters[paramIndex].getName();
					}
				}
				throw new MalformedPluginException("The " + propertySetInfo.getImplementationClass().getName() + " class must have a 'getDefaultProperties' method that takes parameters of types (" + parameterText + ").");
			}
			
			// The '8' bit indicates that the method is static.
			if ((theDefaultPropertiesMethod.getModifiers() & 8) != 8) {
				throw new MalformedPluginException("The 'getDefaultProperties' method in " + propertySetInfo.getImplementationClass().getName() + " class must be static.");
			}
			
			// The '1' bit indicates that the method is public.
			// (2 is private, 4 is protected, 1,2 & 4 bits off is default).
			if ((theDefaultPropertiesMethod.getModifiers() & 7) != 1) {
				throw new MalformedPluginException("The 'getDefaultProperties' method in " + propertySetInfo.getImplementationClass().getName() + " class must be public.");
			}
		} else {
			// No default properties method is required for derived property sets
			// so set to null.
			theDefaultPropertiesMethod = null;
		}
		
		// Complete the initialization of the properties
		
		for (Iterator iter = properties.iterator(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			propertyAccessor.initMethods();
		}
	}


	/**
	 * @return The number of properties in this property set.
	 *		This number is the same as the number of elements
	 * 		enumerated by the iterator returned by getPropertyIterator1.
	 */
	private int getPropertyCount() {
		return properties.size();
	}


	/**
	 * This method may not be called on derivable property sets.
	 *  
	 * @return
	 */
	public Vector getConstructorProperties() {
		return constructorProperties;
	}


	/**
	 * 
	 * @param propertySetId
	 * @param basePropertySetId If this property set is derived from
	 * 			another property set then the id of the base property set,
	 * 			otherwise null. 
	 * @param propertySetInfo Null if property set data is found in
	 * 			the datastore but no plug-in defined a property set
	 * 			with this id. 
	 * @return
	 */
	static private void registerExtendablePropertySet(String propertySetId, String basePropertySetId, IExtensionPropertySetInfo propertySetInfo) {
		if (basePropertySetId != null) {
			PropertySet basePropertySet = (PropertySet)allPropertySetsMap.get(basePropertySetId);
			if (basePropertySet == null) {
				throw new RuntimeException("No extendable property set with an id of " + basePropertySetId + " exists.");
			}
			if (basePropertySet.isExtension()) {
				throw new RuntimeException(basePropertySetId + " is an extension property set.  Property sets cannot be derived from extension property sets, but " + propertySetId + " is declared as a derivation of " + basePropertySetId + ".");
			}
			
		}
		
		// Objects of this class self-register, so we need
		// only construct the object.
		new PropertySet(propertySetId, false, basePropertySetId, propertySetInfo);
	}
	
	/**
	 * 
	 * @param propertySetId 
	 * @param propertySetInfo Null if property set data is found in
	 * 			the datastore but no plug-in defined a property set
	 * 			with this id. 
	 * @return
	 */
	static private void registerExtensionPropertySet(String propertySetId, String extendablePropertySetId, IExtensionPropertySetInfo propertySetInfo) {
		PropertySet extendablePropertySet = (PropertySet)allPropertySetsMap.get(extendablePropertySetId);
		if (extendablePropertySet == null) {
			throw new RuntimeException("No extendable property set with an id of " + extendablePropertySetId + " exists.");
		}
		if (extendablePropertySet.isExtension()) {
			throw new RuntimeException("extension on extension");
		}
		// Objects of this class self-register, so we need
		// only construct the object.
		new PropertySet(propertySetId, true, extendablePropertySetId, propertySetInfo);
	}
	
	/**
	 * This version is called when one plug-in wants to access a property
	 * in another plug-in's property set.  Callers must be able to handle
	 * the case where the requested property set is not found.  The plug-in
	 * must catch PropertySetNotFoundException and supply appropriate behavior
	 * (not an error from the user's perspective).
	 */
	static public PropertySet getPropertySet(String propertySetId) throws PropertySetNotFoundException {
		PropertySet propertySet = (PropertySet)allPropertySetsMap.get(propertySetId);
		if (propertySet == null) {
			throw new PropertySetNotFoundException(propertySetId);
		}
		return propertySet;
	}

	
	/**
	 * This method will find the PropertySet object, given the class of an
	 * object.  The class must be an implementation of an extendable property
	 * set.  (The class may not be an implementation of an extension
	 * property set).
	 * <P>
	 * This method should be called when we have an object, but we do not know
	 * exactly of what derived class the object is.  By calling this method,
	 * we can get the actual set of properties for this object.
	 * For example, if one wants to display the properties for
	 * a CapitalAccount object, then call this method to get the property
	 * set for the actual object and you will then see properties for
	 * this particular object (bank account properties if the object
	 * is a bank account, credit card account properties if the
	 * object is a credit card account and so on).
	 * <P>
	 * This method is also useful when introspection gives us the interface
	 * (which extends IExtendableObject) of the object returned by a property 
	 * getter and we need to obtain the property set that is represented by
	 * that interface.
	 */
	static public PropertySet getPropertySet(Class propertySetClass) {
		// The classToPropertySetMap contains mappings for final extendable property
		// sets only, so there should only be a single interface or superclass found
		// in the object's class hierarchy that is in the map.
		
		// Check the superclasses
		Class thisClass = propertySetClass;
		do {
			PropertySet result = (PropertySet)classToPropertySetMap.get(thisClass);
			if (result != null) {
				return result;
			}
			
			result = searchInterfaces(thisClass);
			if (result != null) {
				return result;
			}
			
			thisClass = thisClass.getSuperclass();
		} while (!thisClass.equals(Object.class));
		
		throw new RuntimeException("No property set found for object of class " + propertySetClass.getName() + ".");
	}

	private static PropertySet searchInterfaces(Class parentClass) {
		// Check the interfaces
		Class interfaces[] = parentClass.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			PropertySet result = (PropertySet)classToPropertySetMap.get(interfaces[i]);
			if (result != null) {
				return result;
			}

			result = searchInterfaces(interfaces[i]);
			if (result != null) {
				return result;
			}
		}
		
		return null;
	}

	
	/**
	 * This version is called when loading data from an extension
	 * in a datastore.
	 * The datastore will store the property set id and will pass it.
	 * If no property set with the given id exists in the map then
	 * we create one.  However there will be no class or informatation
	 * available about the property set.
	 */
	static public PropertySet getPropertySetCreatingIfNecessary(String propertySetId, String extendablePropertySetId) {
		PropertySet extendablePropertySet = (PropertySet)allPropertySetsMap.get(extendablePropertySetId);
		if (extendablePropertySet == null) {
			throw new RuntimeException("No extendable property set with an id of " + extendablePropertySetId + " exists.");
		}
		if (extendablePropertySet.isExtension()) {
			throw new RuntimeException("extension on extension error");
		}
		PropertySet key = (PropertySet)extendablePropertySet.extensionPropertySets.get(propertySetId);
		if (key == null) {
			key = new PropertySet(propertySetId, true, extendablePropertySetId, null);
		}
		return key;
	}
	
	public String toString() {
		return propertySetId;
	}
	
	
	/**
	 * @return The globally unique id of the property set.
	 */
	public String getId() {
		return propertySetId;
	}
	
	public boolean isExtensionClassKnown() {
		return propertySetInfo != null;
	}
	
	/**
	 * @return The interface that contains getters for the
	 * properties in this property set.  All objects that
	 * contain properties from this property set will support
	 * this interface.
	 */
	public Class getInterfaceClass() {
		return propertySetInfo.getInterfaceClass();
	}
	
	/**
	 * @return The implementation class.
	 * The interface that contains both getters and setters
	 * for the properties in this property set.
	 */
	public Class getImplementationClass() {
		return propertySetInfo.getImplementationClass();
	}
	
	/**
	 * @return
	 */
	public Object[] getDefaultPropertyValues2() {
		// We do not cache the array returned by this method.
		// The reason is that the default values may contain a
		// timestamp or may depend on user options.
		try {
			Object [] values = (Object[])theDefaultPropertiesMethod.invoke(null, null);
			return values;
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Method '" + theDefaultPropertiesMethod.getName() + "' in '" + getInterfaceClass().getName() + "' must be public.");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
	}

	/**
	 * @return
	 */
	public ExtensionObject getDefaultPropertyValues() {
		return defaultExtension;
	}
	
	/**
	 * Get the property accessor for a property in an 
	 * extension property set.
	 *
	 * @param name The local name of the property.  This name does not
	 *          include the dotted prefix.
	 */
	public PropertyAccessor getProperty(String name) throws PropertyNotFoundException {
		for (Iterator propertyIterator = properties.iterator(); propertyIterator.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIterator.next();
			if (propertyAccessor.getLocalName().equals(name)) {
				return propertyAccessor;
			}
		}
		throw new PropertyNotFoundException(propertySetId, name);
	}
	
	/**
	 * Gets a list of all property sets.  This method is used
	 * by the Propagator class and also by the datastore plug-ins.
	 *  
	 * @return An iterator that will iterate over the full set
	 * 		of property sets, returning for each the PropertySet
	 * 		object.
	 */
	static public Iterator getPropertySetIterator() {
		return allPropertySetsMap.values().iterator();
	}
	
	/**
	 * @return An iterator that iterates over all properties
	 * 		in this property set, returning, for each property,
	 * 		the PropertyAccessor object for that property.
	 */
	public Iterator getPropertyIterator1() {
		return properties.iterator();
	}
	
	/**
	 * Returns an iterator which iterates over all properties
	 * of the given set of property sets, including both properties in the 
	 * extendable object and properties in extension property sets.
	 * <P>
	 * Properties from base property sets and properties from
	 * derived property sets are not returned.
	 * 
	 * @ return An iterator which iterates over a set of
	 * 		<code>PropertyAccessor</code> objects.
	 */
	// This method may be called on extendable property sets only
	// (i.e. not on extension property sets).
	public Iterator getPropertyIterator2() {
		// Build an array - not efficient but easy and avoids concurrency problems.
		// TODO: write a proper iterator, or at least cache this vector.
		Vector fields = new Vector();

		// Properties in this extendable object
		for (Iterator propertyIterator = getPropertyIterator1(); propertyIterator.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIterator.next();
			fields.add(propertyAccessor);
		}

		// Properties in the extensions
		for (Iterator propertySetIterator = extensionPropertySets.values().iterator(); propertySetIterator.hasNext(); ) {
			PropertySet extensionPropertySet = (PropertySet)propertySetIterator.next();
			
			for (Iterator propertyIterator = extensionPropertySet.getPropertyIterator1(); propertyIterator.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIterator.next();
				fields.add(propertyAccessor);
			}
		}
		
		return fields.iterator();
	}


	/**
	 * Returns an iterator which iterates over all properties
	 * of the given set of property sets, including all
	 * extension properties, and all base properties including
	 * all extension properties to the base property sets.
	 * <P>
	 * This is the set of properties that can be set against
	 * an object that implements this property set.
	 * <P>
	 * Properties are returned with the properties from the
	 * base-most class first, then properties from the class
	 * immediately derived from the base-most class, and so
	 * on with the properties from this property set being
	 * last.  This order gives the most intuitive order from
	 * the user's perspective.
	 * 
	 * @ return An iterator which iterates over a set of
	 * 		<code>PropertyAccessor</code> objects.
	 */
	// This method may be called on extendable property sets only
	// (i.e. not on extension property sets).
	public Iterator getPropertyIterator3() {
		// Build an array - not efficient but easy and avoids concurrency problems.
		// TODO: write a proper iterator, or at least cache this vector.
		Vector fields = new Vector();

		// Properties in this and all the base property sets
		PropertySet extendablePropertySet = this;
		do {
			int index= 0;
			for (Iterator propertyIterator = extendablePropertySet.getPropertyIterator2(); propertyIterator.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIterator.next();
				fields.insertElementAt(propertyAccessor, index++);
			}
			extendablePropertySet = extendablePropertySet.getBasePropertySet();
		} while (extendablePropertySet != null);

		return fields.iterator();
	}

	/**
	 * Returns an iterator which iterates over all properties
	 * of the given set of property sets, including both properties in the 
	 * extendable object and properties in extension property sets.
	 * <P>
	 * This method does not have access to an instance of the property set class,
	 * and so it must return all properties that may be in an
	 * object including properties that may not be applicable in
	 * some objects.  We therefore include all properties in
	 * all base classes, all classes derived from this class, and all
	 * extensions to any base or derived classes.  (Properties in classes
	 * or extensions of classes that are derived from a base class of
	 * the given class are not returned).
	 * 
	 * @ return An iterator which iterates over a set of
	 * 		<code>PropertyAccessor</code> objects.
	 */
	// This method may be called on extendable property sets only
	// (i.e. not on extension property sets).
	public Iterator getPropertyIterator4() {
		// Build an array - not efficient but easy and avoids concurrency problems.
		// TODO: write a proper iterator, or at least cache this vector.
		Vector fields = new Vector();

		// Properties in this and all the base property sets
		for (Iterator propertyIterator = getPropertyIterator3(); propertyIterator.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIterator.next();
			fields.add(propertyAccessor);
		}
		
		// Properties from derived property sets
		addPropertiesFromDerivedPropertySets(fields);
		
		return fields.iterator();
	}

	// private helper method.
	// Method valid for extendable property set only.
	void addPropertiesFromDerivedPropertySets(Vector fields) {
		for (Iterator propertySetIterator = derivedPropertySets.iterator(); propertySetIterator.hasNext(); ) {
			PropertySet derivedPropertySet = (PropertySet)propertySetIterator.next();
			
			for (Iterator propertyIterator = derivedPropertySet.getPropertyIterator2(); propertyIterator.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIterator.next();
				fields.add(propertyAccessor);
			}
			
			derivedPropertySet.addPropertiesFromDerivedPropertySets(fields);
		}
	}		

	/**
	 * Gets a list of all property sets that extend the given property
	 * set.  This method is used by the Propagator class only.
	 * <P>
	 * Note:
	 * This method does not return derived property sets.
	 * This method does not return property sets that extend any 
	 * property sets from which this property set is derived.
	 *  
	 * @param extendablePropertySetId
	 * @return An iterator that iterates over the property sets
	 * 			that extend the given extendable property set,
	 * 			returning for each property set a String containing
	 * 			the id of that property set.
	 */
	static Iterator getExtensionPropertySetIterator(PropertySet extendablePropertySet) {
		if (extendablePropertySet.isExtension()) {
			throw new RuntimeException("cannot iterate extension property sets of an extension");
		}
		return extendablePropertySet.extensionPropertySets.values().iterator();
	}
	
	
	/**
	 * Gets the accessor for a property given the full name of the property.
	 *
	 * This method is used when a column name is persisted in, say, a preferences file.
	 * This method is also used to get the base properties that are hard coded
	 * into both the framework and possibly plug-ins.
	 * 
	 * @param name The fully qualified property name
	 */
	public static PropertyAccessor getPropertyAccessor(String fullPropertyName) throws PropertyNotFoundException {
		// The local name for properties are the property names that
		// match the getter and setter method names according to the
		// Java Bean property patterns.  They cannot therefore contain
		// a dot.  We are therefore able to separate the property set id
		// and the local property name by looking for the last dot in
		// the full property name.
		
		int dotLocation = fullPropertyName.lastIndexOf('.');
		if (dotLocation <= 0) return null;
		if (dotLocation == fullPropertyName.length() - 1) return null;
		
		String propertySetId = fullPropertyName.substring(0, dotLocation);
		String localName = fullPropertyName.substring(dotLocation + 1);
		
		PropertySet propertySet = (PropertySet)allPropertySetsMap.get(propertySetId);
		if (propertySet == null) {
			// TODO should we use the different exception for
			// the case where the property set is not found?
			throw new PropertyNotFoundException(propertySetId, localName);
		}
		
		PropertyAccessor propertyAccessor = propertySet.getProperty(localName);
		if (propertyAccessor == null) {
			throw new PropertyNotFoundException(propertySetId, localName);
		} 
		
		return propertyAccessor;
	}
	
	
	/**
	 * Gets the accessor for a property given the local name of the property.
	 *
	 * This method searches only this property set and any base
	 * property sets.  No extensions are searched.
	 * <P>
	 * This method is used when a column name is persisted in, say, a file
	 * and we are keen to keep the data in the file as simple and short as
	 * possible.  We therefore allow local names only to be specified.
	 * Local names may not be unique when extensions are included, so we
	 * must require fully qualified names for extensions.
	 * 
	 * @param name The local property name
	 */
	public PropertyAccessor getPropertyAccessorGivenLocalNameAndExcludingExtensions(String localPropertyName) throws PropertyNotFoundException {
		PropertySet thisPropertySet = this;
		do {
			try {
				return thisPropertySet.getProperty(localPropertyName);
			} catch (PropertyNotFoundException e) {
			}
			thisPropertySet = thisPropertySet.getBasePropertySet();
		} while (thisPropertySet != null);
		
		throw new PropertyNotFoundException(propertySetId, localPropertyName);
	}
	
	
	/**
	 * @return
	 */
	public boolean isExtension() {
		return isExtension;
	}


	/**
	 * This method is valid for extendable property sets only.
	 * Do not call this method on extension property sets.
	 * 
	 * @return If this property set is derived from another property
	 * 			set then the base property set is returned, otherwise
	 * 			null is returned.
	 */
	public PropertySet getBasePropertySet() {
		if (isExtension) {
			throw new RuntimeException("getBasePropertySet called for an extension.");
		}
		
		return basePropertySet;
	}
	
	/**
	 * This method is valid for extension property sets only.
	 * Do not call this method on extendable property sets.
	 * 
	 * @return The property set being extended by this property set.
	 */
	public PropertySet getExtendablePropertySet() {
		if (!isExtension) {
			throw new RuntimeException("getExtensionPropertySet called for an extendable property set.");
		}
		
		return extendablePropertySet;
	}	


	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * 
	 * @return The full constructor.  The full constructor takes
	 * 		a set of parameters sufficient to fully construct the
	 * 		object.
	 */
	public Constructor getConstructor() {
		return implementationClassConstructor;
	}


	/**
	 * @return True if this property set can only be used by
	 * 			deriving another property set from it, false
	 * 			if property sets cannot be derived from this
	 * 			property set.
	 */
	public boolean isDerivable() {
		return derivablePropertySet;
	}


}
