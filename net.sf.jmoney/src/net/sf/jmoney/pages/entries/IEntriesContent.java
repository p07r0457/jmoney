package net.sf.jmoney.pages.entries;

import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.model2.Entry;

/**
 * Provides content to the entries table control.
 * 
 * @author Nigel Westbury
 */
public interface IEntriesContent {
	/**
	 * @return
	 */
	Vector<IEntriesTableProperty> getAllEntryDataObjects();

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
	Collection<Entry> getEntries();

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
	 * @param transData
	 * @return
	 */
	boolean filterEntry(IDisplayableItem data);

	/**
	 * @return
	 */
	long getStartBalance();

	/**
	 * When a new entry is created in a list of entries, certain
	 * properties must be set in the entry to ensure that the new
	 * entry is included in the list.  For example, if the list
	 * contains all entries that appear on a given statement of
	 * a given account, then when the user creates a new entry in
	 * that list, the account and statement must be set in the entry.
	 *
	 * @param newEntry
	 */
	void setNewEntryProperties(Entry newEntry);
}
