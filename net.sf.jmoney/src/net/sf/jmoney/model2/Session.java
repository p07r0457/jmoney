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

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.SessionInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.isolation.TransactionManager;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Holds the fields that will be saved in a file.
 */
public class Session extends ExtendableObject implements IAdaptable {

    protected IObjectKey defaultCurrencyKey;
    
    private IListManager<Commodity> commodities;
    
    private IListManager<Account> accounts;  // Only the Accounts of Level 0

    private IListManager<Transaction> transactions;

    Hashtable<String, Currency> currencies = new Hashtable<String, Currency>();
        
	private ChangeManager changeManager = new ChangeManager();

	private IUndoContext undoContext = new UndoContext();

	/**
     * Constructor used by datastore plug-ins to create
     * a session object.
     */
    public Session(
    		IObjectKey objectKey,
    		Map<ExtensionPropertySet, Object[]> extensions,
			IObjectKey parentKey,
    		IListManager<Commodity> commodities,
			IListManager<Account> accounts,
			IListManager<Transaction> transactions,
			IObjectKey defaultCurrencyKey) {
    	super(objectKey, extensions, parentKey);

    	this.commodities = commodities;
    	this.accounts = accounts;
    	this.transactions = transactions;
    	this.defaultCurrencyKey = defaultCurrencyKey;
    	
        // Set up a hash table that maps currency codes to
        // the currency object.
    	// It may be that no 
    	this.currencies = new Hashtable<String, Currency>();
    	for (Commodity commodity: commodities) {
    		if (commodity instanceof Currency) {
    			Currency currency = (Currency)commodity;
    			if (currency.getCode() != null) {
    				this.currencies.put(currency.getCode(), currency);
    			}
    		}
    	}
    }

