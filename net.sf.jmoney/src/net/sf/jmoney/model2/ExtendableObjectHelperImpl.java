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

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;

//Required for converting extensions to and from strings
import java.beans.*;
import java.lang.reflect.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A partial implementation of an extendable object.  Plug-ins that provide
 * a datastore implementation must implement certain interfaces that
 * extend the IExtendableObject interface.
 * <P>
 * This class provides a partial implementation of the IExtendableObject interface.
 * By extending this class, a full implementation of the IExtendableObject interface
 * can be coded with less code than would be necessary if a full
 * implementation of the IExtendableObject interface were to be developed without
 * assistance of this class.  Whileas all datastore implementations
 * provided with jmoney use this helper class, it is not required that
 * plug-ins use this class.  A plug-in may provide an IExtendableObject implementation
 * without using this class.
 * <P>
 * For some methods in the IExtendableObject interface, this class provides a
 * full implementation.  For other methods, protected helper methods be 
 * provided to aid in the implementation.
 * 
 * @author  Nigel
 */
public abstract class ExtendableObjectHelperImpl implements IExtendableObject {
	
	/**
	 * The key from which this object can be fetched from
	 * the datastore and a reference to this object obtained.
	 */
	IObjectKey objectKey;
	
	/**
	 * Extendable objects may have extensions containing additional data
	 * needed by the plugins.  Each plugin will have a different
	 * extension.
	 * This map will map plugins to the appropriate extension object for
	 * this object.
	 */
	protected Map extensions = new Hashtable();
	
	protected boolean alwaysReturnNonNullExtensions = false;
	
	protected abstract boolean isMutable();
	
	/**
	 * If this object is mutable, get the original object, if any.
	 * If this is a newly created object that has not yet been committed
	 * to the datastore then null will be returned.
	 * <P>
	 * This is an abstract method that allows the implementation of
	 * this class to get the original object from the derived class.
	 * The original object is kept in the derived class and not in
	 * this class so that the field can be of the appropriate type
	 * and the derived class does not have to cast the reference.
	 */
	protected abstract IExtendableObject getOriginalObject();
	
	protected abstract String getExtendablePropertySetId();
/*
	// TODO: We probably want to remove this constructor.
	protected ExtendableObjectHelperImpl() {
	}
*/	
	/**
	 * @param extensions A map from PropertySet objects representing
	 * 			extension property sets to the parameter lists from
	 * 			which the extension property set objects can be
	 * 			constructed.
	 */
	protected ExtendableObjectHelperImpl(IObjectKey objectKey, Map extensionParameters) {
		this.objectKey = objectKey;
		
		if (extensionParameters != null) {
			for (Iterator iter = extensionParameters.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry entry = (Map.Entry)iter.next();
				PropertySet propertySet = (PropertySet)entry.getKey();
				Object[] constructorParameters = (Object[])entry.getValue();
				
				Constructor constructor = propertySet.getConstructor();
				ExtensionObject extensionObject;
				try {
					extensionObject = (ExtensionObject)constructor.newInstance(constructorParameters);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw new RuntimeException("internal error");
				} catch (InstantiationException e) {
					e.printStackTrace();
					throw new RuntimeException("internal error");
				} catch (IllegalAccessException e) {
					throw new MalformedPluginException("Constructor must be public.");
				} catch (InvocationTargetException e) {
					throw new MalformedPluginException("An exception occured within a constructor in a plug-in.");
				}
				
				extensionObject.setBaseObject(this);
				extensionObject.setPropertySet(propertySet);
				extensions.put(propertySet, extensionObject);
			}
		}
	}
	
	public IObjectKey getObjectKey() {
		return objectKey;
	}
	
