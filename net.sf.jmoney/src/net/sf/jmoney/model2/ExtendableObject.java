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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is the base class for all objects that may have extension
 * property sets added by plug-ins.  The framework supports the
 * following objects that may be extended:
 * <UL>
 * <LI>Session</LI>
 * <LI>Commodity</LI>
 * <LI>Account</LI>
 * <LI>Transaction</LI>
 * <LI>Entry</LI>
 * </UL>
 * <P>
 * Plug-ins are also able to create new classes of extendable
 * objects by deriving classes from this class.
 * <P>
 * This class contains abstract methods for which an implementation
 * must be provided.
 * 
 * @author  Nigel Westbury
 */
public abstract class ExtendableObject {
	
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
	
	protected abstract String getExtendablePropertySetId();

	/**
	 * @param extensions A map from PropertySet objects representing
	 * 			extension property sets to the parameter lists from
	 * 			which the extension property set objects can be
	 * 			constructed.
	 */
	protected ExtendableObject(IObjectKey objectKey, Map extensionParameters) {
		this.objectKey = objectKey;
		
		if (extensionParameters != null) {
			for (Iterator iter = extensionParameters.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry entry = (Map.Entry)iter.next();
				PropertySet propertySet = (PropertySet)entry.getKey();
				Object[] constructorParameters = (Object[])entry.getValue();
				
				ExtensionObject extensionObject = (ExtensionObject)propertySet.constructImplementationObject(constructorParameters);
				
				extensionObject.setBaseObject(this);
				extensionObject.setPropertySet(propertySet);
				extensions.put(propertySet, extensionObject);
			}
		}
	}
	
	/**
	 * @return The key that fetches this object.
	 */
	public IObjectKey getObjectKey() {
		return objectKey;
	}
	
	/**
	 * @return The session containing this object
	 */
	public Session getSession() {
		// The key must contain the session and so there is no reason
		// for the extendable objects to also contain a session field.
		// Get the session from the key.
		return objectKey.getSession();
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
		if (object instanceof ExtendableObject) {
			ExtendableObject extendableObject = (ExtendableObject)object;
			return getObjectKey().equals(extendableObject.getObjectKey());
		} else {
			return false;
		}
	}
	
	// Should allow default package access and protected access
	// but not public access.  Unfortunately this cannot be done
	// so for time being allow public access.
	public void processPropertyChange(PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
		if (oldValue == newValue ||
				(oldValue != null && oldValue.equals(newValue)))
					return;

		// Update the database.
		IObjectKey key = getObjectKey();
		PropertySet propertySet = PropertySet.getPropertySet(this.getClass());
		key.updateProperties(propertySet, propertyAccessor, oldValue, newValue);

		// Nofify the change manager.
		getSession().getChangeManager().processPropertyUpdate(this, propertyAccessor, oldValue, newValue);

		// Fire an event for this change.
		getSession().objectChanged(
        		this,
        		propertyAccessor,
				oldValue,
				newValue);
	}
	
	// Should allow default package access and protected access
	// but not public access.  Unfortunately this cannot be done
	// so for time being allow public access.
	public void processObjectAddition(PropertyAccessor owningListProperty, final ExtendableObject newObject) {
		getSession().getChangeManager().processObjectCreation(this, owningListProperty, newObject);

		// Fire the event.
		getSession().fireEvent(
            	new ISessionChangeFirer() {
            		public void fire(SessionChangeListener listener) {
            			listener.objectAdded(newObject);
            		}
           		});
	}
	
