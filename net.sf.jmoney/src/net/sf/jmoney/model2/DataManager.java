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
import java.util.Vector;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;

/**
 * An interface to an object that manages a view on the data.
 * This is a base interface that is extended by ISessionManager to
 * manage a view of data committed to a datastore and is also
 * extended by ITransactionManager to manage a view of uncommitted data.
 */
public abstract class DataManager implements IAdaptable {
	
    private Vector<SessionChangeListener> sessionChangeListeners = new Vector<SessionChangeListener>();

    private Vector<SessionChangeFirerListener> sessionChangeFirerListeners = new Vector<SessionChangeFirerListener>();

	private boolean sessionFiring = false;

	/**
     * Adds the listener to the collection of listeners who will be notified
     * when a change is made to the version of the datastore as seen through
     * this data manager.  Notifications will be sent when either a
     * change is committed to this data view by a transaction manager
     * or an uncommitted change is made through this data manager.
     * <P>
     * When listening for changes to a datastore, there are two options.
     * If the listener is interested only in receiving committed changes
     * then the listener should listen to the Session object or the JMoneyPlugin
     * object.  However, if a listener wants to be notified of changes
     * made through a transaction manager, even though those changes are
     * not committed to the datastore, then the listener should add the
     * listener to the transaction manager using this method.
     * <P>
     * The listener will not recieve any notification at the time a transaction
     * is committed as the listener will already have been notified of the
     * changes.  Note that there is no support for rollbacks as the transaction
     * manager can be just dropped (and garbage collected) without ever having been
     * committed, so getting a notification for a change that is never committed is
     * not an issue.  Views should do a full refresh if they change the data manager
     * through which they are obtaining the data to be shown.
	 */
	public void addSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.add(l);
    }
    
	/**
	 * Adds a change listener.
	 * <P>
	 * The listener is active only for as long as the given control exists.  When the
	 * given control is disposed, the listener is removed and will receive no more
	 * notifications.
	 * <P>
	 * This method is generally used when a listener is used to update contents in a
	 * control.  Typically multiple controls are updated by a listener and the parent
	 * composite control is passed to this method.
	 * 
	 * @param listener
	 * @param control
	 */
	public void addSessionChangeListener(final SessionChangeListener listener, Control control) {
        sessionChangeListeners.add(listener);
        
		// Remove the listener when the given control is disposed.
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				sessionChangeListeners.remove(listener);
			}
		});
    }
    
    /**
     * Removes the listener from the collection of listeners who will be notified
     * when a change is made to the version of the datastore as seen through
     * this transaction manager.
     */
    public void removeSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.remove(l);
    }
    
    public void addSessionChangeFirerListener(SessionChangeFirerListener l) {
        sessionChangeFirerListeners.add(l);
    }
    
    public void removeSessionChangeFirerListener(SessionChangeFirerListener l) {
        sessionChangeFirerListeners.remove(l);
    }
    
    /**
     * Send change notifications to all listeners who are listening for
     * changes to the version of the datastore as seen through this
     * data manager.
     * <P>
     * In practice it is likely that the only listener will be the
     * JMoneyPlugin object.  Views should all listen to the JMoneyPlugin
     * class for changes to the model.  The JMoneyPlugin object will pass
     * on events from this session object.
     * <P>
     * Listeners may register directly with a session object.  However
     * if they do so then they must re-register whenever the session
     * object changes.  If a viewer wants to listen for changes to a
     * session even if that session is not the session currently shown
     * in the workbench then it should register with the session object,
     * but if the viewer wants to be told about changes to the current
     * workbench window then it should register with the JMoneyPlugin
     * object.
     */
    public void fireEvent(ISessionChangeFirer firer) {
    	sessionFiring = true;
    	
    	// Notify listeners who are listening to us using the
    	// SessionChangeFirerListener interface.
        if (!sessionChangeFirerListeners.isEmpty()) {
        	// Take a copy of the listener list.  By doing this we
        	// allow listeners to safely add or remove listeners.
        	SessionChangeFirerListener listenerArray[] = new SessionChangeFirerListener[sessionChangeFirerListeners.size()];
        	sessionChangeFirerListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		listenerArray[i].sessionChanged(firer);
        	}
        }
    	
    	// Notify listeners who are listening to us using the
    	// SessionChangeListener interface.
        if (!sessionChangeListeners.isEmpty()) {
        	// Take a copy of the listener list.  By doing this we
        	// allow listeners to safely add or remove listeners.
        	SessionChangeListener listenerArray[] = new SessionChangeListener[sessionChangeListeners.size()];
        	sessionChangeListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		firer.fire(listenerArray[i]);
        	}
        }

        sessionFiring = false;
    }

    /**
     * This method is used by plug-ins so that they know if
     * code is being called from within change notification.
     *
     * It is important for plug-ins to know this.  Plug-ins
     * MUST NOT change the session data while a listener is
     * being notified of a change to the datastore.
     * This can happen very indirectly.  For example, suppose
     * an account is deleted.  The navigation view's listener
     * is notified and so removes the account's node from the
     * navigation tree.  If an account properties panel is
     * open, the panel is destroyed.  Because the panel is
     * being destroyed, the control that had the focus is sent
     * a 'focus lost' notification.  The 'focus lost' notification
     * takes the edited data from the control and writes it to
     * the datastore.
     * <P>
     * Writing data to the datastore during session change notifications
     * can cause serious problems.  The data may conflict.  The
     * undo/redo operations are almost impossible to manage.
     * In the above scenario with the deleted account, an attempt
     * is made to update a property for an object that has been
     * deleted.  The problems are endless.
     * <P>
     * It would be good if the datastore simply ignored such changes.
     * This would provide more robust support for plug-ins, and plug-ins
     * would not have to test this flag.  However, for the time being,
     * plug-ins must test this flag and avoid making changes when this
     * flag is set.  Plug-ins only need to do this in focus lost events
     * as that is the only time I can think of where this problem may
     * occur.
     *  
     * @return true if the session is notifying listeners of
     * 			a change to the session data, otherwise false
     */
    // TODO: Revisit this, especially the last paragraph above.
    public boolean isSessionFiring() {
    	return sessionFiring;
    }

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
	public abstract void startTransaction();

	/**
	 * This method is called when a transaction is to be committed.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to commit the transaction should be put in the implementation
	 * of this method.
	 * 
	 * @see startTransaction
	 */
	public abstract void commitTransaction();

	/** Returns the session object.  The session object must be
	 * non-null.
	 * 
	 * @return the session object
	 */
	public abstract Session getSession();
	
	/**
	 * @param account
	 * @return
	 */
	public abstract boolean hasEntries(Account account);

	/**
	 * @param account
	 * @return
	 */
	public abstract Collection getEntries(Account account);
}
