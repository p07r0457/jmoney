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

/**
 *
 * @author  Nigel
 */
public interface IPropertyRegistrar {
	
	
	/**
	 * Creates an enumerated value.
	 * 
	 * @param propertyAccessor The accessor for the enumerated type
	 * 			to which this value is being added.
	 * @param internalName The id by which this value is identified
	 * 			internally from the code.  The internal names need only
	 * 			by unique within an enumerated value.  Also, the id of 
	 * 			this plug-in will
	 * 			be prepended, so the internal names need only be unique
	 * 			within a plug-in.
	 * @param displayName The text displayed to the user.  This text
	 * 			would be displayed, for example, if the user had to choose
	 * 			a value for this enumerated type.  Normally this text
	 * 			is displayed as is.  However, if two plug-ins create a
	 * 			value with the same text then the id of plug-in is
	 * 			appended in brackets.
	 * @return An EnumerationAccessor object that represents the
	 * 		enumerated value.  This object must be passed back when
	 * 		creating another property which depends on the enumerated
	 * 		type having this value.
	 */
	EnumerationAccessor addEnumeratedValue(
			PropertyAccessor propertyAccessor,
			String internalName, 
			String displayName);
	
	/**
	 * Creates an enumerated value that represents a derived class.
	 * 
	 * @param propertyAccessor
	 * @param internalName
	 * @param displayName
	 * @param derivedClass This parameter must be specified if and
	 * 			only if the enumerated type controls derived classes.
	 * 			<em>derivedClass</em> is a class derived from the class
	 * 			in which the enumerated type occurs.
	 * @return
	 */
	EnumerationAccessor addEnumeratedValue(
			PropertyAccessor propertyAccessor,
			String internalName, 
			String displayName,
			Class derivedClass);
	
	/**
	 * @param location Entry, CapitalAccount, Commodity or Transaction
	 * @param name
	 * @param displayName localized description of the property
	 * @param width
	 * @param currencyControlFactory
	 * @param propertyDependency
	 */
	PropertyAccessor addProperty(
			String name, 
			String displayName, 
			double width, 
			IPropertyControlFactory controlFactory,
			IPropertyDependency propertyDependency);
	
	/**
	 * This method should be called if the property contains
	 * a list of properties.  If an object contains a list
	 * of properties then the object implementation must contain
	 * a method of the form getFooIterator() that returns an
	 * Iterator, where 'foo' is the name of the list property.   
	 *
	 * @param name
	 * @param listItemClass The class for all items in the list.
	 * 			Items in the list may be of a class derived from
	 * 			this class.
	 * @param shortDescription
	 * @param width
	 * @param propertyDependency
	 */
	PropertyAccessor addPropertyList(
			String name,
			String shortDescription, 
			Class listItemClass,
			IPropertyDependency propertyDependency);
	
	/**
	 * This method should be called if the property set is designed to have
	 * further property sets derived from it.  Unlike Java classes, a developer
	 * can derived from an existing property set only if that property set was
	 * designed to have other property sets derive from it.
	 *
	 * @param name The name of a property whose value depends on
	 * 				the derived class.  The implementation class for
	 * 				this property set must define an abstract getter method
	 * 				for this property that returns a string.
	 * 				The implementation classes for any derived
	 * 				property sets must implement the getter method
	 * 				and return a localized string that describes
	 * 				the type of object represented by the derived
	 * 				class.  
	 * @param displayName An internationalized string that describes the enumerated
	 * 			property that indicates the actual derived class.
	 * @return
	 */
	PropertyAccessor setDerivableInfo(String name, String displayName);
	
	/**
	 * This method should be called if the property set is designed to have
	 * further property sets derived from it.  Unlike Java classes, a developer
	 * can derived from an existing property set only if that property set was
	 * designed to have other property sets derive from it.
	 * <P>
	 * This version of this method does not provide text that can
	 * be displayed to the user.
	 * 
	 * TODO decide whether we need this version at all.
	 */
	PropertyAccessor setDerivableInfo();
	
	/**
	 * If the object is not an extension and not derivable then
	 * a localized description of the type of object represented
	 * by this property set must be set by calling this method.
	 */
	void setObjectDescription(String description);
}
