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
 * Interface into a key object that holds the data required to
 * obtain an extendable data object.
 * <P>
 * JMoney data storage is implemented in plug-ins.  This allows
 * a choice of methods of data storage.  Some data storage implementations
 * may load the entire database into memory when the datastore is opened.
 * An example of this type of datastore plug-in is the XML serialized data
 * storage plug-in.  Other plug-ins, however, may only load data on demand.
 * An example is a plug-in that stores the data in a JDBC database.
 * The JDBC database plug-in, for example, constructs an <code>Entry</code> object 
 * only when the user requests an account entry list view of the account in
 * which the <code>Entry</code> occurs.
 * <P>
 * In order to support this, the data model implementation classes do not
 * directly contain references to other classes in the data model.
 * Instead, they contain a reference to an object that
 * implements the <code>IObjectKey</code> interface.  The
 * object key implements the <code>getObject</code>
 * method which returns a reference to the actual extendable 
 * object.  This method should be called when and only when
 * a reference to the property object is required.  The <code>getObject</code> method
 * should be called by the getter for the property.
 * <P>
 * This interface is not used for properties that are lists
 * (regardless of whether the list is a list of objects or
 * a list of scalar values).  List properties use the
 * <code>IListManager</code> interface.
 * <P>
 * This interface is designed to be implemented only by plug-ins
 * that implement a datastore.  Most plug-ins do not need to be
 * aware of this interface or need to call any of the methods in it.
 * Plug-ins that implement property sets that contain properties that
 * reference other data model objects must be aware of this interface.
 * 
 * @see IListManager
 * @author Nigel Westbury
 */
public interface IObjectKey {
	/**
	 * Returns a reference to the actual extendable 
	 * object.  This method should be called when and only when
	 * a reference to the property object is required.  The <code>getObject</code> method
	 * should be called by the getter for the property.
	 * <P>
	 * Implementations of this method may construct the object
	 * every time the method is called.  Construction of the object
	 * may require reading data from a database.  Users of this method
	 * must therefore be aware that
	 * <LI>
	 * <UL>This method may not be efficient.  Users of this method should
	 * 		therefore cache values returned by this method.
	 * </UL>
	 * <UL>This method may return an object with a different java identity
	 * 		each time it is called.  However, every call of this method on
	 * 		a given IObjectKey object will return objects that all are
	 * 		equal when using the <code>equals</code> method.
	 * </UL>
	 * <UL>This method is used when getting property values using the getter methods.
	 * 		users of getter methods that return references to objects in the data model
	 * 		must therefore also be aware of the above two points.
	 * </UL>
	 * @return a reference to the actual extendable object
	 */
	ExtendableObject getObject();

	/**
	 * This method creates an object that 
	 * For example, the list of entries in a given account is often required.
	 * Entries are not owned by the account but entries are owned by the transaction
	 * and the transactions are owned by the session.  Therefore to do a  
	 * 
	 * @see index documentation
	 * @param accountAccessor
	 * @return
	 */
	Collection createIndexValuesList(PropertyAccessor propertyAccessor);

	/**
	 * Set the given property values in the datastore.
	 * <P>
	 * This method does not update the values in the object
	 * itself.  That must be done by the caller after this
	 * method is called.  This method may read the old values
	 * from the object itself to determine which values
	 * have changed.
	 *  
	 * @param oldValues An object from which the old values of the properties may
	 * 			be obtained by calling the getter method for each property.
	 * @param newValues An array containing the new property values.  The elements
	 * 			of this array must match one to one the scalar properties of the object
	 * 			and in the correct order.  Intrinsic properties (int, long etc) must
	 * 			be passed as objects (Integer, Long etc).
	 * @param extensionProperties
	 */
	void updateProperties(PropertySet actualPropertySet, Object[] oldValues, Object[] newValues);

	/**
	 * @param propertySet
	 * @param propertyAccessor
	 * @param oldValue
	 * @param newValue
	 */
	void updateProperties(PropertySet actualPropertySet, PropertyAccessor propertyAccessor, Object oldValue, Object newValue);

	/**
	 * Returns the session object.  Objects often need access to the session object.
	 * For example, the CapitalAccount constructor sets the currency to the default
	 * currency for the session if a null currency is passed.
	 * <P>
	 * The datastore implementation usually require that the object keys have access
	 * to the session manager.  It is therefore very easy to provide the session
	 * through this method, and this saves a lot of code and fields in the 
	 * object implementation classes to keep track of the session.
	 * <P>
	 * WARNING: Be very careful about the use of this method.
	 * The session will not be set until it is constructed.
	 * Therefore any code that may be called during the initial
	 * construction of a session may not call this method.
	 * This includes extendable object constructors that are being
	 * called to construct an object store from a datastore.
	 * It does not include the constructor when adding a new object.
	 * As these are the same constructors, this is very confusing.
	 *   
	 * @return
	 */
	Session getSession();

	/**
	 * @return
	 */
	ISessionManager getSessionManager();
}
