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

import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.model2.DataManager;
import net.sf.jmoney.model2.Entry;

import org.eclipse.core.runtime.Assert;

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
		return entry.getTransaction().getEntryCollection().size() >= 3;
	}

	/**
	 * @return
	 */
	public Entry getOtherEntry() {
		Assert.isTrue(!hasSplitEntries());
		return buildOtherEntriesList().get(0);
	}

	/**
	 * @return
	 */
	public Collection<Entry> getSplitEntries() {
		return buildOtherEntriesList();
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
	private ArrayList<Entry> buildOtherEntriesList() {
		ArrayList<Entry> otherEntries = new ArrayList<Entry>();
			for (Entry entry2 : entry.getTransaction().getEntryCollection()) {
				if (!entry2.equals(entry)) {
					otherEntries.add(entry2);
				}
			}
			return otherEntries;
	}
}
