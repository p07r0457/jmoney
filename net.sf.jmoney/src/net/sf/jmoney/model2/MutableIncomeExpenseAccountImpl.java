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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.jface.util.PropertyChangeEvent;

import net.sf.jmoney.model2.*;

/**
 *
 * @author  Nigel
 */
public class MutableIncomeExpenseAccountImpl  extends ExtendableObjectHelperImpl implements MutableIncomeExpenseAccount {
    
    private IncomeExpenseAccountImpl account;
    
    private IncomeExpenseAccountImpl parent;
    
    private SessionImpl session;
    
    private String name;
    
    /** 
     * Creates an instance of MutableIncomeExpenseAccount that is editing a new sub-account
     */
    public MutableIncomeExpenseAccountImpl(Session session, Account parent, int dummy) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

        this.session = (SessionImpl)session;
        this.parent = (IncomeExpenseAccountImpl)parent;
        this.account = null;
    }

    /** 
     * Creates an instance of MutableIncomeExpenseAccount that is editing a new top level account
     */
    public MutableIncomeExpenseAccountImpl(Session session) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

        this.session = (SessionImpl)session;
        this.parent = null;
        this.account = null;
    }

    /** 
     * Creates an instance of MutableIncomeExpenseAccount that is editing a account
     * that has already been committed to the database.
     */
    public MutableIncomeExpenseAccountImpl(Session session, IncomeExpenseAccount category) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

        this.session = (SessionImpl)session;
        this.parent = (IncomeExpenseAccountImpl)category.getParent();  // TODO: remove cast
        this.account = (IncomeExpenseAccountImpl)category;
        
        this.name = category.getName();
    }

    // Temp method.
    // No object key is set so create one now.
	public IObjectKey getObjectKey() {
    	return new IObjectKey() {
    		public IExtendableObject getObject() {
    			return MutableIncomeExpenseAccountImpl.this;
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
    
    public String getName() {
        return name;
    }

    // Or perhaps this implementation is not going to be thread safe???
    public IncomeExpenseAccount commit() {
        // TODO: Ensure that the vector class is thread safe.
        // The database must be in a valid state at all times, which
        // means updates and reads must be synchronized.
        if (account == null) {
			PropertySet incomeExpenseAccountPropertySet;
			try {
				incomeExpenseAccountPropertySet = PropertySet.getPropertySet("net.sf.jmoney.categoryAccount");
			} catch (PropertySetNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}

			//account = new IncomeExpenseAccountImpl(null, null, name, null);

            if (parent == null) {
    			account = (IncomeExpenseAccountImpl)session.createNewAccount(incomeExpenseAccountPropertySet, this);
            } else {
    			account = (IncomeExpenseAccountImpl)parent.createNewSubAccount(session, incomeExpenseAccountPropertySet, this);
//              parent.addSubAccount(account);
            }
        } else {
        	String oldName = account.getName();
        	if (!name.equals(oldName)) {
        		account.setName(name);
        		
        		// Fire the event.
        		final PropertyChangeEvent event = new PropertyChangeEvent(account, "name", oldName, name);
        		session.fireEvent(
        				new ISessionChangeFirer() {
        					public void fire(SessionChangeListener listener) {
        						listener.accountChange(event);
        					}
        				});
        	}
        }
        
        // remove this mutable object from the map.
//      EditLockMap.remove(lockedObject);
        
        // Mark this object as now being unusable.
//      isDead = true;
        
        session.modified();

        return account;
    }

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.IncomeExpenseAccount#createNewSubAccount(net.sf.jmoney.model2.Session)
	 */
	public MutableIncomeExpenseAccount createNewSubAccount(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.IncomeExpenseAccount#createMutableAccount(net.sf.jmoney.model2.Session)
	 */
	public MutableIncomeExpenseAccount createMutableAccount(Session session) throws ObjectLockedForEditException {
		// TODO Auto-generated method stub
		return null;
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
	public void addSubAccount(IncomeExpenseAccount subAccount) {
		throw new RuntimeException("internal error");
	}
	
/* not needed until multi-threading taken into consideration    
    public void rollback() {
        // remove this mutable object from the map.
        EditLockMap.remove(lockedObject);
    }
 */
	
	public int getLevel () {
	    if (parent == null) {
	        return 0;
	    } else {
	        return parent.getLevel() + 1;
	    }
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
 	   return 0;
 	}

    /**
     * @author Faucheux
     */
 	public long getBalanceWithSubAccounts(Session session, Date fromDate, Date toDate) {
 	    return 0;
 	}


    public void addChild(Account a) {
        account.addChild(a);
    }
}
