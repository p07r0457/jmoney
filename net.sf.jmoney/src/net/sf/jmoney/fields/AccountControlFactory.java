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

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.widgets.Composite;

/**
 * A control factory to select an account.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class AccountControlFactory implements IPropertyControlFactory {
    
    public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor, Session session) {
        return new AccountEditor(parent, propertyAccessor, session);
    }

	public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Account value = (Account) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "<none>" : value.getFullAccountName();
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Account value = (Account) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "" : value.getFullAccountName();
    }

	public boolean isEditable() {
		return true;
	}
}