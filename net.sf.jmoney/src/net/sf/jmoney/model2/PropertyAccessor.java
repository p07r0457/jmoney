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
* This class contains information about a property.  The property may be in the base
* bookkeeping class or in an extension bookkeeping class.
*
* This object contains the metadata.
*
* Some properties may be allowed to take null values and
* some may not.  The determination is made by the JMoney framework
* by looking at the type returned by the getter method.
* <P>
* The following properties may take null values:
* <UL>
* <LI>All properties that reference extendable objects</LI>
* <LI>All properties of type String, Date or other such simple class</LI>
* <LI>All properties where the type is one of the classes representing intrinsic types,
* 			such as Integer, Long</LI>
* </UL>
* The following properties may not take null values:
* <UL>
* <LI>All properties where the type is an intrinsic type, such as int, long</LI>
* </UL>
* 
* @author  Nigel Westbury
*/
public class PropertyAccessor {
   
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
	
   public PropertyAccessor(PropertySet propertySet, String localName, String shortDescription, int weight, int minimumWidth, IPropertyControlFactory propertyControlFactory, IPropertyDependency propertyDependency) {
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
	public PropertyAccessor(PropertySet propertySet, String localName, String shortDescription, Class listItemClass, IPropertyDependency propertyDependency) {
   	this.propertySet = propertySet;
       this.localName = localName;
       this.propertyClass = listItemClass;
       this.shortDescription = shortDescription;
       
       isList = true;

		// Use introspection on the interface to find the getXxxCollection method.
		theGetMethod = findMethod("get", localName + "Collection", null);
		
       if (!ObjectCollection.class.isAssignableFrom(theGetMethod.getReturnType())) { 		
			throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + propertySet.getImplementationClass().getName() + "' must return an ObjectSet type.");
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
	
   /**
    * Returns the property set which contains this property.
    */
	// TODO: Check all the uses of this.  Some of the uses require
	// that for extension property sets, the property set being
	// extended should be returned.  This saves the caller from having
	// to test the property set.
	public PropertySet getPropertySet() {
       return propertySet;
   }
	
   /**
    * Returns the extendable property set which contains this property.
    * 
    * If the property is in an extendable property set then this
    * method returns the same value as <code>getPropertySet()</code>.
    * If the property is in an extension property set then
    * the property set being extended is returned.
    */
   // TODO: Consider removing this method.  It is not used.
	public PropertySet getExtendablePropertySet() {
		if (propertySet.isExtension()) {
			return propertySet.getExtendablePropertySet();
		} else {
			return propertySet;
		}
   }
   
	/**
	 * If the property is a list property then the getter returns
	 * an <code>ObjectSet</code>.
	 */
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
    * If this property is a list property then this method
    * returns the class of the elements in the list.
    * 
    * @return
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
	 * The local name of the property is just the last part of the name, after
	 * the last dot. This will be unique within an extension but may not be
	 * unique across all plugins or even across extensions to different types of
	 * bookkeeping objects (entries, categories, transactions, or commodities)
	 * within a plug-in.
	 */
   public String getLocalName() {
       return localName;
   }
   
   /**
    * A short description that is suitable as a column header when this
    * property is displayed in a table.
    */
   public String getShortDescription() {
       return shortDescription;
   }
   
   /**
    * The width weighting to be used when this property is displayed
    * in a table or a grid.  If the width available for the table
    * or grid is more than the sum of the minimum widths then the
    * excess width is distributed across the columns.
    * This weight indicates how much the property
    * can benefit from being given excess width.  For example
    * a property containing a description can benefit, whereas
    * a property containing a date cannot. 
    */
   public int getWeight() {
       return weight;
   }

   /**
    * The minimum width to be used when this property is displayed
    * in a table or a grid.  
    */
   public int getMinimumWidth() {
       return minimumWidth;
   }

   /**
    * Indicates whether users are able to sort views based on this property.
    */
   public boolean isSortable() {
       return sortable;
   }
   
   /**
    * Indicates if the property is a list of intrinsic values or objects.
    */
   public boolean isList() {
       return isList;
   }
   
   /**
    * Indicates if the property is a single intrinsic value or object
    * (not a list of values)
    */
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
    * If the property definition defines a custom comparator for this property then fetch it.
    * Otherwise return null to indicate that the default comparator for the type
    * should be used.
    */
   public Comparator getCustomComparator() {
       return null;
   }

	/**
	 * Create a Control object that edits the property.
	 * 
	 * @param parent
	 * @return An interface to a wrapper class.
	 */
	public IPropertyControl createPropertyControl(Composite parent) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// We call into that factory to create an edit control.
		return propertyControlFactory.createPropertyControl(parent, this);
	}

//TODO: Remove this method.  Cell editors are no longer used
	// because the mechanism was not flexible enough and it was
	// easy enough to position editors correctly ourselves.
	/**
	 * Create a CellEditor object that enables the property to
	 * be edited in-place in a <code>Table</code>.
	 * 
	 * @param table
	 * @return a cell editor, or null if the property cannot
	 * 			be edited in-place in a <code>Table</code>
	 */
	public CellEditor createCellEditor(Table table) {
		// When a PropertyAccessor object is created, it is
		// provided with an interface to a factory that constructs
		// control objects that edit the property.
		// We call into that factory to create a cell editor.
		return propertyControlFactory.createCellEditor(table);
	}

	// TODO: Remove
	/**
	 * Get the value of a property and return it typed for
	 * use in the cell editor.
	 * <P>
	 * For example, the ComboBoxCellEditor cell editors require the
	 * value as an integer index into the selection list.  Therefore,
	 * if <code>createCellEditor</code> returns a ComboBoxCellEditor
	 * then this method must return the value as an index into the list
	 * of possible values.
	 * 
	 * @param extendableObject
	 * @return
	 */
	public Object getValueTypedForCellEditor(ExtendableObject extendableObject) {
	    if (extendableObject == null) return new Integer (0); // I don't know why -- Faucheux
		return propertyControlFactory.getValueTypedForCellEditor(extendableObject, this);
	}

	// TODO: Remove
	/**
	 * Set the value of a property given a value returned by the cell editor.
	 * <P>
	 * For example, the ComboBoxCellEditor cell editors give the
	 * value as an integer index into the selection list.  Therefore,
	 * if <code>createCellEditor</code> returns a ComboBoxCellEditor
	 * then this method must set the appropriate value as determined from
	 * the given index.
	 * 
	 * @param extendableObject
	 * @param value
	 */
	public void setValueTypedForCellEditor(ExtendableObject extendableObject, Object value) {
		propertyControlFactory.setValueTypedForCellEditor(extendableObject, this, value);
	}

	/**
	 * Format the value of a property so it can be embedded into a
	 * message.
	 *
	 * The returned value will look sensible when embedded in a message.
	 * Therefore null values and empty values are returned as non-empty
	 * text such as "none" or "empty".  Text values are placed in
	 * quotes unless sure that only a single word will be returned that
	 * would be readable without quotes.
	 *
	 * @return The value of the property formatted as appropriate.
	 */
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

	/**
	 * Format the value of a property as appropriate for displaying in a
	 * table.
	 * 
	 * The returned value is expected to be displayed in a table or some similar
	 * view.  Null and empty values are therefore returned as empty strings.
	 * Text values are not quoted.
	 * 
	 * @return The value of the property formatted as appropriate.
	 */
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
	 * @return the index of this property in the list of scalar
	 * 			properties for the class.  This method returns zero
	 * 			for the first scalar property returned by
	 * 			PropertySet.getPropertyIterator3() and so on. 
	 */
	public int getIndexIntoScalarProperties() {
		return indexIntoScalarProperties;
	}
	
	/**
	 * 
	 * @param indexIntoConstructorParameters
	 */
	// TODO: This method should be accessible only from within the package. 
	public void setIndexIntoConstructorParameters(int indexIntoConstructorParameters) {
		this.indexIntoConstructorParameters = indexIntoConstructorParameters;
	}

	public void setIndexIntoScalarProperties(int indexIntoScalarProperties) {
		this.indexIntoScalarProperties = indexIntoScalarProperties;
	}

	/**
	 * Create a dependency that can be used when the applicability of another
	 * property depends on this property.
	 * 
	 * This method may only be called if this property is a boolean property.
	 * 
	 * @return
	 */
	// TODO: Should this be a member of PropertyAssessor, or should it
	// be outside?????
	public IPropertyDependency getTrueValueDependency() {
		if (propertyClass != boolean.class) {
			throw new MalformedPluginException("getTrueValueDependency called on property that is not a boolean.");
		}
		
		return new IPropertyDependency() {
			public boolean isSelected(ExtendableObject object) {
				return object.getBooleanPropertyValue(PropertyAccessor.this);
			}
		};
	}

	/**
	 * Create a dependency that can be used when the applicability of another
	 * property depends on this property.
	 * 
	 * This method may only be called if this property is a boolean property.
	 * 
	 * @return
	 */
	public IPropertyDependency getFalseValueDependency() {
		if (propertyClass != boolean.class) {
			throw new MalformedPluginException("getTrueValueDependency called on property that is not a boolean.");
		}
		
		return new IPropertyDependency() {
			public boolean isSelected(ExtendableObject object) {
				return !object.getBooleanPropertyValue(PropertyAccessor.this);
			}
		};
	}

	/**
	 * Returns an object that indicates whether this property is applicable,
	 * given the values of other properties on which this property depends.
	 * 
	 * @return
	 */
	public IPropertyDependency getDependency() {
		return dependency;
	}
}
