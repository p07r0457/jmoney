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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * This class contains information about an extension property.
 *
 * @author  Nigel
 */
public class PropertyAccessorImpl implements PropertyAccessor {
    
    private PropertySet propertySet;
    
    private String localName;    
    
    private String shortDescription;
    
    // Applies only if scalar property
    private int weight;    
    
    // Applies only if scalar property
    private int minimumWidth;    
    
    // Applies only if scalar property
    private boolean sortable;
    
    // Applies only if scalar property
    private IPropertyControlFactory propertyControlFactory;
    
    /**
     * The class of the property.  It the property is a list
     * property then the class of the elements in the list.
     */
    private Class propertyClass;
    
    // If a list property, this is the iterator getter.
    private Method theGetMethod;
    
    // Applies only if scalar property
    private Method theSetMethod;
    
    // Applies only if list property
    private Method theCreateMethod;
    
    // Applies only if list property
    private Method theDeleteMethod;
    
    // Applies only if scalar property of type ExtendableObject
    private Field theObjectKeyField;
    
	/**
	 * true if property is a list property, i.e. can contain
	 * multiple values.  false otherwise.
	 */
	private boolean isList;
	
	/**
	 * Index into the list of parameters passed to the constructor.
	 * Zero indicates that this property is passed as the first
	 * parameter to the constructor.
	 * 
	 */
	private int indexIntoConstructorParameters = -1;
	
	/**
	 * Index into the array of scalar properties.
	 */
	private int indexIntoScalarProperties = -1;

	private IPropertyDependency dependency;
	
    public PropertyAccessorImpl(PropertySet propertySet, String localName, String shortDescription, int weight, int minimumWidth, IPropertyControlFactory propertyControlFactory, IPropertyDependency propertyDependency) {
    	this.propertySet = propertySet;
        this.localName = localName;
        this.shortDescription = shortDescription;
        this.weight = weight;
        this.minimumWidth = minimumWidth;
        this.sortable = true;
        this.propertyControlFactory = propertyControlFactory;
        this.dependency = propertyDependency;
        
        isList = false;
       
        Class implementationClass = propertySet.getImplementationClass();
        
        // Use introspection on the interface to find the getter method.
        // Following the Java beans pattern, we allow the getter for a
        // boolean property to have a prefix of either "get" or "is".
        try {
        	theGetMethod = findMethod("get", localName, null);
        } catch (MalformedPluginException e) {
            try {
            	theGetMethod = findMethod("is", localName, null);
                if (theGetMethod.getReturnType() != boolean.class) {
                    throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + implementationClass.getName() + "' must return boolean.");
                }
            } catch (MalformedPluginException e2) {
        		String propertyNamePart =
        			localName.toUpperCase().substring(0, 1)
					  + localName.substring(1, localName.length());
    			throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a method with a signature of get" + propertyNamePart + "() or, if a boolean property, a signature of is" + propertyNamePart + "().");
            }
        }
        
