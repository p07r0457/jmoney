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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * A <code>PropertySet</code> contains information about a
 * property set.  A property set is a set of properties
 * that either:
 * <UL>
 * <LI>form the base set of properties in a data model object
 *     </LI>
 * <LI>or are the properties added to a data model object
 * by a derived class (such property sets are known as
 * derived property sets)
 *     </LI>
 * <LI>or are the properties added to a data model object by
 * a plug-in (such property sets are know as extension
 * property sets) 
 *     </LI>
 * </UL>
 * The <code>getBasePropertySet</code> and <code>isExtension</code> methods  
 * can be called to determine in which of the above three categories
 * a property set lies.
 * 
 * @see <a href="propertySets.html">Property Set Documentation</a>
 * @see <a href="extendingDatamodel.html#propertySets">Property Set Documentation</a>
 * @param E the type of the implementation object, which must be
 * 		either an ExtendableObject or an ExtensionObject 
 * @author Nigel Westbury
*/
public abstract class PropertySet<E> {
	
	protected String propertySetId;
	
	protected Class<E> classOfObject;
	
	protected Vector<PropertyAccessor> properties = new Vector<PropertyAccessor>();

	/**
	 * These arrays are built on first use and then cached.
	 */
	private Vector<ScalarPropertyAccessor> scalarProperties1 = null;
	private Vector<ListPropertyAccessor> listProperties1 = null;

	boolean isExtension;
	
	static Vector<PropertySet> allPropertySets = new Vector<PropertySet>();
	static Set<String> allPropertySetIds = new HashSet<String>();
	
	/**
	 * Maps property set id to the property set
	 */
//	protected static Map<String, PropertySet> allPropertySetsMap = new HashMap<String, PropertySet>();
	protected static Map<String, ExtendablePropertySet> allExtendablePropertySetsMap = new HashMap<String, ExtendablePropertySet>();
	protected static Map<String, ExtensionPropertySet> allExtensionPropertySetsMap = new HashMap<String, ExtensionPropertySet>();

	/**
	 * Map extendable classes to property sets.
	 */
	protected static Map<Class<? extends ExtendableObject>, ExtendablePropertySet> classToPropertySetMap = new HashMap<Class<? extends ExtendableObject>, ExtendablePropertySet>();
	
	// Valid for all property sets except those that are
	// extendable and must be derived from.
	private Constructor<E> implementationClassConstructor;
	
	// Valid for all property sets except those that are
	// extendable and must be derived from.
	private Constructor<E> defaultImplementationClassConstructor;
	
	/**
	 * A list of all properties in this and base property sets that are
	 * passed to the 're-instantiating' constructor.  The order of properties in
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
	protected Vector<PropertyAccessor> constructorProperties;
	
	/**
	 * A list of all properties in this and base property sets that are
	 * passed to the 'new object' constructor.  The 'new object' constructor
	 * takes parameters only for the properties that are lists, so this
	 * vector is a list of the list properties.  The order of properties in
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
	protected Vector<PropertyAccessor> defaultConstructorProperties;
	
//	private Method theDefaultPropertiesMethod;
	
	protected PropertySet() {
		// Add to our list of all property sets
		allPropertySets.add(this);
	}
	
	/**
	 * This method is called after all the properties in this property set have
	 * been set.  It completes the initialization of this object.
	 * 
	 * This cannot be done in the constructor because there may be circular references
	 * between property sets, properties in those property sets, and property sets for
	 * the objects referenced by those properties.
	 * 
	 * @param propertySetId 
	 *
	 */
	public void initProperties(String propertySetId) {
		/*
		 * Check that the property set id is unique.
		 */
		if (allPropertySetIds.contains(propertySetId)) {
			throw new MalformedPluginException("More than one property set has an id of " + propertySetId);
		}
		this.propertySetId = propertySetId;
		allPropertySetIds.add(propertySetId);
	}
	
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
						if (!(listener instanceof IPropertySetInfo)) {
							throw new MalformedPluginException(
									"Plug-in " + extensions[i].getNamespaceIdentifier()
									+ " extends the net.sf.jmoney.fields extension point. "
									+ "However, the class specified by the info-class attribute "
									+ "(" + listener.getClass().getName() + ") "
									+ "does not implement the IPropertySetInfo interface. "
									+ "This interface must be implemented by all classes referenced "
									+ "by the info-class attribute.");
						}

						IPropertySetInfo pageListener = (IPropertySetInfo)listener;
						
						String fullPropertySetId = extensions[i].getNamespaceIdentifier();
						String id = elements[j].getAttribute("id");
						if (id != null && id.length() != 0) {
							fullPropertySetId = fullPropertySetId + '.' + id;
						}
						
