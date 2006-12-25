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

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.CapitalAccountInfo;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;

/**
 * The data model for an account.
 */
public abstract class CapitalAccount extends Account {

	protected IListManager<CapitalAccount> subAccounts;
	
	protected String abbreviation = null;

	protected String comment = null;

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
			Map<ExtensionPropertySet, Object[]> extensions, 
			IObjectKey parent,
			String name,
			IListManager<CapitalAccount> subAccounts,
			String abbreviation,
			String comment) {
		super(objectKey, extensions, parent, name);
		
		this.subAccounts = subAccounts;
        this.abbreviation = abbreviation;
        this.comment = comment;
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
			Map<ExtensionPropertySet, Object[]> extensions, 
			IObjectKey parent,
			IListManager<CapitalAccount> subAccounts) {
		super(objectKey, extensions, parent, JMoneyPlugin.getResourceString("Account.newAccount"));
		
		this.subAccounts = subAccounts;
        this.abbreviation = null;
        this.comment = null;
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

	public ObjectCollection<CapitalAccount> getSubAccountCollection() {
		return new ObjectCollection<CapitalAccount>(subAccounts, this, CapitalAccountInfo.getSubAccountAccessor());
	}

	/**
	 * Get the entries in this account sorted according to the given
	 * sort specification.  If the datastore plug-in has implemented
	 * the IEntryQueries interface then pass the request on to the
	 * datastore through the method of the same name in the IEntryQueries
	 * interface.  If the IEntryQueries interface has not been implemented
	 * by the datastore then evaluate ourselves.
	 * <P> 
	 * @return A collection containing the entries of this account.
	 * 				The entries are sorted using the given property and
	 * 				given sort order.  The collection is a read-only
	 * 				collection.
	 */
	public Collection<Entry> getSortedEntries(final ScalarPropertyAccessor<?> sortProperty, boolean descending) {
		IEntryQueries queries = (IEntryQueries)getSession().getAdapter(IEntryQueries.class);
    	if (queries != null) {
    		return queries.getSortedEntries(this, sortProperty, descending);
    	} else {
    		// IEntryQueries has not been implemented in the datastore.
    		// We must therefore provide our own implementation.
    		
    		List<Entry> sortedEntries = new LinkedList<Entry>(getEntries());
    		
    		Comparator<Entry> entryComparator;
    		if (sortProperty.getPropertySet() == EntryInfo.getPropertySet()) {
    			entryComparator = new Comparator<Entry>() {
    				public int compare(Entry entry1, Entry entry2) {
    					return sortProperty.getComparator().compare(entry1, entry2);
    				}
    			};
    		} else if (sortProperty.getPropertySet() == TransactionInfo.getPropertySet()) {
    			entryComparator = new Comparator<Entry>() {
    				public int compare(Entry entry1, Entry entry2) {
    					return sortProperty.getComparator().compare(entry1.getTransaction(), entry2.getTransaction());
    				}
    			};
    		} else if (sortProperty.getPropertySet() == AccountInfo.getPropertySet()) {
    			entryComparator = new Comparator<Entry>() {
    				public int compare(Entry entry1, Entry entry2) {
    					return sortProperty.getComparator().compare(entry1.getAccount(), entry2.getAccount());
    				}
    			};
    		} else {
    			throw new RuntimeException("given property cannot be used for entry sorting");
    		}
    		
    		if (descending) {
    			final Comparator<Entry> ascendingComparator = entryComparator;
    			entryComparator = new Comparator<Entry>() {
    				public int compare(Entry entry1, Entry entry2) {
    					return ascendingComparator.compare(entry2, entry1);
    				}
    			};
    		}
    		
    		Collections.sort(sortedEntries, entryComparator);
    		
    		return sortedEntries;
    	}
	};
	
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

	/**
	 * Create a sub-account of this account.  This method is
	 * identical to calling 
	 * <code>getSubAccountCollection().createNewElement(propertySet)</code>.
	 * 
	 * @param propertySet a property set derived (directly or
	 * 			indirectly) from the CapitalAccount property set.
	 * 			This property set must not be derivable and is
	 * 			the property set for the type of capital account
	 * 			to be created.
	 */
	public CapitalAccount createSubAccount(ExtendablePropertySet<? extends CapitalAccount> propertySet) {
		return getSubAccountCollection().createNewElement(propertySet);
	}
        
	/**
	 * Delete a sub-account of this account.  This method is
	 * identical to calling 
	 * <code>getSubAccountCollection().remove(subAccount)</code>.
	 */
	boolean deleteSubAccount(CapitalAccount subAccount) {
		return getSubAccountCollection().remove(subAccount);
	}

	/**
	 * @param date
	 * @param date2
	 * @param includeSubAccounts
	 * @return
	 */
	public long [] getEntryTotalsByMonth(int startYear, int startMonth, int numberOfMonths, boolean includeSubAccounts) {
		IEntryQueries queries = (IEntryQueries)getSession().getAdapter(IEntryQueries.class);
    	if (queries != null) {
    		return queries.getEntryTotalsByMonth(this, startYear, startMonth, numberOfMonths, includeSubAccounts);
    	} else {
    		// IEntryQueries has not been implemented in the datastore.
    		// We must therefore provide our own implementation.
    		
    		Vector<Entry> entriesList = new Vector<Entry>();
    		entriesList.addAll(getEntries());
    		if (includeSubAccounts) {
    			addEntriesFromSubAccounts(this, entriesList);
    		}
    		
            Collections.sort(entriesList, new Comparator<Entry>() {
                public int compare(Entry entry1, Entry entry2) {
                    return entry1.getTransaction().getDate().compareTo(
                            entry2.getTransaction().getDate());
                }
            });

    		
    		long [] totals = new long[numberOfMonths];

    		Calendar calendar = Calendar.getInstance();

    		
    		
    		// calculate the sum for each month
    		int year = startYear;
    		int month = startMonth; 
            for (int i=0; i<numberOfMonths; i++) {
    			calendar.clear();
    			calendar.setLenient(false);
    			calendar.set(year, month - 1, 1, 0, 0, 0);
    			Date startOfMonth = calendar.getTime();
            	// Date startOfMonth = new Date(year - 1900, month, 1);

    			month++;
            	if (month == 13) {
            		year++;
            		month = 1;
            	}

    			calendar.clear();
    			calendar.setLenient(false);
    			calendar.set(year, month - 1, 1, 0, 0, 0);
    			Date endOfMonth = calendar.getTime();
            	// Date endOfMonth = new Date(year - 1900, month, 1);
            	
            	int total = 0;
            	for (Entry entry: entriesList) {
            		if (entry.getTransaction().getDate().compareTo(startOfMonth) >= 0 
            		 && entry.getTransaction().getDate().compareTo(endOfMonth) < 0) {
            			total += entry.getAmount();
            		}
            	}
            	totals[i] = total;
            }
            
            return totals;
    	}
	}

	public void addEntriesFromSubAccounts(CapitalAccount a, Collection<Entry> entriesList) {
		for (CapitalAccount subAccount: a.getSubAccountCollection()) {
			entriesList.addAll(subAccount.getEntries());
			addEntriesFromSubAccounts(subAccount, entriesList);
		}
	}

}
