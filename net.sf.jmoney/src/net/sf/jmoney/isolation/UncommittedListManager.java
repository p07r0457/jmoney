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

import java.util.Vector;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IListManager;
import net.sf.jmoney.model2.IValues;

/**
 * This is a special implementation of the IListManager interface.
 * It is used only in objects that have not yet been committed to the datastore.
 * This implementation uses the Vector class to keep the list of objects.
 * 
 * @author Nigel Westbury
 */
public class UncommittedListManager<E extends ExtendableObject> extends Vector<E> implements IListManager<E> {

	private static final long serialVersionUID = 196103020038035348L;

	private TransactionManager transactionManager = null;
	
	public UncommittedListManager(TransactionManager transactionManager) {
	 	this.transactionManager = transactionManager;
	 }

	/**
	 * Create a new extendable object in the list represented by this object.
	 * <P>
	 * This method does not create the object in the underlying committed list,
	 * because if it did that then other views would see the object before it is
	 * committed. Instead this method adds the object to a list maintained by
	 * this object. When the consumer iterates over the list, the objects in the
	 * 'added' list are appended to the items returned by the underlying
	 * committed list.
	 */
	public <F extends E> F createNewElement(ExtendableObject parent, ExtendablePropertySet<F> propertySet) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F extendableObject = propertySet.constructDefaultImplementationObject(objectKey, parent.getObjectKey());

		
		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}

	// This method is never used, because new objects are only created
	// with non-default values when objects are being committed.
	// If we support nested transactions then this method will be required.
	public <F extends E> F createNewElement(ExtendableObject parent, ExtendablePropertySet<F> propertySet, IValues values) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F extendableObject = propertySet.constructImplementationObject(objectKey, parent.getObjectKey(), values);

		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}
}
