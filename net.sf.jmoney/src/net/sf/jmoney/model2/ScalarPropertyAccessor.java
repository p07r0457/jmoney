package net.sf.jmoney.model2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

import org.eclipse.swt.widgets.Composite;

public class ScalarPropertyAccessor<V> extends PropertyAccessor {

	   // Applies only if scalar property
	   private int weight;    
	   
	   // Applies only if scalar property
	   private int minimumWidth;    
	   
	   // Applies only if scalar property
	   private boolean sortable;
	   
	   // Applies only if scalar property
	   private IPropertyControlFactory<V> propertyControlFactory;
	   
	   // Applies only if scalar property
	   private Method theSetMethod;
	   
	   // Applies only if scalar property of type ExtendableObject
	   private Field theObjectKeyField;
	   
	   /**
		 * The class of the property values (if the method signatures show int,
		 * long, boolean, or char, this field will be Integer.class, Long.class,
		 * Boolean.class, or Character.class)
		 */
	private Class<V> classOfValueObject;

	/**
	 * The class of the property values (if the method signatures show int,
	 * long, boolean, or char, this field will be int.class, long.class,
	 * boolean.class, or char.class)
	 */
	private Class<?> classOfValueType;
	   
		/**
		 * Index into the array of scalar properties.
		 */
		private int indexIntoScalarProperties = -1;

	   
	public ScalarPropertyAccessor(Class<V> classOfValueObject, PropertySet propertySet, String localName, String displayName, int weight, int minimumWidth, IPropertyControlFactory<V> propertyControlFactory, IPropertyDependency propertyDependency) {
		super(propertySet, localName, displayName, propertyDependency);

	       this.weight = weight;
	       this.minimumWidth = minimumWidth;
	       this.sortable = true;
	       this.propertyControlFactory = propertyControlFactory;
	       
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
	       
	       classOfValueType = theGetMethod.getReturnType();

	       Class<?> actualReturnType;
	       boolean isNullAllowed;
	       if (classOfValueType == int.class) {
	    	   actualReturnType = Integer.class;
	    	   isNullAllowed = false;
	       } else if (classOfValueType == long.class) {
	    	   actualReturnType = Long.class;
	    	   isNullAllowed = false;
	       } else if (classOfValueType == boolean.class) {
	    	   actualReturnType = Boolean.class;
	    	   isNullAllowed = false;
	       } else if (classOfValueType == char.class) {
	    	   actualReturnType = Character.class;
	    	   isNullAllowed = false;
	       } else {
	    	   actualReturnType = classOfValueType;
	    	   isNullAllowed = true;
	       }
 
	       
	       if (actualReturnType != classOfValueObject) {
	           throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + implementationClass.getName() + "' returns type '" + theGetMethod.getReturnType().getName() + "' but code metadata indicates the type should be '" + classOfValueObject.getName() + "'.");
	       }
	       
	       this.classOfValueObject = classOfValueObject;
	       
	       // Use introspection on the implementation class to find the setter method.
	       Class parameterTypes[] = {classOfValueType};
			theSetMethod = findMethod("set", localName, parameterTypes);
	       
	       if (theSetMethod.getReturnType() != void.class) {
	           throw new MalformedPluginException("Method '" + theSetMethod.getName() + "' in '" + implementationClass.getName() + "' must return void type .");
	       }

