/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002, 2004 Johann Gyger <jgyger@users.sf.net>
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

package net.sf.jmoney.ui.internal.pages.account.capital;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import net.sf.jmoney.Constants;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.ui.internal.JMoneyUIPlugin;

/**
 * TODO Move to non-UI plug-in
 * 
 * @author Johann Gyger
 */
public class EntriesFilter implements Constants {

	/**
	 * Get string representations of the available filter types.
	 * 
	 * @return Available filter types
	 */
    public static String[] filterTypes() {
		String[] filterType =
			{
				JMoneyUIPlugin.getResourceString("EntryFilter.entry"),
				JMoneyPlugin.getResourceString("Entry.amount"),
				JMoneyPlugin.getResourceString("Entry.category"),
				JMoneyPlugin.getResourceString("Entry.check"),
				JMoneyPlugin.getResourceString("Entry.date"),
				JMoneyPlugin.getResourceString("Entry.description"),
				JMoneyPlugin.getResourceString("Entry.memo"),
				JMoneyPlugin.getResourceString("Entry.valuta"),
			};
		return filterType;
	}

    /**
     * Filter pattern
     */
    protected String pattern = "";

    /**
     * Filter type
     */
    protected int type = 0;

    protected transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	/**
     * @return The filter pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Set the filter pattern.
     * TODO Maybe support regular expressions.
     * 
     * @param pattern Filter pattern
     */
    public void setPattern(String aPattern) {
        if (aPattern == null) aPattern = "";
        if (!aPattern.equals(pattern)) {
            pattern = aPattern;
            changeSupport.firePropertyChange("pattern", null, null);
        }
    }

	/**
     * Get the filter type which is equivalent to the array index returned by
     * <code>#getFilterType()</code>.
     * 
     * @return Filter type
     */
    public int getType() {
        return type;
    }

	/**
     * Set the filter type.
     * 
     * @param type Filter type
     */
    public void setType(int aType) {
        if (type == aType) return;
        type = aType;
        changeSupport.firePropertyChange("type", null, null);
    }

    /**
     * @return Is the filter pattern empty?
     */
    public boolean isEmpty() {
        return pattern.length() == 0;
    }

    /**
     * Filter the entry according to the provided filter criteria.
     * 
     * @param entry Entry to filter
     * @param account Account whose formatter is used to filter currency properties
     * @param dateFormat Formatter used to filter date properties
     * @param filterType Filter type to use for filtering
     * @return True, if "entry" matches the filter criteria; false, else
     */
	public boolean filterEntry(Entry entry, CurrencyAccount account, VerySimpleDateFormat dateFormat, int filterType) {
        switch (filterType) {
        case 0:
            return checkEntry(entry, account, dateFormat);
        case 1:
            return checkAmount(entry, account);
        case 2:
            return checkCategory(entry);
        case 3:
            return checkCheck(entry);
        case 4:
            return checkDate(entry, dateFormat);
        case 5:
            return checkDescription(entry);
        case 6:
            return checkMemo(entry);
        case 7:
            return checkValuta(entry, dateFormat);
        default:
            return true;
        }
    }

    public boolean checkEntry(Entry entry, CurrencyAccount account, VerySimpleDateFormat df) {
        return pattern.equals("")
            || checkAmount(entry, account)
			|| checkCategory(entry)
			|| checkCheck(entry)
			|| checkDate(entry, df)
			|| checkDescription(entry)
			|| checkMemo(entry)
			|| checkValuta(entry, df);
	}

	public boolean checkAmount(Entry e, CurrencyAccount account) {
        return contains(account.getCurrency().format(e.getAmount()));
    }

    public boolean checkCategory(Entry e) {
        return contains(e.getFullAccountName());
    }

    public boolean checkCheck(Entry e) {
        return contains(e.getCheck());
    }

    public boolean checkDate(Entry e, VerySimpleDateFormat df) {
        return contains(df.format(e.getTransaction().getDate()));
    }

    public boolean checkDescription(Entry e) {
        return contains(e.getDescription());
    }

    public boolean checkMemo(Entry e) {
        return contains(e.getMemo());
    }

    public boolean checkValuta(Entry e, VerySimpleDateFormat df) {
        return contains(df.format(e.getValuta()));
    }

    private boolean contains(String s) {
        if (isEmpty())
            return true;
        else
            return (s != null) && (s.indexOf(pattern) >= 0);
    }

	/**
	 * Add a PropertyChangeListener.
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.addPropertyChangeListener(pcl);
	}

	/**
	 * Remove a PropertyChangeListener.
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.removePropertyChangeListener(pcl);
	}

}