    /**
     * Constructor used by datastore plug-ins to create
     * a session object.
     */
    public Session(
    		IObjectKey objectKey,
    		Map<ExtensionPropertySet, Object[]> extensions,
			IObjectKey parentKey,
    		IListManager<Commodity> commodities,
			IListManager<Account> accounts,
			IListManager<Transaction> transactions) {
    	super(objectKey, extensions, parentKey);

    	this.commodities = commodities;
    	this.accounts = accounts;
    	this.transactions = transactions;
    	
        // Set up a hash table that maps currency codes to
        // the currency object.
    	// It may be that no 
    	this.currencies = new Hashtable<String, Currency>();
    	for (Commodity commodity: commodities) {
    		if (commodity instanceof Currency) {
    			Currency currency = (Currency)commodity;
    			if (currency.getCode() != null) {
    				this.currencies.put(currency.getCode(), currency);
    			}
    		}
    	}
    	
   		this.defaultCurrencyKey = null;
    }

	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.session";
	}
	
    public Currency getDefaultCurrency() {
        return defaultCurrencyKey == null
		? null
				: (Currency)defaultCurrencyKey.getObject();
    }
    
    /**
     * 
     * @param defaultCurrency the default currency, which cannot
     * 			be null because a default currency must always
     * 			be set for a session
     */
    public void setDefaultCurrency(Currency defaultCurrency) {
        Currency oldDefaultCurrency = getDefaultCurrency();
        this.defaultCurrencyKey = defaultCurrency.getObjectKey();

		// Notify the change manager.
		processPropertyChange(SessionInfo.getDefaultCurrencyAccessor(), oldDefaultCurrency, defaultCurrency);
    }
    
	/**
	 * @param code the currency code.
	 * @return the corresponding currency.
	 */
	public Currency getCurrencyForCode(String code) {
		return currencies.get(code);
	}

	public Collection<Account> getAllAccounts() {
        Vector<Account> all = new Vector<Account>();
        for (Account a: getAccountCollection()) {
            all.add(a);
            all.addAll(a.getAllSubAccounts());
        }
        return all;
    }
   
    public Iterator<CapitalAccount> getCapitalAccountIterator() {
        return new Iterator<CapitalAccount>() {
        	Iterator<Account> iter = accounts.iterator();
        	CapitalAccount element;
        	
			public boolean hasNext() {
				while (iter.hasNext()) {
					Account account = iter.next();
					if (account instanceof CapitalAccount) {
						element = (CapitalAccount)account;
						return true; 
					}
				}
				return false;
			}
			public CapitalAccount next() {
				return element;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }
   
    public Iterator<IncomeExpenseAccount> getIncomeExpenseAccountIterator() {
        return new Iterator<IncomeExpenseAccount>() {
        	Iterator<Account> iter = accounts.iterator();
        	IncomeExpenseAccount element;
        	
			public boolean hasNext() {
				while (iter.hasNext()) {
					Account account = iter.next();
					if (account instanceof IncomeExpenseAccount) {
						element = (IncomeExpenseAccount)account;
						return true; 
					}
				}
				return false;
			}
			public IncomeExpenseAccount next() {
				return element;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }

    public ObjectCollection<Commodity> getCommodityCollection() {
    	return new ObjectCollection<Commodity>(commodities, this, SessionInfo.getCommoditiesAccessor());
    }
    
    public ObjectCollection<Account> getAccountCollection() {
    	return new ObjectCollection<Account>(accounts, this, SessionInfo.getAccountsAccessor());
    }
    
    public ObjectCollection<Transaction> getTransactionCollection() {
    	return new ObjectCollection<Transaction>(transactions, this, SessionInfo.getTransactionsAccessor());
    }
    
	/**
	 * Create a new account.  Accounts are abstract, so
	 * a property set derived from the account property
	 * set must be passed to this method.  When an account
	 * is created, various objects, such as the objects that manage collections within
	 * the account, must be created.  The implementation of
	 * these objects depends on the datastore and must be passed
	 * to the constructor, so the actual construction of the object
	 * is delegated to the collection object that will hold this
	 * new account.  (The implementation of the collection object
	 * is provided by the datastore plug-in, so this object knows
	 * how to create an object in a way that is appropriate for
	 * the datastore).
	 * 
	 * The collection object will get the properties for the new
	 * object from the given interface.  Scalar properties are
	 * simply set.  References to other objects are likewise set.
	 * This means any referenced object must have been fetched by
	 * the datastore.
	 * 
	 * @param accountPropertySet
	 * @param account
	 * @return
	 */
	public <A extends Account> A createAccount(ExtendablePropertySet<A> propertySet) {
		return getAccountCollection().createNewElement(propertySet);
	}

	/**
	 * Create a new commodity.  Commodities are abstract, so
	 * a property set derived from the commodity property
	 * set must be passed to this method.  When an commodity
	 * is created, various objects, such as the objects that manage collections within
	 * the commodity, must be created.  The implementation of
	 * these objects depends on the datastore and must be passed
	 * to the constructor, so the actual construction of the object
	 * is delegated to the collection object that will hold this
	 * new commodity.  (The implementation of the collection object
	 * is provided by the datastore plug-in, so this object knows
	 * how to create an object in a way that is appropriate for
	 * the datastore).
	 * 
	 * The collection object will get the properties for the new
	 * object from the given interface.  Scalar properties are
	 * simply set.  References to other objects are likewise set.
	 * This means any referenced object must have been fetched by
	 * the datastore.
	 * 
	 * @param commodityPropertySet
	 * @param commodity
	 * @return
	 */
	public <E extends Commodity> E createCommodity(ExtendablePropertySet<E> propertySet) {
		return getCommodityCollection().createNewElement(propertySet);
	}

	public Transaction createTransaction() {
		return getTransactionCollection().createNewElement(TransactionInfo.getPropertySet());
	}
	
    public boolean deleteCommodity(Commodity commodity) {
   		return getCommodityCollection().remove(commodity);
    }

    /**
     * Removes the specified account from this collection,
     * if it is present.
     * <P>
     * Note that accounts may be sub-accounts.  Only top
     * level accounts are in this session's account list.
     * Sub-accounts must be removed by calling the 
     * <code>removeSubAccount</code> method on the parent account.
     * 
     * @param account Account to be removed from this collection.
     * 				This parameter may not be null.
     * @return true if the account was present, false if the account
     * 				was not present in the collection.
     */
    public boolean deleteAccount(Account account) {
        Account parent = account.getParent();
        if (parent == null) {
        	return getAccountCollection().remove(account);
        } else {
        	// Pass the request on to the parent account.
            return parent.deleteSubAccount(account);
        }
    }

   	public boolean deleteTransaction(Transaction transaction) {
   		return getTransactionCollection().remove(transaction);
    }
    
	public ChangeManager getChangeManager() {
		return changeManager;
	}

    /**
     * @author Faucheux
     * TODO: Faucheux - not the better algorythm!
     */
	public Account getAccountByFullName(String name) {
		for (Account a: getAllAccounts()) {
	        if (JMoneyPlugin.DEBUG) System.out.println("Compare " + name + " to " + a.getFullAccountName());
	        if (a.getFullAccountName().equals(name))
	            return a;
	    }
	    return null;
	}

    /**
     * @throws InvalidParameterException
     * @author Faucheux
     * TODO: Faucheux - not the better algorythm!
     */
	public Account getAccountByShortName(String name) throws SeveralAccountsFoundException, NoAccountFoundException{
	    Account foundAccount = null;
	    Iterator it = getAllAccounts().iterator();
	    while (it.hasNext()) {
	        Account a = (Account) it.next();
	        if (JMoneyPlugin.DEBUG) System.out.println("Compare " + name + " to " + a.getName());
	        if (a.getName().equals(name)) {
	            if (foundAccount != null) {
	                throw new SeveralAccountsFoundException ();
	            } else {
	                foundAccount = a;
	            }
	        } 
	    }
	    if (foundAccount == null) throw new NoAccountFoundException();
	    return foundAccount;
	}

	/**
	 * Certain operations may be executed more efficiently by the datastore.
	 * For example, if the datastore is implemented on top of a database and
	 * we need to get an account balance, it is far more efficient to submit 
	 * a statement of the form "select sum(amount) from entries where account = ?"
	 * to the database than to iterate through the entries, a process that
	 * requires constructing each entry from data in the database.
	 * <P>
	 * Datastores may optionally implement any adapter interface.
	 * A datastore does not have to implement an adapter interface.  Therefore
	 * all consumers must provide an alternative algorithm for obtaining the
	 * same results.
	 * <P>
	 * These interfaces are obtained as adapters for two reasons:
	 * <OL>
	 * <LI>The implementation is optional
	 *     </LI>
	 * <LI>More importantly, new types of queries may be added by plug-ins.
	 *     The datastore plug-ins may not even know about such queries.
	 *     </LI>
	 * </OL>
	 */
	public Object getAdapter(Class adapter) {
		// Pass the request on to the session manager.
		// The SessionManager object is implemented by the datastore plug-in
		// and therefore can provide a set of interface implementations that
		// are optimized for the datastore.
		return objectKey.getSessionManager().getAdapter(adapter);
	}
	
    public class NoAccountFoundException extends Exception {
		private static final long serialVersionUID = -6022196945540827504L;
	};

	public class SeveralAccountsFoundException extends Exception {
		private static final long serialVersionUID = -6427097946645258873L;
	}

	/**
	 * JMoney supports undo/redo using a context that is based on the data
	 * manager.
	 * 
	 * The underlying data manager (supported by the underlying datastore) and
	 * the transaction manager session objects each have a different context.
	 * 
	 * Changes within a transaction manager have a separate context. This is
	 * necessary because if a view is making changes to an through a transaction
	 * manager to an uncommitted version of the datastore then the undo/redo
	 * menu needs to show those changes for the view.
	 * 
	 * @return the undo/redo context to be used for changes made to this session
	 */
	public IUndoContext getUndoContext() {
		// If not in a transaction, use the workbench context.
		// This may need some tidying up, but by using a common context,
		// this allows undo/redo to work even across a closing or
		// opening of a session.  There may be a better way of doing this.
		if (this.getObjectKey().getSessionManager() instanceof TransactionManager) {
			return undoContext;
		} else {
			return JMoneyPlugin.getDefault().getWorkbench().getOperationSupport().getUndoContext();
		}
	};
}
