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
//  private Method theAddMethod;
    
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
     * List of listeners that are listening for changes to the value of this property
     * in any entry.
     */
    private PropertyChangeSupport propertySupport;

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
        
        // Use introspection on the interface to find the setter method.
        // This must be done on the mutable interface.
        Class mutableInterfaceClass = propertySet.getMutableInterfaceClass();
        
        String theSetMethodName	= "set"
			+ localName.toUpperCase().charAt(0)
			+ localName.substring(1, localName.length());
        Class parameterTypes[] = {propertyClass};
        
        try {
            this.theSetMethod = mutableInterfaceClass.getDeclaredMethod(theSetMethodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new MalformedPluginException("Method '" + theSetMethodName + "' in '" + mutableInterfaceClass.getName() + "' was not found.");
        }
        
        if (theSetMethod.getReturnType() != void.class) {
            throw new MalformedPluginException("Method '" + theSetMethodName + "' in '" + mutableInterfaceClass.getName() + "' must return void type .");
        }
       
        propertySupport = new PropertyChangeSupport(this);
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
        
        // Use introspection on the interface to find the setter method.
        // This must be done on the mutable interface.
   /*
        Class mutableInterfaceClass = propertySet.getMutableInterfaceClass();
        
        String theAddMethodName	= "add"
			+ localName.toUpperCase().charAt(0)
			+ localName.substring(1, localName.length());
        Class parameterTypes[] = {propertyClass};
        
        try {
            this.theAddMethod = mutableInterfaceClass.getDeclaredMethod(theAddMethodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new MalformedPluginException("Method '" + theAddMethodName + "' in '" + mutableInterfaceClass.getName() + "' was not found.");
        }
        
        if (theAddMethod.getReturnType() != void.class) {
            throw new MalformedPluginException("Method '" + theAddMethodName + "' in '" + mutableInterfaceClass.getName() + "' must return void type .");
        }
*/     
        propertySupport = new PropertyChangeSupport(this);
	}

	/**
	 * Gets a method from an interface.  
	 * Whereas Class.getDeclaredMethod finds a method from an
	 * interface, it will not find the method if the method is
	 * defined in an interface which the given interface extends.
	 * This method will find the method if an of the interfaces
	 * extended by this interface define the method. 
	 */
	private Method getDeclaredMethodRecursively(Class interfaceClass, String methodName, Class[] arguments)
		throws NoSuchMethodException {
        try {
    		return interfaceClass.getDeclaredMethod(methodName, arguments);
        } catch (NoSuchMethodException e) {
    		Class[] interfaces = interfaceClass.getInterfaces();
    		for (int i = 0; i < interfaces.length; i++) {
    	        try {
    	        	return getDeclaredMethodRecursively(interfaces[i], methodName, arguments);
    	        } catch (NoSuchMethodException e2) {
    	        }
    		}
            throw new NoSuchMethodException("Method '" + methodName + "' was not found in '" + interfaceClass.getName() + "' nor in any of the interfaces extended by it.");
        }
	}
	
	public PropertySet getPropertySet() {
        return propertySet;
    }
    
    public Method getTheGetMethod() {
        return theGetMethod;
    }
    
    public Method getTheSetMethod() {
        return theSetMethod;
    }
/*    
    public Method getTheAddMethod() {
        return theAddMethod;
    }
*/    
    public Class getValueClass() {
        return propertyClass;
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

    /**
     * When someone is listening for changes to a property in an entry, they usually
     * want to know about a change to the property in any of the entries.
     * It would be very inefficient to add yourself as a listener to every entry object.
     * Therefore one can instead add a listener to the PropertyAccessor object
     * and be told about changes to the property in any entry object.
     *
     * To implement this, all entries will, when a property changes, tell the
     * PropertyAccessor object.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */

    public void addPropertyChangeListener(
    String propertyName,
    PropertyChangeListener listener) {
        // This object can only process one property, so check the given property
        // name matches the name of the property that this object represents.
        if (!localName.equals(propertyName)) {
            throw new RuntimeException("Property name mismatch");
        }
        
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    
    public void removePropertyChangeListener(
    String propertyName,
    PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }
    
    /**
     * This method must be called the value of this property is changed in any entry.
     */
    public void firePropertyChange(IExtendableObject source, Object oldValue, Object newValue) {
        // It may be that properties are being set because we are copying values
        // into a mutable copy of an entry.
        // We must not fire any events in this case.  If we did we would get
        // bad recursion, as values are propagated through adaptors, other
        // extensions would also be created, even though these would all be
        // identical to the original.
        
        // Properties must be propagated through adaptors whenever a mutable
        // entry property is changed.  This ensures the other views of the
        // property are seen before the entry is committed.
        
        // TODO: this has not been implemented.  Therefore, currently, when
        // a copy of an extension is taken so that it can be edited, other
        // extensions to which properties propagate will be copied too.
        
        // The event must show the entry as the source, not this object.
        // We therefore create our own event and fire that to the listeners.
        PropertyChangeEvent evt = new PropertyChangeEvent(source,
    	    localName, oldValue, newValue);
        propertySupport.firePropertyChange(evt);
        
        // We also need to call any adaptors which set entry properties from
        // other extensions based on the value of this extension property.
        // (We could have implemented this by calling addPropertyChangeListener
        // on this object for each adaptor.  However it would be more work to
        // maintain these listeners correctly as plugins are added and removed
        // and also we would have to add the listener to every property in
        // an entry extension).
        Propagator.fireAdaptors(source, this);
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

	public void setIndexIntoConstructorParameters(int indexIntoConstructorParameters) {
		this.indexIntoConstructorParameters = indexIntoConstructorParameters;
	}
}