	/**
	 * Two or more instantiated objects may represent the same object
	 * in the datastore.  Such objects should be considered
	 * the same.  Therefore this method overrides the default
	 * implementation that is based on Java identity.
	 * <P>
	 * @return true if the two objects represent the same object
	 * 		in the datastore, false otherwise.
	 */
	public boolean equals(Object object) {
		// Two objects represent the same object if and only if
		// the keys from which they were created are the same.
		// Therefore we compare the key objects to see if they
		// both contain the same data.
		if (object instanceof ExtendableObjectHelperImpl) {
			ExtendableObjectHelperImpl extendableObject = (ExtendableObjectHelperImpl)object;
			if (isMutable() || extendableObject.isMutable()) {
				// Mutable objects are not considered the same as the
				// original, nor are two mutable objects considered the
				// same just because they are based on the same original
				// object.
				return this == extendableObject;
			} else {
				return getObjectKey().equals(extendableObject.getObjectKey());
			}
		} else {
			return false;
		}
	}
	/**
	 * This method may be called by the datastore plug-in immediately
	 * after it has constructed this object.
	 * 
	 * If the object type is indexed then this method must be overridden
	 * to add itself to the appropriate indexes.
	 *
	 * Some datastores may not bother to call this method.  For example,
	 * a SQL database updates its indexes itself automatically.  This method
	 * is not technically necessary because the datastore plug-in could keep it's
	 * own index information.  However, it makes things simple and efficient.
	 */
	public void registerWithIndexes() {
	}
	
	public ExtensionObject getExtension(PropertySet propertySetKey) {
		// Perform any processing that must take place after an object
		// has been loaded from the datastore but before the extensions
		// can be accessed.
//		postLoad();
		
		Object extensionObject = extensions.get(propertySetKey);
		
		ExtensionObject extension;
		
		if (extensionObject == null) {
			// Extension does not exist.
			
			if (alwaysReturnNonNullExtensions || isMutable()) {
				// This is a mutable object.
				// Create a new extension and look to the original
				// for default values.
				
				try {
					extension = (ExtensionObject)
					propertySetKey.getInterfaceClass().newInstance();
					// TODO: plugin error if null is returned
				} catch (Exception e) {
					// TODO: ensure that we check for a default constructor
					// at the time the plug-in is loaded.  Errors by plug-in
					// developers should not cause this type of exception.
					throw new RuntimeException("internal error");
				}
				
				// Add the extension now.
				// It is important that this is done before the property
				// values are set in it.  The property may be propagated
				// to other extensions, and back to this extension, through
				// adaptors.  We could get infinite recursion if the propagation
				// got back to this extension and the extension was not set.
				extension.setBaseObject(this);
				extension.setPropertySet(propertySetKey);
				extensions.put(propertySetKey, extension);
				
				if (getOriginalObject() != null) {
					
					ExtensionObject originalExtension = getOriginalObject().getExtension(propertySetKey);
					
					if (originalExtension != null) {
						propertySetKey.copyProperties(originalExtension, extension);
					}
				}
			} else {
				// This is a non-mutable object.
				// Return null to indicate that no extension exists
				// and default values should be used.
				extension = null;
			}
		} else {
			// The extension exists.
			// However, it may be in string format.
			// If the extension is in string format then it must be
			// converted to the appropriate extension object before
			// being returned.
			
			if (extensionObject instanceof String) {
				String extensionString = (String) extensionObject;
				
				try {
					extension = (ExtensionObject)
					propertySetKey.getInterfaceClass().newInstance();
					// TODO: plugin error if null is returned
				} catch (Exception e) {
					// TODO: ensure that we check for a default constructor
					// at the time the plug-in is loaded.  Errors by plug-in
					// developers should not cause this type of exception.
					throw new RuntimeException("internal error");
				}
				
				// Add the extension now.
				// It is important that this is done before the property
				// values are set in it.  The property may be propagated
				// to other extensions, and back to this extension, through
				// adaptors.  We could get infinite recursion if the propagation
				// got back to this extension and the extension was not set.
				
				// TODO: We really should not be propagating properties now.
				// If there are changes, the datastore was inconsistent,
				// and changing other properties now could cause confusion.
				extension.setBaseObject(this);
				extension.setPropertySet(propertySetKey);
				extensions.put(propertySetKey, extension);
				stringToExtension(extensionString, extension);
			} else {
				// Extension object is not a string so it must be
				// an extension object in the de-serialized state.
				// Return the extension as is.
				extension = (ExtensionObject)extensionObject;
			}
		}
		
		return extension;
	}
	
	/** 
	 * Perform any processing that must take place after an object
	 * has been loaded from the datastore but before the extensions
	 * can be accessed.
	 */
/*	
	protected void postLoad() 
	{
	}
	*/;
	