	// Should allow default package access and protected access
	// but not public access.  Unfortunately this cannot be done
	// so for time being allow public access.
	public void processObjectDeletion(PropertyAccessor owningListProperty, final ExtendableObject oldObject) {
		// Notify the change manager.
		getSession().getChangeManager().processObjectDeletion(this, owningListProperty, oldObject);
		
		// Fire the event.
		getSession().fireEvent(
            	new ISessionChangeFirer() {
            		public void fire(SessionChangeListener listener) {
            			listener.objectDeleted(oldObject);
            		}
           		});
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
	
	/**
	 * Get the extension that implements the properties needed by
	 * a given plug-in.
	 */
	public ExtensionObject getExtension(PropertySet propertySet) {
		Object extensionObject = extensions.get(propertySet);
		
		ExtensionObject extension;
		
		if (extensionObject == null) {
			// Extension does not exist.
			
			if (alwaysReturnNonNullExtensions) {
				// Create a new extension and look to the original
				// for default values.
				
				try {
					extension = (ExtensionObject)
					propertySet.getImplementationClass().newInstance();
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
				// propagators.  We could get infinite recursion if the propagation
				// got back to this extension and the extension was not set.
				extension.setBaseObject(this);
				extension.setPropertySet(propertySet);
				extensions.put(propertySet, extension);
			} else {
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
					propertySet.getImplementationClass().newInstance();
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
				// propagators.  We could get infinite recursion if the propagation
				// got back to this extension and the extension was not set.
				
				// TODO: We really should not be propagating properties now.
				// If there are changes, the datastore was inconsistent,
				// and changing other properties now could cause confusion.
				extension.setBaseObject(this);
				extension.setPropertySet(propertySet);
				extensions.put(propertySet, extension);
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
	
	public int getIntegerPropertyValue(PropertyAccessor propertyAccessor) {
		return ((Integer)getPropertyValue(propertyAccessor)).intValue();
	}
	
    public long getLongPropertyValue(PropertyAccessor propertyAccessor) {
        return ((Long)getPropertyValue(propertyAccessor)).longValue();
    }
    
    public String getStringPropertyValue(PropertyAccessor propertyAccessor) {
        return (String)getPropertyValue(propertyAccessor);
    }
    
    public char getCharacterPropertyValue(PropertyAccessor propertyAccessor) {
        return ((Character)getPropertyValue(propertyAccessor)).charValue();
    }
    
    public boolean getBooleanPropertyValue(PropertyAccessor propertyAccessor) {
        return ((Boolean)getPropertyValue(propertyAccessor)).booleanValue();
    }
    
    /**
     * Returns the value of a given property.
     * <P>
     * The property may be any property in the passed object,
     * including properties that are stored in extension objects.
     * The property may be defined in the actual class or
     * any super classes which the class extends.  The property
     * may also be a property in any extension class which extends
     * the class of this object or which extends any super class
     * of the class of this object.
     * <P>
     * If the property is in an extension and that extension does
     * not exist in this object then the default value of the
     * property is returned.
     */
	public Object getPropertyValue(PropertyAccessor propertyAccessor) {
		Object objectWithProperties = getPropertySetInterface(propertyAccessor.getPropertySet());
		
		// If there is no extension then we use a default extension
		// obtained from the propertySet object.  This extension object
		// was constructed using the default constructor.
		// This default extension is never passed outside this package
		// because plugins have no need for it and can cause chaos if
		// they alter it.  However it is useful for use inside the
		// package such as here.
		if (objectWithProperties == null) {
			objectWithProperties = propertyAccessor.getPropertySet().getDefaultPropertyValues();
		}
		
		return  propertyAccessor.invokeGetMethod(objectWithProperties);
	}
	
	/**
	 * Obtain an iterator that iterates over the values of a
	 * list property.
	 * 
	 * @param propertyAccessor The property accessor for the property
	 * 			whose values are to be iterated.  The property
	 * 			must be a list property (and not a scalar property).
	 */
	public Iterator getPropertyIterator(PropertyAccessor propertyAccessor) {
		Object objectWithProperties = getPropertySetInterface(propertyAccessor.getPropertySet());
		
		// If no extension exists then return the empty iterator.
		if (objectWithProperties == null) {
			return new EmptyIterator();
		}
		
		return (Iterator)propertyAccessor.invokeGetMethod(objectWithProperties);
	}
	
	public void setPropertyValue(PropertyAccessor propertyAccessor, Object value) {
		// The problem here is that the XML parser sets the properties directly in
		// the object, without going through a mutable object.
		// We cannot therefore rely on this object being mutable, so temporarily
		// set this flag.
		// TODO: review this method.
		alwaysReturnNonNullExtensions = true;
		Object objectWithProperties = getMutablePropertySetInterface(propertyAccessor.getPropertySet());
		alwaysReturnNonNullExtensions = false;
		
		propertyAccessor.invokeSetMethod(objectWithProperties, value);
	}

	/**
	 * Given a property set, return the actual object against
	 * which the various methods to get and set properties may
	 * be invoked.
	 * <P>
	 * If the property set is an extendable property set then
	 * the methods must be invoked against the extendable object
	 * itself.  If the property set is an extension property set
	 * then we must find the appropriate extension object.
	 * 
	 * @param propertySet
	 * @return The object (extendable or extension object)
	 * 			against which the methods for the given property
	 * 			must be invoked, or null if an extension property
	 * 			and no extension exists.
	 */
	private Object getPropertySetInterface(PropertySet propertySet) {
		if (!propertySet.isExtension()) {
			return this;
		} else {
			ExtensionObject extension = getExtension(propertySet);
			
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

	/**
	 * @param owningListProperty property accessor for the list property into which
	 * 			the newly created object is to be placed
	 * @param actualPropertySet if the list property is typed to contain
	 * 			properties that are derivable then <code>actualPropertySet</code>
	 * 			must contain the non-derivable property set for the object to
	 * 			be created.  f the list property is typed to contain
	 * 			properties that are not derivable then <code>actualPropertySet</code>
	 * 			is ignored and objects are always created as appropriate for
	 * 			the owning list.
	 */
	public ExtendableObject createObject(PropertyAccessor owningListProperty, PropertySet actualPropertySet) {
		// TODO: ensure the following always returns non-null.
		Object objectWithProperties = getMutablePropertySetInterface(owningListProperty.getPropertySet());
		
		if (owningListProperty.getValuePropertySet().isDerivable()) {
			return owningListProperty.invokeCreateMethod(objectWithProperties, actualPropertySet);
		} else {
			return owningListProperty.invokeCreateMethod(objectWithProperties);
		}
	}
	
	/**
	 * @param owningListProperty
	 * @param object
	 */
	public boolean deleteObject(PropertyAccessor owningListProperty, ExtendableObject object) {
		Object objectWithProperties = getMutablePropertySetInterface(owningListProperty.getPropertySet());
		
		// If objectWithProperties is null, the list is in an extension and no properties
		// have been set in the extension.  That means the list is empty so return false
		// to indicate the object to be deleted cannot be found.
		if (objectWithProperties == null) {
			return false;
		}
		
		return owningListProperty.invokeDeleteMethod(objectWithProperties, object);
	}
	
	/**
	 * Return a list of extension that exist for this object.
	 * This is the list of extensions that have actually been
	 * created for this object, not the list of valid extensions
	 * for this object type.  If no property values have yet been set
	 * in an extension that the extension will not have been created
	 * and will thus not be returned by this method.
	 * <P>
	 * It is more efficient to use this method than to loop through
	 * all the possible extension property sets and see which ones exist
	 * in this object.
	 *
	 * @return an Iterator that returns elements of type
	 * 		<code>Map.Entry</code>.  Each Map.Entry contains a
	 * 		key of type PropertySet and a value of
	 * 		ExtensionObject.
	 */
	public Iterator getExtensionIterator() {
		return extensions.entrySet().iterator();
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
		// through the propagators get non-null extensions.
		alwaysReturnNonNullExtensions = true;
		
		PropertySet propertySet = PropertySet.getPropertySetCreatingIfNecessary(propertySetId, getExtendablePropertySetId());
		
		if (!propertySet.isExtensionClassKnown()) {
			// The plug-in that originally implemented this extension
			// is not installed.  We therefore do not know the class
			// that contains the properties.  We must not lose the
			// data in case the plug-in is installed later.
			// We therefore store the data in the map as a String.
			// If the plug-in is ever installed then the string can be
			// de-serialized to produce the correct extension object.
			extensions.put(propertySet, extensionString);
		} else {
			// Because the 'alwaysReturnNonNullExtensions' flag is set,
			// this method will always return  non-null extension.
			ExtensionObject extension = getExtension(propertySet);
			
			stringToExtension(extensionString, extension);
		}
		
		alwaysReturnNonNullExtensions = false;
	}
	
	
	protected static String extensionToString(ExtendableObject extension) {
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
					&& readMethod.getDeclaringClass() != ExtendableObject.class
					&& writeMethod.getDeclaringClass() != ExtendableObject.class
					&& readMethod.getDeclaringClass() != AccountExtension.class
					&& writeMethod.getDeclaringClass() != AccountExtension.class
					&& readMethod.getDeclaringClass() != EntryExtension.class
					&& writeMethod.getDeclaringClass() != EntryExtension.class) {
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
							&& writeMethod.getDeclaringClass() != ExtendableObject.class
							&& writeMethod.getDeclaringClass() != AccountExtension.class
							&& writeMethod.getDeclaringClass() != EntryExtension.class) {
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