						String basePropertySetId = elements[j].getAttribute("base-property-set");
						if (basePropertySetId != null && basePropertySetId.length() == 0) {
							basePropertySetId = null;
						}
						registerExtendablePropertySet(fullPropertySetId, basePropertySetId, pageListener);
					} catch (CoreException e) {
						if (e.getStatus().getException() instanceof ClassNotFoundException) {
							ClassNotFoundException e2 = (ClassNotFoundException)e.getStatus().getException();
							throw new MalformedPluginException(
									"Plug-in " + extensions[i].getNamespaceIdentifier()
									+ " extends the net.sf.jmoney.fields extension point. "
									+ "However, the class specified by the info-class attribute "
									+ "(" + e2.getMessage() + ") "
									+ "could not be found. "
									+ "The info-class attribute must specify a class that implements the "
									+ "IPropertySetInfo interface.");
						}
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
						if (listener instanceof IPropertySetInfo) {
							IPropertySetInfo pageListener = (IPropertySetInfo)listener;
							
							String fullPropertySetId = extensions[i].getNamespaceIdentifier();
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

		/*
		 * Check for property sets that have been created (because
		 * other property sets depended on them) but that have no entry
		 * in a plugin.xml file.  
		 */
		for (PropertySet propertySet: PropertySet.allPropertySets) {
			if (propertySet.getId() == null) {
				throw new MalformedPluginException("The property set for " + propertySet.getImplementationClass().getName() + " has not been registered in the plugin.xml file.");
			}
		}
		
		/*
		 * After all property information has been registered, make a second
		 * pass through the extendable objects. In this pass we do processing of
		 * extendable property sets that requires the complete set of extension
		 * property sets to be available and complete.
		 */
		for (ExtendablePropertySet propertySet: PropertySet.getAllExtendablePropertySets()) {
			propertySet.initPropertiesPass2();
		}
	}
	
	/**
	 * Helper method to find the constructors.
	 * 
	 * This method is called both for extendable property
	 * sets (final, not derivable) and extension property
	 * sets.
	 *  
	 * @param isExtension
	 */
	protected void findConstructors(boolean isExtension) {
		// Build the list of properties that are passed to
		// the 'new object' constructor and another list that
		// are passed to the 're-instantiating' constructor.
		{
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
			
			for (PropertyAccessor propertyAccessor: constructorProperties) {
				if (propertyAccessor.isScalar()) {
					ScalarPropertyAccessor scalarAccessor = (ScalarPropertyAccessor)propertyAccessor;
					if (ExtendableObject.class.isAssignableFrom(scalarAccessor.getClassOfValueType())) { 		
						// For extendable objects, we pass not the object
						// but a key to the object.  This is so that we do not
						// have to read the object from the database unless it
						// is necessary.
						parameters[i] = IObjectKey.class; 
					} else {
						// The property type is either a primative (int, boolean etc),
						// or a non-extendable class (Long, String, Date)
						// or a non-extendable class added by a plug-in.
						parameters[i] = scalarAccessor.getClassOfValueType();
					}
				} else {
					parameters[i] = IListManager.class; 
				}
				i++;
			}
			
			try {
				implementationClassConstructor =
					classOfObject.getConstructor(parameters);
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
				throw new MalformedPluginException("The " + classOfObject.getName() + " class must have a constructor that takes parameters of types (" + parameterText + ").");
			}
		}
		
		// Find the 'new object' constructor which sets the default values for
		// the scalar properties.
		// Build the list of types of the constructor parameters.
		{
			int i = 0;
			int parameterCount = defaultConstructorProperties.size();
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
			
			/*
			 * The default constructor takes a parameter for each list property
			 * but does not take any parameters for the scalar properties.  All
			 * list property parameters are of type IListManager, so add 
			 * IListManager class to the constructor prototype once for each
			 * list property.
			 */
			for (int j = 0; j < defaultConstructorProperties.size(); j++) {
				parameters[i++] = IListManager.class; 
			}
			
			try {
				defaultImplementationClassConstructor =
					classOfObject.getConstructor(parameters);
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
				throw new MalformedPluginException("The " + classOfObject.getName() + " class must have a constructor that takes parameters of types (" + parameterText + ").");
			}
		}
	}
	
	/**
	 * Returns the list of properties whose values are passed as
	 * parameters to the 're-instantiating' constructor of the implementation class.
	 * <P>
	 * All properties, scalar and list properties, are included
	 * in this list.  The collection is ordered in the same order
	 * as the property values are passed to the constructor.
	 * <P>
	 * This method may not be called on derivable property sets
	 * because objects implementing derivable property sets are
	 * abstract and so no constructor is used by the JMoney framework.
	 *  
	 * @return
	 */
	public Collection<PropertyAccessor> getConstructorProperties() {
		return constructorProperties;
	}


	/**
	 * Returns the list of properties whose values are passed as
	 * parameters to the 'new object' constructor of the implementation class.
	 * <P>
	 * Only list properties are included
	 * in this list.  The collection is ordered in the same order
	 * as the property values are passed to the constructor.
	 * <P>
	 * This method may not be called on derivable property sets
	 * because objects implementing derivable property sets are
	 * abstract and so no constructor is used by the JMoney framework.
	 *  
	 * @return
	 */
	public Collection<PropertyAccessor> getDefaultConstructorProperties() {
		return defaultConstructorProperties;
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
	 * @param class1 
	 * @return
	 */
	static private void registerExtendablePropertySet(final String propertySetId, final String basePropertySetId, IPropertySetInfo propertySetInfo) {
		
		// Set up the list of properties.
		// This is done by calling the registerProperties
		// method of the supplied interface.
		PropertySet propertySet = propertySetInfo.registerProperties();
		propertySet.initProperties(propertySetId);
	}
	
	/**
	 * 
	 * @param propertySetId 
	 * @param propertySetInfo Null if property set data is found in
	 * 			the datastore but no plug-in defined a property set
	 * 			with this id. 
	 * @return
	 */
	static private void registerExtensionPropertySet(final String propertySetId, final String extendablePropertySetId, IPropertySetInfo propertySetInfo) {
		// Set up the list of properties.
		// This is done by calling the registerProperties
		// method of the supplied interface.
		PropertySet propertySet = propertySetInfo.registerProperties();
		propertySet.initProperties(propertySetId);
	}
	
	/**
	 * This method is called when a property set id in plugin.xml references
	 * an extendable property set.  The property set object is
	 * returned.
	 */
	static public ExtendablePropertySet getExtendablePropertySet(String propertySetId) throws PropertySetNotFoundException {
		ExtendablePropertySet propertySet = allExtendablePropertySetsMap.get(propertySetId);
		if (propertySet == null) {
			throw new PropertySetNotFoundException(propertySetId);
		}
		return propertySet;
	}

	/**
	 * This method is called when one plug-in wants to access a property
	 * in another plug-in's property set.  Callers must be able to handle
	 * the case where the requested property set is not found.  The plug-in
	 * must catch PropertySetNotFoundException and supply appropriate behavior
	 * (not an error from the user's perspective).
	 */
	static public ExtensionPropertySet getExtensionPropertySet(String propertySetId) throws PropertySetNotFoundException {
		ExtensionPropertySet propertySet = allExtensionPropertySetsMap.get(propertySetId);
		if (propertySet == null) {
			throw new PropertySetNotFoundException(propertySetId);
		}
		return propertySet;
	}

	
	/**
	 * This method will find the PropertySet object, given the class of an
	 * implementation object.  The given class must be an implementation of ExtendableObject
	 * (The class may not be an implementation of an extension
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
	 */
	static public <E extends ExtendableObject> ExtendablePropertySet<?> getPropertySet(Class<E> propertySetClass) {
		return classToPropertySetMap.get(propertySetClass);
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
	
	/**
	 * Returns the implementation class for this property set.
	 * 
	 * The implementation class for a property set is a class that
	 * implements getters and setters for all the properties in
	 * the property set.  Implementation classes for property sets
	 * have a few other rules they must follow too.  For example,
	 * certain constructors must be provided and they must extend
	 * either ExtendableObject or ExtensionObject.
	 * See the documentation on property set implementation classes
	 * for further information.
	 *
	 * @see doc on implemetation classes
	 * @return the implementation class
	 */
	public Class<E> getImplementationClass() {
		return classOfObject;
	}
	
	/**
	 * Get the property accessor for a property in a 
	 * property set.
	 * 
	 * This method looks in only in the given property set
	 * (it will not look in base property sets or extension
	 * property sets).
	 *
	 * @param name The local name of the property.  This name does not
	 *          include the dotted prefix.
	 */
	public PropertyAccessor getProperty(String name) throws PropertyNotFoundException {
		for (PropertyAccessor propertyAccessor: properties) {
			if (propertyAccessor.getLocalName().equals(name)) {
				return propertyAccessor;
			}
		}
		throw new PropertyNotFoundException(propertySetId, name);
	}
	
	/**
	 * Gets a list of all extension property sets.
	 *  
	 * @return the collection of all property sets
	 */
	static public Collection<ExtensionPropertySet> getAllExtensionPropertySets() {
		return allExtensionPropertySetsMap.values();
	}
	
	/**
	 * Gets a list of all extendable property sets.
	 *  
	 * @return the collection of all property sets
	 */
	static public Collection<ExtendablePropertySet> getAllExtendablePropertySets() {
		return allExtendablePropertySetsMap.values();
	}
	
	/**
	 * @return An iterator that iterates over all properties
	 * 		in this property set, returning, for each property,
	 * 		the PropertyAccessor object for that property.
	 */
	// What is the difference between this and the construction
	// properties????
	public Collection<PropertyAccessor> getProperties1() {
		return properties;
	}
	
	public Collection<ScalarPropertyAccessor> getScalarProperties1() {
		if (scalarProperties1 == null) {
			scalarProperties1 = new Vector<ScalarPropertyAccessor>();
			for (PropertyAccessor propertyAccessor: properties) {
				if (propertyAccessor instanceof ScalarPropertyAccessor) {
					scalarProperties1.add((ScalarPropertyAccessor)propertyAccessor);
				}
			}
		}
		
		return scalarProperties1;
	}
	
	public Collection<ListPropertyAccessor> getListProperties1() {
		if (listProperties1 == null) {
			listProperties1 = new Vector<ListPropertyAccessor>();
			for (PropertyAccessor propertyAccessor: properties) {
				if (propertyAccessor instanceof ListPropertyAccessor) {
					listProperties1.add((ListPropertyAccessor)propertyAccessor);
				}
			}
		}
		
		return listProperties1;
	}
	
	/**
	 * @return
	 */
	public boolean isExtension() {
		return isExtension;
	}
	
	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * 
	 * @param constructorParameters an array of values to be passed to
	 * 		the constructor.  If an extendable object is being constructed
	 * 		then the first three elements of this array must be the
	 * 		object key, the extension map, and the parent object key.
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public E constructImplementationObject(Object [] constructorParameters) {
		// TODO: tidy up error processing
		try {
			return implementationClassConstructor.newInstance(constructorParameters);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Constructor must be public.");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			throw new MalformedPluginException("An exception occured within a constructor in a plug-in.", e);
		}
	}


	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * 
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public E constructDefaultImplementationObject(Object [] constructorParameters) {
		// TODO: tidy up error processing
		try {
			return (E)defaultImplementationClassConstructor.newInstance(constructorParameters);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Constructor must be public.");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			throw new MalformedPluginException("An exception occured within a constructor in a plug-in.", e);
		}
	}



	public static <E2 extends ExtendableObject> ExtendablePropertySet<E2> addBasePropertySet(Class<E2> classOfImplementationObject) {
		return new ExtendablePropertySet<E2>(classOfImplementationObject);
	}

	public static <E extends ExtendableObject> ExtendablePropertySet<E> addDerivedPropertySet(Class<E> classOfImplementationObject, ExtendablePropertySet<? super E> basePropertySet) {
		return new ExtendablePropertySet<E>(classOfImplementationObject, basePropertySet);
	}

	public static <E extends ExtensionObject> ExtensionPropertySet<E> addExtensionPropertySet(Class<E> classOfImplementationObject, ExtendablePropertySet<?> extendablePropertySet) {
		return new ExtensionPropertySet<E>(classOfImplementationObject, extendablePropertySet, true);
	}

	public <V> ScalarPropertyAccessor<V> addProperty(String name, String displayName, Class<V> classOfValue, int weight, int minimumWidth, IPropertyControlFactory<V> propertyControlFactory, IPropertyDependency propertyDependency) {
		if (propertyControlFactory == null) {
			throw new MalformedPluginException(
					"No IPropertyControlFactory object has been specified for property " + name
					+ ".  This is needed even if the property is not editable.  (Though the method that gets the" +
			" control may return null if the property is not editable).");
		}

		ScalarPropertyAccessor<V> accessor = new ScalarPropertyAccessor<V>(classOfValue, this, name, displayName, weight, minimumWidth, propertyControlFactory, propertyDependency);
		properties.add(accessor);
		return accessor;
	}

	public <E2 extends ExtendableObject> ListPropertyAccessor<E2> addPropertyList(String name, String displayName, ExtendablePropertySet<E2> elementPropertySet, final IListGetter<E, E2> listGetter, IPropertyDependency propertyDependency) {
		ListPropertyAccessor<E2> accessor = new ListPropertyAccessor<E2>(this, name, displayName, elementPropertySet, propertyDependency) {
			@Override
			public ObjectCollection<E2> getElements(Object invocationTarget) {
				return listGetter.getList((E)classOfObject.cast(invocationTarget));
			}
		};
		
		properties.add(accessor);
		return accessor;
	}

}