	/**
	 * Takes a map of extensions that have been modified and copies
	 * the changes into the extensible object.
	 * <P>
	 * The modified extensions will always be in a de-serialized state.
	 * There will be no extensions represented by a String object in
	 * the modifiedExtensions map.
	 * <P>
	 * NYI: If all the properties in an extension are set to their
	 * default values then the extension is removed from the map.
	 * <P>
	 * NYI: This method checks to see which properties have changed.
	 * Appropriate events are fired for those properties whose values
	 * have changed.
	 */ 
	public void copyExtensions(Map modifiedExtensions) {
		// The mutable object will contain only those extensions
		// that were requested.  Even if there are a large number of
		// extensions in the object, it is expected that only a small
		// number will have been requested by the user from
		// the mutable object.  For those extensions that were not
		// requested, we can safely and efficiently leave the original
		// extension in the original object.
		
		for (Iterator iter = modifiedExtensions.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry mapEntry = (Map.Entry)iter.next();
			
			PropertySet propertySetKey = (PropertySet)mapEntry.getKey();
			IExtendableObject extension = (IExtendableObject)mapEntry.getValue();
			
			if (extensions.get(propertySetKey) != null) {
				// Extension is also in the original.
				
				// TODO: Loop around the properties and see which are
				// different.  Fire events for those that are.
				// Also, see if any properties are different from
				// the default properties.  Just set the extension
				// to null if none are different.
				
				// Replace the extension.
				extensions.put(propertySetKey, extension);
			} else {
				// The extension does not exist in the original.
				
				// TODO: Loop around seeing which extensions are
				// different from the default values.  Fire events
				// for those that are.  Leave null if none are
				// different.
				
				// Add the extension.
				extensions.put(propertySetKey, extension);
			}
		}
	}
	
	public int getIntegerPropertyValue(PropertyAccessor propertyAccessor) {
		return ((Integer)getPropertyValue(propertyAccessor)).intValue();
	}
	
    public long getLongPropertyValue(PropertyAccessor propertyAccessor) {
        return ((Long)getPropertyValue(propertyAccessor)).longValue();
    }
    
    public String getStringPropertyValue(PropertyAccessor propertyAccessor) {
        return (String)getPropertyValue(propertyAccessor);
    }
    
	public Object getPropertyValue(PropertyAccessor propertyAccessor) {
		Object objectWithProperties = getPropertySetInterface(propertyAccessor.getPropertySet());
		
		try {
			return  propertyAccessor.getTheGetMethod().invoke(objectWithProperties, null);
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Method '" + propertyAccessor.getTheGetMethod().getName() + "' in '" + propertyAccessor.getPropertySet().getInterfaceClass().getName() + "' must be public.");
		} catch (Exception e) {
			// TODO:
			return null;
		}
	}
	
	public Iterator getPropertyIterator(PropertyAccessor propertyAccessor) {
		Object objectWithProperties = getPropertySetInterface(propertyAccessor.getPropertySet());
		
		try {
			return  (Iterator)propertyAccessor.getTheGetMethod().invoke(objectWithProperties, null);
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Method '" + propertyAccessor.getTheGetMethod().getName() + "' in '" + propertyAccessor.getPropertySet().getInterfaceClass().getName() + "' must be public.");
		} catch (Exception e) {
			// TODO:
			return null;
		}
	}
	
	public void setPropertyValue(PropertyAccessor propertyAccessor, Object value) {
		// The problem here is that the XML parser sets the properties directly in
		// the object, without going through a mutable object.
		// We cannot therefore rely on this object being mutable, so temporarily
		// set this flag.
		alwaysReturnNonNullExtensions = true;
		Object objectWithProperties = getMutablePropertySetInterface(propertyAccessor.getPropertySet());
		alwaysReturnNonNullExtensions = false;
		Object parameters[] = {value};
		try {
			propertyAccessor.getTheSetMethod().invoke(objectWithProperties, parameters);
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Method '" + propertyAccessor.getTheSetMethod().getName() + "' in '" + propertyAccessor.getPropertySet().getInterfaceClass().getName() + "' must be public.");
		} catch (InvocationTargetException e) {
			throw new MalformedPluginException("Method '" + propertyAccessor.getTheSetMethod().getName() + "' in '" + propertyAccessor.getPropertySet().getInterfaceClass().getName() + "' threw an exception that was not caught by the plug-in.");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			throw new RuntimeException("An unexpected error occurred in ExtendableObjectHelperImpl.setPropertyValue");
		}
	}

