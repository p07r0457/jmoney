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

import java.lang.reflect.*;
import java.beans.*;   // for PropertyChangeSupport and PropertyChangeListener
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JComponent;

import org.eclipse.swt.widgets.Composite;

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
    private double width;    
    
    // Applies only if scalar property
    private boolean sortable;
    
    // Applies only if scalar property
    private IPropertyControlFactory propertyControlFactory;
    
    // Applies only if scalar property
    private Class editorBeanClass;
    
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
	private int indexIntoScalarProperties = -1;
	
    public PropertyAccessorImpl(PropertySet propertySet, String localName, String shortDescription, double width, IPropertyControlFactory propertyControlFactory, Class editorBeanClass, IPropertyDependency propertyDependency) {
    	this.propertySet = propertySet;
        this.localName = localName;
        this.shortDescription = shortDescription;
        this.width = width;
        this.sortable = true;
        this.propertyControlFactory = propertyControlFactory;
        this.editorBeanClass = editorBeanClass;
        
        isList = false;
       
        // Use introspection on the interface to find the getter method.
        Class interfaceClass = propertySet.getInterfaceClass();
        
        String theGetMethodName	= "get"
        	+ localName.toUpperCase().charAt(0)
			+ localName.substring(1, localName.length());
        
        try {
            this.theGetMethod = interfaceClass.getDeclaredMethod(theGetMethodName, null);
        } catch (NoSuchMethodException e) {
            throw new MalformedPluginException("Method '" + theGetMethodName + "' in '" + interfaceClass.getName() + "' was not found.");
        }
        
        if (theGetMethod.getReturnType() == void.class) {
            throw new MalformedPluginException("Method '" + theGetMethodName + "' in '" + interfaceClass.getName() + "' must not return void.");
        }
        
        this.propertyClass = theGetMethod.getReturnType();
        
        // Use introspection on the implementation class to find the setter method.
        Class implementationClass = propertySet.getImplementationClass();
        
        String theSetMethodName	= "set"
			+ localName.toUpperCase().charAt(0)
			+ localName.substring(1, localName.length());
        Class parameterTypes[] = {propertyClass};
        
        try {
            this.theSetMethod = implementationClass.getDeclaredMethod(theSetMethodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new MalformedPluginException("Method '" + theSetMethodName + "' in '" + implementationClass.getName() + "' was not found.");
        }
        
        if (theSetMethod.getReturnType() != void.class) {
            throw new MalformedPluginException("Method '" + theSetMethodName + "' in '" + implementationClass.getName() + "' must return void type .");
        }

//        propertySupport = new PropertyChangeSupport(this);
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
		Class interfaceClass = propertySet.getInterfaceClass();
		
		String theGetMethodName	= "get"
			+ localName.toUpperCase().charAt(0)
			+ localName.substring(1, localName.length())
			+ "Iterator";
		
		try {
			this.theGetMethod = getDeclaredMethodRecursively(interfaceClass, theGetMethodName, null);
		} catch (NoSuchMethodException e) {
			throw new MalformedPluginException("Method '" + theGetMethodName + "' in '" + interfaceClass.getName() + "' was not found.");
		}
		
		if (theGetMethod.getReturnType() != Iterator.class) {
			throw new MalformedPluginException("Method '" + theGetMethodName + "' in '" + interfaceClass.getName() + "' must return an Iterator type.");
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
			
			String methodName = "create"
				+ localName.toUpperCase().charAt(0)
				+ localName.substring(1, localName.length());
			
			try {
				theCreateMethod =
					propertySet.getImplementationClass().getMethod(methodName, parameters);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				String parameterText = "";
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
				throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a '" + methodName + "' method that takes parameters of types (" + parameterText + ").");
			}
			
			if (theCreateMethod.getReturnType() != this.getValueClass()) {
				throw new MalformedPluginException("Method '" + methodName + "' in '" + propertySet.getImplementationClass().getName() + "' must return an object of type " + this.getValueClass() + "."); 
			}
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
	private Method getDeclaredMethodRecursively(Class interfaceClass, String methodName, Class[] arguments)
		throws NoSuchMethodException {
        try {
    		return interfaceClass.getDeclaredMethod(methodName, arguments);
        } catch (NoSuchMethodException e) {
/* check the interfaces.  Actually this is not necessary because we now get an object,
 * and so the method must be declared in this or a base class.
    		Class[] interfaces = interfaceClass.getInterfaces();
    		for (int i = 0; i < interfaces.length; i++) {
    	        try {
    	        	return getDeclaredMethodRecursively(interfaces[i], methodName, arguments);
    	        } catch (NoSuchMethodException e2) {
    	        }
    		}
 */
        	return getDeclaredMethodRecursively(interfaceClass.getSuperclass(), methodName, arguments);

//    		throw new NoSuchMethodException("Method '" + methodName + "' was not found in '" + interfaceClass.getName() + "' nor in any of the interfaces extended by it.");
        }
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
    
    public Method getTheGetMethod() {
        return theGetMethod;
    }
    
    public Method getTheSetMethod() {
        return theSetMethod;
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
				if (propertySet.getInterfaceClass() == propertyClass) {
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
    
    public double getWidth() {
        return width;
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
        // The property is considered editable if and only if a bean is
        // available to do the editing.
        return editorBeanClass != null;
    }
    
    /**
     * Gets an instance of a bean that edits this property.
     *
     * This method can be called only if isEditable returns true.
     */
    public JComponent getEditorBean() {
        try {
            return (JComponent)editorBeanClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("cannot instantiate editor bean");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("cannot access editor bean");
        }
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
		// We call into that factory to create a control.
		return propertyControlFactory.createPropertyControl(parent, this);
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

	public int getIndexIntoScalarProperties() {
		return indexIntoScalarProperties;
	}
	
	public void setIndexIntoConstructorParameters(int indexIntoConstructorParameters) {
		this.indexIntoConstructorParameters = indexIntoConstructorParameters;
	}

	public void setIndexIntoScalarProperties(int indexIntoScalarProperties) {
		this.indexIntoScalarProperties = indexIntoScalarProperties;
	}

	// You may wonder why this code is here when it is also
	// in the ExtendableObject class.
	// The reason is that callers can provide a very basic
	// implementation, often an inline implementation, of
	// the property set getter interface.  There would thus
	// be no code supplied to do all of this logic, and nor
	// do we want to require users to provide this code.
	public Object getValue(IExtendableObject values) {
		Object value;
		Object objectWithProperties = values;				

		// TODO: Return the value of an extension property if
		// neccessary.
		
		try {
			value = getTheGetMethod().invoke(objectWithProperties, null);
		} catch (IllegalAccessException e) {
			throw new MalformedPluginException("Method '" + getTheGetMethod().getName() + "' in '" + getPropertySet().getInterfaceClass().getName() + "' must be public.");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("internal error");
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
		
		return value;
	}
}
