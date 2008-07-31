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


public class ExtensionPropertySet<E extends ExtensionObject> extends PropertySet<E> {

	private ExtendablePropertySet<?> extendablePropertySet;	

	/**
	 * An interface that can be used to construct implementation objects,
	 * or null if this is an abstract property set.
	 */
	IExtensionObjectConstructors<E> constructors;
	
	/**
	 * Constructs an extension property set object.
	 *  
	 * @param classOfObject
	 * @param constructors
	 *            a required interface containing methods for constructing
	 *            implementation objects
	 */
	protected ExtensionPropertySet(Class<E> classOfObject, ExtendablePropertySet<?> extendablePropertySet, IExtensionObjectConstructors<E> constructors) {
		this.isExtension = true;
		this.classOfObject = classOfObject;
		this.extendablePropertySet = extendablePropertySet;
		this.constructors = constructors;

		// TODO: move outside this constructor.
		if (extendablePropertySet == null) {
			throw new MalformedPluginException("A non-null extendable property set must be passed."); //$NON-NLS-1$
		}
	}
	
	@Override
	public void initProperties(String propertySetId) {
		super.initProperties(propertySetId);
		
		// Add to our map that maps ids to ExtensionPropertySet objects.
		allExtensionPropertySetsMap.put(propertySetId, this);

		// Add to our map that maps ids to ExtensionPropertySet objects
		// within a particular extendable object.
		extendablePropertySet.extensionPropertySets.put(propertySetId, this);
/*		
		// Build the list of properties that are passed to
		// the 'new object' constructor and another list that
		// are passed to the 're-instantiating' constructor.

		constructorProperties = new Vector<PropertyAccessor>();
		defaultConstructorProperties = new Vector<PropertyAccessor>();

		// This property set is an extension.
		int parameterIndex = 0;
		for (PropertyAccessor propertyAccessor: properties) {
			constructorProperties.add(propertyAccessor);
			propertyAccessor.setIndexIntoConstructorParameters(parameterIndex++);
			if (propertyAccessor.isList()) {
				defaultConstructorProperties.add(propertyAccessor);
			}
		}
*/		
	}

	public ExtendablePropertySet getExtendablePropertySet() {
		return extendablePropertySet;
	}

	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * @param constructorParameters an array of values to be passed to
	 * 		the constructor.  If an extendable object is being constructed
	 * 		then the first three elements of this array must be the
	 * 		object key, the extension map, and the parent object key.
	 * 
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public E constructImplementationObject(ExtendableObject extendedObject, IValues values) {
		E extensionObject = constructors.construct(extendedObject, values);
		extensionObject.setPropertySet(this);
		return extensionObject;
	}

	/**
	 * This method should be used only by plug-ins that implement
	 * a datastore.
	 * 
	 * @return A newly constructed object, constructed from the given
	 * 		parameters.  This object may be an ExtendableObject or
	 * 		may be an ExtensionObject.
	 */
	public E constructDefaultImplementationObject(ExtendableObject extendedObject) {
		E extensionObject = constructors.construct(extendedObject);
		extensionObject.setPropertySet(this);
		return extensionObject;
	}
	
	@Override
	protected E getImplementationObject(ExtendableObject extendableObject) {
		return extendableObject.getExtension(this, true);
	}
}
