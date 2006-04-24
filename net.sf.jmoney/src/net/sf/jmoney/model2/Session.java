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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Holds the fields that will be saved in a file.
 */
public class Session extends ExtendableObject implements IAdaptable {

    protected IObjectKey defaultCurrencyKey;
    
    private IListManager commodities;
    
    private IListManager accounts;  // Only the Accounts of Level 0

    private IListManager transactions;

    Hashtable currencies = new Hashtable();
        
    private Vector sessionChangeListeners = new Vector();
    private Vector sessionChangeFirerListeners = new Vector();

	private ChangeManager changeManager = new ChangeManager();

	private boolean sessionFiring = false;

    /**
     * Constructor used by datastore plug-ins to create
     * a session object.
     */
    public Session(
    		IObjectKey objectKey,
    		Map extensions,
			IObjectKey parentKey,
    		IListManager commodities,
			IListManager accounts,
			IListManager transactions,
			IObjectKey defaultCurrencyKey) {
    	super(objectKey, extensions, parentKey);

    	this.commodities = commodities;
    	this.accounts = accounts;
    	this.transactions = transactions;
    	this.defaultCurrencyKey = defaultCurrencyKey;
    	
        // Set up a hash table that maps currency codes to
        // the currency object.
    	// It may be that no 
    	this.currencies = new Hashtable();
    	for (Iterator iter = commodities.iterator(); iter.hasNext(); ) {
    		Commodity commodity = (Commodity)iter.next();
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
    		Map extensions,
			IObjectKey parentKey,
    		IListManager commodities,
			IListManager accounts,
			IListManager transactions) {
    	super(objectKey, extensions, parentKey);

    	this.commodities = commodities;
    	this.accounts = accounts;
    	this.transactions = transactions;
    	
        // Set up a hash table that maps currency codes to
        // the currency object.
    	// It may be that no 
    	this.currencies = new Hashtable();
    	for (Iterator iter = commodities.iterator(); iter.hasNext(); ) {
    		Commodity commodity = (Commodity)iter.next();
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
		return (Currency) currencies.get(code);
	}

/*   
    public Iterator getAccountIterator() {
        return new Iterator() {
        	Iterator iter = accounts.iterator();
        	boolean inAccounts = true;
        	
			public boolean hasNext() {
				if (iter.hasNext()) {
					return true;
				} else if (inAccounts) {
					inAccounts = false;
					iter = categories.iterator();
					return iter.hasNext();
				} else {
					return false; 
				}
			}
			public Object next() {
				return iter.next();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }
  */ 

	public Collection getAllAccounts() {
        Vector all = new Vector();
        Iterator rootIt = getAccountCollection().iterator();
        while (rootIt.hasNext()) {
            Account a = (Account) rootIt.next();
            all.add(a);
            all.addAll(a.getAllSubAccounts());
        }
        return all;
    }
   
    public Iterator getCapitalAccountIterator() {
        return new Iterator() {
        	Iterator iter = accounts.iterator();
        	Object element;
        	
			public boolean hasNext() {
				while (iter.hasNext()) {
					element = iter.next();
					if (element instanceof CapitalAccount) {
						return true; 
					}
				}
				return false;
			}
			public Object next() {
				return element;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }
   
    public Account[] getAccountList () {
        Collection allAccounts = getAllAccounts();
        
        Account list[]  = new Account [allAccounts.size()];
        Iterator it = allAccounts.iterator();

        int i = 0;
        while (it.hasNext()) 
            list[i++] = (Account) it.next();
        
        return list;
    }
    
    /**
     * @author Faucheux
     */
    
    public Iterator getCapitalAccountIteratorByLevel(int level) {
    	Iterator itAccounts = getAllAccounts().iterator();
    	Vector   vecResult = new Vector();
    	while (itAccounts.hasNext()) {
    	    Account a = (Account) itAccounts.next();
       	    if (a instanceof CapitalAccount && ((CapitalAccount) a).getLevel() == level) {
    	        vecResult.add(a);
    	    }
    	}
    	return vecResult.iterator();
    }

       public Iterator getIncomeExpenseAccountIterator() {
        return new Iterator() {
        	Iterator iter = accounts.iterator();
        	Object element;
        	
			public boolean hasNext() {
				while (iter.hasNext()) {
					element = iter.next();
					if (element instanceof IncomeExpenseAccount) {
						return true; 
					}
				}
				return false;
			}
			public Object next() {
				return element;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }

    public ObjectCollection getCommodityCollection() {
    	return new ObjectCollection(commodities, this, SessionInfo.getCommoditiesAccessor());
    }
    
    public ObjectCollection getAccountCollection() {
    	return new ObjectCollection(accounts, this, SessionInfo.getAccountsAccessor());
    }
    
    public ObjectCollection getTransactionCollection() {
    	return new ObjectCollection(transactions, this, SessionInfo.getTransactionsAccessor());
    }
    
    public void addSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.add(l);
    }
    
	/**
	 * Adds a change listener.
	 * <P>
	 * The listener is active only for as long as the given control exists.  When the
	 * given control is disposed, the listener is removed and will receive no more
	 * notifications.
	 * <P>
	 * This method is generally used when a listener is used to update contents in a
	 * control.  Typically multiple controls are updated by a listener and the parent
	 * composite control is passed to this method.
	 * 
	 * @param listener
	 * @param control
	 */
	public void addSessionChangeListener(final SessionChangeListener listener, Control control) {
        sessionChangeListeners.add(listener);
        
		// Remove the listener when the given control is disposed.
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				sessionChangeListeners.remove(listener);
			}
		});
    }
    
    public void removeSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.remove(l);
    }
    
    public void addSessionChangeFirerListener(SessionChangeFirerListener l) {
        sessionChangeFirerListeners.add(l);
    }
    
    public void removeSessionChangeFirerListener(SessionChangeFirerListener l) {
        sessionChangeFirerListeners.remove(l);
    }
    
    /**
     * In practice it is likely that the only listener will be the
     * JMoneyPlugin object.  Views should all listen to the JMoneyPlugin
     * class for changes to the model.  The JMoneyPlugin object will pass
     * on events from this session object.
     * <P>
     * Listeners may register directly with a session object.  However
     * if they do so then they must re-register whenever the session
     * object changes.  If a viewer wants to listen for changes to a
     * session even if that session is not the session currently shown
     * in the workbench then it should register with the session object,
     * but if the viewer wants to be told about changes to the current
     * workbench window then it should register with the JMoneyPlugin
     * object.
     */
    public void fireEvent(ISessionChangeFirer firer) {
    	sessionFiring = true;
    	
    	// Notify listeners who are listening to us using the
    	// SessionChangeFirerListener interface.
        if (!sessionChangeFirerListeners.isEmpty()) {
        	// Take a copy of the listener list.  By doing this we
        	// allow listeners to safely add or remove listeners.
        	SessionChangeFirerListener listenerArray[] = new SessionChangeFirerListener[sessionChangeFirerListeners.size()];
        	sessionChangeFirerListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		listenerArray[i].sessionChanged(firer);
        	}
        }
    	
    	// Notify listeners who are listening to us using the
    	// SessionChangeListener interface.
        if (!sessionChangeListeners.isEmpty()) {
        	// Take a copy of the listener list.  By doing this we
        	// allow listeners to safely add or remove listeners.
        	SessionChangeListener listenerArray[] = new SessionChangeListener[sessionChangeListeners.size()];
        	sessionChangeListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		firer.fire(listenerArray[i]);
        	}
        }

