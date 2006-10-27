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

import java.util.Vector;

public class ExtensionPropertySet<E extends ExtensionObject> extends PropertySet<E> {

	private ExtendablePropertySet<?> extendablePropertySet;	

	/**
	 * Constructs an extension property set object.
	 *  
	 * @param classOfObject
	 */
	protected ExtensionPropertySet(Class<E> classOfObject, ExtendablePropertySet<?> extendablePropertySet, boolean isExtension) {
		this.isExtension = true;
		this.classOfObject = classOfObject;
		this.extendablePropertySet = extendablePropertySet;

		// TODO: move outside this constructor.
		if (extendablePropertySet == null) {
			throw new MalformedPluginException("A non-null extendable property set must be passed.");
		}
	}
	
	public void initProperties(String propertySetId) {
		super.initProperties(propertySetId);
		
		// Add to our map that maps ids to ExtensionPropertySet objects.
		allExtensionPropertySetsMap.put(propertySetId, this);

		// Add to our map that maps ids to ExtensionPropertySet objects
		// within a particular extendable object.
		extendablePropertySet.extensionPropertySets.put(propertySetId, this);
		
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

		findConstructors(true);
	}
}