	       // If the property value is an extendable object, use introspection on
			// the implementation class to find the field containing
			// the object key for the object referenced by this property.
	       if (ExtendableObject.class.isAssignableFrom(classOfValueObject)) { 		
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
	    * Indicates whether the property may be edited by the user.
	    */
	   public boolean isEditable() {
	       return (propertyControlFactory != null && propertyControlFactory.isEditable());
	   }
	   
	   /**
	    * Return a comparator to be used to compare values of this property.
	    * If the property definition defines a custom comparator for this property then return that,
	    * otherwise if the values are of a class that implements the Comparable interface,
	    * use that, otherwise return a comparator that orders the values by getting the String
	    * values to be used when displaying the properties in a table and ordering those
	    * strings.
	    * 
	    * Note that the comparator defines an ordering of the objects that contain the properties,
	    * not the values of the properties.  You should pass the ExtendableObject objects
	    * to the comparator and the comparator will then lookup the values.
	    * 
	    * This method always returns a non-null comparator.
	    */
	   public Comparator<ExtendableObject> getComparator() {
		   /*
		    * No support has yet been added to allow custom comparators to
		    * be defined for properties.  We therefore return default comparators
		    * based on the type of the values.
		    * 
		    */
		   if (Comparable.class.isAssignableFrom(getClassOfValueObject())) {
			   return new Comparator<ExtendableObject> () {
				   public int compare(ExtendableObject object1, ExtendableObject object2) {
					   V value1 = object1.getPropertyValue(ScalarPropertyAccessor.this);
					   V value2 = object2.getPropertyValue(ScalarPropertyAccessor.this);
					   if (value1 == null && value2 == null) return 0;
					   if (value1 == null) return 1;
					   if (value2 == null) return -1;

					   return ((Comparable)value1).compareTo(value2);
				   }
			   };
		   }

		   /* No custom comparator and not a known type, so sort according to the
			 text value that is displayed when the property is shown
			 in a table (ignoring case).
		    */
		   return new Comparator<ExtendableObject> () {
			   public int compare(ExtendableObject object1, ExtendableObject object2) {
				   String text1 = propertyControlFactory.formatValueForTable(object1, ScalarPropertyAccessor.this);
				   if (text1 == null) {
					   text1 = "";
				   }
				   String text2 = propertyControlFactory.formatValueForTable(object2, ScalarPropertyAccessor.this);
				   if (text2 == null) {
					   text2 = "";
				   }
				   return text1.compareToIgnoreCase(text2);
			   }
		   };		   
	   }

		/**
		 */
	   public V invokeGetMethod(Object invocationTarget) {
			try {
				Object value = theGetMethod.invoke(invocationTarget, (Object [])null);
				return classOfValueObject.cast(value);
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
	   
	   public void invokeSetMethod(Object invocationTarget, V value) {
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

		/**
		 * Create a Control object that edits the property.
		 * 
		 * @param parent
		 * @return An interface to a wrapper class.
		 */
		public IPropertyControl createPropertyControl(Composite parent, Session session) {
			// When a PropertyAccessor object is created, it is
			// provided with an interface to a factory that constructs
			// control objects that edit the property.
			// We call into that factory to create an edit control.
			return propertyControlFactory.createPropertyControl(parent, this, session);
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
		    * Indicates if the property is a list of objects.
		    */
		   public boolean isList() {
		       return false;
		   }

		   /**
			 * Returns the class for the values of this property. This is the
			 * class that is returned by the getter method.
			 * 
			 * @return
			 */
		   public Class<V> getClassOfValueObject() {
		       return classOfValueObject;
		   }
		   
		   /**
			 * Returns the class for the values of this property. This is
			 * usually the class that is returned by the getter method, but if
			 * the getter method returns int, long, boolean, or char then this
			 * method will return Integer.class, Long.class, Boolean.class, or
			 * Character.class.
			 * 
			 * @return
			 */
		   Class<?> getClassOfValueType() {
		       return classOfValueType;
		   }
		   
		   /**
		    * Given an object (which must be of a class that contains this
		    * property), return the object key to this property.
		    *   
		    * @param object
		    * @return
		    */
		   public IObjectKey invokeObjectKeyField(ExtendableObject object) {
			   if (getPropertySet().isExtension()) {
				   ExtensionObject extension = object.getExtension(getPropertySet());
				   if (extension == null) {
					   return null;
				   } else {
					   return (IObjectKey)extension.getProtectedFieldValue(theObjectKeyField);
				   }
			   } else {
				   return (IObjectKey)object.getProtectedFieldValue(theObjectKeyField);
			   }
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
			// TODO: must also check that boolean property is not allowed
			// to be null.
			public IPropertyDependency getTrueValueDependency() {
				if (classOfValueObject != Boolean.class) {
					throw new MalformedPluginException("getTrueValueDependency called on property that is not a boolean.");
				}
				
				return new IPropertyDependency() {
					public boolean isSelected(ExtendableObject object) {
						// TODO: How do we ensure this method is defined only for
						// PropertyAccessor_Scalar<Boolean> objects?
						// We should remove from this class altogether and move outside.
						return (Boolean)object.getPropertyValue(ScalarPropertyAccessor.this);
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
			// TODO: must also check that boolean property is not allowed
			// to be null.
			public IPropertyDependency getFalseValueDependency() {
		        if (classOfValueObject != Boolean.class) {
		            throw new MalformedPluginException(
		                    "getTrueValueDependency called on property that is not a boolean.");
		        }

		        return new IPropertyDependency() {
		            public boolean isSelected(ExtendableObject object) {
		                if (object == null) {
		                    return false;
		                }

						// TODO: How do we ensure this method is defined only for
						// PropertyAccessor_Scalar<Boolean> objects?
						// We should remove from this class altogether and move outside.
		                return !(Boolean)object.getPropertyValue(ScalarPropertyAccessor.this);
		            }
		        };
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
			 * properties are returned by the PropertySet.getPropertyIterator_Scalar3().
			 * i.e. if this method returns n then in every case where the
			 * collection returned by getPropertyIterator_Scalar3 contains this property,
			 * this property will be returned as the (n+1)'th element in the collection.
			 * 
			 * @return the index of this property in the list of scalar
			 * 			properties for the class.  This method returns zero
			 * 			for the first scalar property returned by
			 * 			PropertySet.getPropertyIterator3() and so on. 
			 */
			public int getIndexIntoScalarProperties() {
				return indexIntoScalarProperties;
			}
			
			// TODO: This method should be accessible only from within the package. 
			public void setIndexIntoScalarProperties(int indexIntoScalarProperties) {
				this.indexIntoScalarProperties = indexIntoScalarProperties;
			}

}
