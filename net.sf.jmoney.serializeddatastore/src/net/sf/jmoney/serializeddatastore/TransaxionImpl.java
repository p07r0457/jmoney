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

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author  Nigel
 */
//Kludge:  This implements MutableTransaxion.
//You might think it should be implementing only Transaxion,
//and you would be right.  However, the setters are needed
//when reading the data from the XML.  Until this whole data
//storage mess is sorted out, we will leave this as is.
public class TransaxionImpl extends ExtendableObjectHelperImpl implements MutableTransaxion, Serializable {
    
    protected Date date = null;
    
    protected Vector entries = new Vector();
    
    /** Creates a new instance of Transaction */
    public TransaxionImpl() {
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
    
    public Iterator getEntriesIterator() {
        return entries.iterator();
    }
    

    /**
     *DISREGARD ALL THIS
     * This method returns true if two objects represent the same transaction.
     * There will only be one <code>Transaxion</code> object for each transaxion.
     * However, if a transaxion is being edited then one or more 
     * <code>MutableTransaxion</code> objects may exist.  These objects
     * implement the <code>Transaxion</code> interface and represent the
     * same transaction.  Therefore they should be considered equal.
     * (For example, consider the situation where a transaction 
     * is being edited in a view, and the edited
     * transaction is to be included in a list view which is to show the
     * edited state, but the changes to the transaction
     * has not yet been committed.  The <code>MutableTransaxion</code>
     * object will thus appear in the list.  Items in this list may be
     * compared
     * 
     * so the Java equality will give the correct result when both objects
     * are of typ
     * 
    public boolean equals(Object other) {
        if (other instanceof MutableTransaxionImpl) {
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
        if (entries.elementAt(0) == thisEntry) {
            return (Entry)entries.elementAt(1);
        } else if (entries.elementAt(1) == thisEntry) {
            return (Entry)entries.elementAt(0);
        } else {
            throw new RuntimeException("Double entry error");
        }
    }
    
    // Used by the MutableTranxactionImpl and also for serialization:
    
    public void setDate(Date date) {
        this.date = date;
    }
    
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
                Entry mutableEntry = (Entry)iter2.next();
                if (mutableEntry.getOriginalEntry() == entry) {
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
            EntryImpl mutableEntry = (EntryImpl)iter.next();
            if (mutableEntry.getOriginalEntry() != null) {
                //                  assert(this.entries.contains(mutableEntry.getOriginalEntry());
                ((EntryImpl)mutableEntry.getOriginalEntry()).copyProperties(mutableEntry);
            } else {
                EntryImpl entry = new EntryImpl(this);
                entry.copyProperties(mutableEntry);
                this.entries.add(entry);
                newEntries.add(entry);
            }
        }
    }
    
    // TODO remove these two methods
    public void setEntries(Vector entries) {
        this.entries = entries;
    }
    
    public Vector getEntries() {
        return entries;
    }

    // Used by XML reader only
    void addEntry(Entry entry) {
        entries.add(entry);
    }
    
	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.MutableIncomeExpenseAccount#commit()
	 */
	public Transaxion commit() {
		throw new RuntimeException("should never be called");
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.MutableTransaxion#getOriginalTransaxion()
	 */
	public Transaxion getOriginalTransaxion() {
		throw new RuntimeException("should never be called");
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.MutableTransaxion#createEntry()
	 */
	public Entry createEntry() {
		throw new RuntimeException("should never be called");
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.MutableTransaxion#removeEntry(net.sf.jmoney.model2.Entry)
	 */
	public void removeEntry(Entry e) {
		throw new RuntimeException("should never be called");
	}
}
