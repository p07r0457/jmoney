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
import net.sf.jmoney.model2.*;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author  Nigel
 */
public class MutableTransactionImpl extends ExtendableObjectHelperImpl implements MutableTransaction {
    
    private TransactionImpl transaction;
    
    private SessionImpl session;
    
    private Date date;
    
    private Vector entries = new Vector();
    
    /** 
     * Creates an instance of MutableTransaction that is editing a new transaction.
     */
    public MutableTransactionImpl(Session session) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);
    	
        this.session = (SessionImpl)session;
        this.transaction = null;
    }

    /** 
     * Creates an instance of MutableTransaction that is editing a transaction
     * that has already been committed to the database.
     */
    public MutableTransactionImpl(Session session, Transaction transaction) {
    	// Temp code.  The object key is created when
    	// it is requested.
    	super(null, null);

    	this.session = (SessionImpl)session;
        this.transaction = (TransactionImpl)transaction;
        
        this.date = transaction.getDate();
        
        for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
            EntryImpl entry = (EntryImpl)iter.next();
            Entry mutableEntry = new MutableEntryImpl(this, entry);
            this.entries.add(mutableEntry);
        }
    }

    // Temp method.
    // No object key is set so create one now.
	public IObjectKey getObjectKey() {
    	return new IObjectKey() {
    		public IExtendableObject getObject() {
    			return MutableTransactionImpl.this;
    		}
    		
    		public Collection createIndexValuesList(PropertyAccessor propertyAccessor) {
    			throw new RuntimeException("internal error");
    		}

			public void updateProperties(PropertySet actualPropertySet, Object[] oldValues, Object[] newValues, ExtensionProperties[] extensionProperties) {
    			throw new RuntimeException("internal error");
			}

			public Session getSession() {
    			throw new RuntimeException("internal error");
			}
    	};
	}
	
	protected boolean isMutable() {
		return true;
	}

	protected IExtendableObject getOriginalObject() {
		return transaction;
	}
	
	protected String getExtendablePropertySetId() {
		// This method should be called when loading data from the datastore.
		// Mutable objects are not used at that time so this method should
		// never be called.
		throw new RuntimeException("should never be called");
	}
	
   public void setDate(Date date) {
        if (date != null && date.equals(this.date))
            return;
        Date oldDate = this.date;
        this.date = date;
        
        // TODO: figure out how entry property change firings are being used.
//	changeSupport.firePropertyChange("date", oldDate, date);
    }

    public Date getDate() {
        return date;
    }

    public Iterator getEntryIterator() {
        return entries.iterator();
    }

    public Transaction getOriginalTransaxion() {
        return transaction;
    }
    
    public MutableTransaction createMutableTransaction(Session session)
    throws ObjectLockedForEditException {
		throw new RuntimeException("object already mutable");
    }
    
    public MutableEntryImpl createEntry() {
        // Pass this mutable transaction object as the parent of the new entry.
        // This allows the entry to get access to properties from the transaction.
        // This is very useful when, say, sorting entries by date.
        //
        // The parent will be switched to the actual committed transaction when
        // the transaction is committed.
    	MutableEntryImpl newEntry = new MutableEntryImpl(this);
        entries.addElement(newEntry);
        return newEntry;
    }
    
    public void removeEntry(Entry e) {
	entries.removeElement(e);
    }
    
    public Transaction commit() {
        Vector newEntries = new Vector();
        Vector deletedEntries = new Vector();
        
        if (transaction == null) {
    		transaction = (TransactionImpl)session.createTransaction();
        }
        transaction.setDate(date);
        transaction.updateEntries(entries, newEntries, deletedEntries);
        
        // Now the transaction has been added and can been seen by
        // others, fire the change events.
        for (Iterator iter = deletedEntries.iterator(); iter.hasNext(); ) {
            JMoneyPlugin.getDefault().fireEntryDeletedEvent((Entry)iter.next());
        }
        for (Iterator iter = newEntries.iterator(); iter.hasNext(); ) {
        	JMoneyPlugin.getDefault().fireEntryAddedEvent((Entry)iter.next());
        }
                    
        
        // remove this mutable object from the map.
//      EditLockMap.remove(lockedObject);
        
        // Mark this object as now being unusable.
//      isDead = true;
        
        return transaction;
    }

/* not yet implemented    
    public void rollback() {
        // remove this mutable object from the map.
        EditLockMap.remove(lockedObject);
    }
 */

    public Session getSession() {
        return session;
    }
    
    // Helper functions
    
    public boolean hasTwoEntries() {
        return entries.size() == 2;
    }
    
    public boolean hasMoreThanTwoEntries() {
        return entries.size() > 2;
    }
    
    public Entry getOther(Entry thisEntry) {
        if (entries.size() != 2) {
            throw new RuntimeException("Double entry error");
        }
        if (entries.elementAt(0) == thisEntry) {
            return (Entry)entries.elementAt(1);
        } else if (entries.elementAt(1) == thisEntry) {
            return (Entry)entries.elementAt(0);
        } else {
            throw new RuntimeException("Double entry error");
        }
    }

	// This method probably should not be here.  We can remove
	// it is we provide a seperate interface for object initialization.
	public void addEntry(Entry entry) {
		throw new RuntimeException("internal error");
	}
}
