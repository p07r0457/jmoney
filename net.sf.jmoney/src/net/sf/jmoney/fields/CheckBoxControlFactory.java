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
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * A control factory to edit boolean values using a check box.
 * 
 * @author Nigel Westbury
 */
public class CheckBoxControlFactory implements IPropertyControlFactory {

    public CheckBoxControlFactory() {
    }
    
    public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
  		return new CheckMarkEditor(parent, propertyAccessor);
    }

	public CellEditor createCellEditor(Table table) {
		return new CheckboxCellEditor(table);
	}

	public Object getValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
		// CheckboxCellEditor requires the value as a Boolean, so we can
		// get the value and return it as is.
        return extendableObject.getPropertyValue(propertyAccessor);
	}

	public void setValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object value) {
		// CheckboxCellEditor provides the value as a Boolean, so we can
		// set the value as is.
        extendableObject.setPropertyValue(propertyAccessor, extendableObject);
	}

    public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Boolean value = (Boolean) extendableObject.getPropertyValue(propertyAccessor);
        if (value == null) {
            return "N/A";
        } else {
            return value.toString();
        }
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Boolean value = (Boolean) extendableObject.getPropertyValue(propertyAccessor);
        if (value == null) {
            return "";
        } else {
            return value.toString();
        }
    }

	public boolean isEditable() {
		return true;
	}
}