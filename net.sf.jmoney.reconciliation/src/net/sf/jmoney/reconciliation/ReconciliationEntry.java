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
public class ReconciliationEntry extends AbstractEntryExtension {
	
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
	
	/**
	 * Entry is cleared.
	 */
	static String[] statusText = new String[] {
			ReconciliationPlugin.getResourceString("Entry.unclearedShort"),
			ReconciliationPlugin.getResourceString("Entry.reconcilingShort"),
			ReconciliationPlugin.getResourceString("Entry.clearedShort"),
	};
	
	protected int status = 0;
	
	/** Creates a new instance.
	 * A default constructor is mandatory for all extension objects.
	 */
	public ReconciliationEntry() {
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
		firePropertyChange("status", oldStatus, status);
	}
	
	/**
	 * Returns a short String representing the status.
	 */
	public String getStatusString() {
		return statusText[getStatus()];
	}
}
