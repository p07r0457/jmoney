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

package net.sf.jmoney.pages.entries;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;

/**
 * All classes that implement a view of a list of account entries
 * must implement this interface.
 *
 * @author Nigel Westbury
 */
public interface IEntriesControl {
	
	/**
	 * @param selection
	 */
	void setSelection(Entry entryInAccount, Entry entryToSelect);
	
	/**
	 * 
	 * 
	 * @return an entry that has the account property set to the
	 * 		account for this page
	 */
	Entry getSelectedEntryInAccount();
	
	/**
	 * 
	 * 
	 * @return the selected entry
	 */
	Entry getSelectedEntry();
	
	/**
	 * 
	 */
	void dispose();
	
	/**
	 * This method is called when a substantial change has
	 * been made to data that affects the entries list.
	 * The view should be fully refreshed. 
	 */
	void refresh();
	
	/**
	 * This method is called whenever a property in an
	 * <code>Entry</code> property is changed.
	 * <P>
	 * Note that when the account property is changed and the
	 * account is either changed from or changed to the account
	 * for this page, then the removeTransaction or addTransaction
	 * method will be called in addition to this method.
	 * This method thus need be concerned only with updating the
	 * display of the account in the table and need not be concerned
	 * with new transactions to be displayed in the table.
	 * <P>
	 * The two entry parameters may be the same entry.
	 * 
	 * @param entryInAccount The entry in the account being listed
	 * @param entryChanged The entry whose property is changed 
	 */
	void updateEntry(Entry entryInAccount, Entry entryChanged, PropertyAccessor propertyAccessor, Object oldValue, Object newValue);
	
	/**
	 * This method is called whenever a new transaction is added
	 * to the datastore and that transaction contains an entry
	 * in the account for this page.  If the transaction contains
	 * more than one entry in the account then this method will be
	 * called multiple times.
	 * 
	 * @param entry Entry of the transaction that is the entry
	 * 		in the account being listed. 
	 */
	//????	void addEntryInAccount(Entry entry);
	
	/**
	 * @param entry Entry of the transaction that is the entry
	 * 		in the account being listed. 
	 */
	void removeEntryInAccount(Entry entry);
	
	/**
	 * When a transaction property changes, we notify the entries list
	 * control once for each entry in the transaction where the account
	 * of the entry is the account for the entries list.
	 * 
	 * @param entry Entry of the transaction that is the entry
	 * 		in the account being listed. 
	 */
	void updateTransaction(Entry entry);
	
	/**
	 * @param entry
	 * @param newEntry
	 */
	void addEntry(Entry entryInAccount, Entry newEntry);
	
	/**
	 * @param entryInAccount
	 * @param oldEntry
	 */
	void removeEntry(Entry entryInAccount, Entry oldEntry);
	
	/**
	 * @param tableSelectionListener
	 */
	void addSelectionListener(SelectionListener tableSelectionListener);
	
	/**
	 * @return
	 */
	Control getControl();
}
