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
import net.sf.jmoney.fields.TransactionInfo;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author  Nigel
 */
public class Transaction extends ExtendableObject {
    
    protected Date date = null;
    
    protected IListManager entries;
    
	public Transaction(
			IObjectKey objectKey,
    		Map extensions,
			IObjectKey parent,
    		IListManager entries,
    		Date date) {
		super(objectKey, extensions);

		this.entries = entries;
		this.date = date;
	}
	
	public Transaction(
			IObjectKey objectKey,
    		Map extensions,
			IObjectKey parent,
    		IListManager entries) {
		super(objectKey, extensions);

		this.entries = entries;
		// TODO: Check that this sets the date to the current date.
		this.date = new Date();
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
    
    public void setDate(Date date) {
        Date oldDate = this.date;
        this.date = date;

		// Notify the change manager.
		processPropertyChange(TransactionInfo.getDateAccessor(), oldDate, date);
    }
    
    public Iterator getEntryIterator() {
        return entries.iterator();
    }
    
    public Entry createEntry() {
		final Entry newEntry = (Entry)entries.createNewElement(this, JMoneyPlugin.getEntryPropertySet());

		processObjectAddition(TransactionInfo.getEntriesAccessor(), newEntry);
		
		getObjectKey().getSession().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.entryAdded(newEntry);
					}
				});
		
	    return newEntry;
	}

    public boolean deleteEntry(final Entry entry) {
        boolean found = entries.remove(entry);

		if (found) {
			processObjectDeletion(TransactionInfo.getEntriesAccessor(), entry);

			// In addition to the generic object deletion event, we also fire an event
			// specifically for entry deletion.  The entryDeleted event is superfluous 
			// and it may be simpler if we removed it, so that listeners receive the generic
			// objectDeleted event only.
			getObjectKey().getSession().fireEvent(
		            	new ISessionChangeFirer() {
		            		public void fire(SessionChangeListener listener) {
		            			listener.entryDeleted(entry);
		            		}
		           		});
		}

		// TODO: This is not correct.  Move it into the
        // above 'remove' method or something.
        if (found) {
        	// TODO: at some time, keep these lists for categories too
        	Account category = entry.getAccount();
        	if (category instanceof CapitalAccount) {
        		((CapitalAccount)category).removeEntry(entry);
        	}
        }
        
    	return found;
    }
    
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
    
	static public Object [] getDefaultProperties() {
		// TODO: Check if this is set to the current day.
		return new Object [] { new Date() };
	}
}
