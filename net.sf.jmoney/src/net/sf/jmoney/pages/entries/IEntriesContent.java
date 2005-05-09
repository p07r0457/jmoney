package net.sf.jmoney.pages.entries;

import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;

/**
 * Provides content to the entries table control.
 * 
 * @author Nigel Westbury
 */
public interface IEntriesContent {
	/**
	 * @return
	 */
	Vector getAllEntryDataObjects();

	/**
	 * @return
	 */
	IEntriesTableProperty getDebitColumnManager();

	/**
	 * @return
	 */
	IEntriesTableProperty getCreditColumnManager();

	/**
	 * @return
	 */
	IEntriesTableProperty getBalanceColumnManager();

	/**
	 * Get the list of entries to be shown in the table.
	 * <P>
	 * It may be that the table is configured to show the other
	 * entries in the same transaction as an entry in the table.
	 * Those other entries are not included in this collection.
	 * It is the responsibility of the table control to fetch those
	 * entries if it needs them.
	 * <P>
	 * If the entries are being filtered, those entries that are
	 * filtered out will still be included in this list.
	 */
	Collection getEntries();

	/**
	 * Determine if a given entry is included in the list.
	 * <P>
	 * This method must use the same rules as the getEntries()
	 * method.  If an entry would be in the collection returned
	 * by getEntries() then this method must return true for that
	 * entry, and if the entry would not be included in the collection
	 * returned by getEntries() then this method must return false. 
	 */
	boolean isEntryInTable(Entry entry);

	/**
	 * Determine if a given entry is included in the list.
	 * <P>
	 * This method is similar to the above method.  However, the
	 * given property in the Entry is assumed to have the value given
	 * by the <code>value</code> parameter, not the value contained
	 * in the Entry object.   determination
	 * is made gimust use the same rules as the getEntries()
	 * method.  If an entry would be in the collection returned
	 * by getEntries() then this method must return true for that
	 * entry, and if the entry would not be included in the collection
	 * returned by getEntries() then this method must return false. 
	 */
	boolean isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object value);

	/**
	 * @param transData
	 * @return
	 */
	boolean filterEntry(IDisplayableItem data);

	/**
	 * @return
	 */
	long getStartBalance();
}