	private Object getPropertySetInterface(PropertySet propertySet) {
		if (!propertySet.isExtension()) {
			return this;
		} else {
			ExtensionObject extension = getExtension(propertySet);
			
			// If there is no extension then we use a default extension
			// obtained from the plugin object.  This extension object
			// was constructed using the default constructor.
			// This default extension is never passed outside this package
			// because plugins have no need for it and can cause chaos if
			// they alter it.  However it is useful for use inside the
			// package such as here.
			if (extension == null) {
				extension = propertySet.getDefaultPropertyValues();
			}
			
			return extension;
		}
	}
	
	private Object getMutablePropertySetInterface(PropertySet propertySet) {
		if (!propertySet.isExtension()) {
			return this;
		} else {
			// Because this is a mutable object, the following call will always
			// return a non-null extension.
			return getExtension(propertySet);
		}
	}
	
	public String getPropertyValueAsString(PropertyAccessor propertyAccessor) {
		Object objectWithProperties = getPropertySetInterface(propertyAccessor.getPropertySet());
		
		// Now we use introspection on the interface to find this property
		try {
			String theGetStringMethodName 
			= propertyAccessor.getTheGetMethod().getName()
			+ "String";
			
			try {
				Method theGetStringMethod = objectWithProperties.getClass().getDeclaredMethod(theGetStringMethodName, null);
				
				if (theGetStringMethod.getReturnType() != String.class) {
					throw new MalformedPluginException("Method '" + theGetStringMethodName + "' must return a String.");
				}
				
				Object result = theGetStringMethod.invoke(objectWithProperties, null);
				return (String)result;
			} catch (NoSuchMethodException e) {
				// No special method to get the value as a string, so use the method to
				// get in native type and then we must convert to a string.
				Object result = propertyAccessor.getTheGetMethod().invoke(objectWithProperties, null);
				if (result == null) {
					return "";
				} else {
					return result.toString();
				}
			}
			// Plugin error
		} catch (IllegalAccessException e) {
			// Plugin error
		} catch (InvocationTargetException e) {
			// Plugin error
		}
		
		return null;  // TODO: remove this
	}
	
	public void setIntegerPropertyValue(PropertyAccessor propertyAccessor, int value) {
		setPropertyValue(propertyAccessor, new Integer(value));
	}
	
    public void setLongPropertyValue(PropertyAccessor propertyAccessor, long value) {
        setPropertyValue(propertyAccessor, new Long(value));
    }
    
    public void setStringPropertyValue(PropertyAccessor propertyAccessor, String value) {
        setPropertyValue(propertyAccessor, value);
    }
    
	public void setCharacterPropertyValue(PropertyAccessor propertyAccessor, char value) {
		setPropertyValue(propertyAccessor, new Character(value));
	}
	
