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

package net.sf.jmoney.serializeddatastore;

import net.sf.jmoney.model2.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.Iterator;

/**
 * The data model for an account.
 */
// Kludge:  This implements MutableCapitalAccount.
// You might think it should be implementing only CapitalAccount,
// and you would be right.  However, the setters are needed
// when reading the data from the XML.  Until this whole data
// storage mess is sorted out, we will leave this as is.
public class CapitalAccountImpl extends AbstractAccountImpl implements MutableCapitalAccount, Serializable {

	/**
	 * The entries are ordered by their creation.
	 */
	public static final int CREATION_ORDER = 0;

	/**
	 * The entries are ordered by their date field.
	 */
	public static final int DATE_ORDER = 1;

	/**
	 * The entries are ordered by their check field.
	 */
	public static final int CHECK_ORDER = 2;

	/**
	 * The entries are ordered by their valuta field.
	 */
	public static final int VALUTA_ORDER = 3;

	protected static String[] entryOrderNames;

	protected String name;

	protected Currency currency;

	protected String bank = null;

	protected String accountNumber = null;

	protected long startBalance = 0;

	protected Long minBalance = null;

	protected String abbreviation = null;

	protected String comment = null;

        /**
         * This list is maintained for efficiency only.
         * The master list is the list of transactions, with each
         * transaction containing a list of entries.
         */
	protected transient Vector entries = new Vector();

	protected transient PropertyChangeSupport changeSupport =
		new PropertyChangeSupport(this);

        /**
	 * Used by XMLEncoder.
	 */
	public CapitalAccountImpl() {
	}

	/**
	 * Creates a new account with the provided name
	 * @param aName the name of the account
	 */
	public CapitalAccountImpl(String aName, Currency defaultCurrency) {
		name = aName;
                currency = defaultCurrency;
	}

	protected boolean isMutable() {
		return false;
	}

	protected IExtendableObject getOriginalObject() {
		// This method should be called only if isMutable returns true,
		// which it never does.  However, we must provide an implementation.
		throw new RuntimeException("should never be called");
	}
	
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.account";
	}
	
	/**
	 * @return the name of this account.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the locale of this account.
	 */
	public String getCurrencyCode() {
		return currency.getCode();
	}

	public Currency getCurrency() {
            return currency;
	}

	/**
	 * @return the bank name of this account.
	 */
	public String getBank() {
		return bank;
	};

	/**
	 * @return the account number of this account.
	 */
	public String getAccountNumber() {
		return accountNumber;
	};

	/**
	 * @return the initial balance of this account.
	 */
	public long getStartBalance() {
		return startBalance;
	};

	/**
	 * @return the minimal balance of this account.
	 */
	public Long getMinBalance() {
		return minBalance;
	};

	/**
	 * @return the abbrevation of this account.
	 */
	public String getAbbreviation() {
		return abbreviation;
	};

	/**
	 * @return the comment of this account.
	 */
	public String getComment() {
		return comment;
	};

	/**
	 * @return An iterator that returns the entries of this account.
	 */
	public Iterator getEntriesIterator(Session session) {
		return entries.iterator();
	};

        public boolean hasEntries(Session session) {
            return entries.size() != 0;
        }

/* do not use these.  Add and remove transactions from the session.        
 */
	void addEntry(Entry entry) {
		entries.addElement(entry);
	}

	void removeEntry(Entry entry) {
		entries.removeElement(entry);
	}

/* do not use these.  Add and remove transactions from the session.        
	private void cleanupEntry(Entry entry) {
		if (entry instanceof DoubleEntry)
			 ((DoubleEntry) entry).removeOther();
		else if (entry instanceof SplittedEntry)
			 ((SplittedEntry) entry).removeAllEntries();
	}

	private void initEntry(Entry entry) {
		if (entry instanceof DoubleEntry) {
			DoubleEntry de = (DoubleEntry) entry;
			de.addOther();
			de.getOther().setCategory(this);
		}
	}

	public void replaceEntry(Entry oldEntry, Entry newEntry) {
		int index = entries.indexOf(oldEntry);
		cleanupEntry(oldEntry);
		initEntry(newEntry);
		entries.setElementAt(newEntry, index);
	}
*/
	/**
	 * Parses the amount field.
	 * @param amountString amount that has to be parsed.
	 * @return amount
	 */
