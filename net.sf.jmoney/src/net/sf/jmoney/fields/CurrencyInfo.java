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
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

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
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CurrencyInfo implements IPropertySetInfo {

	private static PropertySet propertySet = null;
	private static PropertyAccessor codeAccessor = null;
	private static PropertyAccessor decimalsAccessor = null;

	public CurrencyInfo() {
    }

	public Class getImplementationClass() {
		return Currency.class;
	}
	
	public void registerProperties(PropertySet propertySet, IPropertyRegistrar propertyRegistrar) {
		CurrencyInfo.propertySet = propertySet;
		IPropertyControlFactory textControlFactory = new TextControlFactory();
		
		codeAccessor = propertyRegistrar.addProperty("code", JMoneyPlugin.getResourceString("Currency.code"), 0, 8, textControlFactory, null);
		decimalsAccessor = propertyRegistrar.addProperty("decimals", JMoneyPlugin.getResourceString("Currency.decimals"), 0, 8, textControlFactory, null);
		
		propertyRegistrar.setObjectDescription("Currency");
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
	public static PropertyAccessor getCodeAccessor() {
		return codeAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getDecimalsAccessor() {
		return decimalsAccessor;
	}	
}
