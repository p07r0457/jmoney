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
import net.sf.jmoney.fields.CapitalAccountInfo;
import net.sf.jmoney.fields.EntryInfo;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;

/**
 * The data model for an account.
 */
public class CapitalAccount extends Account {

	protected String abbreviation = null;

	protected String comment = null;

        /**
         * This list is maintained for efficiency only.
         * The master list is the list of transactions, with each
         * transaction containing a list of entries.
         */
	protected Collection entries;

	/**
	 * The full constructor for a CapitalAccount object.  This constructor is called
	 * only be the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public CapitalAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			String name,
			IListManager subAccounts,
			String abbreviation,
			String comment) {
		super(objectKey, extensions, parent, name, subAccounts);
		
        this.abbreviation = abbreviation;
        this.comment = comment;
        
		this.entries = objectKey.createIndexValuesList(EntryInfo.getAccountAccessor());
	}

	/**
	 * The default constructor for a CapitalAccount object.  This constructor is called
	 * when a new CapitalAccount object is created.  The properties are set to default
	 * values.  The list properties are set to empty lists.  The parameter list for this
	 * constructor is the same as the full constructor except that there are no parameters
	 * for the scalar properties.
	 */
	public CapitalAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			IListManager subAccounts) {
		super(objectKey, extensions, parent, JMoneyPlugin.getResourceString("Account.newAccount"), subAccounts);
		
        this.abbreviation = null;
        this.comment = null;
        
		this.entries = objectKey.createIndexValuesList(EntryInfo.getAccountAccessor());
	}

	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.capitalAccount";
	}
	
	/**
	 * @return the abbrevation of this account.
	 */
	public String getAbbreviation() {
		return abbreviation;
	};

	/**
	 * @return the comment of this account.
	 */
	public String getComment() {
		return comment;
	};

	/**
	 * @return An iterator that returns the entries of this account.
	 */
	public Iterator getEntriesIterator(Session session) {
		return entries.iterator();
	};
	
	/**
	 * @return true if there are any entries in this account,
	 * 			false if no entries are in this account
	 */
	public boolean hasEntries() {
		return entries.size() != 0;
	}
	
	// These methods are used when maintaining the list
	// of entries in each account.
	// TODO: remove these methods when indexes are supported.
	
	public void addEntry(Entry entry) {
		entries.add(entry);
	}

	void removeEntry(Entry entry) {
		entries.remove(entry);
	}

	/**
	 * @param anAbbrevation the abbrevation of this account.
	 */
	
	public void setAbbreviation(String anAbbreviation) {
        String oldAbbreviation = this.abbreviation;
        this.abbreviation = anAbbreviation;

		// Notify the change manager.
        processPropertyChange(CapitalAccountInfo.getAbbreviationAccessor(), oldAbbreviation, anAbbreviation);
	}

	/**
	 * @param aComment the comment of this account.
	 */
	
	public void setComment(String aComment) {
        String oldComment = this.comment;
        this.comment = aComment;

		// Notify the change manager.
        processPropertyChange(CapitalAccountInfo.getCommentAccessor(), oldComment, aComment);
	}

	/**
	 * Sort the entries.
	 */
        // TODO: Sorting the entries affects the view of the data.
        // This sort should be done in the view, not the model, otherwise
        // one view might mess up another view of the data.
        // This must be reviewed if we support SQL databases in any case.
/*	
	public void sortEntries(Comparator c) {
		Collections.sort(entries, c);
	}
*/
	public String toString() {
		return name;
	}

	public String getFullAccountName() {
	    if (getParent() == null) {
		       return name;
		    } else {
		        return getParent().getFullAccountName() + "." + this.name;
		    }
	}

	public int compareTo(Object o) {
		CapitalAccount a = (CapitalAccount) o;
		return getName().compareTo(a.getName());
	}

	/**
	 * This version is required by the JMoney framework.
	 * 
	 * @param propertySet a property set derived (directly or
	 * 			indirectly) from the CapitalAccount property set.
	 * 			This property set must not be derivable and is
	 * 			the property set for the type of capital account
	 * 			to be created.
	 * @return
	 */
	public CapitalAccount createSubAccount(PropertySet propertySet) {
		final CapitalAccount newSubAccount = (CapitalAccount)subAccounts.createNewElement(
				this, 
				propertySet); 

		processObjectAddition(CapitalAccountInfo.getSubAccountAccessor(), newSubAccount);
		
		// In addition to the generic object creation event, we also fire an event
		// specifically for account creation.  The accountAdded event is superfluous 
		// and it may be simpler if we removed it, so that listeners receive the generic
		// objectAdded event only.
		getSession().fireEvent(
				new ISessionChangeFirer() {
					public void fire(SessionChangeListener listener) {
						listener.accountAdded(newSubAccount);
					}
				});
		
		return newSubAccount;
	}
        
	/**
	 * This version is required by the JMoney framework.
	 */
	boolean deleteSubAccount(final CapitalAccount subAccount) {
		boolean found = subAccounts.remove(subAccount);
		if (found) {
			processObjectDeletion(CapitalAccountInfo.getSubAccountAccessor(), subAccount);
			
			// In addition to the generic object deletion event, we also fire an event
			// specifically for account deletion.  The accountDeleted event is superfluous 
			// and it may be simpler if we removed it, so that listeners receive the generic
			// objectDeleted event only.
			getSession().fireEvent(
					new ISessionChangeFirer() {
						public void fire(SessionChangeListener listener) {
							listener.accountDeleted(subAccount);
						}
					});
		}
		return found;
	}
	
	static public Object [] getDefaultProperties() {
		return new Object [] { "new account", null, null, null, new Long(0), null, null, null };
	}
	
}
