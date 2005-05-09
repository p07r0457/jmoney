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

import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Provides the metadata for the extra properties added to each
 * entry by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ReconciliationEntryInfo implements IPropertySetInfo {

	private static PropertySet propertySet = null;
	private static PropertyAccessor statusAccessor = null;
	private static PropertyAccessor statementAccessor = null;
	private static PropertyAccessor uniqueIdAccessor = null;
	
	public Class getImplementationClass() {
		return ReconciliationEntry.class;
	}
	
	public void registerProperties(PropertySet propertySet, IPropertyRegistrar propertyRegistrar) {
		ReconciliationEntryInfo.propertySet = propertySet;

		// TODO: tidy this up
		statusAccessor    = propertyRegistrar.addProperty("status", ReconciliationPlugin.getResourceString("Entry.statusShort"), 1, 20, new StatusControlFactory(), null);
		statementAccessor = propertyRegistrar.addProperty("statement", ReconciliationPlugin.getResourceString("Entry.statusShort"), 1, 20, /*new IntegerControlFactory()*/null, null);
		uniqueIdAccessor  = propertyRegistrar.addProperty("uniqueId", ReconciliationPlugin.getResourceString("Entry.statusShort"), 1, 20, /*new IntegerControlFactory()*/null, null);
	}

	/**
	 * @return
	 */
	public static PropertySet getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static PropertyAccessor getStatusAccessor() {
		return statusAccessor;
	}

	/**
	 * @return
	 */
	public static PropertyAccessor getStatementAccessor() {
		return statementAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getUniqueIdAccessor() {
		return uniqueIdAccessor;
	}
}
