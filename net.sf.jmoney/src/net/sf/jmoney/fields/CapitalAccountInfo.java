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
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the CapitalAccount properties.  By registering
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
public class CapitalAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<CapitalAccount> propertySet = PropertySet.addDerivedPropertySet(CapitalAccount.class, AccountInfo.getPropertySet());
	private static ListPropertyAccessor<CapitalAccount> subAccountAccessor = null;
	private static ScalarPropertyAccessor<String> abbreviationAccessor = null;
	private static ScalarPropertyAccessor<String> commentAccessor = null;

    public PropertySet registerProperties() {
		IPropertyControlFactory<String> textControlFactory = new TextControlFactory();
		IPropertyControlFactory<String> commentControlFactory = new MultiTextControlFactory();
		
		subAccountAccessor = propertySet.addPropertyList("subAccount", JMoneyPlugin.getResourceString("<not used???>"), CapitalAccountInfo.getPropertySet(), null);
		abbreviationAccessor = propertySet.addProperty("abbreviation", JMoneyPlugin.getResourceString("AccountPropertiesPanel.abbrevation"), String.class, 5, 70, textControlFactory, null);
		commentAccessor      = propertySet.addProperty("comment", JMoneyPlugin.getResourceString("AccountPropertiesPanel.comment"), String.class, 5, 150, commentControlFactory, null);
		
		propertySet.setIcon("icons/account.gif");
		propertySet.setDerivable();
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<CapitalAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<CapitalAccount> getSubAccountAccessor() {
		return subAccountAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getAbbreviationAccessor() {
		return abbreviationAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getCommentAccessor() {
		return commentAccessor;
	}	
}
