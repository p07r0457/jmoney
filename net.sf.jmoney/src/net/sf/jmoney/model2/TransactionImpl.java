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
public class TransactionImpl extends ExtendableObject implements Transaction {
    
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
        this.date = date;
    }
    
    public Iterator getEntryIterator() {
        return entries.iterator();
    }
    
    public Entry createEntry() {
		return (EntryImpl)entries.createNewElement(this, JMoneyPlugin.getEntryPropertySet());
	}

    public boolean deleteEntry(Entry entry) {
        boolean found = entries.remove(entry);

        // TODO: This is not correct.  Move it into the
        // above 'remove' method.
        if (found) {
        	// TODO: at some time, keep these lists for categories too
        	Account category = entry.getAccount();
        	if (category instanceof CapitalAccountImpl) {
        		((CapitalAccountImpl)category).removeEntry(entry);
        	}

    	getObjectKey().getSession().objectDeleted(entry);
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
