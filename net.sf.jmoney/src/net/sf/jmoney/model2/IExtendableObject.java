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

import java.util.Iterator;

/**
 * @author Nigel
 *
 * This is the base class for all objects that may have extension
 * property sets added by plug-ins.  The framework supports the
 * following objects that may be extended:
 * <UL>
 * <LI>Session</LI>
 * <LI>Commodity</LI>
 * <LI>Account</LI>
 * <LI>Transaxion</LI>
 * <LI>Entry</LI>
 * </UL>
 * <P>
 * Plug-ins are also able to create new classes of extensible
 * objects by deriving classes from this class.
 */
public interface IExtendableObject {
	/**
	 * Get the extension that implements the properties needed by
	 * a given plug-in.
	 */
	ExtensionPropertySet getExtension(PropertySet propertySetKey);
	
	Object getPropertyValue(PropertyAccessor propertyAccessor);
	
	int getIntegerPropertyValue(PropertyAccessor propertyAccessor);
	
	long getLongPropertyValue(PropertyAccessor propertyAccessor);
	
	String getStringPropertyValue(PropertyAccessor propertyAccessor);

	/**
	 * Obtain an iterator that iterates over the values of a
	 * list property.
	 * 
	 * @param propertyAccessor The property accessor for the property
	 * 			whose values are to be iterated.  The property
	 * 			must be a list property (and not a scalar property).
	 */
	Iterator getPropertyIterator(PropertyAccessor propertyAccessor);
	
	// TODO: check whether we need this method.
	String getPropertyValueAsString(PropertyAccessor propertyAccessor);
}

