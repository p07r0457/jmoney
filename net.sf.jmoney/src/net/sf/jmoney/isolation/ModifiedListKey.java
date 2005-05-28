/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2005 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.isolation;

import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.PropertyAccessor;

/**
 * This class is the class of the keys in the modifiedLists map. An instance of
 * this class contains the parent object (the object containing the list
 * property) and the accessor for the list property. This pair defines exactly a
 * list.
 * <P>
 * The parent is an object in the datastore and will contain lists that contain
 * the data that has been committed to the datastore.
 * 
 * @author Nigel Westbury
 */
class ModifiedListKey {
	IObjectKey parentKey;
	PropertyAccessor listAccessor;
	
	ModifiedListKey(IObjectKey parentKey, PropertyAccessor listAccessor) {
		this.parentKey = parentKey;
		this.listAccessor = listAccessor;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof ModifiedListKey))
			return false;
		ModifiedListKey parentListPair = (ModifiedListKey)obj;
		return parentKey.equals(parentListPair.parentKey)
			&& listAccessor.equals(parentListPair.listAccessor);
	}
	
	public int hashCode() {
		return parentKey.hashCode() ^ listAccessor.hashCode();
	}
}
