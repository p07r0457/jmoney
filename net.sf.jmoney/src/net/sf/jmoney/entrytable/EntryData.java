/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sf.net>
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

package net.sf.jmoney.entrytable;

import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.DataManager;
import net.sf.jmoney.model2.Entry;

/**
 * Class representing a top level entry in the list.
 * <P>
 * Note that, despite the name of this class, it is entries and
 * not transactions that are listed.  For example, if a transaction
 * has two entries in the account then that transaction will appear
 * twice in the list.
 */
public class EntryData {
	private Entry entry;
	private DataManager dataManager;

	private long balance;
	private int index;

	/**
	 * A cache of the entries in this transaction excluding
	 * the entry itself.
	 */
	private Vector<Entry> otherEntries = null;

	/**
	 * @param entry
	 *            the entry to be edited, or null if a new entry is to be
	 *            created
	 * @param dataManager
	 *            the datastore manager into which the entry will be committed,
	 *            which must be the same as the datastore manager for the entry
	 *            parameter if the entry parameter is non-null
	 */
	public EntryData(Entry entry, DataManager dataManager) {
		this.entry = entry;
		this.dataManager = dataManager;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * A transaction with split entries is a transaction that
	 * has entries in three or more accounts (where each account
	 * may be either a capital account or an income and
	 * expense category).
	 */
	public boolean hasSplitEntries() {
		buildOtherEntriesList();
		return otherEntries.size() > 1;
	}

	/**
	 * A double entry transaction is a transaction with two
	 * entries (a credit entry and a debit entry) and where
	 * both entries are capital accounts.
	 * <P>
	 * A double entry is not a special case as far as the model
	 * is concerned.  However, it is a special case as far
	 * as the entries list is concerned because such transactions
	 * are displayed on two lines.  The reason why the transaction
	 * must take two lines is that both entries will have
	 * capital account properties such as the value date
	 * (valuta) and a memo and thus needs two lines to display.
	 */
	public boolean isDoubleEntry() {
		buildOtherEntriesList();
		return otherEntries.size() == 1
				&& otherEntries.firstElement().getAccount() instanceof CapitalAccount;
	}

	/**
	 * A simple entry is a transaction that contains two
	 * entries, one being a capital account and the other
	 * being an income or expense account.  Most transactions
	 * are simple entries.
	 * <P>
	 * Note that jmoney requires all transactions to have
	 * at least two entries.  Therefore, with the rare exception
	 * of a transaction that has two entries both of which
	 * are income and expense accounts, all transactions will
	 * be either a split transaction, a double entry, or a
	 * simple entry.
	 * <P>
	 * The user may not have yet selected the category for a
	 * simple transaction.  Such a transaction is displayed on
	 * a single line and therefore considered a simple transaction.
	 * Therefore the test to be used here is that the category is NOT a
	 * capital account (rather than testing FOR an income and
	 * expense account. 
	 */
	public boolean isSimpleEntry() {
		buildOtherEntriesList();
		return otherEntries.size() == 1
				&& !(otherEntries.firstElement().getAccount() instanceof CapitalAccount);
	}

	/**
	 * @return
	 */
	public Entry getOtherEntry() {
		buildOtherEntriesList();
		JMoneyPlugin.myAssert(isSimpleEntry());
		return otherEntries.firstElement();
	}

	/**
	 * @return
	 */
	public Collection<Entry> getSplitEntries() {
		buildOtherEntriesList();
		return otherEntries;
	}

	public long getBalance() {
		return balance;
	}

	/**
	 * @param balance
	 */
	public void setBalance(long balance) {
		this.balance = balance;
	}

	public Entry getEntry() {
		return entry;
	}

	public DataManager getBaseSessionManager() {
		return dataManager;
	}

	/**
	 * Database reads may be necessary when getting the other entries.
	 * Furthermore, these are not needed unless an entry becomes visible. These
	 * are therefore fetched only when needed (not in the constructor of this
	 * object).
	 * 
	 * We must be careful with this cached list because it is not kept up to date.
	 */
	private void buildOtherEntriesList() {
		if (otherEntries == null) {
			otherEntries = new Vector<Entry>();
			for (Entry entry2 : entry.getTransaction().getEntryCollection()) {
				if (!entry2.equals(entry)) {
					otherEntries.add(entry2);
				}
			}
		}
	}

	/**
	 * This method is used when a new entry is committed.  This 
	 * object is updated to show that it now represents a committed
	 * entry.
	 * 
	 * @param committedEntry
	 */
	public void setEntry(Entry committedEntry) {
		this.entry = committedEntry;
	}
}
