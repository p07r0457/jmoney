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

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.*;

import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author  Nigel
 */
public class MutableTransaxionImpl extends ExtendableObjectHelperImpl implements MutableTransaxion {
    
    private TransaxionImpl transaction;
    
    private SessionImpl session;
    
    private Date date;
    
    private Vector entries = new Vector();
    
    /** 
     * Creates an instance of MutableTransaxion that is editing a new transaction.
     */
    public MutableTransaxionImpl(Session session) {
        this.session = (SessionImpl)session;
        this.transaction = null;
    }

    /** 
     * Creates an instance of MutableTransaxion that is editing a transaction
     * that has already been committed to the database.
     */
    public MutableTransaxionImpl(Session session, Transaxion transaction) {
        this.session = (SessionImpl)session;
        this.transaction = (TransaxionImpl)transaction;
        
        this.date = transaction.getDate();
        
        for (Iterator iter = transaction.getEntriesIterator(); iter.hasNext(); ) {
            EntryImpl entry = (EntryImpl)iter.next();
            EntryImpl mutableEntry = new EntryImpl(this, entry);
            this.entries.add(mutableEntry);
        }
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

    public Iterator getEntriesIterator() {
        return entries.iterator();
    }

    public Transaxion getOriginalTransaxion() {
        return transaction;
    }
    
    public MutableTransaxion createMutableTransaxion(Session session)
    throws ObjectLockedForEditException {
        // TODO: Ensure no mutable interface on this object already.
        return new MutableTransaxionImpl(session, this);
    }
    
    public Entry createEntry() {
        // Pass this mutable transaction object as the parent of the new entry.
        // This allows the entry to get access to properties from the transaction.
        // This is very useful when, say, sorting entries by date.
        //
        // The parent will be switched to the actual committed transaction when
        // the transaction is committed.
        Entry newEntry = new EntryImpl(this);
        entries.addElement(newEntry);
        return newEntry;
    }
    
    public void removeEntry(Entry e) {
	entries.removeElement(e);
    }
    
    public Transaxion commit() {
        Vector newEntries = new Vector();
        Vector deletedEntries = new Vector();
        
        if (transaction == null) {
            transaction = new TransaxionImpl();
            transaction.setDate(date);
            transaction.updateEntries(entries, newEntries, deletedEntries);
            session.addTransaxion(transaction);
        } else {
            transaction.setDate(date);
            transaction.updateEntries(entries, newEntries, deletedEntries);
        }
        
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
        
        session.modified();

        return transaction;
    }

/* not needed until multi-threading taken into consideration    
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
    
    
}