	public void setPropertyValueFromString(PropertyAccessor propertyAccessor, String value) {
		Object objectWithProperties = getMutablePropertySetInterface(propertyAccessor.getPropertySet());
		
		// Now we use introspection on the interface to find this property
		// bit of a kludge here
		try {
			String theSetStringMethodName
			= propertyAccessor.getTheSetMethod().getName() + "String";
			Class setFromStringParameterTypes[] = {String.class};
			
			try {
				Method theSetStringMethod = objectWithProperties.getClass().getDeclaredMethod(theSetStringMethodName, setFromStringParameterTypes);
				
				if (theSetStringMethod.getReturnType() != void.class) {
					throw new MalformedPluginException("Method '" + theSetStringMethodName + "' must return void type .");
				}
				
				Object parameters[] = {value};
				theSetStringMethod.invoke(objectWithProperties, parameters);
			} catch (NoSuchMethodException e) {
				// No special method to set the value as a string, so convert the string
				// to the native type ourselves and then use the normal set method.
				
				Method theSetMethod = propertyAccessor.getTheSetMethod();
				
				Class parameterTypes[] = theSetMethod.getParameterTypes();
				Class parameterType = parameterTypes[0];
				
				// Construct an object of this class from the string value.
				Class constructorParameterTypes[] = {String.class};
				Constructor contructorFromString;
				try {
					contructorFromString = parameterType.getConstructor(constructorParameterTypes);
				} catch (NoSuchMethodException e2) {
					throw new MalformedPluginException("No constructor to construct '" + parameterType.getName() + "' from a string.");
				}
				Object constructorParameters[] = {value};
				Object valueObject = contructorFromString.newInstance(constructorParameters);
				
				Object setMethodParameters[] = {valueObject};
				theSetMethod.invoke(objectWithProperties, setMethodParameters);
			}
			// Plugin error
		} catch (InstantiationException e) {
			// Plugin error
		} catch (IllegalAccessException e) {
			// Plugin error
		} catch (InvocationTargetException e) {
			// Plugin error
		}
	}
	
	/**
	 * This method is called when loading data from a datastore.
	 * Therefore the method can assume that there is no prior extension
	 * in this object for the given property set id.  The results are
	 * undetermined if the extension already exists.
	 */
	protected void importExtensionString(String propertySetId, String extensionString) {
		// This is a bit of a kludge.  We need to put the object
		// into editable mode.  This ensures that a request for an
		// extension will always return a non-null extension.
		// This is necessary when setting properties here, and also
		// necessary that the code that propagates property changes
		// through the adaptors get non-null extensions.
		alwaysReturnNonNullExtensions = true;
		
		PropertySet propertySetKey = PropertySet.getPropertySetCreatingIfNecessary(propertySetId, getExtendablePropertySetId());
		
		if (!propertySetKey.isExtensionClassKnown()) {
			// The plug-in that originally implemented this extension
			// is not installed.  We therefore do not know the class
			// that contains the properties.  We must not lose the
			// data in case the plug-in is installed later.
			// We therefore store the data in the map as a String.
			// If the plug-in is ever installed then the string can be
			// de-serialized to produce the correct extension object.
			extensions.put(propertySetKey, extensionString);
		} else {
			// Because the 'alwaysReturnNonNullExtensions' flag is set,
			// this method will always return  non-null extension.
			ExtensionObject extension = getExtension(propertySetKey);
			
			stringToExtension(extensionString, extension);
		}
		
		alwaysReturnNonNullExtensions = false;
	}
	
	
	protected static String extensionToString(IExtendableObject extension) {
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(extension.getClass());
		} catch (IntrospectionException e) {
			throw new MalformedPluginException("Property set extension caused introspection error");
		}
		
