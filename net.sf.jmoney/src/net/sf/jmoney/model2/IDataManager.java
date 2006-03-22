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

import org.eclipse.core.runtime.IAdaptable;

/**
 * An interface to an object that manages a view on the data.
 * This is a base interface that is extended by ISessionManager to
 * manage a view of data committed to a datastore and is also
 * extended by ITransactionManager to manage a view of uncommitted data.
 */
public interface IDataManager extends IAdaptable {
	/**
	 * This method is called when a transaction is about to start.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to start a transaction should be put in the implementation
	 * of this method.
	 * <P>
	 * The framework will always call this method, then make changes to
	 * the datastore, then call <code>commitTransaction</code> within
	 * a single function call.  The framework also ensures that no events
	 * are fired between the call to <code>startTransaction</code> and
	 * the call to <code>commitTransaction</code>.  The implementation of
	 * this method thus has no need to support or guard against nested
	 * transactions.
	 * 
	 * @see commitTransaction
	 */
	void startTransaction();

	/**
	 * This method is called when a transaction is to be committed.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to commit the transaction should be put in the implementation
	 * of this method.
	 * 
	 * @see startTransaction
	 */
	void commitTransaction();

	/** Returns the session object.  The session object must be
	 * non-null.
	 * 
	 * @return the session object
	 */
	Session getSession();
	
	/**
	 * @param account
	 * @return
	 */
	boolean hasEntries(Account account);

	/**
	 * @param account
	 * @return
	 */
	Collection getEntries(Account account);
}
