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

import org.eclipse.ui.IWorkbenchWindow;

/**
 * This interface must be implemented by all plug-ins that implement a
 * datastore.  The methods in this interface are called only from the
 * framework.  They should not be called directly by other plug-ins.
 * <P>
 * There will always be one SessionManager object for each Session
 * object.
 */
public interface ISessionManager extends IDataManager {

    /** Returns a brief description of the data in the session.
     * This description should be, for example, the file name or the name
     * of the database where the data either is stored or will be stored
     * when saved.  The framework includes this brief description in
     * the main window title.
     *
     * Null may be returned.  (This may be done if, say, the data has
     * not yet been saved to a file).
     */
    String getBriefDescription();
    
    /**
     * Obtain confirmation and/or information from the user so that this
     * session can be saved.
     * 
     * Under certain circumstances, the framework may want to close a session.
     * For example, when the user opens a new session or when the user exits
     * the framework.  A session may need more information from the user before
     * it can be saved (for example, a session that is serialized to a file may
     * not yet have a file name associated with it and so the user must enter
     * a file name before the session can be saved).  A session implementation
     * may also want the user to confirm before a session is closed.
     * <P>
     * This method ensures that all data for the session is saved.  However,
     * the session is still open after
     * this method has been called.  This allows an plug-in action to ensure
     * a session is saved before starting the process of creating a new
     * session but still be able to either keep the old session in the event of a failure
     * or copy data from the previous session to the new session. 
     */
    boolean canClose(IWorkbenchWindow window);

    /**
     * Close the datastore.  Once this method has been called, the session
     * object is not usable.
     *
     * This method should not, in general, contain any interfaces to the user.
     * If it does, then such user interfaces should not give the user any
     * opportunity to cancel the operation.
     */
    void close();

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
}
