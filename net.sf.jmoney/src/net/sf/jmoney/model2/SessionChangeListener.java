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
    void sessionReplaced(Session oldSession, Session newSession);
	
	/**
	 * A scalar property in the session object has changed.
	 */
    void sessionPropertyChange(String propertyName, Object oldValue, Object newValue);
	
	/**
	 * An account has been added.  The account may be a top-level
	 * account or a sub-account.
	 */
    void accountAdded(Account newAccount);

    /**
	 * An account has been deleted.  The account may be a top-level
	 * account or a sub-account.
	 */
    void accountDeleted(Account oldAccount);

    /**
	 * The properties of an account have been changed.  
	 * The account may be a top-level account or a sub-account
	 * and may be a capital account or a category
	 * (income and expense) account.
	 */
    void accountChanged(Account account, PropertyAccessor propertyAccessor, Object oldValue, Object newValue);

    /**
	 * An entry has been added.  Either the entry has been added to
	 * an existing transaction, or a new transaction has been added.
	 * If a new transaction is added then an entryAdded event is
	 * fired for each entry in the transaction.
	 */
    void entryAdded(Entry newEntry);

    /**
	 * An entry has been deleted.  Either the entry has been deleted
	 * from a transaction, or an entire transaction has been deleted.
	 * If a transaction is deleted then an entryDeleted event is
	 * fired for each entry in the transaction.
	 */
    void entryDeleted(Entry oldEntry);

	/**
	 * An extendable object has been added.
	 * <P>
	 * This method is called for all extendable objects added
	 * to the datastore, even if one of the other methods
	 * is also called.
	 *
	 * @param extendableObject
	 */
	void objectAdded(ExtendableObject extendableObject);

	/**
	 * An extendable object has been deleted.
	 */
    void objectDeleted(ExtendableObject extendableObject);

    /**
	 * A scalar property in an extendable object
	 * has been changed.
	 */
    void objectChanged(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object oldValue, Object newValue);

}
