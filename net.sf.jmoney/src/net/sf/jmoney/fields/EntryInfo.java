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

package net.sf.jmoney.fields;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CurrencyImpl;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryImpl;
import net.sf.jmoney.model2.IExtensionPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;

/**
 * @author Nigel
 *
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Entry properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 */
public class EntryInfo implements IExtensionPropertySetInfo {

    public EntryInfo() {
    }

	public Class getImplementationClass() {
		return EntryImpl.class;
	}
	
    public Class getInterfaceClass() {
        return Entry.class;
    }
    
	public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		// TODO: implement the SWT control factories here.
		// This must be done before we can convert the account entries panel
		// over to SWT with generalized property support.
		// For time being, we just pass null factories.
		propertyRegistrar.addProperty("check", JMoneyPlugin.getResourceString("Entry.check"), 8.0, null, null, null);
		propertyRegistrar.addProperty("description", JMoneyPlugin.getResourceString("Entry.description"), 30.0, null, null, null);
		propertyRegistrar.addProperty("account", JMoneyPlugin.getResourceString("Entry.category"), 30.0, null, null, null);
		propertyRegistrar.addProperty("valuta", JMoneyPlugin.getResourceString("Entry.valuta"), 10.0, null, null, null);
		propertyRegistrar.addProperty("memo", JMoneyPlugin.getResourceString("Entry.memo"), 30.0, null, null, null);
		propertyRegistrar.addProperty("amount", JMoneyPlugin.getResourceString("Entry.amount"), 10.0, null, null, null);
		propertyRegistrar.addProperty("creation", JMoneyPlugin.getResourceString("Entry.creation"), 10.0, null, null, null);
	}
}
