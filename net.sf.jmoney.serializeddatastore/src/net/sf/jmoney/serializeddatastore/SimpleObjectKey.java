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

package net.sf.jmoney.serializeddatastore;

import net.sf.jmoney.model2.IExtendableObject;
import net.sf.jmoney.model2.IObjectKey;

/**
 * This class provides the IObjectKey implementation.
 *
 * In this datastore implementation, the entire datastore is
 * read into memory when a session is opened.  The object key
 * implementation is therefore very simple - the object key is
 * simply a reference to the object.
 */
public class SimpleObjectKey implements IObjectKey {
	private IExtendableObject extendableObject;
	
	SimpleObjectKey() {
	}
	
	public IExtendableObject getObject() {
		return extendableObject;
	}

	void setObject(IExtendableObject extendableObject) {
		this.extendableObject = extendableObject;
	}
}
