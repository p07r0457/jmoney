/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.reconciliation;

import net.sf.jmoney.model2.*;

/**
 *
 * @author  Nigel
 */
public class ReconciliationEntry extends EntryExtension {
	
	/**
	 * Entry is uncleared.
	 */
	public static final int UNCLEARED = 0;
	
	/**
	 * Entry is reconciling.
	 */
	public static final int RECONCILING = 1;
	
	/**
	 * Entry is cleared.
	 */
	public static final int CLEARED = 2;
	
	protected int status = 0;
	
	/**
	 * A default constructor is mandatory for all extension objects.
	 * The default constructor sets the extension properties to
	 * appropriate default values.
	 */
	public ReconciliationEntry() {
	}
	
	/**
	 * A Full constructor is mandatory for all extension objects.
	 * This constructor is called by the datastore to construct
	 * the extension objects when loading data.
	 * 
	 */
	public ReconciliationEntry(int status) {
		this.status = status;
	}
	
	/**
	 * Returns the status.
	 */
	public int getStatus() {
		return status;
	}
	
	/**
	 * Sets the check. Either UNCLEARED, RECONCILING or CLEARED.
	 *
	 * Fire a property change event.  Note that listeners cannot listen for changes to a
	 * single entry.  Listeners must be added to the appropriate PropertyAccessor
	 * object and will recieve notification when the property in any entry has changed.
	 */
	public void setStatus(int status) {
		int oldStatus = this.status;
		this.status = status;
		processPropertyChange(ReconciliationEntryInfo.getStatusAccessor(), new Integer(oldStatus), new Integer(status));
	}
	
	static public Object [] getDefaultProperties() {
		return new Object [] { new Integer(UNCLEARED) };
	}
}
