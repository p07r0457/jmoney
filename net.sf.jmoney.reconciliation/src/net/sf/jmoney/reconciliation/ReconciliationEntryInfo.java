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

import net.sf.jmoney.model2.IExtensionPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ReconciliationEntryInfo implements IExtensionPropertySetInfo {

    public ReconciliationEntryInfo() {
    }

    public Class getInterfaceClass() {
        return ReconciliationEntry.class;
    }
    
    public Class getMutableInterfaceClass() {
        return ReconciliationEntry.class;
    }
    
	public void registerProperties(IPropertyRegistrar extensionRegistrar) {
		extensionRegistrar.addProperty("status", ReconciliationPlugin.getResourceString("Entry.statusShort"), 2.0, null, StatusEditor.class, null);
	}

}