        sessionFiring = false;
    }

    /**
     * This method is used by plug-ins so that they know if
     * code is being called from within change notification.
     *
     * It is important for plug-ins to know this.  Plug-ins
     * MUST NOT change the session data while a listener is
     * being notified of a change to the datastore.
     * This can happen very indirectly.  For example, suppose
     * an account is deleted.  The navigation view's listener
     * is notified and so removes the account's node from the
     * navigation tree.  If an account properties panel is
     * open, the panel is destroyed.  Because the panel is
     * being destroyed, the control that had the focus is sent
     * a 'focus lost' notification.  The 'focus lost' notification
     * takes the edited data from the control and writes it to
     * the datastore.
     * <P>
     * Writing data to the datastore during session change notifications
     * can cause serious problems.  The data may conflict.  The
     * undo/redo operations are almost impossible to manage.
     * In the above scenario with the deleted account, an attempt
     * is made to update a property for an object that has been
     * deleted.  The problems are endless.
     * <P>
     * It would be good if the datastore simply ignored such changes.
     * This would provide more robust support for plug-ins, and plug-ins
     * would not have to test this flag.  However, for the time being,
     * plug-ins must test this flag and avoid making changes when this
     * flag is set.  Plug-ins only need to do this in focus lost events
     * as that is the only time I can think of where this problem may
     * occur.
     *  
     * @return True if the session is notifying listeners of
     * 			a change to the session data, otherwise false.
     */
    // TODO: Revisit this, especially the last paragraph above.
    public boolean isSessionFiring() {
    	return sessionFiring;
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
	public Account createAccount(PropertySet propertySet) {
		return (Account)getAccountCollection().createNewElement(propertySet);
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
	public Commodity createCommodity(PropertySet propertySet) {
		return (Commodity)getCommodityCollection().createNewElement(propertySet);
	}

	public Transaction createTransaction() {
		return (Transaction)getTransactionCollection().createNewElement(TransactionInfo.getPropertySet());
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
            return ((Account)parent).deleteSubAccount(account);
        }
    }

   	public boolean deleteTransaction(Transaction transaction) {
   		return getTransactionCollection().remove(transaction);
    }
    
	public ChangeManager getChangeManager() {
		return changeManager;
	}

	static public Object [] getDefaultProperties() {
		return new Object [] { null };
	}
	
	/**
	 * @author Faucheux
	 */
    public Vector getAccountsUntilLevel(int level) {
    	Iterator i = getAllAccounts().iterator();
    	Vector v = new Vector();
    	while (i.hasNext()) {
    		Account a = (Account) i.next();
    		if (a.getLevel() <= level) v.add(a);
    	}
    	
    	return v;
    }

    /**
     * @author Faucheux
     * TODO: Faucheux - not the better algorythm!
     */
	public Account getAccountByFullName(String name) {
	    Account a = null;
	    Iterator it = getAllAccounts().iterator();
	    while (it.hasNext()) {
	        a = (Account) it.next();
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
	 * Indicates the completion of a set of changes to the datastore 
	 * that make a single undo/redo change.  This undo/redo change
	 * will be added to the list of past changes that can be undone.
	 * If there are any undone changes that can be redone then those
	 * are cleared because once a new change is made, undo changes cannot
	 * be redone.
	 * <P>
	 * If no changes were made then this method does nothing.
	 * The change will not appear in the list of changes that can
	 * be undone and the list of changes that can be redone is not cleared.
	 * 
	 * @param string Localized text describing the change.
	 */
	public void registerUndoableChange(String description) {
		// Pass the request on to the change manager.
		changeManager.registerChanges(description);
	}

	/**
	 * @param string
	 */
	public void undoChange(IWorkbenchWindow window) {
		String nextUndoDescription = changeManager.getUndoDescription();
		if (nextUndoDescription == null) {
			MessageDialog.openInformation(
					window.getShell(),
					"Jmoney Plug-in",
					"No change that can be 'undone'.");
			return;
		}
		
 		if (MessageDialog.openQuestion(
				window.getShell(),
				"Jmoney Plug-in",
				"Undo " + nextUndoDescription + "?")) {
 			changeManager.undoChange();
 		}
	}

	/**
	 * @param string
	 */
	public void redoChange(IWorkbenchWindow window) {
		String nextRedoDescription = changeManager.getRedoDescription();
		if (nextRedoDescription == null) {
			MessageDialog.openInformation(
					window.getShell(),
					"Jmoney Plug-in",
					"No change that can be 'redone'.");
			return;
		}
		
 		if (MessageDialog.openQuestion(
				window.getShell(),
				"Jmoney Plug-in",
				"Redo " + nextRedoDescription + "?")) {
 			changeManager.redoChange();
 		}
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
	};

}
