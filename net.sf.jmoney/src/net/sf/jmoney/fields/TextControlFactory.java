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

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.swt.widgets.Composite;

/**
 * A control factory to edit normal text.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class TextControlFactory implements IPropertyControlFactory<String> {

    public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<String> propertyAccessor, Session session) {
        return new TextEditor(parent, 0, propertyAccessor);
    }

   public String formatValueForMessage(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends String> propertyAccessor) {
        String value = extendableObject.getPropertyValue(propertyAccessor);
        if (value == null || value.length() == 0) {
            return "empty";
        } else {
            return "'" + value + "'";
        }
    }

    public String formatValueForTable(ExtendableObject extendableObject, ScalarPropertyAccessor<? extends String> propertyAccessor) {
        return extendableObject.getPropertyValue(propertyAccessor);
    }

	public boolean isEditable() {
		return true;
	}
}