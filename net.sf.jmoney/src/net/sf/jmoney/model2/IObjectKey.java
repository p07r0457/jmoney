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
 * Interface into a key object that holds the data required to
 * obtain an extendable data object.
 * 
 * JMoney data storage is implemented in plug-ins.  This allows
 * a choice of methods of data storage.  Some data storage implementations
 * may load the entire database into memory when the datastore is opened.
 * An example of this type of datastore plug-in is the XML serialized data
 * storage plug-in.  Other plug-ins, however, may only load data on demand.
 * An example is a plug-in that stores the data in a JDBC database.
 * In this case, an <code>Entry</code> object may only be loaded into storage
 * when the user requests an account entry list view of the account in
 * which the <code>Entry</code> occurs.
 * <P>
 * In order to support this, the property set implementations do not
 * directly contain references to their object properties.
 * Instead, they contain a reference to an object that
 * implements the <code>IObjectKey</code> interface.  The
 * object key object implements the <code>getObject</code>
 * method which returns a reference to the actual extendable 
 * object.  This method should be called when and only when
 * a reference to the property object is required.  The method
 * should be called by the getter for the property.
 * <P>
 * This interface is not used for properties that are lists
 * (regardless of whether the list is a list of objects or
 * a list of scalar values).  List properties use the
 * <code>AbstractCollection</code> interface.
 * 
 * @see AbstractCollection
 * @author Nigel Westbury
 */
public interface IObjectKey {
	IExtendableObject getObject();
}
