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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 *
 * @author  Nigel
 */
public class TransactionImpl extends ExtendableObjectHelperImpl implements Transaction {
    
    protected Date date = null;
    
    protected IListManager entries;
    
	public TransactionImpl(
			IObjectKey objectKey,
    		Map extensions,
			IObjectKey parent,
    		IListManager entries,
    		Date date) {
		super(objectKey, extensions);

		this.entries = entries;
		this.date = date;
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
		return "net.sf.jmoney.transaction";
	}
	
    /**
     * Returns the date.
     */
    public Date getDate() {
        return date;
    }
    
    public Iterator getEntryIterator() {
        return entries.iterator();
    }
    
    // TODO: Ensure no mutable interface on this object already.
    public MutableTransaction createMutableTransaction(Session session) throws ObjectLockedForEditException {
        return new MutableTransactionImpl(session, this);
    }

    /**
     *DISREGARD ALL THIS
     * This method returns true if two objects represent the same transaction.
     * There will only be one <code>Transaction</code> object for each transaxion.
     * However, if a transaxion is being edited then one or more 
     * <code>MutableTransaction</code> objects may exist.  These objects
     * implement the <code>Transaction</code> interface and represent the
     * same transaction.  Therefore they should be considered equal.
     * (For example, consider the situation where a transaction 
     * is being edited in a view, and the edited
     * transaction is to be included in a list view which is to show the
     * edited state, but the changes to the transaction
     * has not yet been committed.  The <code>MutableTransaction</code>
     * object will thus appear in the list.  Items in this list may be
     * compared
     * 
     * so the Java equality will give the correct result when both objects
     * are of typ
     * 
    public boolean equals(Object other) {
        if (other instanceof MutableTransactionImpl) {
            return other.equals(this);
        } else {
            return super.equals(other);
        }
*/        

    // Some helper methods:
    
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
        for (Iterator iter = entries.iterator(); iter.hasNext(); ) {
        	Entry entry = (Entry)iter.next();
        	if (!entry.equals(thisEntry)) {
        		return entry;
        	}
        }
        throw new RuntimeException("Double entry error");
    }
    
    // Used by the MutableTranxactionImpl and also for serialization:
    
    public void setDate(Date date) {
        this.date = date;
    }
    
/**
 * 
 * @param newEntryList A vector containing objects of type
 * 			MutableEntryImpl.
 * @param newEntries An output parameter to which are added
 * 			objects of type EntryImpl.
 * @param deletedEntries An output parameter to which are added
 * 			objects of type EntryImpl.
 */
    // Changes must not be fired until all the changes have been committed.
    // Hence the lists of added and removed entries is returned.
    // TODO: We could return a single list of events to be fired,
    // and this could be generalized so all changes keep up the
    // events to be fired and only fire them when the change is committed.
    public void updateEntries(Vector newEntryList, Vector newEntries, Vector deletedEntries) {
        // We want to keep the Java identity of entries the same whereever an entry
        // exists in both the original and the updated state of the tranaction.
        // This is important because there may be other objects with references
        // to the entry.  These references will stop the entry from being deleted,
        // but will not prevent the entry from being altered.
        
        // Any entries no longer used must be removed from the datastore.
        for (Iterator iter = this.entries.iterator(); iter.hasNext(); ) {
            Entry entry = (Entry)iter.next();
            // See if this entry is the original entry in any of the
            // replacement entries.
            boolean found = false;
            for (Iterator iter2 = newEntryList.iterator(); iter2.hasNext(); ) {
                MutableEntryImpl mutableEntry = (MutableEntryImpl)iter2.next();
                if (mutableEntry.getOriginalEntry().equals(entry)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                entry.setDescription("dead entry");  // for debug purposes only
                iter.remove();
                deletedEntries.add(entry);
            }
        }
        
        // Go through replacement entries.  If the entry
        // exists in the original transaction then copy across the properties.
        // If the entry does not exist then add it.
        for (Iterator iter = newEntryList.iterator(); iter.hasNext(); ) {
        	MutableEntryImpl mutableEntry = (MutableEntryImpl)iter.next();
            
            if (mutableEntry.getOriginalEntry() != null) {
                (mutableEntry.getOriginalEntry()).copyProperties(mutableEntry);
            } else {
            	EntryImpl entry = (EntryImpl)this.createEntry();
            	entry.setAccount(mutableEntry.getAccount());
            	entry.setAmount(mutableEntry.getAmount());
            	entry.setCheck(mutableEntry.getCheck());
            	entry.setCreation(mutableEntry.getCreation());
            	entry.setDescription(mutableEntry.getDescription());
            	entry.setMemo(mutableEntry.getMemo());
            	entry.setValuta(mutableEntry.getValuta());
            	
            	newEntries.add(entry);
            }
        }
        
        // For efficiency, we keep a list of entries in each
        // account/category.  We must update this list now.
/* no longer needed here because this is done in the setAccount method.        
        for (Iterator iter = newEntries.iterator(); iter.hasNext(); ) {
            Entry entry = (Entry)iter.next();
            // TODO: at some time, keep these lists for categories too
            Account category = entry.getAccount();
            if (category instanceof CapitalAccountImpl) {
                ((CapitalAccountImpl)category).addEntry(entry);
            }
        }
*/
        // TODO: This should be done in the methods that remove an entry.
        for (Iterator iter = deletedEntries.iterator(); iter.hasNext(); ) {
            Entry entry = (Entry)iter.next();
            // TODO: at some time, keep these lists for categories too
            Account category = entry.getAccount();
            if (category instanceof CapitalAccountImpl) {
                ((CapitalAccountImpl)category).removeEntry(entry);
            }
        }
    }

    public Entry createEntry() {
    	PropertySet entryPropertySet = JMoneyPlugin.getEntryPropertySet();

		EntryImpl newEntry = (EntryImpl)entries.createNewElement(this, entryPropertySet);

		// Fire the event.
/* no - fire events only when the change has been committed.
 * otherwise a listener will see an incomplete transaction.
 		
        final EntryAddedEvent event = new EntryAddedEvent(newEntry);
        session.fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.entryAdded(event);
        		}
       		});
 */       
        return newEntry;
	}

	static public Object [] getDefaultProperties() {
		// TODO: Check if this is set to the current day.
		return new Object [] { new Date() };
	}
}
