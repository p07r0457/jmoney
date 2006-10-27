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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IListManager;

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
		Collection constructorProperties = propertySet.getDefaultConstructorProperties();
		
		JMoneyPlugin.myAssert (!propertySet.isExtension());

		int numberOfParameters = 3 + constructorProperties.size();
		Object[] constructorParameters = new Object[numberOfParameters];
		
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = null;
		constructorParameters[2] = parent.getObjectKey();
		
		// Construct the extendable object using the 'default' constructor.
		// This constructor takes the minimum number of parameters necessary
		// to properly construct the object, setting default values for all
		// the scalar properties.  We must, however, pass objects that manage
		// any lists within the object.
		
		// Add a list manager for each list property in the object.
		int index = 3;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); iter.next()) {
			constructorParameters[index++] = new UncommittedListManager(transactionManager);
		}
		
		// We can now create the object.
		F extendableObject = propertySet.constructDefaultImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}

	// TODO: Complete the implementation of this method.
	// The implementation may be copied from DeltaListManager.
	// (Even better, put the code into a common method to avoid
	// duplicating it).
	
	// This method is never used, because new objects are only created
	// with non-default values when objects are being committed.
	// If we support nested transactions then this method will be required.
	public <F extends E> F createNewElement(ExtendableObject parent, ExtendablePropertySet<F> propertySet, Object[] values/*, ExtensionProperties[] extensionProperties */) {
		Collection constructorProperties = propertySet.getDefaultConstructorProperties();
		
		JMoneyPlugin.myAssert (!propertySet.isExtension());

		int numberOfParameters = 3 + constructorProperties.size();
		Object[] constructorParameters = new Object[numberOfParameters];
		
		UncommittedObjectKey objectKey = new UncommittedObjectKey(transactionManager);
		
		constructorParameters[0] = objectKey;
		constructorParameters[1] = null;
		constructorParameters[2] = parent.getObjectKey();
		
		// Construct the extendable object using the 'default' constructor.
		// This constructor takes the minimum number of parameters necessary
		// to properly construct the object, setting default values for all
		// the scalar properties.  We must, however, pass objects that manage
		// any lists within the object.
		
		// Add a list manager for each list property in the object.
		int index = 3;
		for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); iter.next()) {
			constructorParameters[index++] = new UncommittedListManager(transactionManager);
		}
		
		// We can now create the object.
		F extendableObject = propertySet.constructDefaultImplementationObject(constructorParameters);
		
		objectKey.setObject(extendableObject);

		add(extendableObject);
		
		return extendableObject;
	}
}
