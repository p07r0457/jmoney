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

import java.util.EventListener;

import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Listener interface for addition and deletion of <code>Entry</code>
 * objects.  Entry objects are added to a session when either a transaction
 * is added to the session or when an existing transaction has further
 * entries added to it.  In both cases, an <code>EntryAddedEvent</code>
 * will be fired.  Likewise, entry objects are deleted from a session when either a transaction
 * is deleted from the session or when an existing transaction has entries
 * deleted from it.  In both cases, an <code>EntryDeletedEvent</code>
 * will be fired.
 *
 * @author  Nigel Westbury
 */
public interface SessionChangeListener extends EventListener {
	/**
	 * The session has been replaced.  All views of session data
	 * should be fully refreshed.
	 */
    void sessionReplaced(SessionReplacedEvent event);
	
	/**
	 * A scalar property in the session object has changed.
	 */
    void sessionPropertyChange(String propertyName, Object oldValue, Object newValue);
	
	/**
	 * An account has been added.  The account may be a top-level
	 * account or a sub-account.
	 */
    void accountAdded(AccountAddedEvent event);

    /**
	 * An account has been deleted.  The account may be a top-level
	 * account or a sub-account.
	 */
    void accountDeleted(AccountDeletedEvent event);

    /**
	 * The properties of an account have been changed.  
	 * The account may be a top-level account or a sub-account
	 * and may be a capital account or a category
	 * (income and expense) account.
	 */
    void accountChange(PropertyChangeEvent event);

    /**
	 * An entry has been added.  Either the entry has been added to
	 * an existing transaction, or a new transaction has been added.
	 * If a new transaction is added then an entryAdded event is
	 * fired for each entry in the transaction.
	 */
    void entryAdded(EntryAddedEvent event);

    /**
	 * An entry has been deleted.  Either the entry has been deleted
	 * from a transaction, or an entire transaction has been deleted.
	 * If a transaction is deleted then an entryDeleted event is
	 * fired for each entry in the transaction.
	 */
    void entryDeleted(EntryDeletedEvent event);
}
