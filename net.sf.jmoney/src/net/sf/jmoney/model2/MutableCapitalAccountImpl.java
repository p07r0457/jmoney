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
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.model2.*;

import org.eclipse.jface.util.PropertyChangeEvent;

/**
 *
 * @author  Nigel
 */
public class MutableCapitalAccountImpl extends ExtendableObjectHelperImpl implements MutableCapitalAccount {
	
	private CapitalAccountImpl account;
	
	// changed by Olivier Faucheux:
	// was an "CapitalAccountImpl". Why?
	// changed to Account
	private Account parent;
	
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

	protected Vector children;
	
	/** 
	 * Creates an instance of MutableCapitalAccount that is editing a new top level account
	 */
	public MutableCapitalAccountImpl(Session session) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

    	this.session = (SessionImpl)session;
		this.parent = null;
		this.account = null;
		this.currency = this.session.getDefaultCurrency();
		this.children = new Vector();
	}
	
	/** 
	 * Creates an instance of MutableCapitalAccount that is editing an account
	 * that has already been committed to the database.
	 */
	public MutableCapitalAccountImpl(Session session, CapitalAccountImpl account) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

    	this.session = (SessionImpl)session;
    	Account parentAccount = account.getParent();
		this.parent = parentAccount;
		if (parent!=null) parent.addChild(this);
		this.account = account;
		
		this.name = account.getName();
		this.currency = account.getCurrency();
		this.bank = account.getBank();
		this.accountNumber = account.getAccountNumber();
		this.startBalance = account.getStartBalance();
		this.minBalance = account.getMinBalance();
		this.abbreviation = account.getAbbreviation();
		this.comment = account.getComment();
		this.children = new Vector();
	}
	
	/** 
	 * Creates an instance of MutableCapitalAccount that is editing a new sub-account
	 * of an account that has already been committed to the database.
	 *
	 * Note that the sub-account takes by default the same currency as the
	 * parent account.
	 */
	public MutableCapitalAccountImpl(Session session, CapitalAccount parent, int dummy) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

    	this.session = (SessionImpl)session;
		this.parent = (CapitalAccountImpl)parent;
		if (parent!=null) parent.addChild(this);
		this.account = null;
		this.currency = parent.getCurrency();
	}
	
    // Temp method.
    // No object key is set so create one now.
	public IObjectKey getObjectKey() {
    	return new IObjectKey() {
    		public IExtendableObject getObject() {
    		    // System.out.println("getObject called on " + this + " returns " + MutableCapitalAccountImpl.this.getName());
    			return MutableCapitalAccountImpl.this;
    		}
    		
    		public Collection createIndexValuesList(PropertyAccessor propertyAccessor) {
    			throw new RuntimeException("internal error");
    		}
    	};
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
		// TODO: Ensure extension properties are set.
		if (account == null) {
			PropertySet capitalAccountPropertySet;
			try {
				capitalAccountPropertySet = PropertySet.getPropertySet("net.sf.jmoney.capitalAccount");
			} catch (PropertySetNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
			account = (CapitalAccountImpl)session.createNewAccount(capitalAccountPropertySet, this);
//			account = new CapitalAccountImpl(null);
/* no longer needed because properties are set in the constructor			
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
*/			
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

	/**
	 * @author Faucheux
	 */
	public Account getParent() {
		if (parent instanceof MutableAccount) {
		    return ((MutableAccount) parent).getRealAccount();
		} else
		    return parent;
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

	// This method probably should not be here.  We can remove
	// it is we provide a seperate interface for object initialization.
	public void addSubAccount(CapitalAccount subAccount) {
		throw new RuntimeException("internal error");
	}
	
	/* not needed until multi-threading taken into consideration    
	 public void rollback() {
	 // remove this mutable object from the map.
	  EditLockMap.remove(lockedObject);
	  }
	*/
	
	/**
	 * set the parent of the account. If another parent has already been set, an error is generated.
	 * 
	 * @author Olivier Faucheux
	 */
	public void setParent (CapitalAccount newParent) {
	    Account oldParent = this.parent; 

	    
	    if (oldParent != null & oldParent != newParent) 
	        throw new Error ("The account " + name + " can't become the account " + newParent.getName() + " as parent: it already have a parent (" + oldParent + ").");
	    
	    parent = newParent;
		parent.addChild(this);
		System.out.println("Add a parent to " + this.getName());
	    account.parentKey = newParent.getObjectKey();

	    // Fire the event
		final PropertyChangeEvent event = new PropertyChangeEvent(account, "parent", oldParent, newParent);
		session.fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.accountChange(event);
					}
				});
	}
	
	/**
	 * @author Faucheux
	 */
	public int getLevel () {
	    if (parent == null) 
	        return 0;
	    else 
	        return parent.getLevel() + 1 ;
	}
	
	/**
	 * returns the real Account (not mutable) on which this mutable account points. 
	 * @author Faucheux
	 */
	public Account getRealAccount () {
	    return account;
	}
	
    /**
     * @author Faucheux
     */
 	public long getBalance(Session session, Date fromDate, Date toDate) {
 	   return account.getBalance(session, fromDate, toDate);
 	}

    /**
     * @author Faucheux
     */
 	public long getBalanceWithSubAccounts(Session session, Date fromDate, Date toDate) {
  	   return account.getBalanceWithSubAccounts(session, fromDate, toDate);
 	}

    public void addChild(Account a) {
        children.add(a);
    }
}
