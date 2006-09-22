package net.sf.jmoney.model2;

import java.lang.reflect.InvocationTargetException;

public class ListPropertyAccessor<E extends ExtendableObject> extends PropertyAccessor {

	   /**
	    * 
	    * the class of the elements in the list
	    */
	   private Class<E> propertyClass;
	   
	public ListPropertyAccessor(PropertySet propertySet, String localName, String displayName, Class<E> listItemClass, IPropertyDependency propertyDependency) {
		super(propertySet, localName, displayName, listItemClass, propertyDependency);

	       this.propertyClass = listItemClass;

	       // Use introspection on the interface to find the getXxxCollection method.
		theGetMethod = findMethod("get", localName + "Collection", null);
		
       if (!ObjectCollection.class.isAssignableFrom(theGetMethod.getReturnType())) { 		
			throw new MalformedPluginException("Method '" + theGetMethod.getName() + "' in '" + propertySet.getImplementationClass().getName() + "' must return an ObjectSet type.");
		}
	}

	   /**
	    * Indicates if the property is a list of objects.
	    */
	   public boolean isList() {
	       return true;
	   }

	   /**
		 * Returns the class for the values in the lists. This is the class of
		 * the items contained in the collections returned by the getter method
		 * 
		 * @return
		 */
	   public Class<E> getValueClass() {
	       return propertyClass;
	   }
	   
		/**
		 */
	   public ObjectCollection<E> invokeGetMethod(Object invocationTarget) {
			try {
				return (ObjectCollection<E>)theGetMethod.invoke(invocationTarget, (Object [])null);
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
	   
}