		StringBuffer buffer = new StringBuffer();
		PropertyDescriptor pd[] = beanInfo.getPropertyDescriptors();
		for (int j = 0; j < pd.length; j++) {
			String name = pd[j].getName();
			// Must have read and write method to be serialized.
			Method readMethod = pd[j].getReadMethod();
			Method writeMethod = pd[j].getWriteMethod();
			// TODO figure out a better way of finding our properties
			// than the following.
			if (readMethod != null
					&& writeMethod != null
					&& readMethod.getDeclaringClass() != IExtendableObject.class
					&& writeMethod.getDeclaringClass() != IExtendableObject.class
					&& readMethod.getDeclaringClass() != AccountExtension.class
					&& writeMethod.getDeclaringClass() != AccountExtension.class
					&& readMethod.getDeclaringClass() != AbstractEntryExtension.class
					&& writeMethod.getDeclaringClass() != AbstractEntryExtension.class) {
				Object value;
				try {
					value = readMethod.invoke(extension, null);
				} catch (IllegalAccessException e) {
					throw new MalformedPluginException("Property set extension caused introspection error");
					//                      throw new MalformedPluginException("Method 'getEntryExtensionClass' in '" + pluginBean.getClass().getName() + "' must be public.");
				} catch (InvocationTargetException e) {
					// Plugin error
					throw new RuntimeException("bad error");
				}
				buffer.append('<');
				buffer.append(name);
				buffer.append('>');
				buffer.append(value);
				buffer.append("</");
				buffer.append(name);
				buffer.append('>');
			}
		}
		return buffer.toString();
	}
	
	protected static void stringToExtension(String s, ExtensionObject extension) {
		ByteArrayInputStream bin = new ByteArrayInputStream(s.getBytes());
		
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			HandlerForExtensions handler = new HandlerForExtensions(extension);
			saxParser.parse(bin, handler); 
		} 
		catch (ParserConfigurationException e) {
			throw new RuntimeException("Serious XML parser configuration error");
		} 
		catch (SAXException se) { 
			throw new RuntimeException("SAX exception error");
		}
		catch (IOException ioe) { 
			throw new RuntimeException("IO internal exception error");
		}
		
		try { 
			bin.close(); 
		} 
		catch (IOException e) { 
			throw new RuntimeException("internal error");
		}
	}
	
	private static class HandlerForExtensions extends DefaultHandler {
		
		ExtensionObject extension;
		
		BeanInfo beanInfo;
		
		Method writeMethod = null;
		
		HandlerForExtensions(ExtensionObject extension) {
			this.extension = extension;
			
			try {
				beanInfo = Introspector.getBeanInfo(extension.getClass());
			} catch (IntrospectionException e) {
				throw new MalformedPluginException("Property set extension caused introspection error");
			}
		}
		
		/**
		 * Receive notification of the start of an element.
		 *
		 * <p>See if there is a setter for this element name.  If there is
		 * then set the setter.  Otherwise set the setter to null to indicate
		 * that any character data should be ignored.
		 * </p>
		 * @param name The element type name.
		 * @param attributes The specified or defaulted attributes.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#startElement
		 */
		public void startElement(String uri, String localName,
				String qName, Attributes attributes)
		throws SAXException {
			String propertyName = qName;
			
			PropertyDescriptor pd[] = beanInfo.getPropertyDescriptors();
			for (int j = 0; j < pd.length; j++) {
				String name = pd[j].getName();
				if (name.equals(propertyName)) {
					// Must have write method in the extension class.
					Method writeMethod = pd[j].getWriteMethod();
					// TODO: clean up
					if (writeMethod != null
							&& writeMethod.getDeclaringClass() != IExtendableObject.class
							&& writeMethod.getDeclaringClass() != AccountExtension.class
							&& writeMethod.getDeclaringClass() != AbstractEntryExtension.class) {
						this.writeMethod = writeMethod;
					}
					break;
				}
			}
		}
		
		
		/**
		 * Receive notification of the end of an element.
		 *
		 * <p>Set the setter back to null.
		 * </p>
		 * @param name The element type name.
		 * @param attributes The specified or defaulted attributes.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#endElement
		 */
		public void endElement(String uri, String localName, String qName)
		throws SAXException {
			writeMethod = null;
		}
		
		
		/**
		 * Receive notification of character data inside an element.
		 *
		 * <p>If a setter method is set then the character data is passed
		 * to the setter.  Otherwise the character data is dropped.
		 * </p>
		 * @param ch The characters.
		 * @param start The start position in the character array.
		 * @param length The number of characters to use from the
		 *               character array.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#characters
		 */
		public void characters(char ch[], int start, int length)
		throws SAXException {
			if (writeMethod != null) {
				Class type = writeMethod.getParameterTypes()[0];
				Object value = null;
				
				// TODO: change this.  Find a constructor from string.
				if (type.equals(int.class)) {
					String s = new String(ch, start, length);
					value = new Integer(s);
				} else if (type.equals(String.class)) {
					value = new String(ch, start, length);
				} else if (type.equals(char.class)) {
					value = new Character(ch[start]);
				} else {
					throw new RuntimeException("unsupported type");
				}
				
				try {
					writeMethod.invoke(extension, new Object[] { value });
				} catch (IllegalAccessException e) {
					throw new MalformedPluginException("Property set extension caused introspection error");
					//                      throw new MalformedPluginException("Method 'getEntryExtensionClass' in '" + pluginBean.getClass().getName() + "' must be public.");
				} catch (InvocationTargetException e) {
					// Plugin error
					throw new RuntimeException("bad error");
				}
			}
		}
	}
	
}
