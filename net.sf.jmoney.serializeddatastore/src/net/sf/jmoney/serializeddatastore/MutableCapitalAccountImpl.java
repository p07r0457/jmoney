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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Iterator;

import net.sf.jmoney.model2.*;

/**
 *
 * @author  Nigel
 */
public class MutableCapitalAccountImpl extends ExtendableObjectHelperImpl implements MutableCapitalAccount {
	
	private CapitalAccountImpl account;
	
	private CapitalAccountImpl parent;
	
	private SessionImpl session;
	
	private String name;
	
	protected Currency currency;
	
	protected String bank = null;
	
	protected String accountNumber = null;
	
	protected long startBalance = 0;
	
	protected Long minBalance = null;
	
	protected String abbreviation = null;
	
	protected String comment = null;
	
	protected transient PropertyChangeSupport changeSupport =
		new PropertyChangeSupport(this);

	/** 
	 * Creates an instance of MutableCapitalAccount that is editing a new top level account
	 */
	public MutableCapitalAccountImpl(Session session) {
		this.session = (SessionImpl)session;
		this.parent = null;
		this.account = null;
		this.currency = this.session.getDefaultCurrency();
	}
	
	/** 
	 * Creates an instance of MutableCapitalAccount that is editing an account
	 * that has already been committed to the database.
	 */
	public MutableCapitalAccountImpl(Session session, CapitalAccount account) {
		this.session = (SessionImpl)session;
		this.parent = (CapitalAccountImpl)account.getParent();
		this.account = (CapitalAccountImpl)account;
		
		this.name = account.getName();
		this.currency = account.getCurrency();
		this.bank = account.getBank();
		this.accountNumber = account.getAccountNumber();
		this.startBalance = account.getStartBalance();
		this.minBalance = account.getMinBalance();
		this.abbreviation = account.getAbbreviation();
		this.comment = account.getComment();
	}
	
	/** 
	 * Creates an instance of MutableCapitalAccount that is editing a new sub-account
	 * of an account that has already been committed to the database.
	 *
	 * Note that the sub-account takes by default the same currency as the
	 * parent account.
	 */
	public MutableCapitalAccountImpl(Session session, CapitalAccount parent, int dummy) {
		this.session = (SessionImpl)session;
		this.parent = (CapitalAccountImpl)parent;
		this.account = null;
		this.currency = parent.getCurrency();
	}
	
	protected boolean isMutable() {
		return true;
	}

	protected IExtendableObject getOriginalObject() {
		return account;
	}
	
	protected String getExtendablePropertySetId() {
		// This method should be called when loading data from the datastore.
		// Mutable objects are not used at that time so this method should
		// never be called.
		throw new RuntimeException("should never be called");
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the name of this account.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the default currency for this account.
	 */
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
		bank = aBank;
	}
	
	/**
	 * Sets the account number of this account.
	 * @param anAccountNumber the account number
	 */
	public void setAccountNumber(String anAccountNumber) {
		accountNumber = anAccountNumber;
	}
	
	/**
	 * Sets the initial balance of this account.
	 * @param s the start balance
	 */
	public void setStartBalance(long s) {
		startBalance = s;
	}
	
	/**
	 * @param m the minimal balance which may be null.
	 */
	public void setMinBalance(Long m) {
		minBalance = m;
	}
	
	/**
	 * @param anAbbrevation the abbrevation of this account.
	 */
	public void setAbbreviation(String anAbbreviation) {
		abbreviation = anAbbreviation;
	}
	
	/**
	 * @param aComment the comment of this account.
	 */
	public void setComment(String aComment) {
		comment = aComment;
	}
	
	// Or perhaps this implementation is not going to be thread safe???
	public CapitalAccount commit() {
		// TODO: Ensure that the vector class is thread safe.
		// The database must be in a valid state at all times, which
		// means updates and reads must be synchronized.
		if (account == null) {
			account = new CapitalAccountImpl();
			account.setName(name);
			account.setCurrency(currency);
			account.setBank(bank);
			account.setAccountNumber(accountNumber);
			account.setStartBalance(startBalance);
			account.setMinBalance(minBalance);
			account.setAbbreviation(abbreviation);
			account.setComment(comment);
			
			// We add the account now that its properties are set.
			// A single event
			// will be fired which notifies listeners of the new account.
			session.addAccount(account);
		} else {
			// TODO: Figure out how we manage event firing for property
			// changes.
			account.setName(name);
			account.setCurrency(currency);
			account.setBank(bank);
			account.setAccountNumber(accountNumber);
			account.setStartBalance(startBalance);
			account.setMinBalance(minBalance);
			account.setAbbreviation(abbreviation);
			account.setComment(comment);
		}
		
		
		// remove this mutable object from the map.
//		EditLockMap.remove(lockedObject);
		
		// Mark this object as now being unusable.
//		isDead = true;
		
		return account;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.CapitalAccount#getEntriesIterator(net.sf.jmoney.model2.Session)
	 */
	public Iterator getEntriesIterator(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Adds a PropertyChangeListener.
	 * This method allows property edit controls to listen
	 * for changes to other properties and recieve notifications
	 * when the property value is set in the mutable object
	 * without having to wait for the mutable object to be
	 * committed.
	 * 
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


	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.CapitalAccount#createNewSubAccount(net.sf.jmoney.model2.Session)
	 */
	public MutableCapitalAccount createNewSubAccount(Session session) throws ObjectLockedForEditException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.CapitalAccount#createMutableAccount(net.sf.jmoney.model2.Session)
	 */
	public MutableCapitalAccount createMutableAccount(Session session) throws ObjectLockedForEditException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.CapitalAccount#hasEntries(net.sf.jmoney.model2.Session)
	 */
	public boolean hasEntries(Session session) {
		return account.hasEntries(session);
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.Account#getParent()
	 */
	public Account getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.Account#getSubAccountIterator()
	 */
	public Iterator getSubAccountIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.Account#getFullAccountName()
	 */
	public String getFullAccountName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* not needed until multi-threading taken into consideration    
	 public void rollback() {
	 // remove this mutable object from the map.
	  EditLockMap.remove(lockedObject);
	  }
	*/
}
