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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CapitalAccountInfo;
import net.sf.jmoney.model2.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;

/**
 * The data model for an account.
 */
public class CapitalAccount extends Account {

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
	protected Collection entries;

	protected PropertyChangeSupport changeSupport =
		new PropertyChangeSupport(this);

	/**
	 * The full constructor for a CapitalAccount object.  This constructor is called
	 * only be the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public CapitalAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			String name,
			IListManager subAccounts,
			IObjectKey currencyKey,
			String bank,
			String accountNumber,
			long startBalance,
			Long minBalance,
			String abbreviation,
			String comment) {
		super(objectKey, extensions, parent, subAccounts);
		
		this.name = name;

		// This account is being loaded from the datastore and therefore a currency
		// must be set.  We store internally the Currency object itself, not the 
		// key used to fetch the Currency object.
		if (currencyKey == null) {
			this.currency = objectKey.getSession().getDefaultCurrency();
		} else {
			this.currency = (Currency)currencyKey.getObject();
		}
		
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.startBalance = startBalance;
        this.minBalance = minBalance;
        this.abbreviation = abbreviation;
        this.comment = comment;
        
        PropertyAccessor accountAccessor;
		try {
			accountAccessor = PropertySet.getPropertyAccessor("net.sf.jmoney.entry.account");
		} catch (PropertyNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("internal error");
		}
		
		this.entries = objectKey.createIndexValuesList(accountAccessor);
	}

	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.account";
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

        // These methods are used when maintaining the list
        // of entries in each account.
        // TODO: remove these methods when indexes are supported.
        
	public void addEntry(Entry entry) {
		entries.add(entry);
	}

	void removeEntry(Entry entry) {
		entries.remove(entry);
	}

	public void setCurrency(Currency aCurrency) {
		if (this.currency == aCurrency ||
				(this.currency != null && this.currency.equals(aCurrency)))
				return;
		
		// Notify the change manager.
		processPropertyChange(CapitalAccountInfo.getCurrencyAccessor(), this.currency, aCurrency);

        Currency oldCurrency = currency;
		currency = aCurrency;
		changeSupport.firePropertyChange("currency", oldCurrency, currency);
	}

	/**
	 * @param aBank the name of this account.
	 */

	public void setBank(String aBank) {
        String oldBank = this.bank;
		this.bank = aBank;

		// Notify the change manager.
		processPropertyChange(CapitalAccountInfo.getBankAccessor(), oldBank, aBank);
	}

	/**
	 * Sets the account number of this account.
	 * @param anAccountNumber the account number
	 */
	
	public void setAccountNumber(String anAccountNumber) {
        String oldAccountNumber = this.accountNumber;
        this.accountNumber = anAccountNumber;

		// Notify the change manager.
		processPropertyChange(CapitalAccountInfo.getAccountNumberAccessor(), oldAccountNumber, anAccountNumber);
	}

	/**
	 * Sets the initial balance of this account.
	 * @param s the start balance
	 */
	
	public void setStartBalance(long s) {
        long oldStartBalance = this.startBalance;
		this.startBalance = s;

		// Notify the change manager.
		processPropertyChange(CapitalAccountInfo.getStartBalanceAccessor(), new Long(oldStartBalance), new Long(s));
	}

	/**
	 * @param m the minimal balance which may be null.
	 */
	
	public void setMinBalance(Long m) {
        Long oldMinBalance = this.minBalance;
		this.minBalance = m;

		// Notify the change manager.
		processPropertyChange(CapitalAccountInfo.getMinBalanceAccessor(), oldMinBalance, m);
	}

	/**
	 * @param anAbbrevation the abbrevation of this account.
	 */
	
	public void setAbbreviation(String anAbbreviation) {
        String oldAbbreviation = this.abbreviation;
        this.abbreviation = anAbbreviation;

		// Notify the change manager.
        processPropertyChange(CapitalAccountInfo.getAbbreviationAccessor(), oldAbbreviation, anAbbreviation);
	}

	/**
	 * @param aComment the comment of this account.
	 */
	
	public void setComment(String aComment) {
        String oldComment = this.comment;
        this.comment = aComment;

		// Notify the change manager.
        processPropertyChange(CapitalAccountInfo.getCommentAccessor(), oldComment, aComment);
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
        // This must be reviewed if we support SQL databases in any case.
/*	
	public void sortEntries(Comparator c) {
		Collections.sort(entries, c);
	}
*/
	public String toString() {
		return name;
	}

	public String getFullAccountName() {
	    if (getParent() == null) {
		       return name;
		    } else {
		        return getParent().getFullAccountName() + "." + this.name;
		    }
	}

	public int compareTo(Object o) {
		CapitalAccount a = (CapitalAccount) o;
		return getName().compareTo(a.getName());
	}

	/**
	 * This version is required by the JMoney framework.
	 * 
	 * @param name
	 * @param extensionProperties
	 * @return
	 */
	public CapitalAccount createSubAccount() {
		CapitalAccount newSubAccount = (CapitalAccount)subAccounts.createNewElement(
				this, 
				JMoneyPlugin.getCapitalAccountPropertySet()); 

		processObjectAddition(subAccounts, newSubAccount);
		
		return newSubAccount;
	}
        
	static public Object [] getDefaultProperties() {
		return new Object [] { "new account", null, null, null, new Long(0), null, null, null };
	}
	
	/**
	 * Get the balance at a given date
	 * 
	 * @param date
	 * @return the balance
	 * @author Faucheux
	 */
	public long getBalance(Session session, Date fromDate, Date toDate) {
		System.out.println("Calculing the Balance for >" + name + "< (without sub-accounts) between " + fromDate + " and " + toDate);
		
		long bal = getStartBalance();
		Iterator eIt = null;
		
		// Sum each entry the entry between the two dates 
		eIt = entries.iterator();
		while (eIt.hasNext()) {
			Entry e = (Entry) eIt.next();
			if ((e.getTransaction().getDate().compareTo(fromDate) >= 0)
					&& e.getTransaction().getDate().compareTo(toDate) <= 0){
				bal += e.getAmount();
				
			}
		}
		
		return bal;
	}
	
	/**
	 * Get the balance between two dates , inclusive sub-accounts
	 * 
	 * @param date
	 * @return the balance
	 * @author Faucheux
	 */
	public long getBalanceWithSubAccounts(Session session, Date fromDate, Date toDate) {
		System.out.println("Calculing the Balance for >" + name + "< (with sub-accounts) between " + fromDate + " and " + toDate);
		long bal = getBalance(session, fromDate, toDate);
		
		Iterator aIt = getSubAccountIterator();
		
		while (aIt.hasNext()) {
			bal += ((CapitalAccount) aIt.next()).getBalanceWithSubAccounts(session, fromDate, toDate);
		}
		return bal;
	}
    	
}
