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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;

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
			IObjectKey parentKey,
    		IListManager entries,
    		Date date) {
		super(objectKey, extensions, parentKey);

		this.entries = entries;
		this.date = date;
	}
	
	public Transaction(
			IObjectKey objectKey,
    		Map extensions,
			IObjectKey parentKey,
    		IListManager entries) {
		super(objectKey, extensions, parentKey);

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

    public Entry createEntry() {
    	return new EntryCollection(entries, this, TransactionInfo.getEntriesAccessor()).createEntry();
	}

    public EntryCollection getEntryCollection() {
    	return new EntryCollection(entries, this, TransactionInfo.getEntriesAccessor());
    }
    
    public boolean deleteEntry(Entry entry) {
    	return new EntryCollection(entries, this, TransactionInfo.getEntriesAccessor()).deleteEntry(entry);
    }
    
    // Some helper methods:
    
    public boolean hasTwoEntries() {
        return entries.size() == 2;
    }
    
    public boolean hasMoreThanTwoEntries() {
        return entries.size() > 2;
    }
    
    /**
     * Given an entry in the transaction, return the other entry.
     * <P>
     * If there are more than two entries in the transaction then null is returned.
     * If there is only one entry in the transaction then an
     * exception is throw.  If the given entry is not in the transaction then
     * an exception will be thrown.
     * 
     * @param thisEntry an entry in the transaction
     * @return the other entry in the transaction or null if more than one other entry
     * 				is in the transaction
     */
    public Entry getOther(Entry thisEntry) {
    	boolean thisEntryFound = false;
    	Entry anotherEntry = null;
        for (Iterator iter = entries.iterator(); iter.hasNext(); ) {
        	Entry entry = (Entry)iter.next();
        	if (!entry.equals(thisEntry)) {
                if (anotherEntry != null) {
                	// There is more than one entry other than the given entry
                	return null;
                }
                anotherEntry = entry;
        	} else {
        		thisEntryFound = true;
        	}
        }
        
        if (!thisEntryFound) {
        	throw new RuntimeException("Double entry error");
        }
        
        if (anotherEntry == null) {
        	throw new RuntimeException("Double entry error");
        }
        
        return anotherEntry;
    }
    
	static public Object [] getDefaultProperties() {
		// TODO: Check if this is set to the current day.
		return new Object [] { new Date() };
	}
	
	public class EntryCollection extends ObjectCollection {
		EntryCollection(IListManager listManager, ExtendableObject parent, PropertyAccessor listPropertyAccessor) {
			super(listManager, parent, listPropertyAccessor);
		}
		
	    /**
		 * @param entry
		 * @return
		 */
		public boolean deleteEntry(final Entry entry) {
	    	
	        boolean found = entries.remove(entry);

			if (found) {
				processObjectDeletion(TransactionInfo.getEntriesAccessor(), entry);

				// In addition to the generic object deletion event, we also fire an event
				// specifically for entry deletion.  The entryDeleted event is superfluous 
				// and it may be simpler if we removed it, so that listeners receive the generic
				// objectDeleted event only.
				getSession().fireEvent(
			            	new ISessionChangeFirer() {
			            		public void fire(SessionChangeListener listener) {
			            			listener.entryDeleted(entry);
			            		}
			           		});
			}

			return found;
		}

		public Entry createEntry() {
			final Entry newEntry = (Entry)entries.createNewElement(Transaction.this, EntryInfo.getPropertySet());

			processObjectAddition(TransactionInfo.getEntriesAccessor(), newEntry);
			
			getSession().fireEvent(
					new ISessionChangeFirer() {
						public void fire(SessionChangeListener listener) {
							listener.entryAdded(newEntry);
						}
					});
			
		    return newEntry;
		}

	    public ExtendableObject createNewElement(PropertySet actualPropertySet, Object values[]) {
			final Entry newEntry = (Entry)entries.createNewElement(Transaction.this, EntryInfo.getPropertySet(), values);

			processObjectAddition(TransactionInfo.getEntriesAccessor(), newEntry);
			
			getSession().fireEvent(
					new ISessionChangeFirer() {
						public void fire(SessionChangeListener listener) {
							listener.entryAdded(newEntry);
						}
					});
			
		    return newEntry;
		}

		
	}
}