        if (theGetMethod.getReturnType() == void.class) {
            throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + implementationClass.getName() + "' must not return void.");
        }
        
        this.propertyClass = theGetMethod.getReturnType();
        
        // Use introspection on the implementation class to find the setter method.
        Class parameterTypes[] = {propertyClass};
		theSetMethod = findMethod("set", localName, parameterTypes);
        
        if (theSetMethod.getReturnType() != void.class) {
            throw new MalformedPluginException("Method '" + theSetMethod.getName() + "' in '" + implementationClass.getName() + "' must return void type .");
        }
        
        // If the property value is an extendable object, use introspection on
		// the implementation class to find the field containing
		// the object key for the object referenced by this property.
        if (ExtendableObject.class.isAssignableFrom(propertyClass)) { 		
        	String fieldName = localName + "Key";
        	
        	Class classToTry = propertySet.getImplementationClass();
        	do {
        		try {
        			theObjectKeyField = classToTry.getDeclaredField(fieldName);
        			break;
        		} catch (NoSuchFieldException e) {
        			classToTry = classToTry.getSuperclass();
        		}
        	} while (classToTry != null);
        	
        	if (theObjectKeyField == null) {
        		throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a field called " + fieldName + ".");
        	}
        	
        	if (!IObjectKey.class.isAssignableFrom(theObjectKeyField.getType())) {
        		throw new MalformedPluginException("Field '" + fieldName + "' in '" + implementationClass.getName() + "' must reference an object type that implements IObjectKey.");
        	}

			// (1 is public,  2 is private, 4 is protected, 1,2 & 4 bits off is default).
			if ((theObjectKeyField.getModifiers() & 5) == 0) {
				throw new MalformedPluginException("Field '" + fieldName + "' in '" + implementationClass.getName() + "' must be protected (or public if you insist).");
			}
        }
    }
    
    /**
     * Create a property accessor for a list property.
     * 
	 * @param set
	 * @param name
	 * @param listItemClass
	 * @param shortDescription
	 * @param propertyDependency
	 */
	public PropertyAccessorImpl(PropertySet propertySet, String localName, String shortDescription, Class listItemClass, IPropertyDependency propertyDependency) {
    	this.propertySet = propertySet;
        this.localName = localName;
        this.propertyClass = listItemClass;
        this.shortDescription = shortDescription;
        
        isList = true;

		// Use introspection on the interface to find the getXxxIterator method.
		theGetMethod = findMethod("get", localName + "Iterator", null);
		
		if (theGetMethod.getReturnType() != Iterator.class) {
			throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + propertySet.getImplementationClass().getName() + "' must return an Iterator type.");
		}
	}

	/**
	 * Complete initialization of this object by finding the methods
	 * required by the JMoney framework.  This cannot be done in the
	 * constructor because finding the 'create' methods requires accessing
	 * other PropertyAccessor objects, and so must be done in a second pass
	 * after all PropertyAccessor objects and all PropertySet objects have
	 * been constructed.
	 * <P>
	 * Note that the 'get' methods must be processed in pass 1 (in the constructor).
	 * This is because the class of a property is determined by looking at the
	 * return type of the 'get' method, and the class of other properties is
	 * required by this method.
	 */
	public void initMethods() {
        
		if (isList) {
			// For every list, there must be an create<propertyName>
			// method.  Find this method.
			
			Class parameters[];
			
			if (getValuePropertySet().isDerivable()) {
				// Class is derivable, which means we don't know the property
				// set at compile time, so the method takes the property set
				// as a parameter.
				parameters = new Class [] {
						PropertySet.class, 
				};
			} else {
				parameters = new Class[0];
			}	
			
			theCreateMethod = findMethod("create", localName, parameters);
			
			if (theCreateMethod.getReturnType() != this.getValueClass()) {
				throw new MalformedPluginException("Method '" + theCreateMethod.getName() + "' in '" + propertySet.getImplementationClass().getName() + "' must return an object of type " + this.getValueClass() + "."); 
			}

	        Class deleteParameterTypes[] = {propertyClass};
			theDeleteMethod = findMethod("delete", localName, deleteParameterTypes);
	        
	        if (theDeleteMethod.getReturnType() != boolean.class) {
	            throw new MalformedPluginException("Method '" + theDeleteMethod.getName() + "' in '" + propertySet.getImplementationClass().getName() + "' must return boolean type .");
	        }

		}
	}

	public Method findMethod(String prefix, String propertyName, Class [] parameters) {
		String methodName = prefix
			+ propertyName.toUpperCase().charAt(0)
			+ propertyName.substring(1, propertyName.length());
		
		try {
			return getDeclaredMethodRecursively(propertySet.getImplementationClass(), methodName, parameters);
		} catch (NoSuchMethodException e) {
			String parameterText = "";
			if (parameters != null) {
				for (int paramIndex = 0; paramIndex < parameters.length; paramIndex++) {
					if (paramIndex > 0) {
						parameterText = parameterText + ", ";
					}
					String className = parameters[paramIndex].getName();
					if (parameters[paramIndex].isArray()) {
						// The returned class name seems to be a mess when the class is an array,
						// so we tidy it up.
						parameterText = parameterText + className.substring(2, className.length()-1) + "[]";
					} else {
						parameterText = parameterText + className;
					}
				}
			}
			throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a method with a signature of " + methodName + "(" + parameterText + ").");
		}
	}

	/**
	 * Gets a method from an interface.  
	 * Whereas Class.getDeclaredMethod finds a method from an
	 * interface, it will not find the method if the method is
	 * defined in an interface which the given interface extends.
	 * This method will find the method if any of the interfaces
	 * extended by this interface define the method. 
	 */
	private Method getDeclaredMethodRecursively(Class implementationClass, String methodName, Class[] arguments)
		throws NoSuchMethodException {
		Class classToTry = implementationClass;
		do {
			try {
				return classToTry.getDeclaredMethod(methodName, arguments);
			} catch (NoSuchMethodException e) {
				classToTry = classToTry.getSuperclass();
			}
		} while (classToTry != null);
		
		throw new NoSuchMethodException();
	}
	
	public PropertySet getPropertySet() {
        return propertySet;
    }
	
	public PropertySet getExtendablePropertySet() {
		if (propertySet.isExtension()) {
			return propertySet.getExtendablePropertySet();
		} else {
			return propertySet;
		}
    }
    
    public Object invokeGetMethod(Object invocationTarget) {
		try {
			return theGetMethod.invoke(invocationTarget, null);
		} catch (InvocationTargetException e) {
			// TODO Process this properly
			e.printStackTrace();
			throw new RuntimeException("Plugin error");
		} catch (Exception e) {
			// IllegalAccessException and IllegalArgumentException exceptions should
			// not be possible here because the method was checked
			// for correct access rights and parameters during initialization.
			// Therefore throw a runtime exception.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
    }
    
    public void invokeSetMethod(Object invocationTarget, Object value) {
		try {
			Object parameters[] = new Object[] { value };
			theSetMethod.invoke(invocationTarget, parameters);
		} catch (InvocationTargetException e) {
			// TODO Process this properly
			e.getCause().printStackTrace();
			throw new RuntimeException("Plugin error");
		} catch (Exception e) {
			// IllegalAccessException and IllegalArgumentException exceptions should
			// not be possible here because the method was checked
			// for correct access rights and parameters during initialization.
			// Therefore throw a runtime exception.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
    }

    public IObjectKey invokeObjectKeyField(ExtendableObject object) {
    	return (IObjectKey)object.getProtectedFieldValue(theObjectKeyField);
    }

    /**
     * This version of the method is valid only for list objects where
     * the list is typed to a property set that is not derivable.
     */
    public ExtendableObject invokeCreateMethod(Object invocationTarget) {
		try {
			Object parameters[] = new Object[] { };
			return (ExtendableObject)theCreateMethod.invoke(invocationTarget, parameters);
		} catch (InvocationTargetException e) {
			// TODO Process this properly
			e.printStackTrace();
			throw new RuntimeException("Plugin error");
		} catch (Exception e) {
			// IllegalAccessException and IllegalArgumentException exceptions should
			// not be possible here because the method was checked
			// for correct access rights and parameters during initialization.
			// Therefore throw a runtime exception.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
    }
    
    /**
     * This version of the method is valid only for list objects where
     * the list is typed to a property set that is derivable.
     * The property set of the object to be created must be passed.
     */
    public ExtendableObject invokeCreateMethod(Object invocationTarget, PropertySet actualPropertySet) {
		try {
			Object parameters[] = new Object[] { actualPropertySet };
			return (ExtendableObject)theCreateMethod.invoke(invocationTarget, parameters);
		} catch (InvocationTargetException e) {
			// TODO Process this properly
			e.printStackTrace();
			throw new RuntimeException("Plugin error");
		} catch (Exception e) {
			// IllegalAccessException and IllegalArgumentException exceptions should
			// not be possible here because the method was checked
			// for correct access rights and parameters during initialization.
			// Therefore throw a runtime exception.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
    }
    
    public boolean invokeDeleteMethod(Object invocationTarget, ExtendableObject object) {
		try {
			Object parameters[] = new Object[] { object };
			Boolean result = (Boolean)theDeleteMethod.invoke(invocationTarget, parameters);
			return result.booleanValue();
		} catch (InvocationTargetException e) {
			// TODO Process this properly
			e.printStackTrace();
			throw new RuntimeException("Plugin error");
		} catch (Exception e) {
			// IllegalAccessException and IllegalArgumentException exceptions should
			// not be possible here because the method was checked
			// for correct access rights and parameters during initialization.
			// Therefore throw a runtime exception.
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
    }

    /**
     * Returns the class for the values of this property.
     * This is the class that is returned by the getter method
     * or, if this property is a list, the class of objects
     * returned by the list iterator.
     */
    public Class getValueClass() {
        return propertyClass;
    }
    
    /**
     * Returns the PropertySet for the values of this property.
     * This property must contain a value or values that are
     * extendable objects. 
     */
    public PropertySet getValuePropertySet() {
		for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
			PropertySet propertySet = (PropertySet)iter.next();
			if (!propertySet.isExtension()) {
				if (propertySet.getImplementationClass() == propertyClass) {
					return propertySet;
				}
			}
		}
		
        throw new RuntimeException("No property set found for extendable class object" + propertyClass.getName() + ".");
    }
    
    /**
     * Return a name for this property.
     *
     * This name is used by the framework for persisting information about the property
     * in configuration files etc.  For example, if the user sorts a column based on a
     * property then that information can be stored in a configuration file so that the
     * data is sorted on the column the next time the user loads the view.
     */
    public String getName() {
        // We must uniquify the name, so prepend the property set id.
    	// TODO: this does not uniquify the name because a property may
    	// exist with the same name as both, say, an account property
    	// and an entry property.  We should probably add the base
    	// class name here too.
        return propertySet.toString() + "." + localName;
    }
    
    /**
     * The local name of the property is just the last part of the name, after the last
     * dot.  This will be unique within the property set but may not be unique
     * across all plug-ins.
     */
    public String getLocalName() {
        return localName;
    }
    
    public String getShortDescription() {
        return shortDescription;
    }
    
    public int getWeight() {
        return weight;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public boolean isSortable() {
        return sortable;
    }
    
    public boolean isList() {
        return isList;
    }
    
    public boolean isScalar() {
        return !isList;
    }
    
    /**
     * Indicates whether the property may be edited by the user.
     */
    public boolean isEditable() {
        return (propertyControlFactory != null && propertyControlFactory.isEditable());
    }
    
    /**
     * If the bean defines a custom comparator for this property then fetch it.
     * Otherwise return null to indicate that the default comparator for the type
     * should be used.
     */
    public Comparator getCustomComparator() {
        return null;
    }

	public IPropertyControl createPropertyControl(Composite parent) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// We call into that factory to create an edit control.
		return propertyControlFactory.createPropertyControl(parent, this);
	}


	public CellEditor createCellEditor(Table table) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// We call into that factory to create a cell editor.
		return propertyControlFactory.createCellEditor(table);
	}

	public Object getValueTypedForCellEditor(ExtendableObject extendableObject) {
	    if (extendableObject == null) return new Integer (0); // I don't know why -- Faucheux
		return propertyControlFactory.getValueTypedForCellEditor(extendableObject, this);
	}

	public void setValueTypedForCellEditor(ExtendableObject extendableObject, Object value) {
		propertyControlFactory.setValueTypedForCellEditor(extendableObject, this, value);
	}

	public String formatValueForMessage(ExtendableObject object) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// This factory can also format values for us.
		// We call into that factory to obtain the property value
		// and format it.

		// If null or the empty string is returned to us, 
		// change to "empty".
		String formattedValue = propertyControlFactory.formatValueForMessage(object, this);
		return (formattedValue == null || formattedValue.length() == 0)
			? "empty" : formattedValue;
	}

	public String formatValueForTable(ExtendableObject object) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// This factory can also format values for us.
		// We call into that factory to obtain the property value
		// and format it.
		
		// If null is returned to us, change to the empty string.
		String formattedValue = propertyControlFactory.formatValueForTable(object, this);
		return (formattedValue == null)
			? "" : formattedValue;
	}

	/**
	 * @return the index into the constructor parameters, where
	 * 		an index of zero indicates that the property is the
	 * 		first parameter to the constructor.  An index of -1
	 * 		indicates that the property is not passed to the
	 * 		constructor (the property value is redundant and the
	 * 		object can be fully re-constructed from the other
	 * 		properties).
	 */
	public int getIndexIntoConstructorParameters() {
		return indexIntoConstructorParameters;
	}

	/**
	 * It is often useful to have an array of property values
	 * of an extendable object.  This array contains all scalar
	 * properties in the extendable object, including extension
	 * properties and properties from any base property sets.
	 * <P>
	 * In these arrays, the properties (including extension properties)
	 * from the base property sets are put first in the array.
	 * This means a given property will always be at the same index
	 * in the array regardless of the actual derived property set.
	 * <P>
	 * This index is guaranteed to match the order in which
	 * properties are returned by the PropertySet.getPropertyIterator3().
	 * i.e. if this method returns n then in every case where the
	 * iterator returned by getPropertyIterator3 returns this property,
	 * this property will be returned as the (n+1)'th element in the iterator.
	 * 
	 * @return the index of this property into the arrays of scalar properties.
	 */
	public int getIndexIntoScalarProperties() {
		return indexIntoScalarProperties;
	}
	
	public void setIndexIntoConstructorParameters(int indexIntoConstructorParameters) {
		this.indexIntoConstructorParameters = indexIntoConstructorParameters;
	}

	public void setIndexIntoScalarProperties(int indexIntoScalarProperties) {
		this.indexIntoScalarProperties = indexIntoScalarProperties;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.PropertyAccessor#getTrueValueDependency()
	 */
	// TODO: Should this be a member of PropertyAssessor, or should it
	// be outside?????
	public IPropertyDependency getTrueValueDependency() {
		if (propertyClass != boolean.class) {
			throw new MalformedPluginException("getTrueValueDependency called on property that is not a boolean.");
		}
		
		return new IPropertyDependency() {
			public boolean isSelected(ExtendableObject object) {
				return object.getBooleanPropertyValue(PropertyAccessorImpl.this);
			}
		};
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.PropertyAccessor#getFalseValueDependency()
	 */
	public IPropertyDependency getFalseValueDependency() {
		if (propertyClass != boolean.class) {
			throw new MalformedPluginException("getTrueValueDependency called on property that is not a boolean.");
		}
		
		return new IPropertyDependency() {
			public boolean isSelected(ExtendableObject object) {
				return !object.getBooleanPropertyValue(PropertyAccessorImpl.this);
			}
		};
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.PropertyAccessor#getDependency()
	 */
	public IPropertyDependency getDependency() {
		return dependency;
	}
}