/* moved into the Currency class        
	public long parseAmount(String amountString) {
		Number amount = new Double(0);
		try {
			amount = getCurrency().getNumberFormat().parse(amountString);
		} catch (ParseException pex) {
		}
		return Math.round(
			amount.doubleValue() * getCurrency().getScaleFactor());
	}
 */
/*
	public String formatAmount(long amount) {
		return getCurrency().format(amount);
	}
*/
	/**
	 * @param aName the name of this account.
	 */
	public void setName(String aName) {
		if (name != null && name.equals(aName))
			return;
		name = aName;
		changeSupport.firePropertyChange("name", null, name);
	}

        // TODO: Is this method quite what we want???
	public void fireBulkChange() {
		changeSupport.firePropertyChange("entries", null, entries);
	}
        
	public void setCurrency(Currency aCurrency) {
		if (currency == aCurrency)
			return;
                Currency oldCurrency = currency;
		currency = aCurrency;
		changeSupport.firePropertyChange("currency", oldCurrency, currency);
	}

	/**
	 * @param aBank the name of this account.
	 */
	public void setBank(String aBank) {
		if (bank != null && bank.equals(aBank))
			return;
		bank = aBank;
		changeSupport.firePropertyChange("bank", null, bank);
	}

	/**
	 * Sets the account number of this account.
	 * @param anAccountNumber the account number
	 */
	public void setAccountNumber(String anAccountNumber) {
		if (accountNumber != null && accountNumber.equals(anAccountNumber))
			return;
		accountNumber = anAccountNumber;
		changeSupport.firePropertyChange("accountNumber", null, accountNumber);
	}

	/**
	 * Sets the initial balance of this account.
	 * @param s the start balance
	 */
	public void setStartBalance(long s) {
		if (startBalance == s)
			return;
		startBalance = s;
		changeSupport.firePropertyChange("startBalance", null, new Long(s));
	}

	/**
	 * @param m the minimal balance which may be null.
	 */
	public void setMinBalance(Long m) {
		if (minBalance == m)
			return;
		minBalance = m;
		changeSupport.firePropertyChange("minBalance", null, m);
	}

	/**
	 * @param anAbbrevation the abbrevation of this account.
	 */
	public void setAbbreviation(String anAbbreviation) {
		if (abbreviation != null && abbreviation.equals(anAbbreviation))
			return;
		abbreviation = anAbbreviation;
		changeSupport.firePropertyChange("abbreviation", null, abbreviation);
	}

	/**
	 * @param aComment the comment of this account.
	 */
	public void setComment(String aComment) {
		if (comment != null && comment.equals(aComment))
			return;
		comment = aComment;
		changeSupport.firePropertyChange("comment", null, comment);
	}

	/**
	 * Adds a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.addPropertyChangeListener(pcl);
	}

	/**
	 * Removes a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.removePropertyChangeListener(pcl);
	}

	/**
	 * Sort the entries.
	 */
        // TODO: Sorting the entries affects the view of the data.
        // This sort should be done in the view, not the model, otherwise
        // one view might mess up another view of the data.
        // This must be reviewed if we move over to JDO in any case.
	public void sortEntries(Comparator c) {
		Collections.sort(entries, c);
	}

	public String toString() {
		return name;
	}

	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		changeSupport = new PropertyChangeSupport(this);
	}

	public String getFullAccountName() {
		return SerializedDatastorePlugin.getResourceString("TransferCategory.name")
			+ ":"
			+ getName();
	}

	public int compareTo(Object o) {
		CapitalAccount a = (CapitalAccount) o;
		return getName().compareTo(a.getName());
	}

        // TODO: Ensure no mutable interface on this object already.
        public MutableCapitalAccount createNewSubAccount(Session session) {
            return new MutableCapitalAccountImpl(session, this, 0);
        }
        
        // TODO: Ensure no mutable interface on this object already.
        public MutableCapitalAccount createMutableAccount(Session session) {
            return new MutableCapitalAccountImpl(session, this);
        }

		/* (non-Javadoc)
		 * @see net.sf.jmoney.model2.MutableCapitalAccount#commit()
		 */
		public CapitalAccount commit() {
			throw new RuntimeException("should never be called");
		}
	    
	    // This method is used by the datastore implementations.
	    // TODO: Should this be moved to a separate initialization interface?
		public void addSubAccount(CapitalAccount subAccount) {
			super.addSubAccount(subAccount);
		}
}
