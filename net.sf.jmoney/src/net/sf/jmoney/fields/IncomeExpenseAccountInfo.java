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
import net.sf.jmoney.model2.IExtensionPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountImpl;

/**
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
 * 
 * @author Nigel
 */
public class IncomeExpenseAccountInfo implements IExtensionPropertySetInfo {

    public IncomeExpenseAccountInfo() {
    }

	public Class getImplementationClass() {
		return IncomeExpenseAccountImpl.class;
	}
	
    public Class getInterfaceClass() {
        return IncomeExpenseAccount.class;
    }

    public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		propertyRegistrar.addPropertyList("subAccount", JMoneyPlugin.getResourceString("<not used???>"), IncomeExpenseAccount.class, null);

		// There are no scalar properties actually defined in this property set!
		
		// We should define something for the implied enumerated value
		// that is controlled by the derived class type.  This has no
		// been designed yet, so for time being we have nothing to do.
	}
}