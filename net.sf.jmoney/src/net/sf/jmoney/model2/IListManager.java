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

import java.util.Collection;

/**
 * This interface is the interface to all objects that manage
 * the sets of values that are the values of a multi-valued
 * property.  For example, the set of all accounts in a session
 * are managed by an object that implements this interface.
 */
public interface IListManager extends Collection {

	/**
	 * This method creates a new object in this collection
	 * in the datastore.  The new object will be initialized
	 * with default values.
	 * 
	 * @return the newly created object.
	 */
	IExtendableObject createNewElement(ExtendableObjectHelperImpl parent, PropertySet propertySet);
}
