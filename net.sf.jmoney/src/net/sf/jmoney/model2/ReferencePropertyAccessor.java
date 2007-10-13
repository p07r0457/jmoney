package net.sf.jmoney.model2;



public abstract class ReferencePropertyAccessor<V extends ExtendableObject> extends ScalarPropertyAccessor<V> {

	// Applies only if this property is of type ExtendableObject
//	private Field theObjectKeyField;

	public <E> ReferencePropertyAccessor(Class<V> classOfValueObject, PropertySet<E> propertySet, String localName, String displayName, int weight, int minimumWidth, final IPropertyControlFactory<V> propertyControlFactory, IPropertyDependency<E> propertyDependency) {
		super(classOfValueObject, propertySet, localName, displayName, weight, minimumWidth, propertyControlFactory, propertyDependency);
		
//		Class<E> implementationClass = propertySet.getImplementationClass();
//
//		// If the property value is an extendable object, use introspection on
//		// the implementation class to find the field containing
//		// the object key for the object referenced by this property.
//		if (ExtendableObject.class.isAssignableFrom(classOfValueObject)) { 		
//			String fieldName = localName + "Key";
//
//			Class classToTry = propertySet.getImplementationClass();
//			do {
//				try {
//					theObjectKeyField = classToTry.getDeclaredField(fieldName);
//					break;
//				} catch (NoSuchFieldException e) {
//					classToTry = classToTry.getSuperclass();
//				}
//			} while (classToTry != null);
//
//			if (theObjectKeyField == null) {
//				throw new MalformedPluginException("The " + propertySet.getImplementationClass().getName() + " class must have a field called " + fieldName + ".");
//			}
//
//			if (!IObjectKey.class.isAssignableFrom(theObjectKeyField.getType())) {
//				throw new MalformedPluginException("Field '" + fieldName + "' in '" + implementationClass.getName() + "' must reference an object type that implements IObjectKey.");
//			}
//
//			// (1 is public,  2 is private, 4 is protected, 1,2 & 4 bits off is default).
//			if ((theObjectKeyField.getModifiers() & 5) == 0) {
//				throw new MalformedPluginException("Field '" + fieldName + "' in '" + implementationClass.getName() + "' must be protected (or public if you insist).");
//			}
//		}
//
	}

	/**
	 * Given an object (which must be of a class that contains this
	 * property), return the object key to this property.
	 *   
	 * @param object
	 * @return
	 */
	public abstract IObjectKey invokeObjectKeyField(ExtendableObject parentObject);
	
//	public IObjectKey invokeObjectKeyField(ExtendableObject object) {
//		if (getPropertySet().isExtension()) {
//			ExtensionObject extension = object.getExtension((ExtensionPropertySet<?>)getPropertySet(), false);
//			if (extension == null) {
//				return null;
//			} else {
//				return getObjectKey(extension);
////				return (IObjectKey)extension.getProtectedFieldValue(theObjectKeyField);
//			}
//		} else {
//			return getObjectKey(object);
////			return (IObjectKey)object.getProtectedFieldValue(theObjectKeyField);
//		}
//	}

}
