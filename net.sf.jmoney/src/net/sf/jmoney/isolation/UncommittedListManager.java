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
import net.sf.jmoney.model2.ListKey;

/**
 * This is a special implementation of the IListManager interface. It is used
 * only in objects that have never been committed to the datastore (objects that
 * were created in this transaction). This implementation uses the Vector class
 * to keep the list of objects.
 * 
 * @author Nigel Westbury
 */
public class UncommittedListManager<E extends ExtendableObject> extends Vector<E> implements IListManager<E> {

	private static final long serialVersionUID = 196103020038035348L;

	private ListKey listKey;
	private TransactionManager transactionManager;
	
	public UncommittedListManager(ListKey listKey, TransactionManager transactionManager) {
		this.listKey = listKey;
	 	this.transactionManager = transactionManager;
	 }

	/**
	 * Create a new extendable object in the list represented by this object.
	 */
	@SuppressWarnings("unchecked")
	public <F extends E> F createNewElement(ExtendablePropertySet<F> propertySet) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F extendableObject = propertySet.constructDefaultImplementationObject(objectKey, listKey);

		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}

	/*
	 * This method is used only if a nested transaction manager is commitinsg
	 * its changes into this transaction manager.
	 */
	@SuppressWarnings("unchecked")
	public <F extends E> F createNewElement(ExtendablePropertySet<F> propertySet, IValues values) {
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		F extendableObject = propertySet.constructImplementationObject(objectKey, listKey, values);

		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}

	/*
	 * This method is used only if a nested transaction manager is commitinsg
	 * its changes into this transaction manager.
	 */
	public boolean deleteElement(E extendableObject) {
		return remove(extendableObject);
	}

	public void moveElement(E extendableObject, IListManager originalListManager) {
		/*
		 * It is fairly complex to implement this inside a transaction.
		 * Therefore we do not support this.
		 */ 
		throw new RuntimeException("Not implemented.");
	}
}
