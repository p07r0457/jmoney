/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

import java.beans.PropertyChangeListener;
import java.util.Iterator;

/**
 * The data model for an account.
 */
public interface CapitalAccount extends Account {
	
	/**
	 * @return the currency held in this account.
	 */
	Currency getCurrency();
	
	/**
	 * @return the bank name of this account.
	 */
	String getBank();
	
	/**
	 * @return the account number of this account.
	 */
	String getAccountNumber();
	
	/**
	 * @return the initial balance of this account.
	 */
	long getStartBalance();
	
	/**
	 * @return the minimal balance of this account.
	 */
	Long getMinBalance();
	
	/**
	 * @return the abbrevation of this account.
	 */
	String getAbbreviation();
	
	/**
	 * @return the comment of this account.
	 */
	String getComment();
	
	/**
	 * @return An iterator that returns the entries of this account.
	 */
	Iterator getEntriesIterator(Session session);
	
	/**
	 * Adds a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	// TODO clear out all of these???
	void addPropertyChangeListener(PropertyChangeListener pcl);
	
	/**
	 * Removes a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	void removePropertyChangeListener(PropertyChangeListener pcl);
	
	MutableCapitalAccount createNewSubAccount(Session session) throws ObjectLockedForEditException;
	
	MutableCapitalAccount createMutableAccount(Session session) throws ObjectLockedForEditException;
	
	// Helper methods.
	boolean hasEntries(Session session);
}
